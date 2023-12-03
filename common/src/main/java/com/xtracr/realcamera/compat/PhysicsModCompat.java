package com.xtracr.realcamera.compat;

import com.xtracr.realcamera.utils.ReflectUtils;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public class PhysicsModCompat {
    public static final boolean loaded = ReflectUtils.isLoaded("net.diebuddies.physics.PhysicsMod");

    private static final Optional<Field> EntityRenderDispatcher_worldField;
    private static final Optional<Method> ConfigClient_areOceanPhysicsEnabled;
    private static final Optional<Method> PhysicsMod_getInstance;
    private static final Optional<Method> PhysicsMod_getPhysicsWorld;
    private static final Optional<Method> PhysicsWorld_getOceanWorld;
    private static final Optional<Method> OceanWorld_computeEntityOffset;

    static {
        if (loaded) {
            final String worldFieldName = ReflectUtils.isLoaded("net.fabricmc.loader.api.FabricLoader") ? "field_4684" : "f_114366_";
            final Optional<Class<?>> configClientClass = ReflectUtils.getClass("net.diebuddies.config.ConfigClient");
            final Optional<Class<?>> physicsModClass = ReflectUtils.getClass("net.diebuddies.physics.PhysicsMod");
            final Optional<Class<?>> physicsWorldClass = ReflectUtils.getClass("net.diebuddies.physics.PhysicsWorld");
            final Optional<Class<?>> oceanWorldClass = ReflectUtils.getClass("net.diebuddies.physics.ocean.OceanWorld");
            EntityRenderDispatcher_worldField = ReflectUtils.getDeclaredField(Optional.of(EntityRenderDispatcher.class), worldFieldName);
            ConfigClient_areOceanPhysicsEnabled = ReflectUtils.getDeclaredMethod(configClientClass, "areOceanPhysicsEnabled");
            PhysicsMod_getInstance = ReflectUtils.getDeclaredMethod(physicsModClass, "getInstance", World.class);
            PhysicsMod_getPhysicsWorld = ReflectUtils.getDeclaredMethod(physicsModClass, "getPhysicsWorld");
            PhysicsWorld_getOceanWorld = ReflectUtils.getDeclaredMethod(physicsWorldClass, "getOceanWorld");
            OceanWorld_computeEntityOffset = ReflectUtils.getDeclaredMethod(oceanWorldClass, "computeEntityOffset",
                    Matrix4f.class, Matrix3f.class, World.class, Entity.class, double.class, double.class, double.class, double.class, double.class, double.class, float.class, float.class);
        } else {
            EntityRenderDispatcher_worldField = Optional.empty();
            ConfigClient_areOceanPhysicsEnabled = Optional.empty();
            PhysicsMod_getInstance = Optional.empty();
            PhysicsMod_getPhysicsWorld = Optional.empty();
            PhysicsWorld_getOceanWorld = Optional.empty();
            OceanWorld_computeEntityOffset = Optional.empty();
        }
    }

    public static <E extends Entity> void renderStart(EntityRenderDispatcher dispatcher, E entity, double x, double y, double z, float yRot, float renderPercent, MatrixStack matrixStack) {
        if (!loaded) return;
        Object world = ReflectUtils.getFieldValue(EntityRenderDispatcher_worldField, dispatcher).get();
        if ((boolean) ReflectUtils.invokeMethod(ConfigClient_areOceanPhysicsEnabled, null).orElse(false) && world instanceof ClientWorld clientWorld) {
            Object oceanWorld = ReflectUtils.invokeMethod(PhysicsWorld_getOceanWorld, ReflectUtils.invokeMethod(PhysicsMod_getPhysicsWorld, ReflectUtils.invokeMethod(PhysicsMod_getInstance, null, clientWorld).get()).get()).get();
            ReflectUtils.invokeMethod(OceanWorld_computeEntityOffset, oceanWorld, matrixStack.peek().getPositionMatrix(), matrixStack.peek().getNormalMatrix(), clientWorld, entity, x, y, z, 0.0D, 0.0D, 0.0D, yRot, renderPercent);
        }
    }
}
