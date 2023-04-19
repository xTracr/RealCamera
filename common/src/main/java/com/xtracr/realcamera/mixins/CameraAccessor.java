package com.xtracr.realcamera.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.render.Camera;

@Mixin(Camera.class)
public interface CameraAccessor {
    
    @Accessor("thirdPerson")
    void setThirdPerson(boolean thirdPerson);

    @Invoker("setRotation")
    void invokeSetRotation(float yaw, float pitch);

    @Invoker("moveBy")
    void invokeMoveBy(double x, double y, double z);
}