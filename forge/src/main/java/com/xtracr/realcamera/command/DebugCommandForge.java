package com.xtracr.realcamera.command;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class DebugCommandForge extends DebugCommand<ServerCommandSource> {

    public static final DebugCommandForge INSTANCE = new DebugCommandForge();

    @Override
    public void sendFeedback(ServerCommandSource source, Text message) {
        source.sendFeedback(message, false);
    }
    
}
