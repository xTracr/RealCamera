package com.xtracr.realcamera;

import com.xtracr.realcamera.api.VirtualRenderer;
import com.xtracr.realcamera.compat.PehkuiCompat;
import com.xtracr.realcamera.compat.PhysicsModCompat;
import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.mixin.PlayerEntityRendererAccessor;
import com.xtracr.realcamera.util.MathUtil;
import com.xtracr.realcamera.util.VertexRecorder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

public class RealCameraCore {
    private static final VertexRecorder recorder = new VertexRecorder();
    public static BindingTarget currentTarget = new BindingTarget();
    private static String status = "Successful";
    private static boolean renderingPlayer = false;
    private static boolean active = false;
    private static float pitch, yaw, roll;
    private static Vec3d pos = Vec3d.ZERO, cameraPos = Vec3d.ZERO;

    public static String getStatus() {
        return status;
    }

    public static boolean isRenderingPlayer() {
        return renderingPlayer;
    }

    public static void setRenderingPlayer(boolean value) {
        renderingPlayer = value;
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
        active = config().isEnabled() && client.options.getPerspective().isFirstPerson() && client.gameRenderer.getCamera() != null
                && client.player != null && !config().shouldDisableMod(client);
    }

    public static boolean isActive() {
        return active;
    }

    public static void renderPlayer(VertexConsumerProvider vertexConsumers) {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw + 180.0f));
        Matrix3f normalMatrix = matrixStack.peek().getNormalMatrix().transpose().invert();
        Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix().transpose().invertAffine()
                .translate((float) -pos.getX(), (float) -pos.getY(), (float) -pos.getZ());
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
        roll = config().getClassicRoll();
        currentTarget = new BindingTarget();
        if (config().isClassic()) return;

        // GameRenderer.renderWorld
        recorder.clear();
        MatrixStack matrixStack = new MatrixStack();
        virtualRender(client, tickDelta, matrixStack);
        recorder.buildLastRecord();

        // ModelPart$Cuboid.renderCuboid
        Vector4f offset = matrixStack.peek().getPositionMatrix().transform(new Vector4f((float) (config().getBindingZ() * config().getScale()),
                -(float) (config().getBindingY() * config().getScale()) - 0.125f,
                -(float) (config().getBindingX() * config().getScale()) - 0.225f, 1.0f));
        pos = new Vec3d(offset.x(), offset.y(), offset.z());
        Matrix3f normal = matrixStack.peek().getNormalMatrix().scale(1.0f, -1.0f, -1.0f);
        if (config().binding.experimental) {
            List<BindingTarget> targetList = new ArrayList<>();
            if (config().binding.autoBind) {
                Collection<BindingTarget> targetSet = config().binding.targetMap.values();
                recorder.setCurrent(renderLayer -> targetSet.stream().anyMatch(t -> renderLayer.toString().contains(t.textureId())));
                String textureId = recorder.currentTextureId();
                if (textureId != null) targetList.addAll(targetSet.stream().filter(t -> textureId.contains(t.textureId())).toList());
            }
            targetList.add(config().getTarget(config().binding.nameOfList));
            for (BindingTarget target : targetList) {
                try {
                    recorder.setCurrent(renderLayer -> renderLayer.toString().contains(target.textureId()));
                    if (recorder.quadCount() <= 0) throw new NullPointerException("Vertices not found");
                    pos = recorder.getTargetPosAndRot(target, normal);
                    currentTarget = target;
                    break;
                } catch (Exception ignored) {
                }
            }
        }

        normal.rotateLocal((float) Math.toRadians(currentTarget.yaw()), normal.m10, normal.m11, normal.m12);
        normal.rotateLocal((float) Math.toRadians(currentTarget.pitch()), normal.m00, normal.m01, normal.m02);
        normal.rotateLocal((float) Math.toRadians(currentTarget.roll()), normal.m20, normal.m21, normal.m22);
        Vec3d eulerAngle = MathUtil.getEulerAngleYXZ(normal).multiply(180.0d / Math.PI);
        pitch = (float) eulerAngle.getX();
        yaw = (float) -eulerAngle.getY();
        roll = (float) eulerAngle.getZ();
    }

    private static ModConfig config() {
        return ConfigFile.modConfig;
    }

    private static void virtualRender(MinecraftClient client, float tickDelta, MatrixStack matrixStack) {
        ClientPlayerEntity player = client.player;
        // WorldRenderer.render
        if (player.age == 0) {
            player.lastRenderX = player.getX();
            player.lastRenderY = player.getY();
            player.lastRenderZ = player.getZ();
        }
        // WorldRenderer.renderEntity
        Vec3d renderOffset = new Vec3d(MathHelper.lerp(tickDelta, player.lastRenderX, player.getX()),
                MathHelper.lerp(tickDelta, player.lastRenderY, player.getY()),
                MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ()));
        matrixStack.push();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        dispatcher.configure(client.world, client.gameRenderer.getCamera(), player);
        if (config().binding.experimental) dispatcher.render(player, renderOffset.getX(), renderOffset.getY(), renderOffset.getZ(),
                MathHelper.lerp(tickDelta, player.prevYaw, player.getYaw()), tickDelta, matrixStack, recorder, dispatcher.getLight(player, tickDelta));
        matrixStack.pop();
        // EntityRenderDispatcher.render
        if (config().compatPhysicsMod())
            PhysicsModCompat.renderStart(client.getEntityRenderDispatcher(), player, renderOffset.getX(), renderOffset.getY(),
                    renderOffset.getZ(), MathHelper.lerp(tickDelta, player.prevYaw, player.getYaw()), tickDelta, matrixStack);

        PlayerEntityRenderer playerRenderer = (PlayerEntityRenderer) client.getEntityRenderDispatcher().getRenderer(player);
        renderOffset = renderOffset.add(playerRenderer.getPositionOffset(player, tickDelta));
        matrixStack.translate(renderOffset.getX(), renderOffset.getY(), renderOffset.getZ());

        if (config().compatPehkui()) PehkuiCompat.scaleMatrices(matrixStack, player, tickDelta);

        if (config().isUsingModModel()) {
            status = "Successful";
            try {
                matrixStack.push();
                if (!VirtualRenderer.virtualRender(tickDelta, matrixStack)) {
                    return;
                }
            } catch (Throwable throwable) {
                status = throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getName();
                matrixStack.pop();
            }
        }

        // PlayerEntityRenderer.render
        ((PlayerEntityRendererAccessor) playerRenderer).invokeSetModelPose(player);
        // LivingEntityRenderer.render
        PlayerEntityModel<AbstractClientPlayerEntity> playerModel = playerRenderer.getModel();
        float n;
        Direction direction;
        playerModel.handSwingProgress = player.getHandSwingProgress(tickDelta);
        playerModel.riding = player.hasVehicle();
        playerModel.child = player.isBaby();
        float h = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        float j = MathHelper.lerpAngleDegrees(tickDelta, player.prevHeadYaw, player.headYaw);
        float k = j - h;
        if (player.hasVehicle() && player.getVehicle() instanceof LivingEntity vehicle) {
            h = MathHelper.lerpAngleDegrees(tickDelta, vehicle.prevBodyYaw, vehicle.bodyYaw);
            k = j - h;
            float l = MathHelper.wrapDegrees(k);
            if (l < -85.0f) l = -85.0f;
            else if (l >= 85.0f) l = 85.0f;
            h = j - l;
            if (l * l > 2500.0f) h += l * 0.2f;
            k = j - h;
        }
        float m = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());
        if (PlayerEntityRenderer.shouldFlipUpsideDown(player)) {
            m *= -1.0f;
            k *= -1.0f;
        }
        if (player.isInPose(EntityPose.SLEEPING) && (direction = player.getSleepingDirection()) != null) {
            n = player.getEyeHeight(EntityPose.STANDING) - 0.1f;
            matrixStack.translate((float) (-direction.getOffsetX()) * n, 0.0f, (float) (-direction.getOffsetZ()) * n);
        }
        float l = player.age + tickDelta;
        ((PlayerEntityRendererAccessor) playerRenderer).invokeSetupTransforms(player, matrixStack, l, h, tickDelta);
        matrixStack.scale(-1.0f, -1.0f, 1.0f);
        ((PlayerEntityRendererAccessor) playerRenderer).invokeScale(player, matrixStack, tickDelta);
        matrixStack.translate(0.0f, -1.501f, 0.0f);
        n = 0.0f;
        float o = 0.0f;
        if (!player.hasVehicle() && player.isAlive()) {
            n = player.limbAnimator.getSpeed(tickDelta);
            o = player.limbAnimator.getPos(tickDelta);
            if (player.isBaby()) o *= 3.0f;
            if (n > 1.0f) n = 1.0f;
        }
        playerModel.animateModel(player, o, n, tickDelta);
        playerModel.setAngles(player, o, n, l, k, m);
        // AnimalModel.render
        // ModelPart.render
        config().getVanillaModelPart().get(playerRenderer.getModel()).rotate(matrixStack);
    }
}
