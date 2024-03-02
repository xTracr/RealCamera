package com.xtracr.realcamera.compat;

import com.xtracr.realcamera.util.ReflectUtil;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public class PhysicsModCompat {
    public static final boolean loaded = ReflectUtil.isLoaded("net.diebuddies.physics.PhysicsMod");

    private static final Optional<Field> EntityRenderDispatcher_worldField;
    private static final Optional<Method> ConfigClient_areOceanPhysicsEnabled;
    private static final Optional<Method> PhysicsMod_getInstance;
    private static final Optional<Method> PhysicsMod_getPhysicsWorld;
    private static final Optional<Method> PhysicsWorld_getOceanWorld;
    private static final Optional<Method> OceanWorld_computeEntityOffset;

    static {
        if (loaded) {
            final String worldFieldName = ReflectUtil.isLoaded("net.fabricmc.loader.api.FabricLoader") ? "field_4684" : "f_114366_";
            final Optional<Class<?>> configClientClass = ReflectUtil.getClass("net.diebuddies.config.ConfigClient");
            final Optional<Class<?>> physicsModClass = ReflectUtil.getClass("net.diebuddies.physics.PhysicsMod");
            final Optional<Class<?>> physicsWorldClass = ReflectUtil.getClass("net.diebuddies.physics.PhysicsWorld");
            final Optional<Class<?>> oceanWorldClass = ReflectUtil.getClass("net.diebuddies.physics.ocean.OceanWorld");
            EntityRenderDispatcher_worldField = ReflectUtil.getDeclaredField(Optional.of(EntityRenderDispatcher.class), worldFieldName);
            ConfigClient_areOceanPhysicsEnabled = ReflectUtil.getDeclaredMethod(configClientClass, "areOceanPhysicsEnabled");
            PhysicsMod_getInstance = ReflectUtil.getDeclaredMethod(physicsModClass, "getInstance", World.class);
            PhysicsMod_getPhysicsWorld = ReflectUtil.getDeclaredMethod(physicsModClass, "getPhysicsWorld");
            PhysicsWorld_getOceanWorld = ReflectUtil.getDeclaredMethod(physicsWorldClass, "getOceanWorld");
            OceanWorld_computeEntityOffset = ReflectUtil.getDeclaredMethod(oceanWorldClass, "computeEntityOffset",
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
        try {
            Object world = ReflectUtil.getFieldValue(EntityRenderDispatcher_worldField, dispatcher).get();
            if ((boolean) ReflectUtil.invokeMethod(ConfigClient_areOceanPhysicsEnabled, null).orElse(false) && world instanceof ClientWorld clientWorld) {
                Object oceanWorld = ReflectUtil.invokeMethod(PhysicsWorld_getOceanWorld, ReflectUtil.invokeMethod(PhysicsMod_getPhysicsWorld, ReflectUtil.invokeMethod(PhysicsMod_getInstance, null, clientWorld).get()).get()).get();
                ReflectUtil.invokeMethod(OceanWorld_computeEntityOffset, oceanWorld, matrixStack.peek().getPositionMatrix(), matrixStack.peek().getNormalMatrix(), clientWorld, entity, x, y, z, 0.0d, 0.0d, 0.0d, yRot, renderPercent);
            }
        } catch (Exception ignored) {
        }
    }
}
