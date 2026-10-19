package com.xtracr.realcamera.util;

import com.xtracr.realcamera.config.binding.TargetConfig;
import com.xtracr.realcamera.renderer.IdentifiedGeometry;
import com.xtracr.realcamera.renderer.state.Primitive;
import com.xtracr.realcamera.renderer.state.VertexData.UV;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3fc;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/**
 * Mutable lookup state for one target within a capture. Primitives retain their capture coordinates.
 */
public final class LookupContext {
    private final UV[] uvs;
    private final @Nullable Primitive[] primitives = new Primitive[3];
    private final @Nullable Vec3[] vectors = new Vec3[3];

    public LookupContext(TargetConfig config) {
        uvs = new UV[]{config.positionUV(), config.forwardUV(), config.upwardUV()};
    }

    public void find(IdentifiedGeometry geometry, LookupMode mode) {
        find(geometry, mode, null, null);
    }

    /**
     * Transform new vectors into the shared frame before accumulating them; null leaves coordinates unchanged.
     */
    public void find(IdentifiedGeometry geometry, LookupMode mode, @Nullable Matrix4fc positionTransform, @Nullable Matrix3fc directionTransform) {
        for (int i = 0; i < vectors.length; i++) {
            if (vectors[i] == null) primitives[i] = null;
        }
        if (allPrimitivesFound()) return;
        if (mode != LookupMode.SCAN_ONLY) {
            readCached(geometry);
        }
        if (mode != LookupMode.CACHE_ONLY && !allPrimitivesFound()) {
            scan(geometry.primitives(), geometry.renderType());
        }
        // Accept vectors after searching, so an invalid first hit is retried on a later find call.
        for (int i = 0; i < vectors.length; i++) {
            Primitive primitive = primitives[i];
            if (vectors[i] != null || primitive == null) continue;
            Vec3 vector;
            if (i == 0) {
                vector = primitive.positionAtUV(uvs[i].u(), uvs[i].v());
                if (positionTransform != null) vector = new Vec3(vector.toVector3f().mulPosition(positionTransform));
            } else {
                vector = primitive.faceNormal();
                if (directionTransform != null)
                    vector = new Vec3(vector.toVector3f().mul(directionTransform)).normalize();
            }
            if (!Double.isFinite(vector.lengthSqr()) || (i != 0 && vector.equals(Vec3.ZERO))) continue;
            vectors[i] = vector;
        }
    }

    public @Nullable Primitive positionPrimitive() {
        return primitives[0];
    }

    public @Nullable Primitive forwardPrimitive() {
        return primitives[1];
    }

    public @Nullable Primitive upwardPrimitive() {
        return primitives[2];
    }

    public boolean isEmpty() {
        return primitives[0] == null && primitives[1] == null && primitives[2] == null;
    }

    public @Nullable ReferenceFrame tryBuild() {
        return tryBuild(false);
    }

    public @Nullable ReferenceFrame tryBuild(boolean reverseNormals) {
        Vec3 position = vectors[0], forward = vectors[1], upward = vectors[2];
        if (position == null || forward == null || upward == null) return null;
        return ReferenceFrame.tryFromForwardUp(position, reverseNormals ? forward.scale(-1) : forward, reverseNormals ? upward.scale(-1) : upward);
    }

    private void readCached(IdentifiedGeometry geometry) {
        var cache = RenderTypeCache.getPrimitiveCache(geometry.renderType());
        if (cache == null) return;
        for (int i = 0; i < uvs.length; i++) {
            if (primitives[i] != null) continue;
            UV uv = uvs[i];
            int index = cache.getInt(uv);
            if (index < 0 || index >= geometry.primitiveCount()) continue;
            Primitive primitive = geometry.readPrimitiveAt(index);
            if (primitive.containsUV(uv.u(), uv.v())) primitives[i] = primitive;
        }
    }

    private void scan(Iterable<Primitive> geometry, RenderType renderType) {
        int primitiveIndex = 0;
        for (Primitive primitive : geometry) {
            Primitive retained = null;
            for (int i = 0; i < uvs.length; i++) {
                UV uv = uvs[i];
                if (primitives[i] != null || !primitive.containsUV(uv.u(), uv.v())) continue;
                if (retained == null) retained = primitive.asImmutable();
                primitives[i] = retained;
                RenderTypeCache.cachePrimitive(renderType, uv, primitiveIndex);
            }
            if (allPrimitivesFound()) return;
            primitiveIndex++;
        }
    }

    private boolean allPrimitivesFound() {
        for (Primitive primitive : primitives) {
            if (primitive == null) return false;
        }
        return true;
    }

    public enum LookupMode {CACHE_ONLY, SCAN_ONLY, CACHE_THEN_SCAN}
}
