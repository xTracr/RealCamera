package com.xtracr.realcamera.config;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class ItemSelectorEntriesFabric {
    private ItemSelectorEntriesFabric() {
    }

    public static void register() {
        ItemSelectorEntries.setProvider(ItemSelectorEntriesFabric::createEntry);
    }

    private static AbstractConfigListEntry<?> createEntry(Component option,
                                                         List<String> value,
                                                         List<String> defaultValue,
                                                         Component tooltip,
                                                         Consumer<List<String>> saveConsumer,
                                                         List<String> selections,
                                                         Component resetButtonKey) {
        ItemSelectorListEntry entry = new ItemSelectorListEntry(option, value, false, null, saveConsumer, () -> defaultValue,
                resetButtonKey, false, true, true, selections);
        entry.setTooltipSupplier(() -> tooltip == null ? Optional.empty() : Optional.of(new Component[]{tooltip}));
        return entry;
    }
}
