package com.xtracr.realcamera;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtracr.realcamera.compat.DisableHelper;
import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.LocUtil;
import com.xtracr.realcamera.util.MathUtil;
import com.xtracr.realcamera.util.VertexRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class RealCameraCore {
    private static final VertexRecorder recorder = new VertexRecorder();
    public static BindingTarget currentTarget = new BindingTarget();
    private static Vec3 pos = Vec3.ZERO, cameraPos = Vec3.ZERO, offset = Vec3.ZERO;
    private static boolean active = false, rendering = false, readyToSendMessage = true;
    private static float pitch, yaw, roll;

    public static float getPitch(float f) {
        if (currentTarget.bindRotation) return pitch;
        return f;
    }

    public static float getYaw(float f) {
        if (currentTarget.bindRotation) return yaw;
        return f;
    }

    public static float getRoll(float f) {
        if (ConfigFile.config().isClassic()) return f + ConfigFile.config().getClassicRoll();
        if (currentTarget.bindRotation) return roll;
        return f;
    }

    public static Vec3 getPos(Vec3 vec) {
        return new Vec3(currentTarget.bindX ? pos.x() : vec.x(), currentTarget.bindY ? pos.y() : vec.y(), currentTarget.bindZ ? pos.z() : vec.z());
    }

    public static Vec3 getCameraPos(Vec3 vec) {
        return new Vec3(currentTarget.bindX ? cameraPos.x() : vec.x(), currentTarget.bindY ? cameraPos.y() : vec.y(), currentTarget.bindZ ? cameraPos.z() : vec.z());
    }

    public static void setCameraPos(Vec3 vec) {
        cameraPos = vec;
    }

    public static void initialize(Minecraft client) {
        Entity entity = client.getCameraEntity();
        active = ConfigFile.config().enabled() && client.options.getCameraType().isFirstPerson() && entity != null && !DisableHelper.isDisabled("mainFeature", entity);
        rendering = active && ConfigFile.config().renderModel() && !DisableHelper.isDisabled("renderModel", entity);
    }

    public static void readyToSendMessage() {
        readyToSendMessage = ConfigFile.config().enabled();
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isRendering() {
        return active && rendering;
    }

    public static void updateModel(Minecraft client, float tickDelta) {
        recorder.clear();
        // WorldRenderer.render
        Entity entity = client.getCameraEntity();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        dispatcher.prepare(client.level, client.gameRenderer.getMainCamera(), client.crosshairPickEntity);
        if (entity.tickCount == 0) {
            entity.xOld = entity.getX();
            entity.yOld = entity.getY();
            entity.zOld = entity.getZ();
        }
        // WorldRenderer.renderEntity
        offset = new Vec3(Mth.lerp(tickDelta, entity.xOld, entity.getX()), Mth.lerp(tickDelta, entity.yOld, entity.getY()), Mth.lerp(tickDelta, entity.zOld, entity.getZ()));
        dispatcher.render(entity, 0, 0, 0, tickDelta, new PoseStack(), recorder, dispatcher.getPackedLightCoords(entity, tickDelta));
        recorder.buildRecords();
    }

    public static void renderCameraEntity(MultiBufferSource bufferSource, Matrix4f projectionMatrix) {
        Vector3f vertexOffset = offset.subtract(pos).toVector3f();
        Matrix3f normalMatrix = new Matrix3f().rotateZ((float) Math.toRadians(roll)).rotateX((float) Math.toRadians(pitch)).rotateY((float) Math.toRadians(yaw + 180.0f)).transpose().invert();
        Matrix4f positionMatrix = new Matrix4f(normalMatrix).translate(vertexOffset);
        final double m02 = positionMatrix.m02(), m12 = positionMatrix.m12(), m22 = positionMatrix.m22(), m32 = positionMatrix.m32();
        normalMatrix.mulLocal(new Matrix3f(projectionMatrix).invert());
        positionMatrix.set(normalMatrix).translate(vertexOffset);
        recorder.forEachRecord(record -> {
            if (currentTarget.disabledTextureIds.stream().anyMatch(record.textureId()::contains)) return;
            VertexConsumer buffer = bufferSource.getBuffer(record.renderType());
            if (!record.renderType().canConsolidateConsecutiveGeometry()) {
                VertexRecorder.renderVertices(record.vertices(), buffer);
                return;
            }
            final double depth = currentTarget.disablingDepth;
            for (VertexRecorder.Vertex[] primitive : record.primitives()) {
                for (VertexRecorder.Vertex vertex : primitive) {
                    if (Math.fma(m02, vertex.x(), Math.fma(m12, vertex.y(), Math.fma(m22, vertex.z(), m32))) > -depth) continue;
                    VertexRecorder.renderVertices(primitive, buffer, positionMatrix, normalMatrix);
                    break;
                }
            }
        });
    }

    public static void computeCamera() {
        currentTarget = new BindingTarget();
        Matrix3f normal = new Matrix3f();
        for (BindingTarget target : ConfigFile.config().getTargetList()) {
            Vector3f position  = new Vector3f();
            if (recorder.getTargetPosAndRot(target, normal, position, false) == null || !(Math.abs(normal.determinant() - 1) <= 0.01f) || !Float.isFinite(position.lengthSquared())) continue;
            pos = new Vec3(position).add(offset);
            currentTarget = target;
            break;
        }
        if (currentTarget.isEmpty()) {
            Player player = Minecraft.getInstance().player;
            if (readyToSendMessage && player != null) player.displayClientMessage(LocUtil.MESSAGE("bindingFailed", LocUtil.MOD_NAME(), LocUtil.MODEL_VIEW_TITLE()), false);
            active = readyToSendMessage = false;
        } else readyToSendMessage = true;
        normal.rotateLocal((float) Math.toRadians(currentTarget.getYaw()), normal.m10, normal.m11, normal.m12);
        normal.rotateLocal((float) Math.toRadians(currentTarget.getPitch()), normal.m00, normal.m01, normal.m02);
        normal.rotateLocal((float) Math.toRadians(currentTarget.getRoll()), normal.m20, normal.m21, normal.m22);
        Vec3 eulerAngle = MathUtil.getEulerAngleYXZ(normal).scale(Math.toDegrees(1));
        pitch = (float) eulerAngle.x();
        yaw = (float) -eulerAngle.y();
        roll = (float) eulerAngle.z();
    }
}
