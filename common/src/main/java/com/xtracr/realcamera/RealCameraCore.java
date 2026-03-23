package com.xtracr.realcamera;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.api.BindResult;
import com.xtracr.realcamera.api.RealCameraAPI;
import com.xtracr.realcamera.compat.DisableHelper;
import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.BindTarget.DisableConfig;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.renderer.BuiltIterableBuffer;
import com.xtracr.realcamera.renderer.MultiVertexCatcher;
import com.xtracr.realcamera.renderer.VertexData;
import com.xtracr.realcamera.util.CameraTransform;
import com.xtracr.realcamera.util.LocUtil;
import com.xtracr.realcamera.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RealCameraCore {
    private static final CameraTransform smoothedCamera = new CameraTransform();
    private static BindResult lastResult = BindResult.EMPTY, newResult = BindResult.EMPTY;
    private static boolean active = false, rendering = false;
    private static int failureFrames = 0;

    public static boolean isActive() {
        return active;
    }

    public static boolean isRendering() {
        return isActive() && rendering;
    }

    public static BindTarget currentTarget() {
        return lastResult.target;
    }

    public static void initialize(Minecraft client, boolean renderLevel) {
        Entity entity = client.getCameraEntity();
        active = renderLevel && ConfigFile.config().enabled() && client.options.getCameraType().isFirstPerson() && entity != null && !DisableHelper.MAIN_FEATURE.disabled(entity);
        rendering = ConfigFile.config().renderModel() && !DisableHelper.RENDER_MODEL.disabled(entity);
    }

    public static void reset() {
        smoothedCamera.setPosition(Vec3.ZERO);
        smoothedCamera.setRotation(new Matrix3f());
        failureFrames = 0;
    }

    public static Vec3 getRawPos(Vec3 cameraPos, Vec3 entityPos) {
        Vec3 rawPos = smoothedCamera.getPosition().add(entityPos);
        BindTarget.BindConfig bindConfig = currentTarget().bindConfig();
        return new Vec3(bindConfig.bindX() ? rawPos.x() : cameraPos.x(), bindConfig.bindY() ? rawPos.y() : cameraPos.y(), bindConfig.bindZ() ? rawPos.z() : cameraPos.z());
    }

    public static Vec3 getEulerAngle(float pitch, float yaw, float roll) {
        if (!currentTarget().bindConfig().bindRotation()) return new Vec3(pitch, yaw, roll);
        return MathUtil.getEulerAngleYXZ(smoothedCamera.getRotation()).scale(Math.toDegrees(1));
    }

    public static void computeCamera(Minecraft client, float partialTicks) {
        Entity entity = client.getCameraEntity();
        boolean invisible = entity.isInvisible();
        entity.setInvisible(false);
        newResult = RealCameraAPI.computeBindResult(client, partialTicks);
        if (!newResult.available()) {
            EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
            MultiVertexCatcher catcher = MultiVertexCatcher.defaultImpl();
            SubmitNodeStorage storage = new SubmitNodeStorage();
            dispatcher.submit(dispatcher.extractEntity(entity, partialTicks), new CameraRenderState(), 0, 0, 0, new PoseStack(), storage);
            catcher.renderTranslucentFeatures(storage);
            storage.clear();
            catcher.endCatching(RealCameraCore::computeBindResult);
        }
        entity.setInvisible(invisible);
        if (newResult.available()) {
            failureFrames = 0;
            lastResult = newResult.computeCamera(false);
        } else {
            failureFrames++;
            Player player = client.player;
            int retentionFrames = ConfigFile.config().getBindResultRetentionFrames();
            if (!ConfigFile.config().hideBindingFailureMessage() && failureFrames == retentionFrames + 1 && player != null) {
                player.sendSystemMessage(LocUtil.MESSAGE("bindingFailed", LocUtil.MOD_NAME(), LocUtil.MODEL_VIEW_TITLE(), KeyMappings.MODEL_VIEW_SCREEN.getTranslatedKeyMessage()));
            }
            if (!lastResult.available() || failureFrames > retentionFrames) {
                lastResult = BindResult.EMPTY;
                active = false;
                return;
            }
        }
        smoothedCamera.lerpPosition(lastResult.getPosition(), 1 - ConfigFile.config().getDisplacementSmoothFactor());
        smoothedCamera.slerpRotation(lastResult.getRotation(), 1 - ConfigFile.config().getRotationSmoothFactor());
    }

    public static void renderCameraEntity(Minecraft client, float partialTicks, SubmitNodeCollector submitNodeCollector, Matrix4f modelView) {
        Vec3 targetEulerAngle = MathUtil.getEulerAngleYXZ(lastResult.getRotation());
        Matrix4f invertedCameraPose = new Matrix4f()
                .rotateZ((float) targetEulerAngle.z())
                .rotateX((float) targetEulerAngle.x())
                .rotateY((float) (Math.PI - targetEulerAngle.y()))
                .transpose()
                .invert()
                .translate(Vec3.ZERO.subtract(lastResult.getPosition()).toVector3f());
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(new Matrix4f(invertedCameraPose).mulLocal(modelView.invert(new Matrix4f())));
        Entity entity = client.getCameraEntity();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        MultiVertexCatcher catcher = MultiVertexCatcher.defaultImpl();
        SubmitNodeStorage storage = new SubmitNodeStorage();
        dispatcher.submit(dispatcher.extractEntity(entity, partialTicks), new CameraRenderState(), 0, 0, 0, poseStack, storage);
        catcher.renderTranslucentFeatures(storage);
        storage.clear();
        final float m02 = modelView.m02(), m12 = modelView.m12(), m22 = modelView.m22(), m32 = modelView.m32();
        final float depth = currentTarget().disablingDepth();
        dispatcher.preventBuilding();
        catcher.endCatching(builtBuffer -> {
            DisableConfig[] disableConfigs = currentTarget().filteredDisableConfigs(config -> builtBuffer.textureId().contains(config.textureId()));
            for (DisableConfig config : disableConfigs) if (config.disableAll()) return;
            submitNodeCollector.submitCustomGeometry(poseStack, builtBuffer.renderType(), (_, buffer) -> {
                if (!builtBuffer.renderType().canConsolidateConsecutiveGeometry()) {
                    for (VertexData vertex : builtBuffer.vertexBuffer()) vertex.render(buffer);
                    return;
                }
                builtBuffer.vertexBuffer().primitiveStream().forEach(primitive -> {
                    primitiveFor:
                    for (VertexData vertex : primitive) {
                        if (Math.fma(m02, vertex.x(), Math.fma(m12, vertex.y(), Math.fma(m22, vertex.z(), m32))) > -depth) continue;
                        for (DisableConfig config : disableConfigs) {
                            if (config.disable(vertex)) continue primitiveFor;
                        }
                        for (VertexData vertexData : primitive) vertexData.render(buffer);
                        break;
                    }
                });
            });
        });
    }

    private static void computeBindResult(BuiltIterableBuffer builtBuffer) {
        if (newResult.available()) return;
        targetFor:
        for (BindTarget target : ConfigFile.config().getBindTargetList(builtBuffer.textureId())) {
            BindResult result = new BindResult(target);
            BindTarget.TargetConfig config = target.targetConfig();
            VertexData.UV[] uvs = {new VertexData.UV(config.posU(), config.posV()), new VertexData.UV(config.forwardU(), config.forwardV()), new VertexData.UV(config.upwardU(), config.upwardV())};
            VertexData[][] primitives = builtBuffer.findPrimitivesInCache(uvs);
            if (builtBuffer.anyNotCached(uvs)) {
                for (int i = 0; i < primitives.length; i++) {
                    if (primitives[i] != null) uvs[i] = null;
                }
                VertexData[][] newPrimitives = builtBuffer.findPrimitives(uvs);
                for (int i = 0; i < primitives.length; i++) {
                    if (newPrimitives[i] == null && primitives[i] == null) continue targetFor;
                    else if (newPrimitives[i] != null) primitives[i] = newPrimitives[i];
                }
            }
            if (primitives[0] != null) result.setPosition(VertexData.position(primitives[0], config.posU(), config.posV()));
            if (primitives[1] != null) result.setForward(VertexData.normal(primitives[1]));
            if (primitives[2] != null) result.setUpward(VertexData.normal(primitives[2]));
            if (!result.available()) continue;
            newResult = result;
            return;
        }
    }
}