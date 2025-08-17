package com.xtracr.realcamera.api;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public interface PoseHandler {
    Minecraft getClient();

    float getDeltaTick();

    void setPosition(Vec3 position);

    void setForward(Vec3 vec);

    void setUpward(Vec3 vec);
}
