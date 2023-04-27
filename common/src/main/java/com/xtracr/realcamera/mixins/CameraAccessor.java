package com.xtracr.realcamera.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.render.Camera;

@Mixin(Camera.class)
public interface CameraAccessor {
    
    @Accessor
    void setThirdPerson(boolean thirdPerson);

    @Invoker
    void invokeSetRotation(float yaw, float pitch);

    @Invoker
    void invokeMoveBy(double x, double y, double z);
}
