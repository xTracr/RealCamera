package com.xtracr.realcamera;

import com.xtracr.realcamera.api.VirtualRenderer;
import com.xtracr.realcamera.compat.PehkuiCompat;
import com.xtracr.realcamera.compat.PhysicsModCompat;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.mixin.PlayerEntityRendererAccessor;
import com.xtracr.realcamera.util.MathUtil;
import com.xtracr.realcamera.util.VertexRecorder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class RealCameraCore {
    private static final ModConfig config = ConfigFile.modConfig;
    private static VertexRecorder recorder = new VertexRecorder();
    private static String status = "Successful";
    private static boolean renderingPlayer = false;
    private static boolean active = false;
    private static float pitch, yaw, roll;
    private static Vec3d pos = Vec3d.ZERO;
    private static Vec3d modelOffset = Vec3d.ZERO;

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
        if (config.isPitchingBound()) return pitch;
        else return f + config.getBindingPitch();
    }

    public static float getYaw(float f) {
        if (config.isYawingBound()) return yaw;
        else return f - config.getBindingYaw();
    }

    public static float getRoll() {
        return roll;
    }

    public static Vec3d getPos() {
        return pos;
    }

    public static Vec3d getModelOffset() {
        return modelOffset;
    }

    public static void setModelOffset(Vec3d vec3d) {
        modelOffset = vec3d;
    }

    public static void init(MinecraftClient client) {
        active = config.isEnabled() && client.options.getPerspective().isFirstPerson() && client.gameRenderer.getCamera() != null
                && client.player != null && !config.shouldDisableMod(client);
    }

    public static boolean isActive() {
        return active;
    }

    public static void renderPlayer(Vec3d offset, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        matrices.push();
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix().transpose().invertAffine()
                .translate((float) -offset.getX(), (float) -offset.getY(), (float) -offset.getZ());
        Matrix3f normalMatrix = matrices.peek().getNormalMatrix().transpose().invert();
        VertexRecorder.VertexPredicate predicate = (renderLayer, vertices, index) -> {
            Vec3d center = Vec3d.ZERO;
            double depth = config.disable.depth;
            for (VertexRecorder.Vertex vertex : vertices) {
                center = center.add(vertex.pos());
                if (vertex.z() < -depth) return true;
            }
            return center.getZ() < -depth * vertices.length;
        };
        recorder.drawByAnother(vertex -> vertex.transform(positionMatrix, normalMatrix), vertexConsumers, renderLayer -> true, predicate);
        matrices.pop();
    }

    public static void computeCamera(MinecraftClient client, float tickDelta) {
        modelOffset = Vec3d.ZERO;
        roll = config.getClassicRoll();
        if (config.isClassic()) return;

        // GameRenderer.renderWorld
        MatrixStack matrixStack = new MatrixStack();
        recorder = new VertexRecorder();
        virtualRender(client, tickDelta, matrixStack, recorder);
        recorder.buildLastRecord();

        // ModelPart$Cuboid.renderCuboid
        Vector4f offset = matrixStack.peek().getPositionMatrix().transform(new Vector4f((float) (config.getBindingZ() * config.getScale()),
                -(float) (config.getBindingY() * config.getScale()) - 0.125f,
                -(float) (config.getBindingX() * config.getScale()) - 0.225f, 1.0F));
        pos = new Vec3d(offset.x(), offset.y(), offset.z());
        Matrix3f normal = matrixStack.peek().getNormalMatrix().scale(1.0F, -1.0F, -1.0F);
        if (config.binding.experimental) try {
            ModConfig.Binding.Target target = config.binding.targetMap.get(config.binding.nameOfList);
            recorder.setCurrent(renderLayer -> renderLayer.toString().equals(target.renderTypeName()), 0); // TODO
            if (recorder.quadCount() <= 0) throw new NullPointerException("Vertices not found");
            Vec3d front = recorder.getNormal(target.frontIndex());
            Vec3d up = recorder.getNormal(target.upIndex());
            Vec3d center = recorder.getCenter(target.posIndex());
            if (!MathUtil.isFinite(front) || !MathUtil.isFinite(up) || !MathUtil.isFinite(center)) throw new ArithmeticException();
            normal.set(up.crossProduct(front).toVector3f(), up.toVector3f(), front.toVector3f());
            Vector3f vec3f = normal.transform(new Vector3f((float) (config.getBindingZ() * config.getScale()),
                    (float) (config.getBindingY() * config.getScale()),
                    (float) (config.getBindingX() * config.getScale())));
            pos = center.add(vec3f.x(), vec3f.y(), vec3f.z());
        } catch (Exception ignored) {
        }

        normal.rotateLocal((float) Math.toRadians(config.getBindingYaw()), normal.m10, normal.m11, normal.m12);
        normal.rotateLocal((float) Math.toRadians(config.getBindingPitch()), normal.m00, normal.m01, normal.m02);
        normal.rotateLocal((float) Math.toRadians(config.getBindingRoll()), normal.m20, normal.m21, normal.m22);
        Vec3d eulerAngle = MathUtil.getEulerAngleYXZ(normal).multiply(180.0D / Math.PI);
        pitch = (float) eulerAngle.getX();
        yaw = (float) -eulerAngle.getY();
        roll = config.isRollingBound() ? (float) eulerAngle.getZ() : config.getBindingRoll();
    }

    private static void virtualRender(MinecraftClient client, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider consumers) {
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
        if (config.binding.experimental) dispatcher.render(player, renderOffset.getX(), renderOffset.getY(), renderOffset.getZ(),
                MathHelper.lerp(tickDelta, player.prevYaw, player.getYaw()), tickDelta, matrixStack, consumers, dispatcher.getLight(player, tickDelta));
        matrixStack.pop();
        // EntityRenderDispatcher.render
        if (config.compatPhysicsMod())
            PhysicsModCompat.renderStart(client.getEntityRenderDispatcher(), player, renderOffset.getX(), renderOffset.getY(),
                    renderOffset.getZ(), MathHelper.lerp(tickDelta, player.prevYaw, player.getYaw()), tickDelta, matrixStack);

        PlayerEntityRenderer playerRenderer = (PlayerEntityRenderer) client.getEntityRenderDispatcher().getRenderer(player);
        renderOffset = renderOffset.add(playerRenderer.getPositionOffset(player, tickDelta));
        matrixStack.translate(renderOffset.getX(), renderOffset.getY(), renderOffset.getZ());

        if (config.compatPehkui()) PehkuiCompat.scaleMatrices(matrixStack, player, tickDelta);

        if (config.isUsingModModel()) {
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
        config.getVanillaModelPart().get(playerRenderer.getModel()).rotate(matrixStack);
    }
}
