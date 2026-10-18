package com.xtracr.realcamera.util;

import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.jspecify.annotations.Nullable;

import java.lang.Math;

/**
 * An immutable rigid local-to-parent frame. Scale and reflection belong in rendering matrices.
 */
public final class ReferenceFrame {
    public static final ReferenceFrame IDENTITY = new ReferenceFrame(Vec3.ZERO, new Quaterniond());
    private static final Vec3 X_AXIS = new Vec3(1, 0, 0), Y_AXIS = new Vec3(0, 1, 0), Z_AXIS = new Vec3(0, 0, 1);
    private final Vec3 origin;
    private final Quaterniond orientation;

    /**
     * The origin is valid; the normalized quaternion is owned exclusively by immutable frames.
     */
    private ReferenceFrame(Vec3 origin, Quaterniond orientation) {
        this.origin = origin;
        this.orientation = orientation;
    }

    public static ReferenceFrame fromOrientation(Vec3 origin, Quaterniondc orientation) {
        if (!Double.isFinite(origin.lengthSqr())) throw new IllegalArgumentException("Invalid frame origin");
        Quaterniond rotation = new Quaterniond(orientation);
        double lengthSquared = rotation.lengthSquared();
        if (!(lengthSquared > 0) || !Double.isFinite(lengthSquared))
            throw new IllegalArgumentException("Invalid frame orientation");
        return new ReferenceFrame(origin, rotation.normalize());
    }

    public static ReferenceFrame fromForwardUp(Vec3 origin, Vec3 forward, Vec3 upward) {
        ReferenceFrame frame = tryFromForwardUp(origin, forward, upward);
        if (frame == null) throw new IllegalArgumentException("Expected a finite origin and nonparallel directions");
        return frame;
    }

    /**
     * Validate geometry only: a finite origin and finite nonzero directions that are not nearly parallel.
     * Normalize both directions, keeping forward fixed and orthogonalizing upward against it.
     * A zero origin and an identity orientation are valid; path applicability is the caller's decision.
     */
    public static @Nullable ReferenceFrame tryFromForwardUp(Vec3 origin, Vec3 forward, Vec3 upward) {
        double forwardLength = forward.lengthSqr(), upwardLength = upward.lengthSqr();
        if (!Double.isFinite(origin.lengthSqr()) || !(forwardLength > 0) || !Double.isFinite(forwardLength)
                || !(upwardLength > 0) || !Double.isFinite(upwardLength)) return null;
        Vec3 z = forward.scale(1 / Math.sqrt(forwardLength));
        Vec3 x = upward.scale(1 / Math.sqrt(upwardLength)).cross(z);
        double crossLength = x.lengthSqr();
        if (crossLength <= 1.0e-12) return null;
        x = x.scale(1 / Math.sqrt(crossLength));
        Vec3 y = z.cross(x);
        Matrix3d basis = new Matrix3d().set(new Vector3d(x.x, x.y, x.z), new Vector3d(y.x, y.y, y.z), new Vector3d(z.x, z.y, z.z));
        return new ReferenceFrame(origin, new Quaterniond().setFromNormalized(basis).normalize());
    }

    public static ReferenceFrame fromRotation(Vec3 origin, Matrix3fc rotation) {
        if (!Double.isFinite(origin.lengthSqr())) throw new IllegalArgumentException("Invalid frame origin");
        Vec3 x = new Vec3(rotation.m00(), rotation.m01(), rotation.m02());
        Vec3 y = new Vec3(rotation.m10(), rotation.m11(), rotation.m12());
        Vec3 z = new Vec3(rotation.m20(), rotation.m21(), rotation.m22());
        if (!(Math.abs(x.lengthSqr() - 1) < 0.01 && Math.abs(y.lengthSqr() - 1) < 0.01
                && Math.abs(z.lengthSqr() - 1) < 0.01 && Math.abs(x.dot(y)) < 0.01
                && Math.abs(y.dot(z)) < 0.01 && Math.abs(z.dot(x)) < 0.01
                && Math.abs(rotation.determinant() - 1) < 0.01)) {
            throw new IllegalArgumentException("Expected a right-handed orthonormal rotation");
        }
        return new ReferenceFrame(origin, new Quaterniond().setFromNormalized(rotation).normalize());
    }

    public Vec3 origin() {
        return origin;
    }

    public Vec3 left() {
        return transformVector(X_AXIS);
    }

    public Vec3 upward() {
        return transformVector(Y_AXIS);
    }

    public Vec3 forward() {
        return transformVector(Z_AXIS);
    }

    public Vec3 transformPoint(Vec3 point) {
        return origin.add(transformVector(point));
    }

    public Vec3 transformVector(Vec3 vector) {
        Vector3d result = orientation.transform(new Vector3d(vector.x, vector.y, vector.z));
        return new Vec3(result.x, result.y, result.z);
    }

    public ReferenceFrame withOrigin(Vec3 origin) {
        if (!Double.isFinite(origin.lengthSqr())) throw new IllegalArgumentException("Invalid frame origin");
        return new ReferenceFrame(origin, orientation);
    }

    public ReferenceFrame translatedLocal(Vec3 offset) {
        return withOrigin(transformPoint(offset));
    }

    public ReferenceFrame rotatedLocal(Quaterniondc rotation) {
        Quaterniond localRotation = new Quaterniond(rotation);
        double lengthSquared = localRotation.lengthSquared();
        if (!(lengthSquared > 0) || !Double.isFinite(lengthSquared))
            throw new IllegalArgumentException("Invalid local rotation");
        return new ReferenceFrame(origin, new Quaterniond(orientation).mul(localRotation.normalize()).normalize());
    }

    public ReferenceFrame interpolate(ReferenceFrame target, double positionAlpha, double rotationAlpha) {
        return fromOrientation(origin.lerp(target.origin, positionAlpha),
                new Quaterniond(orientation).slerp(target.orientation, rotationAlpha));
    }

    public Matrix3f toRotationMatrix(Matrix3f destination) {
        return orientation.get(destination);
    }

    public Matrix4d toMatrix(Matrix4d destination) {
        return orientation.get(destination).setTranslation(origin.x, origin.y, origin.z);
    }
}
