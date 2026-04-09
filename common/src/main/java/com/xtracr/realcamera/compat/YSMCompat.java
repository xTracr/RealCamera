package com.xtracr.realcamera.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.api.BindResult;
import com.xtracr.realcamera.api.RealCameraAPI;
import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.renderer.BuiltIterableBuffer;
import com.xtracr.realcamera.renderer.MultiVertexCatcher;
import com.xtracr.realcamera.renderer.state.VertexData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

public class YSMCompat {
    private static final MultiVertexCatcher vertexCatcher = MultiVertexCatcher.create();
    private static final Map<BindTarget, BindResult> resultMap = new HashMap<>();
    private static final TransformedVertexRecorder[] transformedRecorders = new TransformedVertexRecorder[4];
    private static BindResult bindResult = BindResult.EMPTY;
    private static boolean allCached = false;

    static {
        final float pitch = 1.9106332f, yaw = 2.0943951f;
        transformedRecorders[0] = new TransformedVertexRecorder();
        transformedRecorders[1] = new TransformedVertexRecorder().setRotation(pitch, 0);
        transformedRecorders[2] = new TransformedVertexRecorder().setRotation(pitch, yaw);
        transformedRecorders[3] = new TransformedVertexRecorder().setRotation(pitch, 2 * yaw);
    }

    public static void register() {
        RealCameraAPI.registerFunction(-100, YSMCompat::computeBindResult);
    }

    private static BindResult computeBindResult(Minecraft client, float partialTicks) {
        resultMap.clear();
        bindResult = BindResult.EMPTY;
        allCached = true;
        Entity entity = client.getCameraEntity();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        PoseStack poseStack = new PoseStack();
        for (TransformedVertexRecorder transformedRecorder : transformedRecorders) {
            poseStack.pushPose();
            poseStack.mulPose(transformedRecorder.matrix4f.invert(new Matrix4f()));
            dispatcher.submit(dispatcher.extractEntity(entity, partialTicks), new CameraRenderState(), 0, 0, 0, poseStack, vertexCatcher.initCollector());
            vertexCatcher.endCatching(transformedRecorder::computeBindResultInCache);
            poseStack.popPose();
            if (bindResult.available()) return bindResult;
        }
        if (allCached) return BindResult.EMPTY;
        for (TransformedVertexRecorder transformedRecorder : transformedRecorders) {
            poseStack.pushPose();
            poseStack.mulPose(transformedRecorder.matrix4f.invert(new Matrix4f()));
            dispatcher.submit(dispatcher.extractEntity(entity, partialTicks), new CameraRenderState(), 0, 0, 0, poseStack, vertexCatcher.initCollector());
            vertexCatcher.endCatching(transformedRecorder::computeBindResult);
            poseStack.popPose();
            if (bindResult.available()) return bindResult;
        }
        return BindResult.EMPTY;
    }

    protected static class TransformedVertexRecorder {
        protected final Matrix4f matrix4f = new Matrix4f();
        protected final Matrix3f matrix3f = new Matrix3f();

        public TransformedVertexRecorder setRotation(float pitch, float yaw) {
            matrix4f.rotationYXZ(yaw, pitch, 0).invert();
            matrix3f.rotationYXZ(yaw, pitch, 0).invert();
            return this;
        }

        public void computeBindResultInCache(BuiltIterableBuffer builtBuffer) {
            if (bindResult.available()) return;
            for (BindTarget target : ConfigFile.config().getBindTargetList(builtBuffer.textureId())) {
                BindResult result = resultMap.computeIfAbsent(target, _ -> new BindResult(target));
                BindTarget.TargetConfig config = target.targetConfig();
                VertexData.UV posUV = new VertexData.UV(config.posU(), config.posV());
                VertexData.UV forwardUV = new VertexData.UV(config.forwardU(), config.forwardV());
                VertexData.UV upwardUV = new VertexData.UV(config.upwardU(), config.upwardV());
                if (builtBuffer.anyNotCached(new VertexData.UV[]{posUV, forwardUV, upwardUV})) allCached = false;
                if (result.getPosition() != Vec3.ZERO) posUV = null;
                if (result.getForward() != Vec3.ZERO) forwardUV = null;
                if (result.getUpward() != Vec3.ZERO) upwardUV = null;
                VertexData[][] primitives = builtBuffer.findPrimitivesInCache(new VertexData.UV[]{posUV, forwardUV, upwardUV});
                if (primitives[0] != null) result.setPosition(new Vec3(VertexData.position(primitives[0], config.posU(), config.posV()).toVector3f().mulPosition(matrix4f)));
                if (primitives[1] != null) result.setForward(new Vec3(VertexData.normal(primitives[1]).toVector3f().mul(matrix3f)));
                if (primitives[2] != null) result.setUpward(new Vec3(VertexData.normal(primitives[2]).toVector3f().mul(matrix3f)));
                if (!result.available()) continue;
                bindResult = result;
                return;
            }
        }

        public void computeBindResult(BuiltIterableBuffer builtBuffer) {
            if (bindResult.available()) return;
            for (BindTarget target : ConfigFile.config().getBindTargetList(builtBuffer.textureId())) {
                BindResult result = resultMap.computeIfAbsent(target, _ -> new BindResult(target));
                BindTarget.TargetConfig config = target.targetConfig();
                VertexData.UV posUV = result.getPosition() == Vec3.ZERO ? new VertexData.UV(config.posU(), config.posV()) : null;
                VertexData.UV forwardUV = result.getForward() == Vec3.ZERO ? new VertexData.UV(config.forwardU(), config.forwardV()) : null;
                VertexData.UV upwardUV = result.getUpward() == Vec3.ZERO ? new VertexData.UV(config.upwardU(), config.upwardV()) : null;
                VertexData[][] primitives = builtBuffer.findPrimitives(new VertexData.UV[]{posUV, forwardUV, upwardUV});
                if (primitives[0] != null) result.setPosition(new Vec3(VertexData.position(primitives[0], config.posU(), config.posV()).toVector3f().mulPosition(matrix4f)));
                if (primitives[1] != null) result.setForward(new Vec3(VertexData.normal(primitives[1]).toVector3f().mul(matrix3f)));
                if (primitives[2] != null) result.setUpward(new Vec3(VertexData.normal(primitives[2]).toVector3f().mul(matrix3f)));
                if (!result.available()) continue;
                bindResult = result;
                return;
            }
        }
    }
}
