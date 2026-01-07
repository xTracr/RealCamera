package com.xtracr.realcamera;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

public class EventHandler {
    public static void addListeners() {
        NeoForge.EVENT_BUS.addListener(EventHandler::onClientTick);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        KeyMappings.handle(Minecraft.getInstance());
    }
}
