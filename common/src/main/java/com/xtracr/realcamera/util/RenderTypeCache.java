package com.xtracr.realcamera.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.xtracr.realcamera.mixin.accessor.RenderSetupAccessor;
import com.xtracr.realcamera.mixin.accessor.RenderTypeAccessor;
import com.xtracr.realcamera.renderer.state.VertexData.UV;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class RenderTypeCache {
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
        public @NonNull String load(@NonNull RenderType renderType) {
            RenderSetup renderSetup = ((RenderTypeAccessor) renderType).getState();
            Map<String, RenderSetup.TextureBinding> textureBindings = ((RenderSetupAccessor) (Object) renderSetup).getTextureBindings();
            RenderSetup.TextureBinding sampler0 = textureBindings.get("Sampler0");
            if (sampler0 != null) return sampler0.location().toString();
            return renderType.toString();
        }
    }

    private static class PrimitiveCacheLoader extends CacheLoader<RenderType, Object2IntMap<UV>> {
        @Override
        public @NonNull Object2IntMap<UV> load(@NonNull RenderType renderType) {
            Object2IntOpenHashMap<UV> map = new Object2IntOpenHashMap<>(4);
            map.defaultReturnValue(-1);
            return map;
        }
    }
}
