package com.xtracr.realcamera.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.config.BindTarget.DisableConfig;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public final class RealCameraRenderTypes extends RenderType {
    private static final int BUFFER_SIZE = 1536;
    private static final int MASK_TEXTURE_UNIT = 3;
    private static final ShaderStateShard ENTITY_SOLID_SHADER = new ShaderStateShard(RealCameraShaders::entitySolid);
    private static final ShaderStateShard ENTITY_CUTOUT_SHADER = new ShaderStateShard(RealCameraShaders::entityCutout);
    private static final ShaderStateShard ENTITY_TRANSLUCENT_SHADER = new ShaderStateShard(RealCameraShaders::entityTranslucent);
    private static final Map<RenderKey, RenderType> CACHE = new HashMap<>();
    private static final Method CREATE_METHOD = findCreateMethod();

    private RealCameraRenderTypes() {
        super("realcamera_dummy", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 0, false, false, () -> {}, () -> {});
    }

    public static @Nullable MaskedRenderType getMaskedRenderType(RenderType base, String textureId, DisableConfig[] disableConfigs) {
        if (CREATE_METHOD == null) return null;
        ResourceLocation texture = ResourceLocation.tryParse(textureId);
        if (texture == null) return null;
        RenderTypeKind kind = classify(base, texture);
        if (kind == null) return null;
        ShaderInstance shader = shaderFor(kind);
        if (shader == null) return null;
        ResourceLocation mask = DisableMaskCache.getMask(textureId, disableConfigs);
        boolean affectsOutline = base.outline().isPresent();
        RenderKey key = new RenderKey(kind, texture, mask, affectsOutline);
        RenderType masked = CACHE.get(key);
        if (masked == null) {
            masked = buildRenderType(key.kind(), key.texture(), key.mask(), key.affectsOutline());
            if (masked == null) return null;
            CACHE.put(key, masked);
        }
        return new MaskedRenderType(masked, shader);
    }

    private static RenderTypeKind classify(RenderType base, ResourceLocation texture) {
        if (base == RenderType.entitySolid(texture)) return RenderTypeKind.ENTITY_SOLID;
        if (base == RenderType.entityCutout(texture)) return RenderTypeKind.ENTITY_CUTOUT;
        if (base == RenderType.entityCutoutNoCull(texture) || base == RenderType.entityCutoutNoCull(texture, false)) {
            return RenderTypeKind.ENTITY_CUTOUT_NO_CULL;
        }
        if (base == RenderType.entityCutoutNoCullZOffset(texture) || base == RenderType.entityCutoutNoCullZOffset(texture, false)) {
            return RenderTypeKind.ENTITY_CUTOUT_NO_CULL_Z_OFFSET;
        }
        if (base == RenderType.entityTranslucent(texture) || base == RenderType.entityTranslucent(texture, false)) {
            return RenderTypeKind.ENTITY_TRANSLUCENT;
        }
        if (base == RenderType.entityTranslucentCull(texture)) return RenderTypeKind.ENTITY_TRANSLUCENT_CULL;
        return null;
    }

    private static ShaderInstance shaderFor(RenderTypeKind kind) {
        return switch (kind) {
            case ENTITY_SOLID -> RealCameraShaders.entitySolid();
            case ENTITY_CUTOUT, ENTITY_CUTOUT_NO_CULL, ENTITY_CUTOUT_NO_CULL_Z_OFFSET -> RealCameraShaders.entityCutout();
            case ENTITY_TRANSLUCENT, ENTITY_TRANSLUCENT_CULL -> RealCameraShaders.entityTranslucent();
        };
    }

    private static ShaderStateShard shaderStateFor(RenderTypeKind kind) {
        return switch (kind) {
            case ENTITY_SOLID -> ENTITY_SOLID_SHADER;
            case ENTITY_CUTOUT, ENTITY_CUTOUT_NO_CULL, ENTITY_CUTOUT_NO_CULL_Z_OFFSET -> ENTITY_CUTOUT_SHADER;
            case ENTITY_TRANSLUCENT, ENTITY_TRANSLUCENT_CULL -> ENTITY_TRANSLUCENT_SHADER;
        };
    }

    private static RenderType buildRenderType(RenderTypeKind kind, ResourceLocation texture, ResourceLocation mask, boolean affectsOutline) {
        CompositeState.CompositeStateBuilder builder = CompositeState.builder()
                .setShaderState(shaderStateFor(kind))
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTexturingState(maskTexturingState(mask));
        switch (kind) {
            case ENTITY_SOLID, ENTITY_CUTOUT -> builder
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY);
            case ENTITY_CUTOUT_NO_CULL -> builder
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY);
            case ENTITY_CUTOUT_NO_CULL_Z_OFFSET -> builder
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY);
            case ENTITY_TRANSLUCENT -> builder
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY);
            case ENTITY_TRANSLUCENT_CULL -> builder
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY);
        }
        CompositeState state = builder.createCompositeState(affectsOutline);
        try {
            return (RenderType) CREATE_METHOD.invoke(null, renderTypeName(kind), DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, BUFFER_SIZE, true, sortOnUpload(kind), state);
        } catch (ReflectiveOperationException e) {
            RealCamera.LOGGER.error("Failed to create render type for mask shader", e);
            return null;
        }
    }

    private static boolean sortOnUpload(RenderTypeKind kind) {
        return kind == RenderTypeKind.ENTITY_TRANSLUCENT || kind == RenderTypeKind.ENTITY_TRANSLUCENT_CULL;
    }

    private static TexturingStateShard maskTexturingState(ResourceLocation mask) {
        return new TexturingStateShard("realcamera_mask", () -> {
            DEFAULT_TEXTURING.setupRenderState();
            RenderSystem.setShaderTexture(MASK_TEXTURE_UNIT, mask);
        }, DEFAULT_TEXTURING::clearRenderState);
    }

    private static String renderTypeName(RenderTypeKind kind) {
        return switch (kind) {
            case ENTITY_SOLID -> "realcamera_entity_solid";
            case ENTITY_CUTOUT -> "realcamera_entity_cutout";
            case ENTITY_CUTOUT_NO_CULL -> "realcamera_entity_cutout_no_cull";
            case ENTITY_CUTOUT_NO_CULL_Z_OFFSET -> "realcamera_entity_cutout_no_cull_z_offset";
            case ENTITY_TRANSLUCENT -> "realcamera_entity_translucent";
            case ENTITY_TRANSLUCENT_CULL -> "realcamera_entity_translucent_cull";
        };
    }

    private static @Nullable Method findCreateMethod() {
        try {
            Method method = RenderType.class.getDeclaredMethod("create", String.class, VertexFormat.class, VertexFormat.Mode.class, int.class, boolean.class, boolean.class, CompositeState.class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            RealCamera.LOGGER.error("RenderType.create method not found; GPU masking is disabled", e);
            return null;
        }
    }

    public record MaskedRenderType(RenderType renderType, ShaderInstance shader) {
    }

    private enum RenderTypeKind {
        ENTITY_SOLID,
        ENTITY_CUTOUT,
        ENTITY_CUTOUT_NO_CULL,
        ENTITY_CUTOUT_NO_CULL_Z_OFFSET,
        ENTITY_TRANSLUCENT,
        ENTITY_TRANSLUCENT_CULL
    }

    private record RenderKey(RenderTypeKind kind, ResourceLocation texture, ResourceLocation mask, boolean affectsOutline) {
    }
}
