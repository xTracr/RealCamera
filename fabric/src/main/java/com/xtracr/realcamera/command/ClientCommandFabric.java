package com.xtracr.realcamera.command;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public class ClientCommandFabric extends ClientCommand<FabricClientCommandSource> {

    public static final ClientCommandFabric INSTANCE = new ClientCommandFabric();

    @Override
    public void sendFeedback(FabricClientCommandSource source, Text message) {
        source.sendFeedback(message);
    }

}
