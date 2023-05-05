package com.xtracr.realcamera.command;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xtracr.realcamera.api.VirtualRenderer;
import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;

import net.minecraft.command.CommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public abstract class ClientCommand<S extends CommandSource> {

    private static final ModConfig config = ConfigFile.modConfig;
    private static final Set<Supplier<String>> feedbackSupplier = new HashSet<>();

    @Nullable
    public static Exception virtualRenderException = null;
    
    public static void registerFeedback(Supplier<String> feedback) {
        feedbackSupplier.add(feedback);
    }

    public void register(CommandDispatcher<S> dispatcher) {
        final LiteralArgumentBuilder<S> builder = literal(RealCamera.MODID);
        builder.then(literal("config").executes(this::config));

        dispatcher.register(builder);
    }
    
    private int config(CommandContext<S> context) throws CommandSyntaxException {
        final S source = context.getSource();
        String s = "";
        this.sendFeedback(source, new LiteralText("--[ CONFIG INFO ]-- \n")
            .append("Camera Mode: " + (config.isClassic() ? "[classic]" : "[binding]") + "\n")
            .append("Vanilla Model Part: [" + config.getVanillaModelPart().name() + "]\n")
            .append("Target Mod Model Part: [" + config.getModelModID() + ":" + config.getModModelPartName() + "]"));
        
        for (String modid : VirtualRenderer.getRegisteredMods()) {
            s += " [" + modid + "]";
        }
        this.sendFeedback(source, new LiteralText("Mods registered:" + s));
        s = "";
        
        if (virtualRenderException != null) {
            this.sendFeedback(source, new LiteralText("\nFailed to bind camera: " + virtualRenderException.getClass().getSimpleName()));
        }
        
        this.sendFeedback(source, new LiteralText("\nAdditional Feedbacks: "));
        feedbackSupplier.forEach(feedback -> this.sendFeedback(source, new LiteralText(feedback.get())));
        return 0;
    }

    protected abstract void sendFeedback(S source, Text message);
    
    private LiteralArgumentBuilder<S> literal(final String name) {
        return LiteralArgumentBuilder.literal(name);
    }

}
