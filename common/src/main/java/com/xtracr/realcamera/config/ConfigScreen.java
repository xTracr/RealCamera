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
    
    public static final String CATEGORY = "config.category.xtracr_"+RealCamera.MODID+"_";
    public static final String OPTION = "config.option.xtracr_"+RealCamera.MODID+"_";
    public static final String TOOLTIP = "config.tooltip.xtracr_"+RealCamera.MODID+"_";
    
    public static Screen create(Screen parent) {

        ConfigFile.load();
        final ModConfig config = ConfigFile.modConfig;

        final ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setSavingRunnable(ConfigFile::save)
            .setTitle(new TranslatableText("config.title.xtracr_"+RealCamera.MODID));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(new TranslatableText(CATEGORY+"general"));
        ConfigCategory binding = builder.getOrCreateCategory(new TranslatableText(CATEGORY+"binding"));
        ConfigCategory classic = builder.getOrCreateCategory(new TranslatableText(CATEGORY+"classic"));
        ConfigCategory compats = builder.getOrCreateCategory(new TranslatableText(CATEGORY+"compats"));
        ConfigCategory disables = builder.getOrCreateCategory(new TranslatableText(CATEGORY+"disables"));

        general.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"enabled"), config.general.enabled)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(TOOLTIP+"enabled"))
            .setSaveConsumer(b -> config.general.enabled = b)
            .build());
        general.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"classic"), config.general.classic)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(TOOLTIP+"classic"))
            .setSaveConsumer(b -> config.general.classic = b)
            .build());
        general.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"dynamicCrosshair"), config.general.dynamicCrosshair)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(TOOLTIP+"dynamicCrosshair"))
            .setSaveConsumer(b -> config.general.dynamicCrosshair = b)
            .build());
        general.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"renderModel"), config.general.renderModel)
            .setDefaultValue(true)
            .setTooltip(new TranslatableText(TOOLTIP+"renderModel"))
            .setSaveConsumer(b -> config.general.renderModel = b)
            .build());
        general.addEntry(entryBuilder.startDoubleField(new TranslatableText(OPTION+"adjustStep"), config.general.adjustStep)
            .setDefaultValue(0.25D)
            .setMin(0.0D)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(new TranslatableText(TOOLTIP+"adjustStep"))
            .setSaveConsumer(d -> config.general.adjustStep = d)
            .build());
        general.addEntry(entryBuilder.startDoubleField(new TranslatableText(OPTION+"scale"), config.general.scale)
            .setDefaultValue(1.0D)
            .setMin(0.0D)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(new TranslatableText(TOOLTIP+"scale"))
            .setSaveConsumer(d -> config.general.scale = d)
            .build());
        
        binding.addEntry(entryBuilder.startEnumSelector(new TranslatableText(OPTION+"vanillaModelPart"), VanillaModelPart.class, config.binding.vanillaModelPart)
            .setDefaultValue(VanillaModelPart.head)
            .setTooltip(new TranslatableText(TOOLTIP+"vanillaModelPart"))
            .setSaveConsumer(e -> config.binding.vanillaModelPart = e)
            .build());
        binding.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"adjustOffset"), config.binding.adjustOffset)
            .setDefaultValue(true)
            .setTooltip(new TranslatableText(TOOLTIP+"adjustOffset"))
            .setSaveConsumer(b -> config.binding.adjustOffset = b)
            .build());
        binding.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"bindDirection"), config.binding.bindDirection)
            .setDefaultValue(true)
            .setTooltip(new TranslatableText(TOOLTIP+"bindDirection"))
            .setSaveConsumer(b -> config.binding.bindDirection = b)
            .build());
        binding.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"lockRolling"), config.binding.lockRolling)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(TOOLTIP+"lockRolling"))
            .setSaveConsumer(b -> config.binding.lockRolling = b)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(new TranslatableText(OPTION+"bindingX"), config.binding.cameraX)
            .setDefaultValue(3.25D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(new TranslatableText(TOOLTIP+"bindingX"))
            .setSaveConsumer(d -> config.binding.cameraX = d)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(new TranslatableText(OPTION+"bindingY"), config.binding.cameraY)
            .setDefaultValue(2.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(new TranslatableText(TOOLTIP+"bindingY"))
            .setSaveConsumer(d -> config.binding.cameraY = d)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(new TranslatableText(OPTION+"bindingZ"), config.binding.cameraZ)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(new TranslatableText(TOOLTIP+"bindingZ"))
            .setSaveConsumer(d -> config.binding.cameraZ = d)
            .build());
        binding.addEntry(entryBuilder.startFloatField(new TranslatableText(OPTION+"pitch"), config.binding.pitch)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setTooltip(new TranslatableText(TOOLTIP+"pitch"))
            .setSaveConsumer(f -> config.binding.pitch = f)
            .build());
        binding.addEntry(entryBuilder.startFloatField(new TranslatableText(OPTION+"yaw"), config.binding.yaw)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setTooltip(new TranslatableText(TOOLTIP+"yaw"))
            .setSaveConsumer(f -> config.binding.yaw = f)
            .build());
        binding.addEntry(entryBuilder.startFloatField(new TranslatableText(OPTION+"roll"), config.binding.roll)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setTooltip(new TranslatableText(TOOLTIP+"roll"))
            .setSaveConsumer(f -> config.binding.roll = f)
            .build());
        
        classic.addEntry(entryBuilder.startEnumSelector(new TranslatableText(OPTION+"classicAdjustMode"), ModConfig.Classic.AdjustMode.class, config.classic.adjustMode)
            .setDefaultValue(ModConfig.Classic.AdjustMode.CAMERA)
            .setTooltip(new TranslatableText(TOOLTIP+"classicAdjustMode"))
            .setSaveConsumer(e -> config.classic.adjustMode = e)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(new TranslatableText(OPTION+"classicX"), config.classic.cameraX)
            .setDefaultValue(3.25D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(new TranslatableText(TOOLTIP+"classicX"))
            .setSaveConsumer(d -> config.classic.cameraX = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(new TranslatableText(OPTION+"classicY"), config.classic.cameraY)
            .setDefaultValue(2.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(new TranslatableText(TOOLTIP+"classicY"))
            .setSaveConsumer(d -> config.classic.cameraY = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(new TranslatableText(OPTION+"classicZ"), config.classic.cameraZ)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(new TranslatableText(TOOLTIP+"classicZ"))
            .setSaveConsumer(d -> config.classic.cameraZ = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(new TranslatableText(OPTION+"centerX"), config.classic.centerX)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(new TranslatableText(TOOLTIP+"centerX"))
            .setSaveConsumer(d -> config.classic.centerX = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(new TranslatableText(OPTION+"centerY"), config.classic.centerY)
            .setDefaultValue(-3.4D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(new TranslatableText(TOOLTIP+"centerY"))
            .setSaveConsumer(d -> config.classic.centerY = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(new TranslatableText(OPTION+"centerZ"), config.classic.centerZ)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(new TranslatableText(TOOLTIP+"centerZ"))
            .setSaveConsumer(d -> config.classic.centerZ = d)
            .build());
        classic.addEntry(entryBuilder.startFloatField(new TranslatableText(OPTION+"pitch"), config.classic.pitch)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setTooltip(new TranslatableText(TOOLTIP+"pitch"))
            .setSaveConsumer(f -> config.classic.pitch = f)
            .build());
        classic.addEntry(entryBuilder.startFloatField(new TranslatableText(OPTION+"yaw"), config.classic.yaw)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setTooltip(new TranslatableText(TOOLTIP+"yaw"))
            .setSaveConsumer(f -> config.classic.yaw = f)
            .build());
        classic.addEntry(entryBuilder.startFloatField(new TranslatableText(OPTION+"roll"), config.classic.roll)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setTooltip(new TranslatableText(TOOLTIP+"roll"))
            .setSaveConsumer(f -> config.classic.roll = f)
            .build());
        
        compats.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"useModModel"), config.compats.useModModel)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(TOOLTIP+"useModModel"))
            .setSaveConsumer(b -> config.compats.useModModel = b)
            .build());
        compats.addEntry(entryBuilder.startStrField(new TranslatableText(OPTION+"modelModID"), config.compats.modelModID)
            .setDefaultValue("minecraft")
            .setTooltip(new TranslatableText(TOOLTIP+"modelModID"))
            .setSaveConsumer(s -> config.compats.modelModID = s)
            .build());
        compats.addEntry(entryBuilder.startStrField(new TranslatableText(OPTION+"modModelPart"), config.compats.modModelPart)
            .setDefaultValue("head")
            .setTooltip(new TranslatableText(TOOLTIP+"modModelPart"))
            .setSaveConsumer(s -> config.compats.modModelPart = s)
            .build());
        if (DoABarrelRollCompat.loaded)
        compats.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"doABarrelRoll"), config.compats.doABarrelRoll)
            .setDefaultValue(true)
            .setTooltip(new TranslatableText(TOOLTIP+"doABarrelRoll"))
            .setSaveConsumer(b -> config.compats.doABarrelRoll = b)
            .build());
        if (PehkuiCompat.loaded)
        compats.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"pehkui"), config.compats.pehkui)
            .setDefaultValue(true)
            .setTooltip(new TranslatableText(TOOLTIP+"pehkui"))
            .setSaveConsumer(b -> config.compats.pehkui = b)
            .build());
        
        disables.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"fallFlying"), config.disables.fallFlying)
            .setDefaultValue(true)
            .setTooltip(new TranslatableText(TOOLTIP+"fallFlying"))
            .setSaveConsumer(b -> config.disables.fallFlying = b)
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"swiming"), config.disables.swiming)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(TOOLTIP+"swiming"))
            .setSaveConsumer(b -> config.disables.swiming = b)
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"crawling"), config.disables.crawling)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(TOOLTIP+"crawling"))
            .setSaveConsumer(b -> config.disables.crawling = b)
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"sneaking"), config.disables.sneaking)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(TOOLTIP+"sneaking"))
            .setSaveConsumer(b -> config.disables.sneaking = b)
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"sleeping"), config.disables.sleeping)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(TOOLTIP+"sleeping"))
            .setSaveConsumer(b -> config.disables.sleeping = b)
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"scoping"), config.disables.scoping)
            .setDefaultValue(true)
            .setTooltip(new TranslatableText(TOOLTIP+"scoping"))
            .setSaveConsumer(b -> config.disables.scoping = b)
            .build());

        return builder.build();
    }
}
