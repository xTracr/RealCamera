package com.xtracr.realcamera.config;

import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.compat.DoABarrelRollCompat;
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

        final ConfigBuilder builder = ConfigBuilder.create()
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
        general.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"renderModel"), config.general.renderModel)
            .setDefaultValue(true)
            .setTooltip(new TranslatableText(Tooltip+"renderModel"))
            .setSaveConsumer(b -> config.general.renderModel = b)
            .build());
        general.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"adjustStep"), config.general.adjustStep)
            .setDefaultValue(0.25D)
            .setMin(0.0D)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"adjustStep"))
            .setSaveConsumer(d -> config.general.adjustStep = d)
            .build());
        general.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"scale"), config.general.scale)
            .setDefaultValue(1.0D)
            .setMin(0.0D)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"scale"))
            .setSaveConsumer(d -> config.general.scale = d)
            .build());
        
        binding.addEntry(entryBuilder.startEnumSelector(new TranslatableText(Option+"vanillaModelPart"), VanillaModelPart.class, config.binding.vanillaModelPart)
            .setDefaultValue(VanillaModelPart.head)
            .setTooltip(new TranslatableText(Tooltip+"vanillaModelPart"))
            .setSaveConsumer(e -> config.binding.vanillaModelPart = e)
            .build());
        binding.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"adjustOffset"), config.binding.adjustOffset)
            .setDefaultValue(true)
            .setTooltip(new TranslatableText(Tooltip+"adjustOffset"))
            .setSaveConsumer(b -> config.binding.adjustOffset = b)
            .build());
        binding.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"bindDirection"), config.binding.bindDirection)
            .setDefaultValue(true)
            .setTooltip(new TranslatableText(Tooltip+"bindDirection"))
            .setSaveConsumer(b -> config.binding.bindDirection = b)
            .build());
        binding.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"lockRolling"), config.binding.lockRolling)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(Tooltip+"lockRolling"))
            .setSaveConsumer(b -> config.binding.lockRolling = b)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"bindingX"), config.binding.cameraX)
            .setDefaultValue(3.25D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"bindingX"))
            .setSaveConsumer(d -> config.binding.cameraX = d)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"bindingY"), config.binding.cameraY)
            .setDefaultValue(2.0D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"bindingY"))
            .setSaveConsumer(d -> config.binding.cameraY = d)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"bindingZ"), config.binding.cameraZ)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"bindingZ"))
            .setSaveConsumer(d -> config.binding.cameraZ = d)
            .build());
        binding.addEntry(entryBuilder.startFloatField(new TranslatableText(Option+"pitch"), config.binding.pitch)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setTooltip(new TranslatableText(Tooltip+"pitch"))
            .setSaveConsumer(f -> config.binding.pitch = f)
            .build());
        binding.addEntry(entryBuilder.startFloatField(new TranslatableText(Option+"yaw"), config.binding.yaw)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setTooltip(new TranslatableText(Tooltip+"yaw"))
            .setSaveConsumer(f -> config.binding.yaw = f)
            .build());
        binding.addEntry(entryBuilder.startFloatField(new TranslatableText(Option+"roll"), config.binding.roll)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setTooltip(new TranslatableText(Tooltip+"roll"))
            .setSaveConsumer(f -> config.binding.roll = f)
            .build());
        
        classic.addEntry(entryBuilder.startEnumSelector(new TranslatableText(Option+"classicAdjustMode"), ModConfig.Classic.AdjustMode.class, config.classic.adjustMode)
            .setDefaultValue(ModConfig.Classic.AdjustMode.camera)
            .setTooltip(new TranslatableText(Tooltip+"classicAdjustMode"))
            .setSaveConsumer(e -> config.classic.adjustMode = e)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"classicX"), config.classic.cameraX)
            .setDefaultValue(3.25D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"classicX"))
            .setSaveConsumer(d -> config.classic.cameraX = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"classicY"), config.classic.cameraY)
            .setDefaultValue(2.0D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"classicY"))
            .setSaveConsumer(d -> config.classic.cameraY = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"classicZ"), config.classic.cameraZ)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"classicZ"))
            .setSaveConsumer(d -> config.classic.cameraZ = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"centerX"), config.classic.centerX)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"centerX"))
            .setSaveConsumer(d -> config.classic.centerX = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"centerY"), config.classic.centerY)
            .setDefaultValue(-3.4D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"centerY"))
            .setSaveConsumer(d -> config.classic.centerY = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(new TranslatableText(Option+"centerZ"), config.classic.centerZ)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(new TranslatableText(Tooltip+"centerZ"))
            .setSaveConsumer(d -> config.classic.centerZ = d)
            .build());
        classic.addEntry(entryBuilder.startFloatField(new TranslatableText(Option+"pitch"), config.classic.pitch)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setTooltip(new TranslatableText(Tooltip+"pitch"))
            .setSaveConsumer(f -> config.classic.pitch = f)
            .build());
        classic.addEntry(entryBuilder.startFloatField(new TranslatableText(Option+"yaw"), config.classic.yaw)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setTooltip(new TranslatableText(Tooltip+"yaw"))
            .setSaveConsumer(f -> config.classic.yaw = f)
            .build());
        classic.addEntry(entryBuilder.startFloatField(new TranslatableText(Option+"roll"), config.classic.roll)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setTooltip(new TranslatableText(Tooltip+"roll"))
            .setSaveConsumer(f -> config.classic.roll = f)
            .build());
        
        compats.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"useModModel"), config.compats.useModModel)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(Tooltip+"useModModel"))
            .setSaveConsumer(b -> config.compats.useModModel = b)
            .build());
        compats.addEntry(entryBuilder.startStrField(new TranslatableText(Option+"modelModID"), config.compats.modelModID)
            .setDefaultValue("minecraft")
            .setTooltip(new TranslatableText(Tooltip+"modelModID"))
            .setSaveConsumer(s -> config.compats.modelModID = s)
            .build());
        compats.addEntry(entryBuilder.startStrField(new TranslatableText(Option+"modModelPart"), config.compats.modModelPart)
            .setDefaultValue("head")
            .setTooltip(new TranslatableText(Tooltip+"modModelPart"))
            .setSaveConsumer(s -> config.compats.modModelPart = s)
            .build());
        if (DoABarrelRollCompat.loaded)
        compats.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"doABarrelRoll"), config.compats.doABarrelRoll)
            .setDefaultValue(true)
            .setTooltip(new TranslatableText(Tooltip+"doABarrelRoll"))
            .setSaveConsumer(b -> config.compats.doABarrelRoll = b)
            .build());
        if (PehkuiCompat.loaded)
        compats.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"pehkui"), config.compats.pehkui)
            .setDefaultValue(true)
            .setTooltip(new TranslatableText(Tooltip+"pehkui"))
            .setSaveConsumer(b -> config.compats.pehkui = b)
            .build());
        
        disables.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(Option+"fallFlying"), config.disables.fallFlying)
            .setDefaultValue(true)
            .setTooltip(new TranslatableText(Tooltip+"fallFlying"))
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
