package com.xtracr.realcamera.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.Camera;

@Mixin(Camera.class)
public interface CameraAccessor {
	
	@Accessor
	float getEyeHeight();

	@Accessor
	float getEyeHeightOld();

	@Invoker
	void invokeSetRotation(float yRot, float xRot);

	@Invoker
	void invokeSetPosition(double x, double y, double z);

	@Invoker
	void invokeMove(double x, double y, double z);
	
}
