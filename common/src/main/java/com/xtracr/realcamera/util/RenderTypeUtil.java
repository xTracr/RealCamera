package com.xtracr.realcamera.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.xtracr.realcamera.mixin.accessor.CompositeRenderTypeAccessor;
import com.xtracr.realcamera.mixin.accessor.CompositeStateAccessor;
import com.xtracr.realcamera.mixin.accessor.EmptyTextureStateShardAccessor;
import com.xtracr.realcamera.util.VertexData.UV;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class RenderTypeUtil {
    private static final LoadingCache<RenderType, String> TEXTURE_ID_CACHE = CacheBuilder.newBuilder()
            .maximumSize(64)
            .expireAfterAccess(60, TimeUnit.SECONDS)
            .build(new TextureIdCacheLoader());
    private static final LoadingCache<RenderType, Map<UV, Integer>> PRIMITIVE_CACHE = CacheBuilder.newBuilder()
            .maximumSize(64)
            .expireAfterAccess(60, TimeUnit.SECONDS)
            .build(new PrimitiveCacheLoader());

    public static String getTextureId(RenderType renderType) {
        return TEXTURE_ID_CACHE.getUnchecked(renderType);
    }

    public static @Nullable Map<UV, Integer> getPrimitiveCache(RenderType renderType) {
        return PRIMITIVE_CACHE.getIfPresent(renderType);
    }

    public static void cachePrimitive(RenderType renderType, UV uv, int primitiveIndex) {
        PRIMITIVE_CACHE.getUnchecked(renderType).put(uv, primitiveIndex);
    }

    private static class TextureIdCacheLoader extends CacheLoader<RenderType, String> {
        @Override
        public @NotNull String load(@NotNull RenderType renderType) {
            try {
                RenderType.CompositeState state = ((CompositeRenderTypeAccessor) (Object) renderType).invokeState();
                RenderStateShard.EmptyTextureStateShard textureState = ((CompositeStateAccessor) (Object) state).getTextureState();
                Optional<ResourceLocation> textureId = ((EmptyTextureStateShardAccessor) (Object) textureState).invokeCutoutTexture();
                if (textureId.isPresent()) return textureId.get().toString();
            } catch (ClassCastException | NullPointerException ignored) {
            }
            return renderType.toString();
        }
    }

    private static class PrimitiveCacheLoader extends CacheLoader<RenderType, Map<UV, Integer>> {
        @Override
        public @NotNull Map<UV, Integer> load(@NotNull RenderType renderType) {
            return new HashMap<>(4);
        }
    }
}
