package com.xtracr.realcamera.api;

import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.OffsetConfig;
import com.xtracr.realcamera.util.CameraTransform;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public class BindResult extends CameraTransform {
    public static final BindResult EMPTY = new BindResult(BindTarget.EMPTY);
    public final BindTarget target;
    private Vec3 forward = Vec3.ZERO, upward = Vec3.ZERO;

    public BindResult(BindTarget target) {
        this.target = target;
    }

    public static BindResult getOrCreate(String name) {
        List<BindTarget> fixedTargets = ConfigFile.config().binding.fixedTargetList;
        BindTarget target = fixedTargets.stream()
                .filter(t -> t.name().equals(name))
                .findFirst()
                .orElseGet(() -> {
                    BindTarget blank = BindTarget.blank(name, "");
                    fixedTargets.add(blank);
                    return blank;
                });
        return new BindResult(target);
    }

    public boolean available() {
        return !target.isEmpty() && !forward.equals(Vec3.ZERO) && !upward.equals(Vec3.ZERO) && Double.isFinite(position.lengthSqr()) && Math.abs(rotation.determinant() - 1) < 0.01f;
    }

    public boolean weakAvailable() {
        return !target.isEmpty() && (forward != Vec3.ZERO || upward != Vec3.ZERO || position != Vec3.ZERO) && Double.isFinite(position.lengthSqr());
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

    public BindResult computeCamera(boolean mirrored) {
        return computeCamera(mirrored, 0);
    }

    public BindResult computeCamera(boolean mirrored, float pitchAdjustment) {
        if (!available()) return this;
        final int orientation = mirrored ? -1 : 1;
        upward = forward.cross(upward.cross(forward)).normalize();
        Vec3 left = upward.cross(forward).scale(orientation);
        rotation.set(left.toVector3f(), upward.toVector3f(), forward.toVector3f());
        OffsetConfig offsets = target.offsets();
        Vector3f offset = new Vector3f(offsets.z, offsets.y, offsets.x).mul(offsets.scale).mul(rotation);
        position = position.add(offset.x(), offset.y(), offset.z());
        rotation.rotateLocal(orientation * (float) Math.toRadians(offsets.yaw), rotation.m10, rotation.m11, rotation.m12);
        rotation.rotateLocal(orientation * (float) Math.toRadians(offsets.pitch + pitchAdjustment), rotation.m00, rotation.m01, rotation.m02);
        rotation.rotateLocal(orientation * (float) Math.toRadians(offsets.roll), rotation.m20, rotation.m21, rotation.m22);
        return this;
    }
}
