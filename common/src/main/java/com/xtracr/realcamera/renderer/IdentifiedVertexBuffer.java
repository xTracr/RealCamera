package com.xtracr.realcamera.renderer;

import com.mojang.blaze3d.vertex.MeshData;
import com.xtracr.realcamera.renderer.state.ModelSnapshot;
import com.xtracr.realcamera.renderer.state.Primitive;
import com.xtracr.realcamera.renderer.state.VertexData;
import com.xtracr.realcamera.util.RenderTypeCache;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class IdentifiedVertexBuffer extends IterableVertexBuffer implements IdentifiedGeometry {
    private final RenderType renderType;
    private final String textureId;

    IdentifiedVertexBuffer(RenderType renderType, String textureId, MeshData meshData) {
        super(meshData);
        this.renderType = renderType;
        this.textureId = textureId;
    }

    public static IdentifiedVertexBuffer create(RenderType renderType, MeshData meshData) {
        return new IdentifiedVertexBuffer(renderType, RenderTypeCache.getTextureId(renderType), meshData);
    }

    @Override
    public RenderType renderType() {
        return renderType;
    }

    @Override
    public String textureId() {
        return textureId;
    }

    /** Primitives share the snapshot's immutable vertices. */
    public ModelSnapshot snapshot() {
        VertexData[] vertices = stream().map(VertexData::asImmutable).toArray(VertexData[]::new);
        Primitive[] primitives = new Primitive[primitiveCount];
        for (int i = 0, k = 0; i < primitiveCount; i++, k += primitiveStride) {
            VertexData first = vertices[startWithFirst ? 0 : k];
            primitives[i] = switch (primitiveLength) {
                case 1 -> Primitive.immutablePoint(first);
                case 2 -> Primitive.immutableLine(first, vertices[k + 1]);
                case 3 -> Primitive.immutableTriangle(first, vertices[k + 1], vertices[k + 2]);
                case 4 -> Primitive.immutableQuad(first, vertices[k + 1], vertices[k + 2], vertices[k + 3]);
                default -> throw new IllegalStateException("Unsupported primitive vertex count: " + primitiveLength);
            };
        }
        return new ModelSnapshot(renderType, textureId, vertices, primitives);
    }
}
