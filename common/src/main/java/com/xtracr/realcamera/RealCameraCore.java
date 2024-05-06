package com.xtracr.realcamera;

import com.xtracr.realcamera.compat.DisableHelper;
import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.LocUtil;
import com.xtracr.realcamera.util.MathUtil;
import com.xtracr.realcamera.util.VertexRecorder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Objects;

public class RealCameraCore {
    private static final VertexRecorder recorder = new VertexRecorder();
    public static BindingTarget currentTarget = new BindingTarget();
    private static Vec3d pos = Vec3d.ZERO, cameraPos = Vec3d.ZERO, offset = Vec3d.ZERO;
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

    public static Vec3d getPos(Vec3d vec3d) {
        return new Vec3d(currentTarget.bindX ? pos.getX() : vec3d.getX(), currentTarget.bindY ? pos.getY() : vec3d.getY(), currentTarget.bindZ ? pos.getZ() : vec3d.getZ());
    }

    public static Vec3d getCameraPos(Vec3d vec3d) {
        return new Vec3d(currentTarget.bindX ? cameraPos.getX() : vec3d.getX(), currentTarget.bindY ? cameraPos.getY() : vec3d.getY(), currentTarget.bindZ ? cameraPos.getZ() : vec3d.getZ());
    }

    public static void setCameraPos(Vec3d vec3d) {
        cameraPos = vec3d;
    }

    public static void initialize(MinecraftClient client) {
        Entity entity = client.getCameraEntity();
        active = ConfigFile.config().enabled() && client.options.getPerspective().isFirstPerson() && client.gameRenderer.getCamera() != null && entity != null && !DisableHelper.isDisabled("mainFeature", entity);
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

    public static void updateModel(MinecraftClient client, float tickDelta) {
        recorder.clear();
        // WorldRenderer.render
        Entity entity = client.getCameraEntity();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        dispatcher.configure(client.world, client.gameRenderer.getCamera(), client.targetedEntity);
        if (entity.age == 0) {
            entity.lastRenderX = entity.getX();
            entity.lastRenderY = entity.getY();
            entity.lastRenderZ = entity.getZ();
        }
        // WorldRenderer.renderEntity
        offset = new Vec3d(MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX()), MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()), MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ()));
        dispatcher.render(entity, 0, 0, 0, MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw()), tickDelta, new MatrixStack(), recorder, dispatcher.getLight(entity, tickDelta));
        recorder.buildRecords();
    }

    public static void renderCameraEntity(VertexConsumerProvider vertexConsumers) {
        Matrix3f normalMatrix = new Matrix3f().rotate(RotationAxis.POSITIVE_Z.rotationDegrees(roll))
                .rotate(RotationAxis.POSITIVE_X.rotationDegrees(pitch))
                .rotate(RotationAxis.POSITIVE_Y.rotationDegrees(yaw + 180.0f))
                .transpose().invert();
        Matrix4f positionMatrix = new Matrix4f(normalMatrix).translate(offset.subtract(pos).toVector3f());
        recorder.drawByAnother(vertexConsumers, record -> {
            if (currentTarget.disabledTextureIds.stream().anyMatch(record.textureId()::contains)) return new VertexRecorder.Vertex[0][];
            final double depth = currentTarget.disablingDepth;
            final int vertexCount = record.additionalVertexCount();
            return Arrays.stream(record.vertices()).map(quad -> {
                VertexRecorder.Vertex[] newQuad = new VertexRecorder.Vertex[vertexCount];
                for (int j = 0; j < vertexCount ; j++) newQuad[j] = quad[j].transform(positionMatrix, normalMatrix);
                for (VertexRecorder.Vertex vertex : newQuad) if (vertex.z() < -depth) return newQuad;
                return null;
            }).filter(Objects::nonNull).toArray(VertexRecorder.Vertex[][]::new);
        });
    }

    public static void computeCamera() {
        currentTarget = new BindingTarget();
        Matrix3f normal = new Matrix3f();
        for (BindingTarget target : ConfigFile.config().getTargetList()) {
            Vector3f position  = new Vector3f();
            if (recorder.getTargetPosAndRot(target, normal, position) == null || !(Math.abs(normal.determinant() - 1) <= 0.01f) || !Float.isFinite(position.lengthSquared())) continue;
            pos = new Vec3d(position).add(offset);
            currentTarget = target;
            break;
        }
        if (currentTarget.isEmpty()) {
            Entity player = MinecraftClient.getInstance().player;
            if (readyToSendMessage && player != null) player.sendMessage(LocUtil.MESSAGE("bindingFailed", LocUtil.MOD_NAME(), LocUtil.MODEL_VIEW_TITLE()));
            active = readyToSendMessage = false;
        } else readyToSendMessage = true;
        normal.rotateLocal((float) Math.toRadians(currentTarget.getYaw()), normal.m10, normal.m11, normal.m12);
        normal.rotateLocal((float) Math.toRadians(currentTarget.getPitch()), normal.m00, normal.m01, normal.m02);
        normal.rotateLocal((float) Math.toRadians(currentTarget.getRoll()), normal.m20, normal.m21, normal.m22);
        Vec3d eulerAngle = MathUtil.getEulerAngleYXZ(normal).multiply(Math.toDegrees(1));
        pitch = (float) eulerAngle.getX();
        yaw = (float) -eulerAngle.getY();
        roll = (float) eulerAngle.getZ();
    }
}
