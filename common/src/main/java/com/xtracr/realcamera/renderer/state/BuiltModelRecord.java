package com.xtracr.realcamera.renderer.state;

import com.xtracr.realcamera.renderer.VertexData;
import net.minecraft.client.renderer.rendertype.RenderType;

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
