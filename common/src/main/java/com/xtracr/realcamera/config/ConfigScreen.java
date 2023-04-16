package com.xtracr.realcamera.config;

import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.compat.PehkuiCompat;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.TranslatableText;

public class ConfigScreen {
    
    public static final String Category = "config.category.xtracr_"+RealCamera.MODID+"_";
    public static final String Option = "config.option.xtracr_"+RealCamera.MODID+"_";
    public static final String Tooltip = "config.tooltip.xtracr_"+RealCamera.MODID+"_";
    
    public static Screen create(Screen parent) {

        ConfigFile.load();
        final ModConfig config = ConfigFile.modConfig;

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setSavingRunnable(ConfigFile::save)
            .setTitle(new TranslatableText("config.title.xtracr_"+RealCamera.MODID));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(new TranslatableText(Category+"general"));
        ConfigCategory binding = builder.getOrCreateCategory(new TranslatableText(Category+"binding"));
        ConfigCategory classic = builder.getOrCreateCategory(new TranslatableText(Category+"classic"));
        ConfigCategory compats = builder.getOrCreateCategory(new TranslatableText(Category+"compats"));
        ConfigCategory disables = builder.getOrCreateCategory(new TranslatableText(Category+"disables"));

        general.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"enabled"), config.general.enabled)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(Tooltip+"enabled"))
            .setSaveConsumer(b -> config.general.enabled = b)
            .build());
        general.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"classic"), config.general.classic)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(Tooltip+"classic"))
            .setSaveConsumer(b -> config.general.classic = b)
            .build());
        general.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"rendermodel"), config.general.renderModel)
            .setDefaultValue(true)
            .setTooltip(new TranslatableText(Tooltip+"rendermodel"))
            .setSaveConsumer(b -> config.general.renderModel = b)
            .build());
        general.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"camerastep"), config.general.cameraStep)
            .setDefaultValue(0.25D)
            .setMin(0.0D)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"camerastep"))
            .setSaveConsumer(d -> config.general.cameraStep = d)
            .build());
        general.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"scale"), config.general.scale)
            .setDefaultValue(1.0D)
            .setMin(0.0D)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"scale"))
            .setSaveConsumer(d -> config.general.scale = d)
            .build());
        
        binding.addEntry(entryBuilder.startEnumSelector(new TranslatableText(Option+"vanillamodelpart"), VanillaModelPart.class, config.binding.vanillaModelPart)
            .setDefaultValue(VanillaModelPart.head)
            .setTooltip(new TranslatableText(Tooltip+"vanillamodelpart"))
            .setSaveConsumer(e -> config.binding.vanillaModelPart = e)
            .build());
        binding.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"binddirection"), config.binding.bindDirection)
            .setDefaultValue(true)
            .setTooltip(new TranslatableText(Tooltip+"binddirection"))
            .setSaveConsumer(b -> config.binding.bindDirection = b)
            .build());
        binding.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"lockrolling"), config.binding.lockRolling)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(Tooltip+"lockrolling"))
            .setSaveConsumer(b -> config.binding.lockRolling = b)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"bindingX"), config.binding.bindingX)
            .setDefaultValue(3.25D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"bindingX"))
            .setSaveConsumer(d -> config.binding.bindingX = d)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"bindingY"), config.binding.bindingY)
            .setDefaultValue(2.0D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"bindingY"))
            .setSaveConsumer(d -> config.binding.bindingY = d)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"bindingZ"), config.binding.bindingZ)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"bindingZ"))
            .setSaveConsumer(d -> config.binding.bindingZ = d)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"pitch"), config.binding.pitch)
            .setDefaultValue(0.0D)
            .setMin(-180.0D)
            .setMax(180.0D)
            .setTooltip(new TranslatableText(Tooltip+"pitch"))
            .setSaveConsumer(d -> config.binding.pitch = d)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"yaw"), config.binding.yaw)
            .setDefaultValue(0.0D)
            .setMin(-180.0D)
            .setMax(180.0D)
            .setTooltip(new TranslatableText(Tooltip+"yaw"))
            .setSaveConsumer(d -> config.binding.yaw = d)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"roll"), config.binding.roll)
            .setDefaultValue(0.0D)
            .setMin(-180.0D)
            .setMax(180.0D)
            .setTooltip(new TranslatableText(Tooltip+"roll"))
            .setSaveConsumer(d -> config.binding.roll = d)
            .build());
        
        classic.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"cameraX"), config.classic.cameraX)
            .setDefaultValue(3.25D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"cameraX"))
            .setSaveConsumer(d -> config.classic.cameraX = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"cameraY"), config.classic.cameraY)
            .setDefaultValue(2.0D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"cameraY"))
            .setSaveConsumer(d -> config.classic.cameraY = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"cameraZ"), config.classic.cameraZ)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"cameraZ"))
            .setSaveConsumer(d -> config.classic.cameraZ = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"centerY"), config.classic.centerY)
            .setDefaultValue(-3.4D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"centerY"))
            .setSaveConsumer(d -> config.classic.centerY = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"centerstep"), config.classic.centerStep)
            .setDefaultValue(0.25D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"centerstep"))
            .setSaveConsumer(d -> config.classic.centerStep = d)
            .build());
        
        compats.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"usemodmodel"), config.compats.useModModel)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(Tooltip+"usemodmodel"))
            .setSaveConsumer(b -> config.compats.useModModel = b)
            .build());
        compats.addEntry(entryBuilder.startStrField(new TranslatableText(Option+"modelmodid"), config.compats.modelModID)
            .setDefaultValue("minecraft")
            .setTooltip(new TranslatableText(Tooltip+"modelmodid"))
            .setSaveConsumer(s -> config.compats.modelModID = s)
            .build());
        compats.addEntry(entryBuilder.startStrField(new TranslatableText(Option+"modmodelpart"), config.compats.modModelPart)
            .setDefaultValue("head")
            .setTooltip(new TranslatableText(Tooltip+"modmodelpart"))
            .setSaveConsumer(s -> config.compats.modModelPart = s)
            .build());
        if (PehkuiCompat.loaded)
        compats.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"pehkui"), config.compats.pehkui)
            .setDefaultValue(true)
            .setTooltip(new TranslatableText(Tooltip+"pehkui"))
            .setSaveConsumer(b -> config.compats.pehkui = b)
            .build());
        
        disables.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"fallflying"), config.disables.fallFlying)
            .setDefaultValue(true)
            .setTooltip(new TranslatableText(Tooltip+"fallflying"))
            .setSaveConsumer(b -> config.disables.fallFlying = b)
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"swiming"), config.disables.swiming)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(Tooltip+"swiming"))
            .setSaveConsumer(b -> config.disables.swiming = b)
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"crawling"), config.disables.crawling)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(Tooltip+"crawling"))
            .setSaveConsumer(b -> config.disables.crawling = b)
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"sneaking"), config.disables.sneaking)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(Tooltip+"sneaking"))
            .setSaveConsumer(b -> config.disables.sneaking = b)
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"sleeping"), config.disables.sleeping)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(Tooltip+"sleeping"))
            .setSaveConsumer(b -> config.disables.sleeping = b)
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"scoping"), config.disables.scoping)
            .setDefaultValue(true)
            .setTooltip(new TranslatableText(Tooltip+"scoping"))
            .setSaveConsumer(b -> config.disables.scoping = b)
            .build());

        return builder.build();
    }
}
