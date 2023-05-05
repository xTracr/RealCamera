package com.xtracr.realcamera.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

@Mixin(Camera.class)
public interface CameraAccessor {
    
    @Accessor
    BlockView getArea();

    @Accessor
    void setThirdPerson(boolean thirdPerson);

    @Invoker
    void invokeMoveBy(double x, double y, double z);
    
    @Invoker
    void invokeSetRotation(float yaw, float pitch);

    @Invoker
    void invokeSetPos(Vec3d pos);

}
