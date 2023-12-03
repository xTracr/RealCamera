package com.xtracr.realcamera.api;

import com.xtracr.realcamera.mixins.PlayerEntityRendererAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * This example uses {@code reflection} to {@code register} and {@code getModelPartName}.
 *
 * <p>If you don't want to use reflection, an (optional) dependency should be added to call the methods.
 * Don't know how to add a dependency? Go to <a href="https://jitpack.io">jitpack.io</a>
 * or <a href="https://jitpack.io/#xTracr/RealCamera/">jitpack.io/#xTracr/RealCamera/</a>
 * for information about it.</p>
 */
public class CompatExample {
    /**
     * <b>mandatory</b>
     */
    public static final String modid = "minecraft";

    private static final Map<String, String> nameMap = new HashMap<>();
    /**
     * {@code = VirtualRenderer.class.getDeclaredMethod("getModelPartName")}
     *
     * <p>return the value of {@link com.xtracr.realcamera.config.ModConfig.Compats#modModelPart modModelPart}
     * option in the config.</p>
     *
     * @see #register()
     * @see VirtualRenderer#getModelPartName() getModelPartName()
     */
    private static Method getModelPartNameMethod;

    static {
        // These data were obtained from the mapping file.
        try {
            // Fabric
            Class.forName("net.fabricmc.loader.api.FabricLoader");
            nameMap.put("head", "field_3398");
            nameMap.put("hat", "field_3394");
            nameMap.put("body", "field_3391");
            nameMap.put("rightArm", "field_3401");
            nameMap.put("leftArm", "field_27433");
            nameMap.put("rightLeg", "field_3392");
            nameMap.put("leftLeg", "field_3397");
            nameMap.put("leftSleeve", "field_3484");
            nameMap.put("rightSleeve", "field_3486");
            nameMap.put("leftPants", "field_3482");
            nameMap.put("rightPants", "field_3479");
            nameMap.put("jacket", "field_3483");
            nameMap.put("cloak", "field_3485");
            nameMap.put("ear", "field_3481");
        } catch (ClassNotFoundException exception) {
            // Forge
            nameMap.put("head", "f_102808_");
            nameMap.put("hat", "f_102809_");
            nameMap.put("body", "f_102810_");
            nameMap.put("rightArm", "f_102811_");
            nameMap.put("leftArm", "f_102812_");
            nameMap.put("rightLeg", "f_102813_");
            nameMap.put("leftLeg", "f_102814_");
            nameMap.put("leftSleeve", "f_103374_");
            nameMap.put("rightSleeve", "f_103375_");
            nameMap.put("leftPants", "f_103376_");
            nameMap.put("rightPants", "f_103377_");
            nameMap.put("jacket", "f_103378_");
            nameMap.put("cloak", "f_103373_");
            nameMap.put("ear", "f_103379_");
        }
    }

    /**
     * Your should register before the first time camera setup.
     *
     * <p>This method is called in {@link com.xtracr.realcamera.RealCamera#setup()}.</p>
     *
     * @see VirtualRenderer#register(String, BiPredicate)
     */
    public static void register() {
        //if ( Real Camera isn't loaded ) return;
        try {
            final Class<?> virtualRendererClass = Class.forName("com.xtracr.realcamera.api.VirtualRenderer");
            getModelPartNameMethod = virtualRendererClass.getDeclaredMethod("getModelPartName");

            final Method registerMethod = virtualRendererClass.getDeclaredMethod("register", String.class, BiPredicate.class);
            final BiPredicate<Float, MatrixStack> function = CompatExample::virtualRender;
            registerMethod.invoke(null, modid, function);

        } catch (Exception exception) {
            // handle exception
        }
    }

    /**
     * <b>mandatory</b>
     *
     * <p>This method's code should include as much as possible all parts related to {@code matrixStack}
     * in the code that renders the player model, to ensure that the result of {@code matrixStack}
     * after processing is identical to the actual rendering.</p>
     *
     * <p>When you need to handle a {@code Throwable} that makes it impossible for the method to continue running,
     * simply wrap it in a {@link RuntimeException} and throw it, just like in {@link #getModelPart} here.</p>
     *
     * @param tickDelta   or partialTick(s) (official mapping)
     * @param matrixStack or poseStack (official mapping)
     * @return {@code boolean} turn to vanilla rendering if true
     * @see net.minecraft.client.render.entity.EntityRenderDispatcher#render
     * @see net.minecraft.client.render.entity.PlayerEntityRenderer#render
     * @see net.minecraft.client.render.entity.LivingEntityRenderer#render
     * @see net.minecraft.client.render.entity.model.AnimalModel#render
     * @see net.minecraft.client.model.ModelPart#render
     */
    public static boolean virtualRender(float tickDelta, MatrixStack matrixStack) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        PlayerEntityRenderer renderer = (PlayerEntityRenderer) client.getEntityRenderDispatcher().getRenderer(player);
        // PlayerEntityRenderer.render
        ((PlayerEntityRendererAccessor) renderer).invokeSetModelPose(player);
        // LivingEntityRenderer.render
        PlayerEntityModel<AbstractClientPlayerEntity> playerModel = renderer.getModel();
        float n;
        Direction direction;
        playerModel.handSwingProgress = player.getHandSwingProgress(tickDelta);
        playerModel.riding = player.hasVehicle();
        playerModel.child = player.isBaby();
        float h = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        float j = MathHelper.lerpAngleDegrees(tickDelta, player.prevHeadYaw, player.headYaw);
        float k = j - h;
        if (player.hasVehicle() && player.getVehicle() instanceof LivingEntity) {
            LivingEntity vehicle = (LivingEntity) player.getVehicle();
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
        if (player.isInPose(EntityPose.SLEEPING) && (direction = player.getSleepingDirection()) != null) {
            n = player.getEyeHeight(EntityPose.STANDING) - 0.1f;
            matrixStack.translate((float) (-direction.getOffsetX()) * n, 0.0f, (float) (-direction.getOffsetZ()) * n);
        }
        float l = player.age + tickDelta;
        ((PlayerEntityRendererAccessor) renderer).invokeSetupTransforms(player, matrixStack, l, h, tickDelta);
        matrixStack.scale(-1.0f, -1.0f, 1.0f);
        ((PlayerEntityRendererAccessor) renderer).invokeScale(player, matrixStack, tickDelta);
        matrixStack.translate(0.0f, -1.501f, 0.0f);
        n = 0.0f;
        float o = 0.0f;
        if (!player.hasVehicle() && player.isAlive()) {
            n = player.limbAnimator.getSpeed(tickDelta);
            o = player.limbAnimator.getPos(tickDelta);
            if (player.isBaby()) {
                o *= 3.0f;
            }
            if (n > 1.0f) {
                n = 1.0f;
            }
        }
        playerModel.animateModel(player, o, n, tickDelta);
        playerModel.setAngles(player, o, n, l, k, m);
        ModelPart modelPart = getModelPart(playerModel);
        // AnimalModel.render
        /*
        if (playerModel.child) {
            float f;
            if (...) {
                if (playerModel.headScaled) {
                    f = 1.5f / playerModel.invertedChildHeadScale;
                    matrixStack.scale(f, f, f);
                }
                matrixStack.translate(0.0f, playerModel.childHeadYOffset / 16.0f, playerModel.childHeadZOffset / 16.0f);
            } else {
                f = 1.0f / playerModel.invertedChildBodyScale;
                matrixStack.scale(f, f, f);
                matrixStack.translate(0.0f, playerModel.childBodyYOffset / 16.0f, 0.0f);
            }
        }
         */
        // ModelPart.render
        modelPart.rotate(matrixStack);
        return false;
    }

    private static ModelPart getModelPart(PlayerEntityModel<AbstractClientPlayerEntity> playerModel) {
        try {
            final String fieldName = nameMap.get((String) getModelPartNameMethod.invoke(null));
            final Field modelPartField = playerModel.getClass().getField(fieldName);
            return (ModelPart) modelPartField.get(playerModel);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
