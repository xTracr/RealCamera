package com.xtracr.realcamera.api;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.ReferenceFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix3fc;
import org.joml.Quaterniondc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public final class RealCameraAPI {
    private static final List<Registration> REGISTRATIONS = new ArrayList<>();

    /**
     * Construct a frame from a finite origin and nonparallel forward/up directions.
     */
    @ApiStatus.Experimental
    public static ReferenceFrame createReferenceFrame(Vec3 origin, Vec3 forward, Vec3 upward) {
        return ReferenceFrame.fromForwardUp(origin, forward, upward);
    }

    /**
     * Return null when the origin or directions cannot define a valid frame.
     */
    @ApiStatus.Experimental
    public static @Nullable ReferenceFrame tryCreateReferenceFrame(Vec3 origin, Vec3 forward, Vec3 upward) {
        return ReferenceFrame.tryFromForwardUp(origin, forward, upward);
    }

    /**
     * Construct a frame from a finite origin and a right-handed orthonormal rotation.
     */
    @ApiStatus.Experimental
    public static ReferenceFrame createReferenceFrame(Vec3 origin, Matrix3fc rotation) {
        return ReferenceFrame.fromRotation(origin, rotation);
    }

    /**
     * Construct a frame from a finite origin and a finite nonzero quaternion; the quaternion is normalized and copied.
     */
    @ApiStatus.Experimental
    public static ReferenceFrame createReferenceFrame(Vec3 origin, Quaterniondc orientation) {
        return ReferenceFrame.fromOrientation(origin, orientation);
    }

    @ApiStatus.Experimental
    public static void register(String namespace, BindingProvider provider) {
        register(namespace, 0, provider);
    }

    @ApiStatus.Experimental
    public static void register(String namespace, int priority, BindingProvider provider) {
        // Validate the namespace using Minecraft's own error messages; "head" is only a valid placeholder path.
        Identifier.fromNamespaceAndPath(namespace, "head");
        Objects.requireNonNull(provider);
        register(priority, (client, partialTicks) -> {
            BindingResult result = provider.compute(new BindingContext(namespace, client, partialTicks));
            if (result != null && !result.id().getNamespace().equals(namespace)) {
                throw new IllegalArgumentException("Binding result belongs to another namespace: " + result.id());
            }
            return result;
        });
    }

    private static void register(int priority, BiFunction<Minecraft, Float, @Nullable BindingResult> function) {
        REGISTRATIONS.add(new Registration(priority, function));
        REGISTRATIONS.sort(Comparator.comparingInt(Registration::priority).reversed());
    }

    public static @Nullable BindingResult computeResult(Minecraft client, float partialTicks) {
        for (var registration : REGISTRATIONS) {
            BindingResult result = registration.function.apply(client, partialTicks);
            if (result != null) return result;
        }
        return null;
    }

    /**
     * @deprecated Use {@link #register(String, BindingProvider)}.
     */
    @Deprecated
    public static void registerFunction(BiFunction<Minecraft, Float, BindResult> function) {
        registerFunction(0, function);
    }

    /**
     * @deprecated Use {@link #register(String, int, BindingProvider)}.
     */
    @Deprecated
    public static void registerFunction(int priority, BiFunction<Minecraft, Float, BindResult> function) {
        Objects.requireNonNull(function);
        register(priority, (client, partialTicks) -> {
            BindResult result = function.apply(client, partialTicks);
            return result == null ? null : result.toBindingResult();
        });
    }

    /**
     * @deprecated Use {@link #computeResult(Minecraft, float)}.
     */
    @Deprecated
    public static BindResult computeBindResult(Minecraft client, float partialTicks) {
        BindingResult result = computeResult(client, partialTicks);
        if (result == null) return BindResult.EMPTY;
        BindResult legacy = new BindResult(ConfigFile.config().getBindTarget(result.id()));
        legacy.setPosition(result.referenceFrame().origin());
        legacy.setForward(result.referenceFrame().forward());
        legacy.setUpward(result.referenceFrame().upward());
        return legacy;
    }

    @FunctionalInterface
    public interface BindingProvider {
        /**
         * Return an applicable path and a valid raw frame, or null when no binding was found.
         */
        @Nullable BindingResult compute(BindingContext context);
    }

    private record Registration(int priority, BiFunction<Minecraft, Float, @Nullable BindingResult> function) {
    }
}
