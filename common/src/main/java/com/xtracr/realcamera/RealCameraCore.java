package com.xtracr.realcamera;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtracr.realcamera.api.RealCameraAPI;
import com.xtracr.realcamera.compat.DisableHelper;
import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.BindTarget.DisableConfig;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RealCameraCore {
    private static final VertexRecorder defaultRecorder = new VertexRecorder();
    private static VertexRecorder activeRecorder = defaultRecorder;
    private static BindResult bindResult = BindResult.EMPTY;
    private static Vec3 cameraPos = Vec3.ZERO, eulerAngle = Vec3.ZERO;
    private static boolean active = false, rendering = false;
    private static int failureFrames = 0;

    public static boolean isActive() {
        return active;
    }

    public static boolean isRendering() {
        return isActive() && rendering;
    }

    public static void setActiveRecorder(VertexRecorder recorder) {
        activeRecorder = recorder;
    }

    public static BindTarget currentTarget() {
        return bindResult.target;
    }

    public static void initialize(Minecraft client) {
        Entity entity = client.getCameraEntity();
        active = ConfigFile.config().enabled() && client.options.getCameraType().isFirstPerson() && entity != null && !DisableHelper.MAIN_FEATURE.disabled(entity);
        rendering = ConfigFile.config().renderModel() && !DisableHelper.RENDER_MODEL.disabled(entity);
        activeRecorder.records().clear();
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
        Vec3 rawPos = SmoothUtil.smoothPosition(bindResult.getPosition()).add(entityPos);
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
        BindResult newResult = RealCameraAPI.computeBindResult(client, deltaTick);
        if (!newResult.available()) {
            activeRecorder.updateModel(client, entity, deltaTick, new PoseStack());
            newResult = activeRecorder.computeBindResult();
        }
        entity.setInvisible(invisible);
        if (activeRecorder.records().isEmpty() || invisible) newResult.skipRendering = false;
        if (!newResult.available()) {
            failureFrames++;
            Entity player = client.player;
            int retentionFrames = ConfigFile.config().getBindResultRetentionFrames();
            if (!ConfigFile.config().hideBindingFailureMessage() && failureFrames == retentionFrames + 1 && player != null) {
                player.sendSystemMessage(LocUtil.MESSAGE("bindingFailed", LocUtil.MOD_NAME(), LocUtil.MODEL_VIEW_TITLE(), KeyBindings.MODEL_VIEW_SCREEN.getTranslatedKeyMessage()));
            }
            if (!bindResult.available() || failureFrames > retentionFrames) {
                active = false;
                return;
            }
        } else {
            failureFrames = 0;
            bindResult = newResult.init();
        }
        eulerAngle = MathUtil.getEulerAngleYXZ(SmoothUtil.smoothRotation(bindResult.getRotation())).scale(Math.toDegrees(1));
    }

    public static void renderCameraEntity(Minecraft client, float deltaTick, MultiBufferSource bufferSource) {
        Vec3 targetEulerAngle = MathUtil.getEulerAngleYXZ(bindResult.getRotation());
        Matrix4f invertedCameraPose = new Matrix4f()
                .rotateZ((float) targetEulerAngle.z())
                .rotateX((float) targetEulerAngle.x())
                .rotateY((float) (Math.PI - targetEulerAngle.y()))
                .transpose()
                .invert()
                .translate(Vec3.ZERO.subtract(bindResult.getPosition()).toVector3f());
        PoseStack poseStack = new PoseStack();
        if (!bindResult.skipRendering || ConfigFile.config().rerenderModel()) {
            poseStack.mulPoseMatrix(new Matrix4f(invertedCameraPose));
            activeRecorder.updateModel(client, client.getCameraEntity(), deltaTick, poseStack);
        }
        Matrix4f positionMatrix = new Matrix4f(invertedCameraPose).mul(poseStack.last().pose().invert(new Matrix4f()));
        Matrix3f normalMatrix = new Matrix3f(positionMatrix);
        activeRecorder.records().forEach(record -> {
            DisableConfig[] disableConfigs = currentTarget().filteredDisableConfigs(config -> record.textureId().contains(config.textureId()));
            for (DisableConfig config : disableConfigs) {
                if (config.disableAll()) return;
            }
            VertexConsumer buffer = bufferSource.getBuffer(record.renderType());
            if (!record.renderType().canConsolidateConsecutiveGeometry()) {
                VertexData.renderVertices(record.vertices(), buffer, positionMatrix, normalMatrix);
                return;
            }
            final double depth = currentTarget().disablingDepth();
            final int primitiveLength = record.renderType().mode().primitiveLength;
            for (VertexData[] primitive : record.primitives()) {
                VertexData[] newPrimitive = new VertexData[primitiveLength];
                for (int j = 0; j < primitiveLength ; j++) newPrimitive[j] = primitive[j].transform(positionMatrix, normalMatrix);
                outer:
                for (VertexData vertex : newPrimitive) {
                    if (vertex.z() > -depth) continue;
                    for (DisableConfig config : disableConfigs) {
                        if (config.test(vertex)) continue outer;
                    }
                    VertexData.renderVertices(newPrimitive, buffer);
                    break;
                }
            }
        });
    }
}
