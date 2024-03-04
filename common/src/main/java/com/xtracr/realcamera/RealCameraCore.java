package com.xtracr.realcamera;

import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.util.MathUtil;
import com.xtracr.realcamera.util.VertexRecorder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.function.BiFunction;

public class RealCameraCore {
    private static final VertexRecorder recorder = new VertexRecorder();
    public static BindingTarget currentTarget = new BindingTarget();
    private static Vec3d pos = Vec3d.ZERO, cameraPos = Vec3d.ZERO;
    private static boolean renderingPlayer = false;
    private static boolean active = false;
    private static float pitch, yaw, roll;

    public static boolean isRenderingPlayer() {
        return renderingPlayer;
    }

    public static float getPitch(float f) {
        if (currentTarget.bindRotation()) return pitch;
        return f;
    }

    public static float getYaw(float f) {
        if (currentTarget.bindRotation()) return yaw;
        return f;
    }

    public static float getRoll(float f) {
        if (config().isClassic()) return f + config().getClassicRoll();
        if (currentTarget.bindRotation()) return roll;
        return f;
    }

    public static Vec3d getPos(Vec3d vec3d) {
        return new Vec3d(currentTarget.bindX() ? pos.getX() : vec3d.getX(),
                currentTarget.bindY() ? pos.getY() : vec3d.getY(),
                currentTarget.bindZ() ? pos.getZ() : vec3d.getZ());
    }

    public static Vec3d getCameraPos(Vec3d vec3d) {
        return new Vec3d(currentTarget.bindX() ? cameraPos.getX() : vec3d.getX(),
                currentTarget.bindY() ? cameraPos.getY() : vec3d.getY(),
                currentTarget.bindZ() ? cameraPos.getZ() : vec3d.getZ());
    }

    public static void setCameraPos(Vec3d vec3d) {
        cameraPos = vec3d;
    }

    public static void init(MinecraftClient client) {
        active = config().isEnabled() && client.options.getPerspective().isFirstPerson() && client.gameRenderer.getCamera() != null && client.player != null;
    }

    public static boolean isActive() {
        return active;
    }

    public static void renderPlayer(VertexConsumerProvider vertexConsumers) {
        Matrix3f normalMatrix = new Matrix3f().rotate(RotationAxis.POSITIVE_Z.rotationDegrees(roll))
                .rotate(RotationAxis.POSITIVE_X.rotationDegrees(pitch))
                .rotate(RotationAxis.POSITIVE_Y.rotationDegrees(yaw + 180.0f))
                .transpose().invert();
        Matrix4f positionMatrix = new Matrix4f(normalMatrix).translate((float) -pos.getX(), (float) -pos.getY(), (float) -pos.getZ());
        BiFunction<RenderLayer, VertexRecorder.Vertex[], VertexRecorder.Vertex[]> function = (renderLayer, vertices) -> {
            double depth = currentTarget.disablingDepth(), centerZ = 0;
            int count = vertices.length;
            VertexRecorder.Vertex[] quad = new VertexRecorder.Vertex[count];
            for (int i = 0; i < count; i++) quad[i] = vertices[i].transform(positionMatrix, normalMatrix);
            for (VertexRecorder.Vertex vertex : quad) {
                if (vertex.z() < -depth) return quad;
                centerZ += vertex.z();
            }
            return centerZ < -depth * count ? quad : null;
        };
        recorder.drawByAnother(vertexConsumers, renderLayer -> true, function);
    }

    public static void computeCamera(MinecraftClient client, float tickDelta) {
        currentTarget = new BindingTarget();
        if (config().isClassic()) return;

        // GameRenderer.renderWorld
        recorder.clear();
        ClientPlayerEntity player = client.player;
        // WorldRenderer.render
        if (player.age == 0) {
            player.lastRenderX = player.getX();
            player.lastRenderY = player.getY();
            player.lastRenderZ = player.getZ();
        }
        // WorldRenderer.renderEntity
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        dispatcher.configure(client.world, client.gameRenderer.getCamera(), player);
        renderingPlayer = true;
        dispatcher.render(player, MathHelper.lerp(tickDelta, player.lastRenderX, player.getX()),
                MathHelper.lerp(tickDelta, player.lastRenderY, player.getY()),
                MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ()),
                MathHelper.lerp(tickDelta, player.prevYaw, player.getYaw()),
                tickDelta, new MatrixStack(), recorder, dispatcher.getLight(player, tickDelta));
        renderingPlayer = false;
        recorder.buildLastRecord();

        Matrix3f normal = new Matrix3f();
        for (BindingTarget target : config().getTargetList()) {
            try {
                if (!recorder.setCurrent(renderLayer -> renderLayer.toString().contains(target.textureId()))) continue;
                pos = recorder.getTargetPosAndRot(target, normal);
                currentTarget = target;
                break;
            } catch (Exception ignored) {
            }
        }
        if (currentTarget.isEmpty()) active = false;
        normal.rotateLocal((float) Math.toRadians(currentTarget.yaw()), normal.m10, normal.m11, normal.m12);
        normal.rotateLocal((float) Math.toRadians(currentTarget.pitch()), normal.m00, normal.m01, normal.m02);
        normal.rotateLocal((float) Math.toRadians(currentTarget.roll()), normal.m20, normal.m21, normal.m22);
        Vec3d eulerAngle = MathUtil.getEulerAngleYXZ(normal).multiply(Math.toDegrees(1));
        pitch = (float) eulerAngle.getX();
        yaw = (float) -eulerAngle.getY();
        roll = (float) eulerAngle.getZ();
    }

    private static ModConfig config() {
        return ConfigFile.modConfig;
    }
}
