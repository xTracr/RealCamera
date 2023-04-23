package com.xtracr.realcamera.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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

/**
 * 
 * This example uses {@code reflection} to {@code register} and {@code getModelPart}
 * 
 * <p>{@code Note:} the variables and methods should all be {@code static}
 * 
 * <p>If you don't want to use reflection, here're other examples:
 * {@code VirtualRenderer.register(modid, CompatExample::virtualRender, nameMap)} 
 * or {@code VirtualRenderer.register(CompatExample.class)}
 * 
 * <p>However, an (optional) dependency should be added to call the method {@link com.xtracr.realcamera.utils.VirtualRenderer#register()}
 * <p>If you don't know how to add a dependency, see {@link https://jitpack.io or https://jitpack.io/#xTracr/RealCamera/} to get information about it
 * 
 */
public class CompatExample {

    /**
     * 
     * It's not necessary to use your {@code modid}. 
     * <p>However, It is strongly recommended to use {@code modid} directly.
     * 
     */
    public static final String modid = "minecraft";
    /**
     * 
     * {@code nameMap} is a mapping from the {@link com.xtracr.realcamera.config.ModConfig.Compats#modModelPart name} of {@code ModelPart}
     * to the name of the {@link java.lang.reflect.Field field} of {@code ModelPart} in the code.
     * <p>The situation is somewhat unique for Minecraft Vanilla. In the case of a mod, {@code nameMap} may not be necessary.
     * <p>Therefore, you can directly register a {@code null} or not declare a {@code nameMap}, so that no mapping will be performed when obtaining the name.
     * 
     */
    public static final Map<String, String> nameMap = new HashMap<>();

    /**
     * 
     * {@code = VirtualRenderer.class.getDeclaredMethod("getModelPart", Object.class)}
     * @see #register()
     * @see com.xtracr.realcamera.utils.VirtualRenderer#getModelPart(Object) getModelPart(Object)
     * 
     */
    private static Method getModelPartMethod;

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
     * 
     * This method is called in {@link com.xtracr.realcamera.RealCamera#setup()}
     * <p>Your should register before the first time camera setup
     * @see com.xtracr.realcamera.utils.VirtualRenderer#register methods to register
     * 
     */
    public static void register() {
        //if ( Real Camera isn't loaded ) return; -- in fact not necessary here
        try {
            final Class<?> virtualRendererClass = Class.forName("com.xtracr.realcamera.utils.VirtualRenderer");
            getModelPartMethod = virtualRendererClass.getDeclaredMethod("getModelPart", Object.class);

            final Method registerA = virtualRendererClass.getDeclaredMethod("register", Class.class);
            registerA.invoke(null, CompatExample.class);

            // -- another way to register
            //final Method registerB = VirtualRenderersClass.getDeclaredMethod("register", String.class, Class.class, String.class, Map.class);
            //registerB.invoke(null, modid, CompatExample.class, "virtualRender", nameMap);
            
        } catch (ClassNotFoundException exception) {
            // handle ClassNotFoundException
        } catch (NoSuchMethodException | SecurityException exception) {
            // handle NoSuchMethodException && SecurityException
        } catch (Exception exception) {
            // handle the rest exceptions...
        }
    }

    /**
     * 
     * This method's code should include as much as possible all parts related to {@code matrixStack} in the code that renders the player model, 
     * to ensure that the result of {@code matrixStack} after processing is identical to the actual rendering.
     * @param tickDelta or particalTick(s) (official mapping)
     * @param matrixStack or poseStack (official mapping)
     * @return {@code boolean} skip rendering if true
     * @see net.minecraft.client.render.entity.EntityRenderDispatcher#render
     * @see net.minecraft.client.render.entity.PlayerEntityRenderer#render
     * @see net.minecraft.client.render.entity.LivingEntityRenderer#render
     * @see net.minecraft.client.render.entity.model.AnimalModel#render
     * @see net.minecraft.client.model.ModelPart#render
     * 
     */
    @SuppressWarnings("resource")
    public static boolean virtualRender(float tickDelta, MatrixStack matrixStack)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        PlayerEntityRenderer renderer = (PlayerEntityRenderer)MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(player);
        // PlayerEntityRenderer.render
        ((PlayerEntityRendererAccessor)renderer).invokeSetModelPose(player);
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
        if (player.isInPose(EntityPose.SLEEPING) && (direction = player.getSleepingDirection()) != null) {
            n = player.getEyeHeight(EntityPose.STANDING) - 0.1f;
            matrixStack.translate((float)(-direction.getOffsetX()) * n, 0.0f, (float)(-direction.getOffsetZ()) * n);
        }
        float l = player.age + tickDelta;
        ((PlayerEntityRendererAccessor)renderer).invokeSetupTransforms(player, matrixStack, l, h, tickDelta);
        matrixStack.scale(-1.0f, -1.0f, 1.0f);
        ((PlayerEntityRendererAccessor)renderer).invokeScale(player, matrixStack, tickDelta);
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
        // AnimalModel.render
        /*
        if (playerModel.child) {
            float f;
            matrixStack.push();
            if (playerModel.headScaled) {
                f = 1.5f / playerModel.invertedChildHeadScale;
                matrixStack.scale(f, f, f);
            }
            matrixStack.translate(0.0f, playerModel.childHeadYOffset / 16.0f, playerModel.childHeadZOffset / 16.0f);
            ((ModelPart)getModelPartMethod.invoke(null, playerModel)).rotate(matrixStack);
            if (...) return; 
            matrixStack.pop();
            matrixStack.push();
            f = 1.0f / playerModel.invertedChildBodyScale;
            matrixStack.scale(f, f, f);
            matrixStack.translate(0.0f, playerModel.childBodyYOffset / 16.0f, 0.0f);
            ((ModelPart)getModelPartMethod.invoke(null, playerModel)).rotate(matrixStack);
        }
         */
        // ModelPart.render
        ((ModelPart)getModelPartMethod.invoke(null, playerModel)).rotate(matrixStack);
        return false;
    }

}
