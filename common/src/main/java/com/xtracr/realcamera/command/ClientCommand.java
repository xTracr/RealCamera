package com.xtracr.realcamera.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.utils.VertexDataAnalyser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

public class ClientCommand<S extends CommandSource>{
    private static final String KEY_COMMAND = "message.xtracr_" + RealCamera.MODID + "_command_";

    public void register(CommandDispatcher<S> dispatcher, CommandRegistryAccess access) {
        final LiteralArgumentBuilder<S> builder = literal(RealCamera.MODID);
        builder.then(literal("analyse")
                .then(literal("auto").executes(context -> this.startAnalysis(context, 0)))
                .then(literal("pose")
                        .then(argument("accuracy", IntegerArgumentType.integer(10, 10000000)).executes(context -> this.startAnalysis(context, 1))))
                .then(literal("vertex")
                        .then(argument("targetIndex", IntegerArgumentType.integer(0))
                                .then(argument("accuracy", IntegerArgumentType.integer(10, 10000000)).executes(context -> this.startAnalysis(context, 2))))));
        builder.then(literal("autoBind").executes(this::autoBind));
        builder.then(literal("showResult")
                .then(argument("number", IntegerArgumentType.integer(1))
                        .then(argument("detail", BoolArgumentType.bool()).executes(this::showResult))));

        dispatcher.register(builder);
    }

    private int startAnalysis(CommandContext<S> context, int mode) {
        if (mode == 2) mode += IntegerArgumentType.getInteger(context, "targetIndex");
        final float accuracy = mode == 0 ? 100 : IntegerArgumentType.getInteger(context, "accuracy");
        VertexDataAnalyser.start(mode, mode == 0 ? 80 : 120, 1 / accuracy);
        return 1;
    }

    private int autoBind(CommandContext<S> context) {
        return 1;
    }

    private int showResult(CommandContext<S> context) {
        final boolean detail = BoolArgumentType.getBool(context, "detail");
        final int number = IntegerArgumentType.getInteger(context, "number");
        VertexDataAnalyser.showResult(number, detail);
        return 1;
    }

    private static void printGameMessage(Text text) {
        MinecraftClient.getInstance().getMessageHandler().onGameMessage(text, false);
    }

    private  <T> RequiredArgumentBuilder<S, T> argument(final String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    private LiteralArgumentBuilder<S> literal(final String name) {
        return LiteralArgumentBuilder.literal(name);
    }
}
