package com.xtracr.realcamera.config;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public enum CameraPosture {
    BASE,
    SWIMMING,
    CRAWLING;

    public static CameraPosture from(Entity entity) {
        if (!(entity instanceof Player player)) return BASE;
        if (player.isSwimming()) return SWIMMING;
        if (player.isVisuallyCrawling()) return CRAWLING;
        return BASE;
    }
}
