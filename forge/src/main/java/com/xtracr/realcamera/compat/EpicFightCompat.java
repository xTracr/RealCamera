package com.xtracr.realcamera.compat;

import com.xtracr.realcamera.api.VirtualRenderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.client.model.ClientModel;
import yesman.epicfight.api.client.model.ClientModels;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

public class EpicFightCompat {

    public static final String modid = "epicfight";

    public static void register() {
        VirtualRenderer.register(modid, EpicFightCompat::virtualRender);
    }

    @SuppressWarnings({ "unchecked", "null" })
    public static boolean virtualRender(Float partialTicks, MatrixStack poseStack) {
        // RenderEngine.renderLivingEvent
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity entityIn = mc.player;
        AbstractClientPlayerPatch<AbstractClientPlayerEntity> entitypatch = (AbstractClientPlayerPatch<AbstractClientPlayerEntity>)entityIn
                .getCapability(EpicFightCapabilities.CAPABILITY_ENTITY, null).orElse(null);
        if (entitypatch == null || entitypatch.shouldSkipRender()) {
            return true;
        }
        // RenderEngine.renderEntityArmatureModel
        PPlayerRenderer patchedRenderer = (PPlayerRenderer)ClientEngine.instance.renderEngine.getEntityRenderer(entityIn);

        // PatchedLivingEntityRenderer.render
        ClientModel model = entitypatch.getEntityModel(ClientModels.LOGICAL_CLIENT);
        Armature armature = model.getArmature();
        patchedRenderer.mulPoseStack(poseStack, armature, entityIn, entitypatch, partialTicks);
        OpenMatrix4f[] poses = patchedRenderer.getPoseMatrices(entitypatch, armature, partialTicks);

        // ClientModel.drawAnimatedModelNoTexture
        /* Deprecated
        Matrix4f matrix4f = new Matrix4f();
        Matrix3f matrix3f = new Matrix3f();
        matrix4f.loadIdentity();
        matrix3f.loadIdentity();
        Joint root = armature.getJointHierarcy();
        String path = root.searchPath(";", VirtualRenderer.getModelPartName());
        Joint[] jointChain = new Joint[path.length()];
        jointChain[0] = root;
        for (int i = 0; i < path.length()-1; i++) {
            jointChain[i+1] = jointChain[i].getSubJoints().get(path.charAt(path.length()-2-i) - '1');
        }
        for (int i = 1; i < path.length(); i++) {
            matrix4f.multiply(OpenMatrix4f.exportToMojangMatrix(poses[jointChain[i].getId()]));
            matrix3f.multiply(poses[jointChain[i].getId()].removeTranslation().toQuaternion());
        }
        matrix3f.transpose();
        poseStack.peek().getPositionMatrix().multiply(matrix4f);
        poseStack.peek().getNormalMatrix().multiply(matrix3f);
         */
        Joint target = armature.searchJointByName(VirtualRenderer.getModelPartName());
        poseStack.multiplyPositionMatrix(OpenMatrix4f.exportToMojangMatrix(poses[target.getId()]));
        poseStack.peek().getNormalMatrix().multiply(poses[target.getId()].removeTranslation().transpose().toQuaternion());
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.translate(0.0f, -1.501f, 0.0f);
        return false;
    }

}
