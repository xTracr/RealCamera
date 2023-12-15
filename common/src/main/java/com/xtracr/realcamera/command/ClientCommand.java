package com.xtracr.realcamera.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.utils.VertexDataAnalyser;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;

public class ClientCommand<S extends CommandSource>{

    public void register(CommandDispatcher<S> dispatcher, CommandRegistryAccess access) {
        final LiteralArgumentBuilder<S> builder = literal(RealCamera.MODID);
        builder.then(literal("analyse")
                .then(literal("pose").executes(context -> this.startAnalyse(context, 1)))
                .then(literal("vertex")
                        .then(argument("index", IntegerArgumentType.integer(0)).executes(context -> this.startAnalyse(context, 2)))));
        builder.then(literal("autoBind").executes(context -> this.startAnalyse(context, 0)));
        builder.then(literal("showResult")
                .then(argument("number", IntegerArgumentType.integer(1))
                        .then(argument("detail", BoolArgumentType.bool()).executes(this::showResult))));

        dispatcher.register(builder);
    }

    private int startAnalyse(CommandContext<S> context, int mode) throws CommandSyntaxException {
        if (mode == 2) mode += IntegerArgumentType.getInteger(context, "index");
        VertexDataAnalyser.start(mode, mode == 0 ? 80 : 120);
        return 1;
    }

    private int showResult(CommandContext<S> context) throws CommandSyntaxException {
        final boolean detail = BoolArgumentType.getBool(context, "detail");
        final int number = IntegerArgumentType.getInteger(context, "number");
        VertexDataAnalyser.showResult(number, detail);
        return 1;
    }

    private  <T> RequiredArgumentBuilder<S, T> argument(final String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    private LiteralArgumentBuilder<S> literal(final String name) {
        return LiteralArgumentBuilder.literal(name);
    }
}
