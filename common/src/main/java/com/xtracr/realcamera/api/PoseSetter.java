package com.xtracr.realcamera.api;

import net.minecraft.world.phys.Vec3;

public interface PoseSetter {
    void setPosition(Vec3 position);

    void setDirections(Vec3 forward, Vec3 upward);
}
