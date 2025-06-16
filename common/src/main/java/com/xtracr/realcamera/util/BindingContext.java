package com.xtracr.realcamera.util;

import com.xtracr.realcamera.api.PoseHandler;
import com.xtracr.realcamera.config.BindingTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public class BindingContext implements PoseHandler {
    public static final BindingContext EMPTY = new BindingContext(new BindingTarget(), false);
    public final BindingTarget target;
    public final Matrix3f normal = new Matrix3f();
    public final boolean mirrored;
    private final Minecraft client;
    private final float deltaTick;
    public boolean skipRendering = true;
    private Vec3 position = Vec3.ZERO, forward = Vec3.ZERO, upward = Vec3.ZERO, eulerAngle = Vec3.ZERO;

    public BindingContext(BindingTarget target, boolean mirrored) {
        this(target, Minecraft.getInstance(), 0, mirrored);
    }

    public BindingContext(BindingTarget target, Minecraft client, float deltaTick, boolean mirrored) {
        this.target = target;
        this.client = client;
        this.deltaTick = deltaTick;
        this.mirrored = mirrored;
    }

    public boolean available() {
        return !target.isEmpty() && !forward.equals(Vec3.ZERO) && !upward.equals(Vec3.ZERO) && Double.isFinite(position.lengthSqr()) && Math.abs(normal.determinant() - 1) < 0.01f;
    }

    public Vec3 getPosition() {
        return position;
    }

    @Override
    public void setPosition(Vec3 position) {
        this.position = position;
    }

    public Vec3 getEulerAngle() {
        return eulerAngle;
    }

    @Override
    public Minecraft getClient() {
        return client;
    }

    @Override
    public float getDeltaTick() {
        return deltaTick;
    }

    @Override
    public void setDirections(Vec3 forward, Vec3 upward) {
        this.forward = forward.normalize();
        this.upward = forward.cross(upward.cross(forward)).normalize();
    }

    public void init() {
        final int orientation = mirrored ? -1 : 1;
        Vec3 left = upward.cross(forward).scale(orientation);
        normal.set(left.toVector3f(), upward.toVector3f(), forward.toVector3f());
        Vector3f offset = new Vector3f((float) target.getOffsetZ(), (float) target.getOffsetY(), (float) target.getOffsetX()).mul((float) target.getScale()).mul(normal);
        position = position.add(offset.x(), offset.y(), offset.z());
        normal.rotateLocal(orientation * (float) Math.toRadians(target.getYaw()), normal.m10, normal.m11, normal.m12);
        normal.rotateLocal(orientation * (float) Math.toRadians(target.getPitch()), normal.m00, normal.m01, normal.m02);
        normal.rotateLocal(orientation * (float) Math.toRadians(target.getRoll()), normal.m20, normal.m21, normal.m22);
        eulerAngle = MathUtil.getEulerAngleYXZ(normal).scale(Math.toDegrees(1));
    }
}
