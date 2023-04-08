package com.xtracr.realcamera.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ConfigScreen implements ConfigScreenFactory<Screen> {
    
    private static final String Category = "config.category.xtracr_realcamera_";
    private static final String Option = "config.option.xtracr_realcamera_";
    private static final String Tooltip = "config.tooltip.xtracr_realcamera_";

    @Override
    public Screen create(Screen parent) {

        ConfigFile.load();
        ModConfig config = ConfigFile.modConfig;

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.translatable("config.title.xtracr_realcamera"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable(Category+"general"));
        ConfigCategory bindingMode = builder.getOrCreateCategory(Text.translatable(Category+"bindingmode"));
        ConfigCategory classicMode = builder.getOrCreateCategory(Text.translatable(Category+"classicmode"));
        ConfigCategory compats = builder.getOrCreateCategory(Text.translatable(Category+"compats"));
        ConfigCategory disables = builder.getOrCreateCategory(Text.translatable(Category+"disables"));

        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"enabled"), config.general.enabled)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(Tooltip+"enabled"))
            .setSaveConsumer(b -> { config.general.enabled = b; ConfigFile.save(); })
            .build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"classic"), config.general.classic)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(Tooltip+"classic"))
            .setSaveConsumer(b -> { config.general.classic = b; ConfigFile.save(); })
            .build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"rendermodel"), config.general.renderModel)
            .setDefaultValue(true)
            .setTooltip(Text.translatable(Tooltip+"rendermodel"))
            .setSaveConsumer(b -> { config.general.renderModel = b; ConfigFile.save(); })
            .build());
        general.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"camerastep"), config.general.cameraStep)
            .setDefaultValue(0.25D)
            .setMin(0.0D)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"camerastep"))
            .setSaveConsumer(d -> { config.general.cameraStep = d; ConfigFile.save(); })
            .build());
        general.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"scale"), config.general.scale)
            .setDefaultValue(1.0D)
            .setMin(0.0D)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"scale"))
            .setSaveConsumer(d -> { config.general.scale = d; ConfigFile.save(); })
            .build());
        
        bindingMode.addEntry(entryBuilder.startEnumSelector(Text.translatable(Option+"modelpart"), AcceptableModelParts.class, config.bindingMode.modelPart)
            .setDefaultValue(AcceptableModelParts.HEAD)
            .setTooltip(Text.translatable(Tooltip+"modelpart"))
            .setSaveConsumer(e -> { config.bindingMode.modelPart = e; ConfigFile.save(); })
            .build());
        bindingMode.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"binddirection"), config.bindingMode.bindDirection)
            .setDefaultValue(true)
            .setTooltip(Text.translatable(Tooltip+"binddirection"))
            .setSaveConsumer(b -> { config.bindingMode.bindDirection = b; ConfigFile.save(); })
            .build());
        bindingMode.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"lockrolling"), config.bindingMode.lockRolling)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(Tooltip+"lockrolling"))
            .setSaveConsumer(b -> { config.bindingMode.lockRolling = b; ConfigFile.save(); })
            .build());
        bindingMode.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"bindingX"), config.bindingMode.bindingX)
            .setDefaultValue(3.25D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"bindingX"))
            .setSaveConsumer(d -> { config.bindingMode.bindingX = d; ConfigFile.save(); })
            .build());
        bindingMode.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"bindingY"), config.bindingMode.bindingY)
            .setDefaultValue(2.0D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"bindingY"))
            .setSaveConsumer(d -> { config.bindingMode.bindingY = d; ConfigFile.save(); })
            .build());
        bindingMode.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"bindingZ"), config.bindingMode.bindingZ)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"bindingZ"))
            .setSaveConsumer(d -> { config.bindingMode.bindingZ = d; ConfigFile.save(); })
            .build());
        bindingMode.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"pitch"), config.bindingMode.pitch)
            .setDefaultValue(0.0D)
            .setMin(-180.0D)
            .setMax(180.0D)
            .setTooltip(Text.translatable(Tooltip+"pitch"))
            .setSaveConsumer(d -> { config.bindingMode.pitch = d; ConfigFile.save(); })
            .build());
        bindingMode.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"yaw"), config.bindingMode.yaw)
            .setDefaultValue(0.0D)
            .setMin(-180.0D)
            .setMax(180.0D)
            .setTooltip(Text.translatable(Tooltip+"yaw"))
            .setSaveConsumer(d -> { config.bindingMode.yaw = d; ConfigFile.save(); })
            .build());
        bindingMode.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"roll"), config.bindingMode.roll)
            .setDefaultValue(0.0D)
            .setMin(-180.0D)
            .setMax(180.0D)
            .setTooltip(Text.translatable(Tooltip+"roll"))
            .setSaveConsumer(d -> { config.bindingMode.roll = d; ConfigFile.save(); })
            .build());
        
        classicMode.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"cameraX"), config.classicMode.cameraX)
            .setDefaultValue(3.25D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"cameraX"))
            .setSaveConsumer(d -> { config.classicMode.cameraX = d; ConfigFile.save(); })
            .build());
        classicMode.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"cameraY"), config.classicMode.cameraY)
            .setDefaultValue(2.0D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"cameraY"))
            .setSaveConsumer(d -> { config.classicMode.cameraY = d; ConfigFile.save(); })
            .build());
        classicMode.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"cameraZ"), config.classicMode.cameraZ)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"cameraZ"))
            .setSaveConsumer(d -> { config.classicMode.cameraZ = d; ConfigFile.save(); })
            .build());
        classicMode.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"centerY"), config.classicMode.centerY)
            .setDefaultValue(-3.4D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"centerY"))
            .setSaveConsumer(d -> { config.classicMode.centerY = d; ConfigFile.save(); })
            .build());
        classicMode.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"centerstep"), config.classicMode.centerStep)
            .setDefaultValue(0.25D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"centerstep"))
            .setSaveConsumer(d -> { config.classicMode.centerStep = d; ConfigFile.save(); })
            .build());
        
        compats.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"pehkui"), config.compats.pehkui)
            .setDefaultValue(true)
            .setTooltip(Text.translatable(Tooltip+"pehkui"))
            .setSaveConsumer(b -> { config.compats.pehkui = b; ConfigFile.save(); })
            .build());
        
        disables.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"fallflying"), config.disables.fallFlying)
            .setDefaultValue(true)
            .setTooltip(Text.translatable(Tooltip+"fallflying"))
            .setSaveConsumer(b -> { config.disables.fallFlying = b; ConfigFile.save(); })
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"swiming"), config.disables.swiming)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(Tooltip+"swiming"))
            .setSaveConsumer(b -> { config.disables.swiming = b; ConfigFile.save(); })
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"crawling"), config.disables.crawling)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(Tooltip+"crawling"))
            .setSaveConsumer(b -> { config.disables.crawling = b; ConfigFile.save(); })
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"sneaking"), config.disables.sneaking)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(Tooltip+"sneaking"))
            .setSaveConsumer(b -> { config.disables.sneaking = b; ConfigFile.save(); })
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"sleeping"), config.disables.sleeping)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(Tooltip+"sleeping"))
            .setSaveConsumer(b -> { config.disables.sleeping = b; ConfigFile.save(); })
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"scoping"), config.disables.scoping)
            .setDefaultValue(true)
            .setTooltip(Text.translatable(Tooltip+"scoping"))
            .setSaveConsumer(b -> { config.disables.scoping = b; ConfigFile.save(); })
            .build());

        return builder.build();
    }
}
