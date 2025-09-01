package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.ConfigFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VertexRecorder {
    protected static final Pattern textureIdPattern = Pattern.compile("texture\\[Optional\\[(.*?)]");
    protected final List<BuiltRecord> records = new ArrayList<>();
    private MultiVertexCatcher catcher = MultiVertexCatcher.defaultImpl();

    protected static Vec3 getPosition(VertexData[] primitive, float u, float v) {
        if (primitive.length < 3) return primitive[0].pos();
        float u0 = primitive[0].u(), v0 = primitive[0].v(), u1 = primitive[1].u(), v1 = primitive[1].v(), u2 = primitive[2].u(), v2 = primitive[2].v();
        float alpha = ((u - u1) * (v1 - v2) - (v - v1) * (u1 - u2)) / ((u0 - u1) * (v1 - v2) - (v0 - v1) * (u1 - u2)),
                beta = ((u - u2) * (v2 - v0) - (v - v2) * (u2 - u0)) / ((u1 - u2) * (v2 - v0) - (v1 - v2) * (u2 - u0));
        return primitive[0].pos().scale(alpha).add(primitive[1].pos().scale(beta)).add(primitive[2].pos().scale(1 - alpha - beta));
    }

    public static BuiltRecord buildVertices(RenderType renderType, VertexData[] vertices) {
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

    public List<BuiltRecord> records() {
        return records;
    }

    public void setCatcher(MultiVertexCatcher catcher) {
        this.catcher = catcher;
    }

    public void updateModel(Minecraft client, Entity entity, float deltaTick, PoseStack poseStack) {
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        dispatcher.render(entity, 0, 0, 0, Mth.lerp(deltaTick, entity.yRotO, entity.getYRot()), deltaTick, poseStack, catcher, dispatcher.getPackedLightCoords(entity, deltaTick));
        records.clear();
        catcher.sendVertices(this);
    }

    public BindResult computeBindResult() {
        BindResult result;
        for (BindTarget target : ConfigFile.config().getTargetList()) {
            for (BuiltRecord record : records) {
                result = new BindResult(target, false);
                record.exportToBindResult(result);
                if (result.available()) return result;
            }
        }
        return BindResult.EMPTY;
    }

    public record BuiltRecord(RenderType renderType, String textureId, VertexData[] vertices, VertexData[][] primitives) {
        public Optional<VertexData[]> findPrimitive(float u, float v) {
            final int resolution = 1000000;
            for (VertexData[] primitive : primitives) {
                int length = primitive.length;
                int[] us = new int[length], vs = new int[length];
                for (int i = 0; i < length; i++) {
                    us[i] = (int) (resolution * primitive[i].u());
                    vs[i] = (int) (resolution * primitive[i].v());
                }
                if (new Polygon(us, vs, length).contains(resolution * u, resolution * v)) return Optional.of(primitive);
            }
            return Optional.empty();
        }

        public void exportToBindResult(BindResult result) {
            if (!textureId.contains(result.target.textureId())) return;
            BindTarget.TargetConfig config = result.target.targetConfig();
            findPrimitive(config.posU(), config.posV()).ifPresent(primitive -> result.setPosition(getPosition(primitive, config.posU(), config.posV())));
            findPrimitive(config.forwardU(), config.forwardV()).ifPresent(primitive -> result.setForward(VertexData.normal(primitive)));
            findPrimitive(config.upwardU(), config.upwardV()).ifPresent(primitive -> result.setUpward(VertexData.normal(primitive)));
        }

        public void exportToBindResult(BindResult result, Matrix4f positionMatrix, Matrix3f normalMatrix) {
            if (!textureId.contains(result.target.textureId())) return;
            BindTarget.TargetConfig config = result.target.targetConfig();
            findPrimitive(config.posU(), config.posV()).ifPresent(primitive -> result.setPosition(new Vec3(getPosition(primitive, config.posU(), config.posV()).toVector3f().mulPosition(positionMatrix))));
            findPrimitive(config.forwardU(), config.forwardV()).ifPresent(primitive -> result.setForward(new Vec3(VertexData.normal(primitive).toVector3f().mul(normalMatrix))));
            findPrimitive(config.upwardU(), config.upwardV()).ifPresent(primitive -> result.setUpward(new Vec3(VertexData.normal(primitive).toVector3f().mul(normalMatrix))));
        }
    }
}
