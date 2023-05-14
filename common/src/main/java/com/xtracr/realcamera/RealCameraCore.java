package com.xtracr.realcamera;

import com.xtracr.realcamera.api.VirtualRenderer;
import com.xtracr.realcamera.compat.PehkuiCompat;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.mixins.CameraAccessor;
import com.xtracr.realcamera.mixins.PlayerEntityRendererAccessor;
import com.xtracr.realcamera.utils.MathUtils;
import com.xtracr.realcamera.utils.Matrix3fc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vector4f;
import net.minecraft.world.RaycastContext;

public class RealCameraCore {

    private static final ModConfig config = ConfigFile.modConfig;

    private static float cameraRoll = 0.0F;

    public static String status = "Successful";
    public static boolean isRenderingWorld = false;

    public static float getRoll() {
        return cameraRoll;
    }

    public static boolean isActive() {
        MinecraftClient client = MinecraftClient.getInstance();
        return config.isEnabled() && client.options.getPerspective().isFirstPerson() && client.gameRenderer.getCamera() != null 
            && client.player != null && !config.isDisabledWhen(client.player);
    }

    public static void updateCamera(Camera camera, MinecraftClient client, float tickDelta) {
        cameraRoll = 0.0F;

        if (config.isRendering() && !config.onlyDisableRenderingWhen(client.player)) {
            ((CameraAccessor)camera).setThirdPerson(true);
        }

        if (config.isClassic()) {
            classicModeUpdate(camera, client, tickDelta);
        } else {
            bindingModeUpdate(camera, client, tickDelta);
        }
    }

    private static void classicModeUpdate(Camera camera, MinecraftClient client, float tickDelta) {
        CameraAccessor cameraAccessor = (CameraAccessor)camera;
        ClientPlayerEntity player = client.player;

        float centerYaw = camera.getYaw();
        float pitch = camera.getPitch() + config.getClassicPitch();
        float yaw = centerYaw - config.getClassicYaw();
        cameraRoll = config.getClassicRoll();
        Vec3d refer = new Vec3d(config.getClassicRX(), config.getClassicRY(), config.getClassicRZ()).multiply(config.getScale());
        Vec3d offset = new Vec3d(config.getClassicX(), config.getClassicY(), config.getClassicZ()).multiply(config.getScale());
        Vec3d center = new Vec3d(config.getCenterX(), config.getCenterY(), config.getCenterZ()).multiply(config.getScale());

        if (player.isSneaking()) {
            center = center.add(0.0D, -0.021875D, 0.0D);
        }
        if (config.compatPehkui()) {
            refer = PehkuiCompat.scaleVec3d(refer, player, tickDelta);
            offset = PehkuiCompat.scaleVec3d(offset, player, tickDelta);
            center = PehkuiCompat.scaleVec3d(center, player, tickDelta);
        }
        if (player.isInSwimmingPose()) {
            refer = refer.rotateZ((float)Math.PI/2);
            offset = offset.rotateZ((float)Math.PI/2);
            center = center.rotateZ((float)Math.PI/2);
        }

        cameraAccessor.invokeSetRotation(centerYaw, 0.0F);
        cameraAccessor.invokeMoveBy(center.getX(), center.getY(), center.getZ());
        cameraAccessor.invokeSetRotation(yaw, pitch);
        offset = offset.subtract(refer);
        cameraAccessor.invokeMoveBy(refer.getX(), refer.getY(), refer.getZ());
        Vec3d referVec = camera.getPos();
        cameraAccessor.invokeMoveBy(offset.getX(), offset.getY(), offset.getZ());
        clipCameraToSpace(camera, referVec);
    }

    private static void bindingModeUpdate(Camera camera, MinecraftClient client, float tickDelta) {

        // GameRenderer.render
        MatrixStack matrixStack = new MatrixStack();
        // GameRenderer.renderWorld
        matrixStack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(camera.getPitch()));
        matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(camera.getYaw() + 180.0F));
        matrixStack.peek().getNormalMatrix().loadIdentity();
        // WorldRenderer.render
        ClientPlayerEntity player = client.player;
        if (player.age == 0) {
            player.lastRenderX = player.getX();
            player.lastRenderY = player.getY();
            player.lastRenderZ = player.getZ();
        }
        // WorldRenderer.renderEntity
        Vec3d renderOffset = new Vec3d(MathHelper.lerp(tickDelta, player.lastRenderX, player.getX()), 
            MathHelper.lerp(tickDelta, player.lastRenderY, player.getY()), 
            MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ()))
            .subtract(camera.getPos());
        // EntityRenderDispatcher.render
        PlayerEntityRenderer playerRenderer = (PlayerEntityRenderer)client.getEntityRenderDispatcher().getRenderer(player);
        renderOffset = renderOffset.add(playerRenderer.getPositionOffset(player, tickDelta));
        matrixStack.translate(renderOffset.getX(), renderOffset.getY(), renderOffset.getZ());

        if (config.compatPehkui()) PehkuiCompat.scaleMatrices(matrixStack, player, tickDelta);

        virtualRender(player, playerRenderer, tickDelta, matrixStack);

        // ModelPart$Cuboid.renderCuboid
        Vector4f refer = new Vector4f((float)(config.getBindingRZ() * config.getScale()), 
            -(float)(config.getBindingRY() * config.getScale()), 
            -(float)(config.getBindingRX() * config.getScale()), 1.0F);
        Vector4f offset = new Vector4f((float)(config.getBindingZ() * config.getScale()), 
            -(float)(config.getBindingY() * config.getScale()), 
            -(float)(config.getBindingX() * config.getScale()), 1.0F);
        refer.transform(matrixStack.peek().getPositionMatrix());
        offset.transform(matrixStack.peek().getPositionMatrix());

        offset.add(-refer.getX(), -refer.getY(), -refer.getZ(), 0.0F);
        ((CameraAccessor)camera).invokeMoveBy(-refer.getZ(), refer.getY(), -refer.getX());
        Vec3d referVec = camera.getPos();
        ((CameraAccessor)camera).invokeMoveBy(-offset.getZ(), offset.getY(), -offset.getX());
        clipCameraToSpace(camera, referVec);

        if (config.isRotationBound()) {
            Matrix3fc normal = new Matrix3fc(matrixStack.peek().getNormalMatrix()).scale(1.0F, -1.0F, -1.0F);
            normal.rotateLocal((float)Math.toRadians(config.getBindingYaw()), normal.m10, normal.m11, normal.m12);
            normal.rotateLocal((float)Math.toRadians(config.getBindingPitch()), normal.m00, normal.m01, normal.m02);
            normal.rotateLocal((float)Math.toRadians(config.getBindingRoll()), normal.m20, normal.m21, normal.m22);
            Vec3d eulerAngle = MathUtils.getEulerAngleYXZ(normal).multiply(180.0D/Math.PI);

            ((CameraAccessor)camera).invokeSetRotation((float)-eulerAngle.getY(), (float)eulerAngle.getX());
            if (!config.isRollingLocked()) {
                cameraRoll = (float)eulerAngle.getZ();
            }
        }

    }

    private static void clipCameraToSpace(Camera camera, Vec3d referVec) {
        if (!config.doClipToSpace()) return;
        Vec3d offset = camera.getPos().subtract(referVec);
        final float depth = 0.075f;
        for (int i = 0; i < 8; ++i) {
            float f = depth * ((i & 1) * 2 - 1);
            float g = depth * ((i >> 1 & 1) * 2 - 1);
            float h = depth * ((i >> 2 & 1) * 2 - 1);
            Vec3d start = referVec;
            Vec3d end = referVec.add(offset).add(f, g, h);
            HitResult hitResult = ((CameraAccessor)camera).getArea().raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, camera.getFocusedEntity()));
            double l = hitResult.getPos().distanceTo(referVec);
            if (hitResult.getType() == HitResult.Type.MISS || l >= offset.length()) continue;
            offset = offset.multiply(l/offset.length());
        }
        ((CameraAccessor)camera).invokeSetPos(referVec.add(offset));
    }

    private static void virtualRender(AbstractClientPlayerEntity player, PlayerEntityRenderer playerRenderer, 
            float tickDelta, MatrixStack matrixStack) {

        if (config.isUsingModModel()) {
            status = "Successful";
            try {
                matrixStack.push();
                if (!VirtualRenderer.virtualRender(tickDelta, matrixStack)) {
                    return;
                }
            } catch (Exception exception) {
                status = exception.getMessage();
                matrixStack.pop();
            }
        }

        // PlayerEntityRenderer.render
        ((PlayerEntityRendererAccessor)playerRenderer).invokeSetModelPose(player);
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
        if (player.hasVehicle() && player.getVehicle() instanceof LivingEntity) {
            LivingEntity vehicle = (LivingEntity)player.getVehicle();
            h = MathHelper.lerpAngleDegrees(tickDelta, vehicle.prevBodyYaw, vehicle.bodyYaw);
            k = j - h;
            float l = MathHelper.wrapDegrees(k);
            if (l < -85.0f) {
                l = -85.0f;
            }
            if (l >= 85.0f) {
                l = 85.0f;
            }
            h = j - l;
            if (l * l > 2500.0f) {
                h += l * 0.2f;
            }
            k = j - h;
        }
        float m = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());
        if (PlayerEntityRenderer.shouldFlipUpsideDown(player)) {
            m *= -1.0f;
            k *= -1.0f;
        }
        if (player.getPose() == EntityPose.SLEEPING && (direction = player.getSleepingDirection()) != null) {
            n = player.getEyeHeight(EntityPose.STANDING) - 0.1f;
            matrixStack.translate((float)(-direction.getOffsetX()) * n, 0.0f, (float)(-direction.getOffsetZ()) * n);
        }
        float l = player.age + tickDelta;
        ((PlayerEntityRendererAccessor)playerRenderer).invokeSetupTransforms(player, matrixStack, l, h, tickDelta);
        matrixStack.scale(-1.0f, -1.0f, 1.0f);
        ((PlayerEntityRendererAccessor)playerRenderer).invokeScale(player, matrixStack, tickDelta);
        matrixStack.translate(0.0f, -1.501f, 0.0f);
        n = 0.0f;
        float o = 0.0f;
        if (!player.hasVehicle() && player.isAlive()) {
            n = MathHelper.lerp(tickDelta, player.lastLimbDistance, player.limbDistance);
            o = player.limbAngle - player.limbDistance * (1.0f - tickDelta);
            if (player.isBaby()) {
                o *= 3.0f;
            }
            if (n > 1.0f) {
                n = 1.0f;
            }
        }
        playerModel.animateModel(player, o, n, tickDelta);
        playerModel.setAngles(player, o, n, l, k, m);
        // AnimalModel.render
        // ModelPart.render
        config.getVanillaModelPart().get(playerRenderer.getModel()).rotate(matrixStack);
    }
}