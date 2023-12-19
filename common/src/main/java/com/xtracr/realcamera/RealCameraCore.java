package com.xtracr.realcamera;

import com.xtracr.realcamera.api.VirtualRenderer;
import com.xtracr.realcamera.compat.PehkuiCompat;
import com.xtracr.realcamera.compat.PhysicsModCompat;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.mixins.PlayerEntityRendererAccessor;
import com.xtracr.realcamera.utils.MathUtils;
import com.xtracr.realcamera.utils.VertexDataAnalyser;
import com.xtracr.realcamera.utils.VertexDataCatcher;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
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
import org.joml.Vector4f;

import java.util.*;

public class RealCameraCore {
    private static final ModConfig config = ConfigFile.modConfig;
    private static final List<Integer> normalList = new ArrayList<>();
    private static final List<Integer> posList = new ArrayList<>();
    private static String status = "Successful";
    private static boolean vRendering = false;
    private static float pitch = 0.0F;
    private static float yaw = 0.0F;
    private static float roll = 0.0F;
    private static Vec3d pos = Vec3d.ZERO;
    private static Vec3d modelOffset = Vec3d.ZERO;

    public static String getStatus() {
        return status;
    }

    public static boolean isvRendering() {
        return vRendering;
    }

    public static float getPitch() {
        return pitch;
    }

    public static float getYaw() {
        return yaw;
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

    public static boolean isActive() {
        MinecraftClient client = MinecraftClient.getInstance();
        return config.isEnabled() && client.options.getPerspective().isFirstPerson() && client.gameRenderer.getCamera() != null
                && client.player != null && !config.shouldDisableMod(client);
    }

    public static void computeCamera(MinecraftClient client, float tickDelta) {
        modelOffset = Vec3d.ZERO;
        roll = config.getClassicRoll();
        if (config.isClassic()) return;

        // GameRenderer.renderWorld
        MatrixStack matrixStack = new MatrixStack();
        vRendering = true;
        VertexDataCatcher catcher = setupCatcher();
        virtualRender(client, tickDelta, matrixStack, catcher);
        vRendering = false;

        // ModelPart$Cuboid.renderCuboid
        Vector4f offset = matrixStack.peek().getPositionMatrix().transform(new Vector4f(0, 0, 0, 1.0F));
        pos = new Vec3d(offset.x(), offset.y(), offset.z());
        Matrix3f normal = matrixStack.peek().getNormalMatrix().scale(1.0F, -1.0F, -1.0F);
        if (!VertexDataAnalyser.isAnalysing() && config.binding.experimental) try {
            applyAnalysisResult(normal, catcher);
        } catch (Exception ignored) {
        }

        normal.rotateLocal((float) Math.toRadians(config.getBindingYaw()), normal.m10, normal.m11, normal.m12);
        normal.rotateLocal((float) Math.toRadians(config.getBindingPitch()), normal.m00, normal.m01, normal.m02);
        normal.rotateLocal((float) Math.toRadians(config.getBindingRoll()), normal.m20, normal.m21, normal.m22);
        Vec3d eulerAngle = MathUtils.getEulerAngleYXZ(normal).multiply(180.0D / Math.PI);
        pitch = (float) eulerAngle.getX();
        yaw = (float) -eulerAngle.getY();
        roll = config.isRollingBound() ? (float) eulerAngle.getZ() : config.getBindingRoll();
    }

    private static VertexDataCatcher setupCatcher() {
        normalList.clear();
        posList.clear();
        if (VertexDataAnalyser.isAnalysing()) return VertexDataAnalyser.catcher;
        if (config.binding.experimental) try {
            List<Integer> list = config.binding.indexListMap.get(config.binding.nameOfList);
            posList.addAll(list.subList(3, list.size()));
            int leftSgn = list.get(2) >= 0 ? 1 : -1;
            int upSgn = list.get(1) >= 0 ? 1 : -1;
            int leftIndex = list.get(2) * leftSgn + (leftSgn - 1) / 2;
            int upIndex = list.get(1) * upSgn + (upSgn - 1) / 2;
            normalList.addAll(List.of(list.get(0), upIndex, leftIndex));
            if (upSgn == -1) normalList.add(-1);
            if (leftSgn == -1) normalList.add(-2);
        } catch (Exception ignored) {
        }
        return new VertexDataCatcher(normalList::contains, posList::contains);
    }

    private static void applyAnalysisResult(Matrix3f normal, VertexDataCatcher catcher) {
        if (catcher.posRecorder.isEmpty()) throw new NullPointerException("Target vertices not found");
        Vec3d average = Vec3d.ZERO;
        for (Vec3d vec : catcher.posRecorder) {
            average = average.add(vec);
        }
        pos = average.multiply(1 / (double) catcher.posRecorder.size());
        List<Integer> sorted = new ArrayList<>(List.copyOf(normalList.subList(0, 3)));
        List<Integer> order = new ArrayList<>();
        sorted.sort(Comparator.comparingInt(i -> i));
        order.add(sorted.indexOf(normalList.get(0)));
        order.add(sorted.indexOf(normalList.get(1)));
        order.add(sorted.indexOf(normalList.get(2)));
        normal.set(catcher.normalRecorder.get(order.get(2)).multiply(normalList.contains(-2) ? -1 : 1).toVector3f(),
                catcher.normalRecorder.get(order.get(1)).multiply(normalList.contains(-1) ? -1 : 1).toVector3f(),
                catcher.normalRecorder.get(order.get(0)).toVector3f());
    }

    private static void virtualRender(MinecraftClient client, float tickDelta, MatrixStack matrixStack, VertexDataCatcher catcher) {
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
        if (VertexDataAnalyser.preAnalysing() || config.binding.experimental) dispatcher.render(player, renderOffset.getX(),
                renderOffset.getY(), renderOffset.getZ(), 0, tickDelta, matrixStack, layer -> catcher, 0xF000F0);
        VertexDataAnalyser.analyse(player, tickDelta);
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
