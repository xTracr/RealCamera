package com.xtracr.realcamera;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtracr.realcamera.api.BindResult;
import com.xtracr.realcamera.api.RealCameraAPI;
import com.xtracr.realcamera.compat.DisableHelper;
import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.BindTarget.DisableConfig;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class RealCameraCore {
    private static BindResult lastResult = BindResult.EMPTY, newResult = BindResult.EMPTY;
    private static Vec3 cameraPos = Vec3.ZERO, eulerAngle = Vec3.ZERO;
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

    public static void initialize(Minecraft client) {
        Entity entity = client.getCameraEntity();
        active = ConfigFile.config().enabled() && client.options.getCameraType().isFirstPerson() && entity != null && !DisableHelper.MAIN_FEATURE.disabled(entity);
        rendering = ConfigFile.config().renderModel() && !DisableHelper.RENDER_MODEL.disabled(entity);
    }

    public static void reset() {
        cameraPos = eulerAngle = Vec3.ZERO;
        failureFrames = 0;
    }

    public static float getPitch(float f) {
        if (currentTarget().bindConfig().bindRotation()) return (float) eulerAngle.x();
        return f;
    }

    public static float getYaw(float f) {
        if (currentTarget().bindConfig().bindRotation()) return (float) -eulerAngle.y();
        return f;
    }

    public static float getRoll(float f) {
        if (ConfigFile.config().isClassic()) return f + ConfigFile.config().getClassicRoll();
        if (currentTarget().bindConfig().bindRotation()) return (float) eulerAngle.z();
        return f;
    }

    public static Vec3 getRawPos(Vec3 cameraPos, Vec3 entityPos) {
        Vec3 rawPos = SmoothUtil.smoothPosition(lastResult.getPosition()).add(entityPos);
        BindTarget.BindConfig bindConfig = currentTarget().bindConfig();
        return new Vec3(bindConfig.bindX() ? rawPos.x() : cameraPos.x(), bindConfig.bindY() ? rawPos.y() : cameraPos.y(), bindConfig.bindZ() ? rawPos.z() : cameraPos.z());
    }

    public static Vec3 getCameraPos(Vec3 vec) {
        BindTarget.BindConfig bindConfig = currentTarget().bindConfig();
        return new Vec3(bindConfig.bindX() ? cameraPos.x() : vec.x(), bindConfig.bindY() ? cameraPos.y() : vec.y(), bindConfig.bindZ() ? cameraPos.z() : vec.z());
    }

    public static void setCameraPos(Vec3 vec) {
        cameraPos = vec;
    }

    public static void computeCamera(Minecraft client, float deltaTick) {
        Entity entity = client.getCameraEntity();
        boolean invisible = entity.isInvisible();
        entity.setInvisible(false);
        newResult = RealCameraAPI.computeBindResult(client, deltaTick);
        if (!newResult.available()) {
            EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
            MultiVertexCatcher catcher = MultiVertexCatcher.defaultImpl();
            dispatcher.render(entity, 0, 0, 0, Mth.lerp(deltaTick, entity.yRotO, entity.getYRot()), deltaTick, new PoseStack(), catcher, dispatcher.getPackedLightCoords(entity, deltaTick));
            catcher.endCatching(RealCameraCore::computeBindResult);
        }
        entity.setInvisible(invisible);
        if (newResult.available()) {
            failureFrames = 0;
            lastResult = newResult.computeCamera();
        } else {
            failureFrames++;
            Entity player = client.player;
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
        eulerAngle = MathUtil.getEulerAngleYXZ(SmoothUtil.smoothRotation(lastResult.getRotation())).scale(Math.toDegrees(1));
    }

    public static void renderCameraEntity(Minecraft client, float deltaTick, MultiBufferSource bufferSource, Matrix4f modelView) {
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
        dispatcher.render(entity, 0, 0, 0, Mth.lerp(deltaTick, entity.yRotO, entity.getYRot()), deltaTick, poseStack, catcher, dispatcher.getPackedLightCoords(entity, deltaTick));
        final float m02 = modelView.m02(), m12 = modelView.m12(), m22 = modelView.m22(), m32 = modelView.m32();
        final float depth = currentTarget().disablingDepth();
        catcher.endCatching(builtBuffer -> {
            DisableConfig[] disableConfigs = currentTarget().filteredDisableConfigs(config -> builtBuffer.textureId().contains(config.textureId()));
            for (DisableConfig config : disableConfigs) {
                if (config.disableAll()) return;
            }
            VertexConsumer buffer = bufferSource.getBuffer(builtBuffer.renderType());
            if (!builtBuffer.renderType().canConsolidateConsecutiveGeometry()) {
                for (VertexData vertex : builtBuffer.vertexBuffer()) vertex.render(buffer);
                return;
            }
            builtBuffer.vertexBuffer().primitiveStream().forEach(primitive -> {
                primitiveFor:
                for (VertexData vertex : primitive) {
                    if (Math.fma(m02, vertex.x(), Math.fma(m12, vertex.y(), Math.fma(m22, vertex.z(), m32))) > -depth) continue;
                    for (DisableConfig config : disableConfigs) {
                        if (config.test(vertex)) continue primitiveFor;
                    }
                    for (VertexData vertexData : primitive) vertexData.render(buffer);
                    break;
                }
            });
        });
    }

    private static void computeBindResult(BuiltIterableBuffer builtBuffer) {
        if (newResult.available()) return;
        targetFor:
        for (BindTarget target : ConfigFile.config().getBindTargetList()) {
            BindResult result = new BindResult(target, false);
            if (!builtBuffer.textureId().contains(result.target.textureId())) continue;
            BindTarget.TargetConfig config = result.target.targetConfig();
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
