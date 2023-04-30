package com.xtracr.realcamera.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.api.VirtualRenderer;
import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public abstract class DebugCommand<S extends CommandSource> {

    private static final ModConfig config = ConfigFile.modConfig;
    private static final Map<FeedbackType, List<FeedbackProvider>> addnlFeedbackProvider = new HashMap<>();

    @Nullable
    public static Exception virtualRenderException = null;
    
    static {
        for (FeedbackType type : FeedbackType.values()) {
            addnlFeedbackProvider.put(type, new ArrayList<>());
        }
    }

    public static void registerFeedback(FeedbackType type, FeedbackProvider provider) {
        try {
            addnlFeedbackProvider.get(type).add(provider);
        } catch (Exception exception) {

        }
    }

    public void register(CommandDispatcher<S> dispatcher, CommandRegistryAccess registryAccess) {
        final LiteralArgumentBuilder<S> builder = literal(RealCamera.MODID);
        builder.then(literal("debug")
                .then(literal(FeedbackType.config.name()).executes(this::config))
                .then(literal(FeedbackType.camera.name()).executes(this::camera)));

        dispatcher.register(builder);
    }
    
    private int config(CommandContext<S> context) throws CommandSyntaxException {
        final S source = context.getSource();
        String interim = "";
        this.sendFeedback(source, Text.literal("Camera Mode: " + (config.isClassic() ? "[classic]" : "[binding]") + "\n")
            .append("Vanilla Model Part: [" + config.getVanillaModelPart().name() + "]\n")
            .append("Target Mod Model Part: [" + config.getModelModID() + ":" + config.getModModelPartName() + "]\n")
            .append("Mapped Model Part Name: [" + VirtualRenderer.getModelPartFieldName() + "]\n"));
        
        for (String modid : VirtualRenderer.getFunctionsKeys()) {
            interim += " [" + modid + "]";
        }
        this.sendFeedback(source, Text.literal("Mods with function registered:" + interim));
        interim = "";
        for (String modid : VirtualRenderer.getMethodsKeys()) {
            interim += " [" + modid + "]";
        }
        this.sendFeedback(source, Text.literal("Mods with method registered:" + interim + "\n"));
        interim = "";
        
        if (virtualRenderException != null) {
            this.sendFeedback(source, Text.literal("Failed to bind camera: " + virtualRenderException.getClass().getSimpleName() + "\n"));
        }
        
        addnlFeedbackProvider.get(FeedbackType.config).forEach((provider) -> {
            this.sendFeedback(source, Text.literal(provider.provide()));
        });

        return 0;
    }

    private int camera(CommandContext<S> context) throws CommandSyntaxException {
        final S source = context.getSource();
        final MinecraftClient client = MinecraftClient.getInstance();
        final Camera camera = client.gameRenderer.getCamera();
        final ClientPlayerEntity player = client.player;
        Vec3d offset = camera.getPos().subtract(player.getCameraPosVec(client.getTickDelta()));
        this.sendFeedback(source, Text.literal("Camera offset: " + offset.toString() + "\n")
            .append("Camera rotation: (" + camera.getPitch() + ", " + camera.getYaw() + ", " + RealCameraCore.cameraRoll + ")\n"));
        
        addnlFeedbackProvider.get(FeedbackType.camera).forEach((provider) -> {
            this.sendFeedback(source, Text.literal(provider.provide()));
        });
        
        return 0;
    }
    
    protected abstract void sendFeedback(S source, Text message);
    
    private LiteralArgumentBuilder<S> literal(final String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public enum FeedbackType {
        config,
        camera;
    }

    @FunctionalInterface
    public interface FeedbackProvider{
        String provide();
    }

}
