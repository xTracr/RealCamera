package com.xtracr.realcamera.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.render.Camera;

@Mixin(Camera.class)
public interface CameraAccessor {
    
    @Accessor("cameraY")
    float getCameraY();

    @Accessor("lastCameraY")
    float getLastCameraY();

    @Accessor("thirdPerson")
    void setThirdPerson(boolean thirdPerson);

    @Invoker("setRotation")
    void invokeSetRotation(float yaw, float pitch);

	@Invoker("setPos")
	void invokeSetPos(double x, double y, double z);

    @Invoker("moveBy")
    void invokeMoveBy(double x, double y, double z);
}
