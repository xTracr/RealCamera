package com.xtracr.realcamera;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtracr.realcamera.api.RealCameraAPI;
import com.xtracr.realcamera.compat.DisableHelper;
import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.BindingContext;
import com.xtracr.realcamera.util.LocUtil;
import com.xtracr.realcamera.util.VertexData;
import com.xtracr.realcamera.util.VertexRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RealCameraCore {
    private static final VertexRecorder defaultRecorder = new VertexRecorder();
    private static VertexRecorder activeRecorder = defaultRecorder;
    private static BindingContext bindingContext = BindingContext.EMPTY;
    private static Vec3 cameraPos = Vec3.ZERO, entityPos = Vec3.ZERO;
    private static boolean active = false, rendering = false, readyToSendMessage = true;

    public static void setActiveRecorder(VertexRecorder recorder) {
        activeRecorder = recorder;
    }

    public static BindingTarget currentTarget() {
        return bindingContext.target;
    }

    public static float getPitch(float f) {
        if (currentTarget().isBindRotation()) return (float) bindingContext.getEulerAngle().x();
        return f;
    }

    public static float getYaw(float f) {
        if (currentTarget().isBindRotation()) return (float) -bindingContext.getEulerAngle().y();
        return f;
    }

    public static float getRoll(float f) {
        if (ConfigFile.config().isClassic()) return f + ConfigFile.config().getClassicRoll();
        if (currentTarget().isBindRotation()) return (float) bindingContext.getEulerAngle().z();
        return f;
    }

    public static Vec3 getRawPos(Vec3 vec) {
        Vec3 rawPos = bindingContext.getPosition().add(entityPos);
        return new Vec3(currentTarget().isBindX() ? rawPos.x() : vec.x(), currentTarget().isBindY() ? rawPos.y() : vec.y(), currentTarget().isBindZ() ? rawPos.z() : vec.z());
    }

    public static Vec3 getCameraPos(Vec3 vec) {
        return new Vec3(currentTarget().isBindX() ? cameraPos.x() : vec.x(), currentTarget().isBindY() ? cameraPos.y() : vec.y(), currentTarget().isBindZ() ? cameraPos.z() : vec.z());
    }

    public static void setCameraPos(Vec3 vec) {
        cameraPos = vec;
    }

    public static void initialize(Minecraft client) {
        Entity entity = client.getCameraEntity();
        active = ConfigFile.config().enabled() && client.options.getCameraType().isFirstPerson() && entity != null && !DisableHelper.MAIN_FEATURE.disabled(entity);
        rendering = active && ConfigFile.config().renderModel() && !DisableHelper.RENDER_MODEL.disabled(entity);
        activeRecorder.records().clear();
    }

    public static void readyToSendMessage() {
        readyToSendMessage = ConfigFile.config().enabled();
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isRendering() {
        return isActive() && rendering;
    }

    public static void computeCamera(Minecraft client, float deltaTick) {
        Entity entity = client.getCameraEntity();
        if (entity.tickCount == 0) {
            entity.xOld = entity.getX();
            entity.yOld = entity.getY();
            entity.zOld = entity.getZ();
        }
        entityPos = new Vec3(Mth.lerp(deltaTick, entity.xOld, entity.getX()), Mth.lerp(deltaTick, entity.yOld, entity.getY()), Mth.lerp(deltaTick, entity.zOld, entity.getZ()));

        BindingContext apiContext = RealCameraAPI.genBindingContext(client, deltaTick);
        if (apiContext.available()) bindingContext = apiContext;
        else {
            activeRecorder.updateModel(client, client.getCameraEntity(), deltaTick, new PoseStack());
            bindingContext = activeRecorder.genContext();
        }
        if (activeRecorder.records().isEmpty()) bindingContext.skipRendering = false;
        if (!bindingContext.available()) {
            Player player = client.player;
            if (readyToSendMessage && player != null) player.displayClientMessage(LocUtil.MESSAGE("bindingFailed", LocUtil.MOD_NAME(), LocUtil.MODEL_VIEW_TITLE()), false);
            active = readyToSendMessage = false;
            return;
        }
        readyToSendMessage = true;
        bindingContext.init();
    }

    public static void renderCameraEntity(Minecraft client, float deltaTick, MultiBufferSource bufferSource, Matrix4f cameraPose) {
        Vec3 eulerAngle = bindingContext.getEulerAngle();
        Matrix4f invertedCameraPose = new Matrix4f()
                .rotateZ((float) Math.toRadians(eulerAngle.z()))
                .rotateX((float) Math.toRadians(eulerAngle.x()))
                .rotateY((float) Math.toRadians(180.0f - eulerAngle.y()))
                .transpose()
                .invert()
                .translate(Vec3.ZERO.subtract(bindingContext.getPosition()).toVector3f());
        PoseStack poseStack = new PoseStack();
        if (!bindingContext.skipRendering || ConfigFile.config().rerenderModel()) {
            poseStack.mulPose(new Matrix4f(invertedCameraPose).mulLocal(cameraPose.invert(new Matrix4f())));
            activeRecorder.updateModel(client, client.getCameraEntity(), deltaTick, poseStack);
        }
        Matrix4f positionMatrix = new Matrix4f(invertedCameraPose).mul(poseStack.last().pose().invert(new Matrix4f()));
        final double m02 = positionMatrix.m02(), m12 = positionMatrix.m12(), m22 = positionMatrix.m22(), m32 = positionMatrix.m32();
        positionMatrix.mulLocal(cameraPose.invert(new Matrix4f()));
        Matrix3f normalMatrix = new Matrix3f(positionMatrix);
        activeRecorder.records().forEach(record -> {
            if (currentTarget().getDisabledTextureIds().stream().anyMatch(record.textureId()::contains)) return;
            VertexConsumer buffer = bufferSource.getBuffer(record.renderType());
            if (!record.renderType().canConsolidateConsecutiveGeometry()) {
                VertexData.renderVertices(record.vertices(), buffer);
                return;
            }
            final double depth = currentTarget().getDisablingDepth();
            for (VertexData[] primitive : record.primitives()) {
                for (VertexData vertex : primitive) {
                    if (Math.fma(m02, vertex.x(), Math.fma(m12, vertex.y(), Math.fma(m22, vertex.z(), m32))) > -depth) continue;
                    VertexData.renderVertices(primitive, buffer, positionMatrix, normalMatrix);
                    break;
                }
            }
        });
    }
}
