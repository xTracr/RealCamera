package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtracr.realcamera.api.IMultiVertexCatcher;
import com.xtracr.realcamera.api.IVertexCatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class MultiVertexCatcher implements MultiBufferSource, IMultiVertexCatcher {
    private final Stack<VertexCatcher> catchers = new Stack<>();

    @Override
    public @NotNull VertexConsumer getBuffer(RenderType renderType) {
        if (catchers.isEmpty() || !Objects.equals(catchers.peek().renderType(), renderType) || !renderType.canConsolidateConsecutiveGeometry()) {
            return catchers.push(new VertexCatcher(renderType));
        }
        return catchers.peek();
    }

    @Override
    public void updateModel(Minecraft client, Entity cameraEntity, float x, float y, float z, float yaw, float tickDelta, PoseStack poseStack, int packedLight) {
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        dispatcher.render(cameraEntity, x, y, z, yaw, tickDelta, poseStack, this, packedLight);
    }

    @Override
    public void forEachCatcher(Consumer<IVertexCatcher> consumer) {
        catchers.forEach(consumer);
    }
}
