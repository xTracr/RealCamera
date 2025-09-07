package com.xtracr.realcamera.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YSMCompat implements VertexRecorder {
    public static final YSMCompat INSTANCE = new YSMCompat();
    private final Map<BindTarget, BindResult> resultMap = new HashMap<>();
    private final TransformedVertexRecorder[] transformedRecorders = new TransformedVertexRecorder[4];
    private Minecraft client;
    private Entity cameraEntity;
    private PoseStack poseStack;
    private float deltaTick;

    private YSMCompat() {
        super();
        final float pitch = 1.9106332f, yaw = 2.0943951f;
        transformedRecorders[0] = new TransformedVertexRecorder();
        transformedRecorders[1] = new TransformedVertexRecorder().setRotation(pitch, 0);
        transformedRecorders[2] = new TransformedVertexRecorder().setRotation(pitch, yaw);
        transformedRecorders[3] = new TransformedVertexRecorder().setRotation(pitch, 2 * yaw);
    }

    @Override
    public List<BuiltRecord> records() {
        return transformedRecorders[0].records();
    }

    @Override
    public void setCatcher(MultiVertexCatcher catcher) {
        for (TransformedVertexRecorder recorder : transformedRecorders) {
            recorder.setCatcher(catcher);
        }
    }

    @Override
    public void updateModel(Minecraft client, Entity entity, float deltaTick, PoseStack poseStack) {
        this.client = client;
        this.cameraEntity = entity;
        this.deltaTick = deltaTick;
        this.poseStack = poseStack;
        transformedRecorders[0].updateModel(client, entity, deltaTick, poseStack);
    }

    @Override
    public BindResult computeBindResult() {
        resultMap.clear();
        BindResult result = transformedRecorders[0].computeBindResult();
        if (result.available()) return result;
        for (int i = 1; i < transformedRecorders.length; i++) {
            transformedRecorders[i].updateModel(client, cameraEntity, deltaTick, poseStack);
            result = transformedRecorders[i].computeBindResult();
            if (result.available()) return result;
        }
        for (TransformedVertexRecorder recorder : transformedRecorders) {
            Matrix4f matrix4f = recorder.positionMatrix.invert(new Matrix4f());
            Matrix3f matrix3f = recorder.normalMatrix.invert(new Matrix3f());
            for (BindTarget target : ConfigFile.config().getBindTargetList()) {
                for (BuiltRecord record : recorder.records()) {
                    BindResult bindResult = resultMap.computeIfAbsent(target, k -> new BindResult(target, false));
                    BindTarget.TargetConfig config = bindResult.target.targetConfig();
                    VertexData[] primitive;
                    if (bindResult.getPosition() == Vec3.ZERO && (primitive = record.findPrimitive(config.posU(), config.posV())) != null)
                        result.setPosition(new Vec3(VertexData.position(primitive, config.posU(), config.posV()).toVector3f().mulPosition(matrix4f)));
                    if (bindResult.getForward() == Vec3.ZERO && (primitive = record.findPrimitive(config.forwardU(), config.forwardV())) != null)
                        result.setForward(new Vec3(VertexData.normal(primitive).toVector3f().mul(matrix3f)));
                    if (bindResult.getUpward() == Vec3.ZERO && (primitive = record.findPrimitive(config.upwardU(), config.upwardV())) != null)
                        result.setUpward(new Vec3(VertexData.normal(primitive).toVector3f().mul(matrix3f)));
                    bindResult.skipRendering = false;
                    if (bindResult.available()) return bindResult;
                }
            }
        }
        return BindResult.EMPTY;
    }

    protected class TransformedVertexRecorder extends BasicVertexRecorder {
        protected final Matrix4f positionMatrix = new Matrix4f();
        protected final Matrix3f normalMatrix = new Matrix3f();

        public TransformedVertexRecorder setRotation(float pitch, float yaw) {
            positionMatrix.rotationYXZ(yaw, pitch, 0);
            normalMatrix.rotationYXZ(yaw, pitch, 0);
            return this;
        }

        @Override
        public void updateModel(Minecraft client, Entity entity, float deltaTick, PoseStack poseStack) {
            poseStack.pushPose();
            poseStack.mulPose(positionMatrix);
            super.updateModel(client, entity, deltaTick, poseStack);
            poseStack.popPose();
        }

        @Override
        public BindResult computeBindResult() {
            Matrix4f matrix4f = positionMatrix.invert(new Matrix4f());
            Matrix3f matrix3f = normalMatrix.invert(new Matrix3f());
            for (BindTarget target : ConfigFile.config().getBindTargetList()) {
                for (BuiltRecord record : records) {
                    BindResult result = resultMap.computeIfAbsent(target, k -> new BindResult(target, false));
                    if (!record.textureId().contains(result.target.textureId())) continue;
                    BindTarget.TargetConfig config = result.target.targetConfig();
                    VertexData[] primitive;
                    if (result.getPosition() == Vec3.ZERO && (primitive = record.findPrimitiveInCache(config.posU(), config.posV())) != null)
                        result.setPosition(new Vec3(VertexData.position(primitive, config.posU(), config.posV()).toVector3f().mulPosition(matrix4f)));
                    if (result.getForward() == Vec3.ZERO && (primitive = record.findPrimitiveInCache(config.forwardU(), config.forwardV())) != null)
                        result.setForward(new Vec3(VertexData.normal(primitive).toVector3f().mul(matrix3f)));
                    if (result.getUpward() == Vec3.ZERO && (primitive = record.findPrimitiveInCache(config.upwardU(), config.upwardV())) != null)
                        result.setUpward(new Vec3(VertexData.normal(primitive).toVector3f().mul(matrix3f)));
                    if (result.available()) return result;
                }
            }
            return BindResult.EMPTY;
        }
    }
}
