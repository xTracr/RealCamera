package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Stack;

public class MultiVertexCatcher implements MultiBufferSource {
    private final Stack<VertexCatcher> catchers = new Stack<>();

    public static MultiVertexCatcher getInstance() {
        return new MultiVertexCatcher();
    }

    public void sendVertices(VertexRecorder recorder) {
        recorder.records().clear();
        catchers.forEach(catcher -> recorder.recordVertices(catcher.collectVertices(), catcher.renderType()));
    }

    public void updateModel(Minecraft client, Entity cameraEntity, float x, float y, float z, float yaw, float deltaTick, PoseStack poseStack, int packedLight) {
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        dispatcher.render(cameraEntity, x, y, z, yaw, deltaTick, poseStack, this, packedLight);
    }

    @Override
    public @NotNull VertexConsumer getBuffer(RenderType renderType) {
        if (catchers.isEmpty() || !Objects.equals(catchers.peek().renderType(), renderType) || !renderType.canConsolidateConsecutiveGeometry()) {
            return catchers.push(new VertexCatcher(renderType));
        }
        return catchers.peek();
    }
}
