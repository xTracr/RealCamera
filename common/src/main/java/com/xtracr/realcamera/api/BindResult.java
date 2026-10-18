package com.xtracr.realcamera.api;

import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.codec.ConfigDecoder703;
import com.xtracr.realcamera.util.ReferenceFrame;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.jspecify.annotations.Nullable;

/**
 * @deprecated Return a {@link BindingResult} containing an unadjusted {@link ReferenceFrame} instead.
 */
@Deprecated
public class BindResult {
    public static final BindResult EMPTY = new BindResult(null);
    public final @Nullable BindTarget target;
    protected final Matrix3f rotation = new Matrix3f();
    protected Vec3 position = Vec3.ZERO;
    private Vec3 forward = Vec3.ZERO, upward = Vec3.ZERO;

    public BindResult(@Nullable BindTarget target) {
        this.target = target;
    }

    public static BindResult getOrCreate(String name) {
        return new BindResult(ConfigFile.config().getBindTarget(ConfigDecoder703.legacyId(name)));
    }

    public boolean available() {
        return target != null && !forward.equals(Vec3.ZERO) && !upward.equals(Vec3.ZERO)
                && Double.isFinite(position.lengthSqr()) && Math.abs(rotation.determinant() - 1) < 0.01f;
    }

    public boolean weakAvailable() {
        return target != null && (forward != Vec3.ZERO || upward != Vec3.ZERO || position != Vec3.ZERO)
                && Double.isFinite(position.lengthSqr());
    }

    public final Vec3 getPosition() {
        return position;
    }

    public final void setPosition(Vec3 vec) {
        position = vec;
    }

    public final Matrix3f getRotation() {
        return rotation;
    }

    public final void setRotation(Matrix3fc mat) {
        rotation.set(mat);
    }

    public final void lerpPosition(Vec3 vec, double alpha) {
        position = position.lerp(vec, alpha);
    }

    public final void slerpRotation(Matrix3fc mat, double alpha) {
        slerpRotation(new Quaterniond().setFromNormalized(mat), alpha);
    }

    public final void slerpRotation(Quaterniondc quat, double alpha) {
        new Quaterniond().setFromNormalized(rotation).slerp(quat, alpha).get(rotation);
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

    /** The registration adapter consumes raw vectors; Core applies offsets after selecting a result. */
    @Nullable BindingResult toBindingResult() {
        if (target == null) return null;
        ReferenceFrame frame = frame();
        if (frame == null) return null;
        // Cached legacy results must not replace settings edited after they were created.
        var binding = ConfigFile.config().binding;
        if (target.targetConfig() != null) binding.targetConfigs.putIfAbsent(target.id(), target.targetConfig());
        binding.offsetConfigs.putIfAbsent(target.id(), target.offsets());
        binding.renderConfigs.putIfAbsent(target.id(), target.renderConfig());
        return new BindingResult(target.id(), frame);
    }

    private @Nullable ReferenceFrame frame() {
        if (!available()) return null;
        return ReferenceFrame.tryFromForwardUp(position, forward, upward);
    }

    public BindResult computeCamera(boolean mirrored) {
        if (target == null) return this;
        ReferenceFrame frame = frame();
        if (frame == null) return this;
        upward = frame.upward();
        ReferenceFrame camera = target.offsets().applyTo(frame, mirrored);
        position = camera.origin();
        camera.toRotationMatrix(rotation);
        if (mirrored) rotation.scale(-1, 1, 1);
        return this;
    }
}
