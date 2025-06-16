package com.xtracr.realcamera.api;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public interface PoseHandler {
    Minecraft getClient();

    float getDeltaTick();

    void setPosition(Vec3 position);

    void setDirections(Vec3 forward, Vec3 upward);
}
