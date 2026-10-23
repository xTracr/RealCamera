package com.xtracr.realcamera.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.api.BindingContext;
import com.xtracr.realcamera.api.BindingResult;
import com.xtracr.realcamera.api.RealCameraAPI;
import com.xtracr.realcamera.renderer.MultiVertexCatcher;
import com.xtracr.realcamera.util.LookupContext;
import com.xtracr.realcamera.util.LookupContext.LookupMode;
import com.xtracr.realcamera.util.ReferenceFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class YSMCompat {
    private static final TransformedVertexRecorder[] TRANSFORMED_RECORDERS = new TransformedVertexRecorder[4];

    static {
        final float pitch = 1.9106332f, yaw = 2.0943951f;
        TRANSFORMED_RECORDERS[0] = new TransformedVertexRecorder();
        TRANSFORMED_RECORDERS[1] = new TransformedVertexRecorder().setRotation(pitch, 0);
        TRANSFORMED_RECORDERS[2] = new TransformedVertexRecorder().setRotation(pitch, yaw);
        TRANSFORMED_RECORDERS[3] = new TransformedVertexRecorder().setRotation(pitch, 2 * yaw);
    }

    public static void register() {
        // This workaround solves the same UV paths as ordinary capture, using extra directions.
        RealCameraAPI.register(RealCamera.MOD_ID, -100, YSMCompat::computeResult);
    }

    private static @Nullable BindingResult computeResult(BindingContext context) {
        Minecraft client = context.client();
        float partialTicks = context.partialTicks();
        // Accumulate each path across capture directions and both lookup phases.
        Map<String, LookupContext> lookups = new HashMap<>();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        EntityRenderState renderState = dispatcher.extractEntity(client.getCameraEntity(), partialTicks);
        PoseStack poseStack = new PoseStack();
        for (var recorder : TRANSFORMED_RECORDERS) {
            poseStack.pushPose();
            poseStack.mulPose(recorder.matrix4f.invert(new Matrix4f()));
            dispatcher.submit(renderState, new CameraRenderState(), 0, 0, 0, poseStack, recorder.vertexCatcher.initCollector());
            BindingResult result = recorder.findResult(context, lookups, LookupMode.CACHE_ONLY);
            poseStack.popPose();
            if (result != null) return result;
        }
        for (var recorder : TRANSFORMED_RECORDERS) {
            BindingResult result = recorder.findResult(context, lookups, LookupMode.SCAN_ONLY);
            if (result != null) return result;
        }
        return null;
    }

    private static final class TransformedVertexRecorder {
        private final MultiVertexCatcher vertexCatcher = MultiVertexCatcher.create();
        private final Matrix4f matrix4f = new Matrix4f();
        private final Matrix3f matrix3f = new Matrix3f();

        public TransformedVertexRecorder setRotation(float pitch, float yaw) {
            matrix4f.rotationYXZ(yaw, pitch, 0).invert();
            matrix3f.rotationYXZ(yaw, pitch, 0).invert();
            return this;
        }

        private @Nullable BindingResult findResult(BindingContext context, Map<String, LookupContext> lookups, LookupMode mode) {
            for (var buffer : vertexCatcher.collectBuffers()) {
                for (var target : context.targets(buffer.textureId())) {
                    LookupContext lookup = lookups.computeIfAbsent(target.path(), _ -> new LookupContext(target.config()));
                    lookup.find(buffer, mode, matrix4f, matrix3f);
                    ReferenceFrame frame = lookup.tryBuild();
                    if (frame != null) return context.result(target.path(), frame);
                }
            }
            return null;
        }
    }
}
