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

    public static Vec3 getPos(Vec3 vec3d) {
        return new Vec3(currentTarget.bindX ? pos.x() : vec3d.x(), currentTarget.bindY ? pos.y() : vec3d.y(), currentTarget.bindZ ? pos.z() : vec3d.z());
    }

    public static Vec3 getCameraPos(Vec3 vec3d) {
        return new Vec3(currentTarget.bindX ? cameraPos.x() : vec3d.x(), currentTarget.bindY ? cameraPos.y() : vec3d.y(), currentTarget.bindZ ? cameraPos.z() : vec3d.z());
    }

    public static void setCameraPos(Vec3 vec3d) {
        cameraPos = vec3d;
    }

    public static void initialize(Minecraft client) {
        Entity entity = client.getCameraEntity();
        active = ConfigFile.config().enabled() && client.options.getCameraType().isFirstPerson() && client.gameRenderer.getMainCamera() != null && entity != null && !DisableHelper.isDisabled("mainFeature", entity);
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
        dispatcher.render(entity, 0, 0, 0, Mth.lerp(tickDelta, entity.yRotO, entity.getYRot()), tickDelta, new PoseStack(), recorder, dispatcher.getPackedLightCoords(entity, tickDelta));
        recorder.buildRecords();
    }

    public static void renderCameraEntity(MultiBufferSource vertexConsumers, Matrix4f projectionMatrix) {
        Vector3f vertexOffset = offset.subtract(pos).toVector3f();
        Matrix3f normalMatrix = new Matrix3f().rotateZ((float) Math.toRadians(roll)).rotateX((float) Math.toRadians(pitch)).rotateY((float) Math.toRadians(yaw + 180.0f)).transpose().invert();
        Matrix4f positionMatrix = new Matrix4f(normalMatrix).translate(vertexOffset);
        final double m02 = positionMatrix.m02(), m12 = positionMatrix.m12(), m22 = positionMatrix.m22(), m32 = positionMatrix.m32();
        normalMatrix.mulLocal(new Matrix3f(projectionMatrix).invert());
        positionMatrix.set(normalMatrix).translate(vertexOffset);
        recorder.drawRecords(record -> {
            if (currentTarget.disabledTextureIds.stream().anyMatch(record.textureId()::contains)) return;
            VertexConsumer buffer = vertexConsumers.getBuffer(record.renderLayer());
            if (!record.renderLayer().canConsolidateConsecutiveGeometry()) {
                for (VertexRecorder.Vertex vertex : record.vertices()) vertex.apply(buffer);
                return;
            }
            final double depth = currentTarget.disablingDepth;
            boolean shouldDraw;
            for (VertexRecorder.Vertex[] primitive : record.primitives()) {
                shouldDraw = false;
                for (VertexRecorder.Vertex vertex : primitive) {
                    if (Math.fma(m02, vertex.x(), Math.fma(m12, vertex.y(), Math.fma(m22, vertex.z(), m32))) < -depth) {
                        shouldDraw = true;
                        break;
                    }
                }
                if (shouldDraw) for (VertexRecorder.Vertex vertex : primitive) vertex.transform(positionMatrix, normalMatrix).apply(buffer);
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
            Entity player = Minecraft.getInstance().player;
            if (readyToSendMessage && player != null) player.sendSystemMessage(LocUtil.MESSAGE("bindingFailed", LocUtil.MOD_NAME(), LocUtil.MODEL_VIEW_TITLE()));
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
