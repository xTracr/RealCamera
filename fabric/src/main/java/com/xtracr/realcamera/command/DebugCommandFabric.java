package com.xtracr.realcamera.command;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public class DebugCommandFabric extends DebugCommand<FabricClientCommandSource> {

    public static final DebugCommandFabric INSTANCE = new DebugCommandFabric();

    @Override
    public void sendFeedback(FabricClientCommandSource source, Text message) {
        source.sendFeedback(message);
    }
    
}
