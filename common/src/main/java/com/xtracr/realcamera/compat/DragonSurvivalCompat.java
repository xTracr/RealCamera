package com.xtracr.realcamera.compat;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.DisableConfig;
import com.xtracr.realcamera.renderer.state.BuiltModelRecord;
import com.xtracr.realcamera.renderer.state.MutableVertex;
import com.xtracr.realcamera.renderer.state.VertexData;
import com.xtracr.realcamera.util.RenderTypeCache;
import net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

public final class DragonSurvivalCompat {
    private static final String NAMESPACE = "dragonsurvival:";
    private static volatile Method isDragon;
    private static volatile Field renderInFirstPerson;
    private static volatile Method createUIRenderState;
    private static boolean failureWarned;

    private DragonSurvivalCompat() {
    }

    static void register() {
        try {
            Class<?> provider = Class.forName("by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider");
            isDragon = provider.getMethod("isDragon", Entity.class);
        } catch (Exception | LinkageError e) {
            failDragonState(e);
        }
        try {
            Class<?> renderer = Class.forName("by.dragonsurvivalteam.dragonsurvival.client.render.ClientDragonRenderer");
            renderInFirstPerson = renderer.getField("renderInFirstPerson");
        } catch (Exception | LinkageError e) {
            failOwnership(e);
        }
        if (isDragon != null && renderInFirstPerson != null) {
            DisableHelper.RENDER_MODEL.registerOr(DragonSurvivalCompat::ownsFirstPersonBody);
        }
        try {
            Class<?> renderer = Class.forName("by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon.DragonRenderer");
            createUIRenderState = renderer.getMethod("createUIRenderState", LivingEntity.class, float.class, double.class, double.class, double.class);
        } catch (Exception | LinkageError e) {
            failUI(e);
        }
    }

    private static boolean isDragon(Player player) {
        Method method = isDragon;
        if (method == null) return false;
        try {
            return (boolean) method.invoke(null, player);
        } catch (Exception | LinkageError e) {
            failDragonState(e);
            return false;
        }
    }

    public static @Nullable EntityRenderState createUIRenderState(Entity entity, float partialTicks) {
        Method method = createUIRenderState;
        if (!(entity instanceof Player player) || method == null || !isDragon(player)) return null;
        try {
            Object result = method.invoke(null, player, partialTicks, 0.0,
                    Mth.wrapDegrees(player.yHeadRot - player.yBodyRot), player.getXRot());
            if (!(result instanceof EntityRenderState renderState)) return null;
            if (renderState instanceof LivingEntityRenderState livingState) {
                livingState.bodyRot = 180.0f;
                livingState.yRot = 0.0f;
                livingState.xRot = 0.0f;
            }
            return renderState;
        } catch (Exception | LinkageError e) {
            failUI(e);
            return null;
        }
    }

    public static boolean applyDisableConfigs(
            List<BuiltModelRecord> records,
            BindTarget target,
            Set<String> hiddenNames) {
        if (!isTarget(target)) return false;
        for (int i = 0; i < records.size(); i++) {
            BuiltModelRecord record = records.get(i);
            boolean dragonLayer = record.textureId().contains(NAMESPACE);
            DisableConfig[] configs = target.filteredDisableConfigs(config -> hiddenNames.contains(config.name())
                    && (record.containsTextureId(config.textureId())
                    || dragonLayer && target.textureId().contains(config.textureId())));
            List<VertexData[]> primitives = new ArrayList<>();
            if (!disablesAll(configs)) for (VertexData[] primitive : record.primitives()) {
                if (!disabled(primitive, configs)) primitives.add(primitive);
            }
            records.set(i, new BuiltModelRecord(
                    record.renderType(),
                    record.textureId(),
                    record.vertices(),
                    primitives.toArray(new VertexData[0][])));
        }
        return true;
    }

    public static List<BuiltModelRecord> focusCandidates(
            List<BuiltModelRecord> records,
            BindTarget target,
            String textureId) {
        if (!isTarget(target)) return records;
        String focusTextureId = textureId.isBlank() ? target.textureId() : textureId;
        if (focusTextureId.isBlank()) return records;
        List<BuiltModelRecord> exactMatches = records.stream()
                .filter(record -> record.textureId().equals(focusTextureId))
                .toList();
        return exactMatches.isEmpty()
                ? records.stream().filter(record -> record.containsTextureId(focusTextureId)).toList()
                : exactMatches;
    }

    public static @Nullable BiFunction<RenderType, CustomGeometryRenderer, CustomGeometryRenderer> createGeometryFilter(
            BindTarget target,
            Matrix4fc viewRotationMatrix) {
        if (!isTarget(target) || viewRotationMatrix == null) return null;
        DisableConfig[] configs = target.filteredDisableConfigs(config -> target.textureId().contains(config.textureId()));
        float m02 = viewRotationMatrix.m02();
        float m12 = viewRotationMatrix.m12();
        float m22 = viewRotationMatrix.m22();
        float m32 = viewRotationMatrix.m32();
        float depth = target.disablingDepth();
        return (renderType, renderer) -> {
            if (renderType.mode() != VertexFormat.Mode.QUADS
                    || !RenderTypeCache.getTextureId(renderType).contains(NAMESPACE)) return renderer;
            return (pose, output) -> {
                FilteringVertexConsumer filtering = new FilteringVertexConsumer(output, configs, m02, m12, m22, m32, depth);
                try {
                    renderer.render(pose, filtering);
                } finally {
                    filtering.finish();
                }
            };
        };
    }

    public static boolean ownsFirstPersonBody(Player player) {
        Field field = renderInFirstPerson;
        if (field == null || !isDragon(player)) return false;
        try {
            return Boolean.TRUE.equals(field.get(null));
        } catch (Exception | LinkageError e) {
            failOwnership(e);
            return false;
        }
    }

    private static boolean isTarget(BindTarget target) {
        return isDragon != null && target != null && !target.isEmpty() && target.textureId().contains(NAMESPACE);
    }

    private static boolean disabled(VertexData[] primitive, DisableConfig[] configs) {
        for (VertexData vertex : primitive) {
            for (DisableConfig config : configs) if (config.disable(vertex)) return true;
        }
        return false;
    }

    private static boolean disablesAll(DisableConfig[] configs) {
        for (DisableConfig config : configs) if (config.disableAll()) return true;
        return false;
    }

    private static synchronized void failDragonState(Throwable throwable) {
        isDragon = null;
        warn(throwable);
    }

    private static synchronized void failOwnership(Throwable throwable) {
        renderInFirstPerson = null;
        warn(throwable);
    }

    private static synchronized void failUI(Throwable throwable) {
        createUIRenderState = null;
        warn(throwable);
    }

    private static void warn(Throwable throwable) {
        if (failureWarned) return;
        failureWarned = true;
        Throwable cause = throwable instanceof InvocationTargetException && throwable.getCause() != null
                ? throwable.getCause()
                : throwable;
        RealCamera.LOGGER.warn("Compatibility with Dragon Survival failed: [{}] {}", cause.getClass().getName(), cause.getMessage());
    }

    private static final class FilteringVertexConsumer implements VertexConsumer {
        private final VertexConsumer output;
        private final DisableConfig[] disableConfigs;
        private final MutableVertex[] quad = {
                VertexData.mutable(), VertexData.mutable(), VertexData.mutable(), VertexData.mutable()
        };
        private final float m02;
        private final float m12;
        private final float m22;
        private final float m32;
        private final float depth;
        private final boolean disableAll;
        private int vertexCount;

        private FilteringVertexConsumer(
                VertexConsumer output,
                DisableConfig[] disableConfigs,
                float m02,
                float m12,
                float m22,
                float m32,
                float depth) {
            this.output = output;
            this.disableConfigs = disableConfigs;
            this.m02 = m02;
            this.m12 = m12;
            this.m22 = m22;
            this.m32 = m32;
            this.depth = depth;
            this.disableAll = disablesAll(disableConfigs);
        }

        @Override
        public void addVertex(
                float x,
                float y,
                float z,
                int color,
                float u,
                float v,
                int overlay,
                int light,
                float normalX,
                float normalY,
                float normalZ) {
            MutableVertex vertex = quad[vertexCount++];
            vertex.x = x;
            vertex.y = y;
            vertex.z = z;
            vertex.argb = color;
            vertex.u = u;
            vertex.v = v;
            vertex.overlay = overlay;
            vertex.light = light;
            vertex.normalX = normalX;
            vertex.normalY = normalY;
            vertex.normalZ = normalZ;
            if (vertexCount == quad.length) flushQuad();
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            flushTail();
            return output.addVertex(x, y, z);
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            flushTail();
            return output.setColor(red, green, blue, alpha);
        }

        @Override
        public VertexConsumer setColor(int color) {
            flushTail();
            return output.setColor(color);
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            flushTail();
            return output.setUv(u, v);
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            flushTail();
            return output.setUv1(u, v);
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            flushTail();
            return output.setUv2(u, v);
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            flushTail();
            return output.setNormal(x, y, z);
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            flushTail();
            return output.setLineWidth(width);
        }

        private void finish() {
            flushTail();
        }

        private void flushQuad() {
            if (!disableAll) for (MutableVertex vertex : quad) {
                float cameraZ = Math.fma(m02, vertex.x, Math.fma(m12, vertex.y, Math.fma(m22, vertex.z, m32)));
                if (cameraZ > -depth) continue;
                for (DisableConfig config : disableConfigs) {
                    if (config.disable(vertex)) {
                        vertexCount = 0;
                        return;
                    }
                }
                for (MutableVertex quadVertex : quad) quadVertex.render(output);
                break;
            }
            vertexCount = 0;
        }

        private void flushTail() {
            for (int i = 0; i < vertexCount; i++) quad[i].render(output);
            vertexCount = 0;
        }
    }
}
