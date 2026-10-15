package com.xtracr.realcamera.renderer;

import net.minecraft.client.renderer.rendertype.RenderType;

public interface IdentifiedGeometry extends VertexGeometry {
    RenderType renderType();

    String textureId();

    default boolean containsTextureId(String textureId) {
        return textureId().contains(textureId);
    }
}
