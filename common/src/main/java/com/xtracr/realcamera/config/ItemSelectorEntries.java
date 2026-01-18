package com.xtracr.realcamera.config;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Consumer;

public final class ItemSelectorEntries {
    private static List<String> cachedSelections;
    private static Provider provider;

    private ItemSelectorEntries() {
    }

    public static List<String> itemSelections() {
        if (cachedSelections == null) {
            cachedSelections = BuiltInRegistries.ITEM.keySet().stream()
                    .map(ResourceLocation::toString)
                    .sorted()
                    .toList();
        }
        return cachedSelections;
    }

    public static void setProvider(Provider provider) {
        ItemSelectorEntries.provider = provider;
    }

    public static AbstractConfigListEntry<?> createEntry(Component option,
                                                         List<String> value,
                                                         List<String> defaultValue,
                                                         Component tooltip,
                                                         Consumer<List<String>> saveConsumer,
                                                         List<String> selections,
                                                         Component resetButtonKey) {
        if (provider == null) {
            throw new IllegalStateException("Item selector entry provider is not registered.");
        }
        return provider.create(option, value, defaultValue, tooltip, saveConsumer, selections, resetButtonKey);
    }

    @FunctionalInterface
    public interface Provider {
        AbstractConfigListEntry<?> create(Component option,
                                          List<String> value,
                                          List<String> defaultValue,
                                          Component tooltip,
                                          Consumer<List<String>> saveConsumer,
                                          List<String> selections,
                                          Component resetButtonKey);
    }
}
