package com.xtracr.realcamera.command;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xtracr.realcamera.RealCamera;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

public abstract class ClientCommand<S extends CommandSource> {

    private static final Set<Supplier<String>> feedbackSupplier = new HashSet<>();

    public static void registerFeedback(Supplier<String> feedback) {
        feedbackSupplier.add(feedback);
    }

    public void register(CommandDispatcher<S> dispatcher, CommandRegistryAccess registryAccess) {
        final LiteralArgumentBuilder<S> builder = literal(RealCamera.MODID);
        builder.then(literal("feedback").executes(this::feedback));

        dispatcher.register(builder);
    }

    private int feedback(CommandContext<S> context) throws CommandSyntaxException {
        final S source = context.getSource();
        this.sendFeedback(source, Text.literal("Real Camera Feedbacks: "));
        feedbackSupplier.forEach(feedback -> this.sendFeedback(source, Text.literal(feedback.get())));
        return 0;
    }

    protected abstract void sendFeedback(S source, Text message);

    private LiteralArgumentBuilder<S> literal(final String name) {
        return LiteralArgumentBuilder.literal(name);
    }

}
