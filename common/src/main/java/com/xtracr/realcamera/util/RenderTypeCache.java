package com.xtracr.realcamera.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.xtracr.realcamera.mixin.accessor.CompositeRenderTypeAccessor;
import com.xtracr.realcamera.mixin.accessor.CompositeStateAccessor;
import com.xtracr.realcamera.mixin.accessor.EmptyTextureStateShardAccessor;
import com.xtracr.realcamera.util.VertexData.UV;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class RenderTypeCache {
    private static final LoadingCache<RenderType, String> TEXTURE_ID_CACHE = CacheBuilder.newBuilder()
            .maximumSize(64)
            .expireAfterAccess(60, TimeUnit.SECONDS)
            .build(new TextureIdCacheLoader());
    private static final LoadingCache<PrimitiveLayoutKey, Object2IntMap<UV>> PRIMITIVE_CACHE = CacheBuilder.newBuilder()
            .maximumSize(256)
            .expireAfterAccess(60, TimeUnit.SECONDS)
            .build(new PrimitiveCacheLoader());

    public static String getTextureId(RenderType renderType) {
        return TEXTURE_ID_CACHE.getUnchecked(renderType);
    }

    public static @Nullable Object2IntMap<UV> getPrimitiveCache(PrimitiveLayoutKey layoutKey) {
        return PRIMITIVE_CACHE.getIfPresent(layoutKey);
    }

    public static void cachePrimitive(PrimitiveLayoutKey layoutKey, UV uv, int primitiveIndex) {
        PRIMITIVE_CACHE.getUnchecked(layoutKey).put(uv, primitiveIndex);
    }

    public static void invalidatePrimitiveCache(PrimitiveLayoutKey layoutKey) {
        PRIMITIVE_CACHE.invalidate(layoutKey);
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

    public record PrimitiveLayoutKey(RenderType renderType, String textureId, VertexFormat.Mode mode,
                                     int vertexSize, int vertexCount, int primitiveLength, int primitiveCount,
                                     int meshOrdinal, int vertexLayoutHash) { }

    private static class PrimitiveCacheLoader extends CacheLoader<PrimitiveLayoutKey, Object2IntMap<UV>> {
        @Override
        public @NotNull Object2IntMap<UV> load(@NotNull PrimitiveLayoutKey layoutKey) {
            Object2IntOpenHashMap<UV> map = new Object2IntOpenHashMap<>(4);
            map.defaultReturnValue(-1);
            return map;
        }
    }
}
