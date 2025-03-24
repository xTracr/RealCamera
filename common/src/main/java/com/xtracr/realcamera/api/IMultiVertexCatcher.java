package com.xtracr.realcamera.api;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.util.MultiVertexCatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.function.Consumer;

public interface IMultiVertexCatcher {
    static IMultiVertexCatcher getInstance() {
        return new MultiVertexCatcher();
    }

    void forEachCatcher(Consumer<IVertexCatcher> consumer);

    void updateModel(Minecraft client, Entity cameraEntity, float x, float y, float z, float yaw, float tickDelta, PoseStack poseStack, int packedLight);
}
