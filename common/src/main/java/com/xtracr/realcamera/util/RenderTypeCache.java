package com.xtracr.realcamera.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.xtracr.realcamera.renderer.state.VertexData.UV;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RenderTypeCache {
    private static final Pattern TEXTURE_ID_PATTERN = Pattern.compile("texture\\[Optional\\[(.*?)]");
    private static final LoadingCache<RenderType, String> TEXTURE_ID_CACHE = CacheBuilder.newBuilder()
            .maximumSize(64)
            .expireAfterAccess(60, TimeUnit.SECONDS)
            .build(new TextureIdCacheLoader());
    private static final LoadingCache<RenderType, Object2IntMap<UV>> PRIMITIVE_CACHE = CacheBuilder.newBuilder()
            .maximumSize(64)
            .expireAfterAccess(60, TimeUnit.SECONDS)
            .build(new PrimitiveCacheLoader());

    public static String getTextureId(RenderType renderType) {
        return TEXTURE_ID_CACHE.getUnchecked(renderType);
    }

    public static @Nullable Object2IntMap<UV> getPrimitiveCache(RenderType renderType) {
        return PRIMITIVE_CACHE.getIfPresent(renderType);
    }

    public static void cachePrimitive(RenderType renderType, UV uv, int primitiveIndex) {
        PRIMITIVE_CACHE.getUnchecked(renderType).put(uv, primitiveIndex);
    }

    private static class TextureIdCacheLoader extends CacheLoader<RenderType, String> {
        @Override
        public @NotNull String load(@NotNull RenderType renderType) {
            String renderTypeName = renderType.toString();
            Matcher matcher = TEXTURE_ID_PATTERN.matcher(renderTypeName);
            return matcher.find() ? matcher.group(1) : renderTypeName;
        }
    }

    private static class PrimitiveCacheLoader extends CacheLoader<RenderType, Object2IntMap<UV>> {
        @Override
        public @NotNull Object2IntMap<UV> load(@NotNull RenderType renderType) {
            Object2IntOpenHashMap<UV> map = new Object2IntOpenHashMap<>(4);
            map.defaultReturnValue(-1);
            return map;
        }
    }
}
