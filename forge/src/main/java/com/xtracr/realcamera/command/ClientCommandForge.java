package com.xtracr.realcamera.command;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ClientCommandForge extends ClientCommand<ServerCommandSource> {

    public static final ClientCommandForge INSTANCE = new ClientCommandForge();

    @Override
    public void sendFeedback(ServerCommandSource source, Text message) {
        source.sendFeedback(message, false);
    }
    
}
