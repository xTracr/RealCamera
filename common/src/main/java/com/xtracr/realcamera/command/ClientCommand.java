package com.xtracr.realcamera.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.util.VertexDataAnalyser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.List;

public class ClientCommand<S extends CommandSource>{
    private static final String KEY_COMMAND = "message.xtracr_" + RealCamera.MODID + "_command_";
    private static final ModConfig config = ConfigFile.modConfig;

    public void register(CommandDispatcher<S> dispatcher, CommandRegistryAccess access) {
        final LiteralArgumentBuilder<S> builder = literal(RealCamera.MODID);
        builder.then(literal("analyse").executes(context -> this.startAnalysis(context, 0))
                .then(argument("targetIndex", IntegerArgumentType.integer(0))
                        .then(argument("accuracy", IntegerArgumentType.integer(10, 10000000)).executes(context -> this.startAnalysis(context, 1)))));
        builder.then(literal("autoBind")
                .then(argument("name", StringArgumentType.string()).executes(this::autoBind)));
        builder.then(literal("config")
                .then(literal("delete")
                        .then(argument("name", StringArgumentType.string()).executes(this::deleteList)))
                .then(literal("listAll").executes(this::listAll)));
        builder.then(literal("showResult")
                .then(argument("number", IntegerArgumentType.integer(1)).executes(this::showResult)));

        dispatcher.register(builder);
    }

    private int startAnalysis(CommandContext<S> context, int mode) {
        if (mode == 1) mode += IntegerArgumentType.getInteger(context, "targetIndex");
        final float accuracy = mode == 0 ? 100 : IntegerArgumentType.getInteger(context, "accuracy");
        VertexDataAnalyser.start(mode, mode == 0 ? 80 : 120, 1 / accuracy);
        return 1;
    }

    private int autoBind(CommandContext<S> context) {
        List<Integer> list = VertexDataAnalyser.getFinalResults(-1);
        if (list == null) return 0;
        config.binding.indexListMap.put(StringArgumentType.getString(context, "name"), list);
        ConfigFile.save();
        printGameMessage(Text.translatable(KEY_COMMAND + "autoBind", list.get(0), list.get(0), list.get(1), list.get(2)));
        return 1;
    }

    private int deleteList(CommandContext<S> context) {
        final String name = StringArgumentType.getString(context, "name");
        if (!config.binding.indexListMap.containsKey(name)) {
            printGameMessage(Text.translatable(KEY_COMMAND + "delete_failure", name));
            return 0;
        }
        config.binding.indexListMap.remove(name);
        ConfigFile.save();
        printGameMessage(Text.translatable(KEY_COMMAND + "delete_success", name));
        return 1;
    }

    private int listAll(CommandContext<S> context) {
        StringBuffer buffer = new StringBuffer();
        config.binding.indexListMap.forEach((name, list) -> {
            buffer.append("\n'").append(name).append("' -> [ ");
            list.forEach(i -> buffer.append(i).append(" "));
            buffer.append("]");
        });
        printGameMessage(Text.translatable(KEY_COMMAND + "listAll", config.binding.indexListMap.size(), buffer.toString()));
        return 1;
    }

    private int showResult(CommandContext<S> context) {
        final int number = IntegerArgumentType.getInteger(context, "number");
        VertexDataAnalyser.showResult(number, true);
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
