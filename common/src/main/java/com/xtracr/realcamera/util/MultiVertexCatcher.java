package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.function.Consumer;

public interface MultiVertexCatcher {
    static MultiVertexCatcher getInstance() {
        return new SimpleMultiVertexCatcher();
    }

    default void sendVertices(VertexRecorder recorder) {
        recorder.records().clear();
        forEachCatcher(catcher -> recorder.recordVertices(catcher.collectVertices(), catcher.renderType()));
    }

    void forEachCatcher(Consumer<VertexCatcher> consumer);

    void updateModel(Minecraft client, Entity cameraEntity, float x, float y, float z, float yaw, float tickDelta, PoseStack poseStack, int packedLight);
}
