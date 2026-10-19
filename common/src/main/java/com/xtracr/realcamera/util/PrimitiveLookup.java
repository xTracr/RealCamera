package com.xtracr.realcamera.util;

import com.xtracr.realcamera.renderer.IdentifiedGeometry;
import com.xtracr.realcamera.renderer.state.Primitive;
import com.xtracr.realcamera.renderer.state.VertexData.UV;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.Nullable;

public final class PrimitiveLookup {
    private PrimitiveLookup() {}

    public static @Nullable Primitive[] findPrimitives(IdentifiedGeometry geometry, @Nullable UV[] uvs, LookupMode mode) {
        Primitive[] found = new Primitive[uvs.length];
        if (mode != LookupMode.SCAN_ONLY) {
            readCached(geometry, uvs, found);
        }
        if (mode != LookupMode.CACHE_ONLY && !allFound(uvs, found)) {
            scan(geometry.primitives(), uvs, found, geometry.renderType());
        }
        return found;
    }

    private static void readCached(IdentifiedGeometry geometry, @Nullable UV[] uvs, @Nullable Primitive[] found) {
        var cache = RenderTypeCache.getPrimitiveCache(geometry.renderType());
        if (cache == null) return;
        for (int i = 0; i < uvs.length; i++) {
            UV uv = uvs[i];
            if (uv == null) continue;
            int index = cache.getInt(uv);
            if (index < 0 || index >= geometry.primitiveCount()) continue;
            Primitive primitive = geometry.readPrimitiveAt(index);
            if (primitive.containsUV(uv.u(), uv.v())) found[i] = primitive;
        }
    }

    private static void scan(Iterable<Primitive> primitives, @Nullable UV[] uvs, @Nullable Primitive[] found, RenderType cacheRenderType) {
        int primitiveIndex = 0;
        for (Primitive primitive : primitives) {
            Primitive retained = null;
            for (int i = 0; i < uvs.length; i++) {
                UV uv = uvs[i];
                if (uv == null || found[i] != null || !primitive.containsUV(uv.u(), uv.v())) continue;
                if (retained == null) retained = primitive.asImmutable();
                found[i] = retained;
                RenderTypeCache.cachePrimitive(cacheRenderType, uv, primitiveIndex);
            }
            if (allFound(uvs, found)) return;
            primitiveIndex++;
        }
    }

    private static boolean allFound(@Nullable UV[] uvs, @Nullable Primitive[] found) {
        for (int i = 0; i < uvs.length; i++) {
            if (uvs[i] != null && found[i] == null) return false;
        }
        return true;
    }

    public enum LookupMode { CACHE_ONLY, SCAN_ONLY, CACHE_THEN_SCAN }
}
