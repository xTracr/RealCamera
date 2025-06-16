package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.xtracr.realcamera.config.BindingTarget;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VertexRecorder {
    protected static final Pattern textureIdPattern = Pattern.compile("texture\\[Optional\\[(.*?)]");
    private final List<BuiltRecord> records = new ArrayList<>();

    protected static Vec3 getPosition(VertexData[] primitive, float u, float v) {
        if (primitive.length < 3) return primitive[0].pos();
        float u0 = primitive[0].u(), v0 = primitive[0].v(), u1 = primitive[1].u(), v1 = primitive[1].v(), u2 = primitive[2].u(), v2 = primitive[2].v();
        float alpha = ((u - u1) * (v1 - v2) - (v - v1) * (u1 - u2)) / ((u0 - u1) * (v1 - v2) - (v0 - v1) * (u1 - u2)),
                beta = ((u - u2) * (v2 - v0) - (v - v2) * (u2 - u0)) / ((u1 - u2) * (v2 - v0) - (v1 - v2) * (u2 - u0));
        return primitive[0].pos().scale(alpha).add(primitive[1].pos().scale(beta)).add(primitive[2].pos().scale(1 - alpha - beta));
    }

    public List<BuiltRecord> records() {
        return records;
    }

    public static BuiltRecord buildVertices(VertexData[] vertices, RenderType renderType) {
        String renderTypeName = renderType.toString();
        Matcher matcher = textureIdPattern.matcher(renderTypeName);
        String textureId = matcher.find() ? matcher.group(1) : renderTypeName;
        VertexFormat.Mode drawMode = renderType.mode();
        final int primitiveLength = drawMode.primitiveLength, primitiveStride = drawMode.primitiveStride;
        final int primitiveCount = (vertices.length - primitiveLength) / primitiveStride + 1;
        final boolean startWithFirst = drawMode == VertexFormat.Mode.TRIANGLE_FAN;
        VertexData[][] primitives = new VertexData[primitiveCount][primitiveLength];
        for (int i = 0, k = 0; i < primitiveCount; i++, k += primitiveStride) {
            primitives[i][0] = vertices[startWithFirst ? 0 : k];
            System.arraycopy(vertices, k + 1, primitives[i], 1, primitiveLength - 1);
        }
        return new BuiltRecord(renderType, textureId, vertices, primitives);
    }

    public record BuiltRecord(RenderType renderType, String textureId, VertexData[] vertices, VertexData[][] primitives) {
        public VertexData[] findPrimitive(float u, float v) {
            final int resolution = 1000000;
            for (VertexData[] primitive : primitives) {
                Polygon polygon = new Polygon();
                for (VertexData vertex : primitive) polygon.addPoint((int) (resolution * vertex.u()), (int) (resolution * vertex.v()));
                if (polygon.contains(resolution * u, resolution * v)) return primitive;
            }
            return new VertexData[]{VertexData.ZERO};
        }

        public BindingContext genContext(BindingTarget target, boolean mirrored) {
            if (!textureId.contains(target.textureId)) return BindingContext.EMPTY;
            BindingContext context = new BindingContext(target, mirrored);
            VertexData[] face = findPrimitive(target.getPosU(), target.getPosV());
            context.setPosition(getPosition(face, target.getPosU(), target.getPosV()));
            Vec3 forward = findPrimitive(target.getForwardU(), target.getForwardV())[0].normal();
            Vec3 upward = findPrimitive(target.getUpwardU(), target.getUpwardV())[0].normal();
            context.setDirections(forward, upward);
            return context;
        }
    }
}
