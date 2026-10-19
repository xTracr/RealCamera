package com.xtracr.realcamera.api;

import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.binding.TargetConfig;
import com.xtracr.realcamera.util.ReferenceFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class BindingContext {
    private final String namespace;
    private final Minecraft client;
    private final float partialTicks;

    public BindingContext(String namespace, Minecraft client, float partialTicks) {
        this.namespace = namespace;
        this.client = client;
        this.partialTicks = partialTicks;
    }

    public Minecraft client() {
        return client;
    }

    public float partialTicks() {
        return partialTicks;
    }

    /**
     * UV definitions are optional; an approach may compute its own paths without them.
     */
    public Iterable<Target> targets() {
        return () -> ConfigFile.config().binding.targetConfigs.entrySet().stream()
                .filter(entry -> entry.getKey().getNamespace().equals(namespace))
                .map(entry -> new Target(entry.getKey().getPath(), entry.getValue()))
                .iterator();
    }

    /**
     * Apply the manual index after texture matching.
     */
    public List<Target> targets(String textureId) {
        List<Target> matched = ConfigFile.config().binding.targetConfigs.entrySet().stream()
                .filter(entry -> entry.getKey().getNamespace().equals(namespace))
                .filter(entry -> textureId.contains(entry.getValue().textureId()))
                .map(entry -> new Target(entry.getKey().getPath(), entry.getValue()))
                .toList();
        int index = ConfigFile.config().binding.activeConfigIndex;
        if (index <= 0 || matched.isEmpty()) return matched;
        return List.of(matched.get(Math.clamp(index - 1, 0, matched.size() - 1)));
    }

    public BindingResult result(String path, ReferenceFrame frame) {
        return new BindingResult(Identifier.fromNamespaceAndPath(namespace, path), frame);
    }

    public record Target(String path, TargetConfig config) {
    }
}
