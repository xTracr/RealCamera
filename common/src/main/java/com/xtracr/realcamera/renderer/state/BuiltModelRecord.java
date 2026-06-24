package com.xtracr.realcamera.renderer.state;

import net.minecraft.client.renderer.RenderType;

public record BuiltModelRecord(
        RenderType renderType,
        String textureId,
        VertexData[] vertices,
        VertexData[][] primitives
) {
    public boolean containsTextureId(String textureId) {
        return this.textureId.contains(textureId);
    }
}
