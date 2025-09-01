package com.xtracr.realcamera.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.BindResult;
import com.xtracr.realcamera.util.VertexRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

public class YSMCompat extends VertexRecorder {
    public static final YSMCompat INSTANCE = new YSMCompat();
    private final Map<BindTarget, BindResult> resultMap = new HashMap<>();
    private final Matrix4f positionMatrix = new Matrix4f();
    private final Matrix3f normalMatrix = new Matrix3f();
    private Minecraft client;
    private Entity cameraEntity;
    private float deltaTick;

    private YSMCompat() { super(); }

    private void updateModel(float pitch, float yaw) {
        if (client == null || cameraEntity == null) return;
        positionMatrix.rotationYXZ(yaw, pitch, 0);
        normalMatrix.rotationYXZ(yaw, pitch, 0);
        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(positionMatrix);
        poseStack.last().normal().set(normalMatrix);
        super.updateModel(client, cameraEntity, deltaTick, poseStack);
    }

    private BindResult computeResultInternal() {
        Matrix4f invertedPosition = positionMatrix.invert(new Matrix4f());
        Matrix3f invertedNormal = normalMatrix.invert(new Matrix3f());
        BindResult result;
        for (BindTarget target : ConfigFile.config().getTargetList()) {
            for (BuiltRecord record : records) {
                result = resultMap.computeIfAbsent(target, k -> new BindResult(target, false));
                record.exportToBindResult(result, invertedPosition, invertedNormal);
                result.skipRendering = false;
                if (result.available()) return result;
            }
        }
        return BindResult.EMPTY;
    }

    @Override
    public void updateModel(Minecraft client, Entity entity, float deltaTick, PoseStack poseStack) {
        this.client = client;
        this.cameraEntity = entity;
        this.deltaTick = deltaTick;
        positionMatrix.identity();
        normalMatrix.identity();
        super.updateModel(client, entity, deltaTick, poseStack);
    }

    @Override
    public BindResult computeBindResult() {
        resultMap.clear();
        BindResult result = computeResultInternal();
        if (result.available()) return result;
        final float pitch = 1.9106332f, yaw = 2.0943951f;
        updateModel(pitch, 0);
        result = computeResultInternal();
        if (result.available()) return result;
        updateModel(pitch, yaw);
        result = computeResultInternal();
        if (result.available()) return result;
        updateModel(pitch, 2 * yaw);
        result = computeResultInternal();
        if (result.available()) return result;
        return BindResult.EMPTY;
    }
}
