package com.xtracr.realcamera.compat;

import net.minecraft.core.Position;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SableCompat {
    @Nullable
    private static Object Sable_HELPER;
    @Nullable
    private static Method ActiveSableCompanion_distanceSquaredWithSubLevels;

    static {
        try {
            Class<?> sableClass = Class.forName("dev.ryanhcode.sable.Sable");
            Sable_HELPER = sableClass.getDeclaredField("HELPER").get(null);
            // ActiveSableCompanion.distanceSquaredWithSubLevels(Level, Position, Position)
            Class<?> activeCompanionClass = Sable_HELPER.getClass();
            ActiveSableCompanion_distanceSquaredWithSubLevels = activeCompanionClass.getMethod("distanceSquaredWithSubLevels", Level.class, Position.class, Position.class);
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException ignored) {
        }
    }

    public static double distanceSquaredWithSubLevels(Level level, Vec3 a, Vec3 b) {
        if (Sable_HELPER == null || ActiveSableCompanion_distanceSquaredWithSubLevels == null) return a.distanceToSqr(b);
        try {
            return (double) ActiveSableCompanion_distanceSquaredWithSubLevels.invoke(Sable_HELPER, level, a, b);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return a.distanceToSqr(b);
        }
    }
}
