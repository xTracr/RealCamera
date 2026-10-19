package com.xtracr.realcamera;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.api.BindingContext;
import com.xtracr.realcamera.api.BindingResult;
import com.xtracr.realcamera.api.RealCameraAPI;
import com.xtracr.realcamera.compat.DisableHelper;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.binding.OffsetConfig;
import com.xtracr.realcamera.config.binding.RenderConfig;
import com.xtracr.realcamera.renderer.IdentifiedVertexBuffer;
import com.xtracr.realcamera.renderer.MultiVertexCatcher;
import com.xtracr.realcamera.renderer.RoutingSubmitCollector;
import com.xtracr.realcamera.renderer.state.VertexData;
import com.xtracr.realcamera.util.LocUtil;
import com.xtracr.realcamera.util.LookupContext;
import com.xtracr.realcamera.util.MathUtil;
import com.xtracr.realcamera.util.ReferenceFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class RealCameraCore {
    private static final MultiVertexCatcher vertexCatcher = MultiVertexCatcher.create();
    private static ReferenceFrame smoothedCameraState = ReferenceFrame.IDENTITY;
    private static ReferenceFrame lastCameraState = ReferenceFrame.IDENTITY;
    @Nullable
    private static Identifier currentTarget = null;
    private static boolean active = false, rendering = false;
    private static int failureFrames = 0;

    public static boolean isActive() {
        return active;
    }

    public static boolean isRendering() {
        return isActive() && rendering;
    }

    public static @Nullable Identifier currentTarget() {
        return currentTarget;
    }

    public static @Nullable OffsetConfig currentOffsets() {
        return currentTarget == null ? null : ConfigFile.config().binding.offsetConfigs.get(currentTarget);
    }

    public static void initialize(Minecraft client, boolean advanceGameTime) {
        Entity entity = client.getCameraEntity();
        active = advanceGameTime && ConfigFile.config().enabled && client.options.getCameraType().isFirstPerson() && entity != null && !DisableHelper.MAIN_FEATURE.disabled(entity);
        rendering = ConfigFile.config().renderModel && !DisableHelper.RENDER_MODEL.disabled(entity);
    }

    public static void register() {
        // Ordinary UV capture is the fallback after external and YSM approaches.
        RealCameraAPI.register(RealCamera.MOD_ID, Integer.MIN_VALUE, RealCameraCore::computeResult);
    }

    public static void reset() {
        active = false;
        lastCameraState = ReferenceFrame.IDENTITY;
        smoothedCameraState = ReferenceFrame.IDENTITY;
        currentTarget = null;
        failureFrames = 0;
    }

    public static Vec3 getRawPos(Vec3 cameraPos, Vec3 entityPos) {
        OffsetConfig offsets = currentOffsets();
        if (offsets == null) return cameraPos;
        Vec3 rawPos = smoothedCameraState.origin().add(entityPos);
        return new Vec3(offsets.bindX ? rawPos.x() : cameraPos.x(), offsets.bindY ? rawPos.y() : cameraPos.y(), offsets.bindZ ? rawPos.z() : cameraPos.z());
    }

    public static Vec3 getEulerAngle(float pitch, float yaw, float roll) {
        OffsetConfig offsets = currentOffsets();
        if (offsets == null || !offsets.bindRotation) return new Vec3(pitch, yaw, roll);
        double scale = Math.toDegrees(1);
        return MathUtil.getEulerAngleYXZ(smoothedCameraState.toRotationMatrix(new Matrix3f())).multiply(scale, -scale, scale);
    }

    public static void computeCamera(Minecraft client, float partialTicks) {
        Entity entity = client.getCameraEntity();
        boolean invisible = entity.isInvisible();
        entity.setInvisible(false);
        BindingResult result;
        try {
            result = RealCameraAPI.computeResult(client, partialTicks);
        } finally {
            entity.setInvisible(invisible);
        }
        if (result != null) {
            ReferenceFrame camera = ConfigFile.config().binding.offsets(result.id()).applyTo(result.referenceFrame(), false);
            ConfigFile.config().binding.renderConfig(result.id());
            lastCameraState = camera;
            currentTarget = result.id();
            failureFrames = 0;
        } else {
            failureFrames++;
            Player player = client.player;
            int retentionFrames = ConfigFile.config().binding.bindResultRetentionFrames;
            if (!ConfigFile.config().binding.hideFailureMessage && failureFrames == retentionFrames + 1 && player != null) {
                player.sendSystemMessage(LocUtil.MESSAGE("bindingFailed", LocUtil.MOD_NAME(), LocUtil.MODEL_VIEW_TITLE(), KeyMappings.MODEL_VIEW_SCREEN.getTranslatedKeyMessage()));
            }
            if (currentTarget == null || failureFrames > retentionFrames) {
                currentTarget = null;
                lastCameraState = ReferenceFrame.IDENTITY;
                active = false;
                return;
            }
        }
        smoothedCameraState = smoothedCameraState.interpolate(lastCameraState,
                1 - ConfigFile.config().binding.displacementSmoothFactor,
                1 - ConfigFile.config().binding.rotationSmoothFactor);
    }

    public static void renderCameraEntity(Minecraft client, float partialTicks, SubmitNodeCollector submitNodeCollector, Matrix4f modelView) {
        if (currentTarget == null) return;
        RenderConfig renderConfig = ConfigFile.config().binding.renderConfig(currentTarget);
        Matrix4f invertedCameraPose = new Matrix4f(lastCameraState.toRotationMatrix(new Matrix3f()))
                .scale(-1f, 1f, -1f)
                .invert()
                .translate(Vec3.ZERO.subtract(lastCameraState.origin()).toVector3f());
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(modelView.invert(new Matrix4f()));
        poseStack.mulPose(invertedCameraPose);
        Entity entity = client.getCameraEntity();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        SubmitNodeCollector collector = new RoutingSubmitCollector(submitNodeCollector, vertexCatcher.initCollector());
        dispatcher.submit(dispatcher.extractEntity(entity, partialTicks), new CameraRenderState(), 0, 0, 0, poseStack, collector);
        final float m02 = modelView.m02(), m12 = modelView.m12(), m22 = modelView.m22(), m32 = modelView.m32();
        final float depth = renderConfig.disablingDepth;
        vertexCatcher.collectBuffers().forEach(builtBuffer -> {
            RenderConfig.DisableCache disables = renderConfig.disableCache(builtBuffer.textureId());
            if (disables.disableAll()) return;
            submitNodeCollector.submitCustomGeometry(poseStack, builtBuffer.renderType(), (_, buffer) -> {
                if (!builtBuffer.renderType().canConsolidateConsecutiveGeometry()) {
                    for (VertexData vertex : builtBuffer) vertex.render(buffer);
                    return;
                }
                builtBuffer.primitiveStream().forEach(primitive -> {
                    for (VertexData vertex : primitive) {
                        if (Math.fma(m02, vertex.x(), Math.fma(m12, vertex.y(), Math.fma(m22, vertex.z(), m32))) > -depth)
                            continue;
                        if (disables.disable(vertex.u(), vertex.v())) continue;
                        for (VertexData vertexData : primitive) vertexData.render(buffer);
                        break;
                    }
                });
            });
        });
    }

    private static BindingResult computeResult(BindingContext context) {
        Minecraft client = context.client();
        Entity entity = client.getCameraEntity();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        dispatcher.submit(dispatcher.extractEntity(entity, context.partialTicks()), new CameraRenderState(), 0, 0, 0, new PoseStack(), vertexCatcher.initCollector());
        for (IdentifiedVertexBuffer buffer : vertexCatcher.collectBuffers()) {
            for (var target : context.targets(buffer.textureId())) {
                LookupContext lookup = new LookupContext(target.config());
                lookup.find(buffer, LookupContext.LookupMode.CACHE_THEN_SCAN);
                ReferenceFrame frame = lookup.tryBuild();
                if (frame != null) return context.result(target.path(), frame);
            }
        }
        return null;
    }
}
