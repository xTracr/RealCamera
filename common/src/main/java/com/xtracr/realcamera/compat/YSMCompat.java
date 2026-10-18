package com.xtracr.realcamera.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.api.BindingContext;
import com.xtracr.realcamera.api.BindingResult;
import com.xtracr.realcamera.api.RealCameraAPI;
import com.xtracr.realcamera.config.binding.TargetConfig;
import com.xtracr.realcamera.renderer.IdentifiedVertexBuffer;
import com.xtracr.realcamera.renderer.MultiVertexCatcher;
import com.xtracr.realcamera.renderer.state.Primitive;
import com.xtracr.realcamera.renderer.state.VertexData;
import com.xtracr.realcamera.util.PrimitiveLookup;
import com.xtracr.realcamera.util.PrimitiveLookup.LookupMode;
import com.xtracr.realcamera.util.ReferenceFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
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
        // Accumulate position, forward and upward for this capture only; null means not found.
        Map<String, Vec3[]> matches = new HashMap<>();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        EntityRenderState renderState = dispatcher.extractEntity(client.getCameraEntity(), partialTicks);
        PoseStack poseStack = new PoseStack();
        for (TransformedVertexRecorder recorder : TRANSFORMED_RECORDERS) {
            poseStack.pushPose();
            poseStack.mulPose(recorder.matrix4f.invert(new Matrix4f()));
            dispatcher.submit(renderState, new CameraRenderState(), 0, 0, 0, poseStack, recorder.vertexCatcher.initCollector());
            BindingResult result = recorder.findResult(context, matches, LookupMode.CACHE_ONLY);
            poseStack.popPose();
            if (result != null) return result;
        }
        for (TransformedVertexRecorder recorder : TRANSFORMED_RECORDERS) {
            BindingResult result = recorder.findResult(context, matches, LookupMode.SCAN_ONLY);
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

        private @Nullable BindingResult findResult(BindingContext context, Map<String, Vec3[]> matches, LookupMode mode) {
            for (IdentifiedVertexBuffer buffer : vertexCatcher.collectBuffers()) {
                for (var target : context.targets(buffer.textureId())) {
                    Vec3[] vectors = matches.computeIfAbsent(target.path(), _ -> new Vec3[3]);
                    ReferenceFrame frame = collectFrame(buffer, target.config(), vectors, mode);
                    if (frame != null) return context.result(target.path(), frame);
                }
            }
            return null;
        }

        private @Nullable ReferenceFrame collectFrame(IdentifiedVertexBuffer geometry, TargetConfig config, Vec3[] vectors, LookupMode mode) {
            VertexData.UV[] uvs = {
                    vectors[0] == null ? new VertexData.UV(config.posU(), config.posV()) : null,
                    vectors[1] == null ? new VertexData.UV(config.forwardU(), config.forwardV()) : null,
                    vectors[2] == null ? new VertexData.UV(config.upwardU(), config.upwardV()) : null
            };
            Primitive[] primitives = PrimitiveLookup.findPrimitives(geometry, uvs, mode);
            for (int i = 0; i < primitives.length; i++) {
                Primitive primitive = primitives[i];
                if (primitive == null) continue;
                Vec3 vector = i == 0
                        ? new Vec3(primitive.positionAtUV(config.posU(), config.posV()).toVector3f().mulPosition(matrix4f))
                        : new Vec3(primitive.faceNormal().toVector3f().mul(matrix3f)).normalize();
                if (!Double.isFinite(vector.lengthSqr()) || (i != 0 && vector.equals(Vec3.ZERO))) continue;
                vectors[i] = vector;
            }
            if (vectors[0] == null || vectors[1] == null || vectors[2] == null) return null;
            return ReferenceFrame.tryFromForwardUp(vectors[0], vectors[1], vectors[2]);
        }
    }
}
