package com.xtracr.realcamera.compat;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

final class TetrahedralProbeSet {
    static final int PASS_COUNT = 4;
    private static final int VALID_MASK = (1 << PASS_COUNT) - 1;
    private static final float INVERSE_SQRT_THREE = (float) (1.0 / Math.sqrt(3.0));
    private static final Vector3f[] DIRECTIONS = {
            direction(1, 1, 1),
            direction(1, -1, -1),
            direction(-1, 1, -1),
            direction(-1, -1, 1)
    };
    private static final Matrix4f[] PROJECTIONS = {
            projection(DIRECTIONS[0]),
            projection(DIRECTIONS[1]),
            projection(DIRECTIONS[2]),
            projection(DIRECTIONS[3])
    };

    private TetrahedralProbeSet() {
    }

    static Vector3fc direction(int pass) {
        return DIRECTIONS[pass];
    }

    static Matrix4f projection(int pass) {
        return PROJECTIONS[pass];
    }

    static int[] prioritizedOrder(int preferredMask) {
        int validPreferredMask = (preferredMask & ~VALID_MASK) == 0 ? preferredMask : 0;
        int[] order = new int[PASS_COUNT];
        int next = 0;
        for (int pass = 0; pass < PASS_COUNT; pass++) {
            if ((validPreferredMask & 1 << pass) != 0) order[next++] = pass;
        }
        for (int pass = 0; pass < PASS_COUNT; pass++) {
            if ((validPreferredMask & 1 << pass) == 0) order[next++] = pass;
        }
        return order;
    }

    private static Vector3f direction(float x, float y, float z) {
        return new Vector3f(x, y, z).mul(INVERSE_SQRT_THREE);
    }

    private static Matrix4f projection(Vector3fc direction) {
        Vector3f up = Math.abs(direction.y()) < 0.9f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        Matrix4f view = new Matrix4f().setLookAlong(direction, up);
        return new Matrix4f().setOrtho(-1, 1, -1, 1, -1, 1).mul(view);
    }
}
