package com.xtracr.realcamera.api;

import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.CrosshairUtil;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public final class RealCameraAPI {
    private static final List<Registration> REGISTRATIONS = new ArrayList<>();
    private static final List<Consumer<Boolean>> COMPUTE_HOOKS = new CopyOnWriteArrayList<>();
    private static final List<Consumer<Boolean>> SCREEN_HOOKS = new CopyOnWriteArrayList<>();
    private static boolean renderInCompute;
    private static boolean renderInScreen;

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
     * Register a hook notified whenever the compute stage starts or stops.
     *
     * @param hook receives the new state
     */
    @ApiStatus.Experimental
    public static void registerRenderInComputeHook(Consumer<Boolean> hook) {
        COMPUTE_HOOKS.add(Objects.requireNonNull(hook));
    }

    /**
     * Not for external use; the mod updates this state as the compute stage runs.
     */
    @ApiStatus.Internal
    public static void setRenderInCompute(boolean rendering) {
        renderInCompute = rendering;
        for (Consumer<Boolean> hook : COMPUTE_HOOKS) {
            try {
                hook.accept(rendering);
            } catch (Exception exception) {
                RealCamera.LOGGER.warn("Compute stage hook failed", exception);
            }
        }
    }

    /**
     * Return whether the compute stage is currently running.
     */
    @ApiStatus.Experimental
    public static boolean isRenderInCompute() {
        return renderInCompute;
    }

    /**
     * Register a hook notified whenever the model view screen stage starts or stops.
     *
     * @param hook receives the new state
     */
    @ApiStatus.Experimental
    public static void registerRenderInScreenHook(Consumer<Boolean> hook) {
        SCREEN_HOOKS.add(Objects.requireNonNull(hook));
    }

    /**
     * Not for external use; the mod updates this state as the model view screen stage runs.
     */
    @ApiStatus.Internal
    public static void setRenderInScreen(boolean rendering) {
        renderInScreen = rendering;
        for (Consumer<Boolean> hook : SCREEN_HOOKS) {
            try {
                hook.accept(rendering);
            } catch (Exception exception) {
                RealCamera.LOGGER.warn("Screen stage hook failed", exception);
            }
        }
    }

    /**
     * Return whether the model view screen stage is currently running.
     */
    @ApiStatus.Experimental
    public static boolean isRenderInScreen() {
        return renderInScreen;
    }

    /**
     * Return the crosshair offset computed for the current frame, in projection space.
     * <p>
     * X points right and Y points up, measured in GUI-scaled pixels, so an unmodified GUI pose needs
     * {@code translate(offset.x, -offset.y)}. The Z component is always zero.
     * <p>
     * The value is recomputed once per frame and is {@link Vec3#ZERO} when the dynamic crosshair is
     * disabled, the camera is inactive, or nothing is hit.
     */
    @ApiStatus.Experimental
    public static Vec3 getCrosshairOffset() {
        if (!ConfigFile.config().dynamicCrosshair || !RealCameraCore.isActive()) return Vec3.ZERO;
        return CrosshairUtil.offset;
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
        BindResult legacy = new BindResult(ConfigFile.bindTargets().get(result.id()));
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
