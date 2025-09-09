package com.xtracr.realcamera.api;

import com.xtracr.realcamera.config.BindTarget;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public class BindResult {
    public static final BindResult EMPTY = new BindResult(BindTarget.EMPTY, false);
    public final BindTarget target;
    protected final Matrix3f rotation = new Matrix3f();
    protected final boolean mirrored;
    private Vec3 position = Vec3.ZERO, forward = Vec3.ZERO, upward = Vec3.ZERO;

    public BindResult(String name) {
        this(BindTarget.blank(name, ""), false);
    }

    public BindResult(BindTarget target, boolean mirrored) {
        this.target = target;
        this.mirrored = mirrored;
    }

    public boolean available() {
        return !target.isEmpty() && !forward.equals(Vec3.ZERO) && !upward.equals(Vec3.ZERO) && Double.isFinite(position.lengthSqr()) && Math.abs(rotation.determinant() - 1) < 0.01f;
    }

    public boolean weakAvailable() {
        return !target.isEmpty() && (forward != Vec3.ZERO || upward != Vec3.ZERO || position != Vec3.ZERO) && Double.isFinite(position.lengthSqr());
    }

    public Vec3 getPosition() {
        return position;
    }

    public void setPosition(Vec3 vec) {
        position = vec;
    }

    public Vec3 getForward() {
        return forward;
    }

    public void setForward(Vec3 vec) {
        forward = vec.normalize();
    }

    public Vec3 getUpward() {
        return upward;
    }

    public void setUpward(Vec3 vec) {
        upward = vec.normalize();
    }

    public Matrix3f getRotation() {
        return rotation;
    }

    public BindResult computeCamera() {
        if (!available()) return this;
        final int orientation = mirrored ? -1 : 1;
        upward = forward.cross(upward.cross(forward)).normalize();
        Vec3 left = upward.cross(forward).scale(orientation);
        rotation.set(left.toVector3f(), upward.toVector3f(), forward.toVector3f());
        BindTarget.OffsetConfig offsets = target.offsets();
        Vector3f offset = new Vector3f(offsets.getZ(), offsets.getY(), offsets.getX()).mul(offsets.getScale()).mul(rotation);
        position = position.add(offset.x(), offset.y(), offset.z());
        rotation.rotateLocal(orientation * (float) Math.toRadians(offsets.getYaw()), rotation.m10, rotation.m11, rotation.m12);
        rotation.rotateLocal(orientation * (float) Math.toRadians(offsets.getPitch()), rotation.m00, rotation.m01, rotation.m02);
        rotation.rotateLocal(orientation * (float) Math.toRadians(offsets.getRoll()), rotation.m20, rotation.m21, rotation.m22);
        return this;
    }
}
