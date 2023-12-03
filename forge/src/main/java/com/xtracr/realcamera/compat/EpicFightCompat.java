package com.xtracr.realcamera.compat;

import com.xtracr.realcamera.api.VirtualRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class EpicFightCompat {

    public static final String modid = "epicfight";

    public static void register() {
        VirtualRenderer.register(modid, EpicFightCompat::virtualRender);
    }

    @SuppressWarnings({ "unchecked", "null" })
    public static boolean virtualRender(Float partialTicks, MatrixStack poseStack) {
        // RenderEngine.renderLivingEvent
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity livingEntity = mc.player;

        AbstractClientPlayerPatch<AbstractClientPlayerEntity> entitypatch = (AbstractClientPlayerPatch<AbstractClientPlayerEntity>)
                EpicFightCapabilities.getEntityPatch(livingEntity, LivingEntityPatch.class);
        if (entitypatch == null || !entitypatch.overrideRender()) {
            return true;
        }
        // RenderEngine.renderEntityArmatureModel
        PPlayerRenderer patchedRenderer = (PPlayerRenderer) ClientEngine.getInstance().renderEngine.getEntityRenderer(livingEntity);

        // PatchedLivingEntityRenderer.render
        Armature armature = entitypatch.getArmature();
        patchedRenderer.mulPoseStack(poseStack, armature, livingEntity, entitypatch, partialTicks);
        OpenMatrix4f[] poses = patchedRenderer.getPoseMatrices(entitypatch, armature, partialTicks);

        // AnimatedMesh.drawWithPoseNoTexture
        Joint target = armature.searchJointByName(VirtualRenderer.getModelPartName());
        poseStack.multiplyPositionMatrix(OpenMatrix4f.exportToMojangMatrix(poses[target.getId()]));
        poseStack.peek().getNormalMatrix().multiply(poses[target.getId()].removeTranslation().transpose().toQuaternion());
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        return false;
    }
}
