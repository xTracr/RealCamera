package com.xtracr.realcamera.config;

import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.compat.DoABarrelRollCompat;
import com.xtracr.realcamera.compat.PehkuiCompat;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

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
            .setTitle(Text.translatable("config.title.xtracr_"+RealCamera.MODID));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable(CATEGORY+"general"));
        ConfigCategory binding = builder.getOrCreateCategory(Text.translatable(CATEGORY+"binding"));
        ConfigCategory classic = builder.getOrCreateCategory(Text.translatable(CATEGORY+"classic"));
        ConfigCategory compats = builder.getOrCreateCategory(Text.translatable(CATEGORY+"compats"));
        ConfigCategory disables = builder.getOrCreateCategory(Text.translatable(CATEGORY+"disables"));

        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION+"enabled"), config.general.enabled)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(TOOLTIP+"enabled"))
            .setSaveConsumer(b -> config.general.enabled = b)
            .build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION+"classic"), config.general.classic)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(TOOLTIP+"classic"))
            .setSaveConsumer(b -> config.general.classic = b)
            .build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION+"dynamicCrosshair"), config.general.dynamicCrosshair)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(TOOLTIP+"dynamicCrosshair"))
            .setSaveConsumer(b -> config.general.dynamicCrosshair = b)
            .build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION+"renderModel"), config.general.renderModel)
            .setDefaultValue(true)
            .setTooltip(Text.translatable(TOOLTIP+"renderModel"))
            .setSaveConsumer(b -> config.general.renderModel = b)
            .build());
        general.addEntry(entryBuilder.startDoubleField(Text.translatable(OPTION+"adjustStep"), config.general.adjustStep)
            .setDefaultValue(0.25D)
            .setMin(0.0D)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(Text.translatable(TOOLTIP+"adjustStep"))
            .setSaveConsumer(d -> config.general.adjustStep = d)
            .build());
        general.addEntry(entryBuilder.startDoubleField(Text.translatable(OPTION+"scale"), config.general.scale)
            .setDefaultValue(1.0D)
            .setMin(0.0D)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(Text.translatable(TOOLTIP+"scale"))
            .setSaveConsumer(d -> config.general.scale = d)
            .build());
        
        binding.addEntry(entryBuilder.startEnumSelector(Text.translatable(OPTION+"vanillaModelPart"), VanillaModelPart.class, config.binding.vanillaModelPart)
            .setDefaultValue(VanillaModelPart.head)
            .setTooltip(Text.translatable(TOOLTIP+"vanillaModelPart"))
            .setSaveConsumer(e -> config.binding.vanillaModelPart = e)
            .build());
        binding.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION+"adjustOffset"), config.binding.adjustOffset)
            .setDefaultValue(true)
            .setTooltip(Text.translatable(TOOLTIP+"adjustOffset"))
            .setSaveConsumer(b -> config.binding.adjustOffset = b)
            .build());
        binding.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION+"bindDirection"), config.binding.bindDirection)
            .setDefaultValue(true)
            .setTooltip(Text.translatable(TOOLTIP+"bindDirection"))
            .setSaveConsumer(b -> config.binding.bindDirection = b)
            .build());
        binding.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION+"lockRolling"), config.binding.lockRolling)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(TOOLTIP+"lockRolling"))
            .setSaveConsumer(b -> config.binding.lockRolling = b)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(Text.translatable(OPTION+"bindingX"), config.binding.cameraX)
            .setDefaultValue(3.25D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(Text.translatable(TOOLTIP+"bindingX"))
            .setSaveConsumer(d -> config.binding.cameraX = d)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(Text.translatable(OPTION+"bindingY"), config.binding.cameraY)
            .setDefaultValue(2.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(Text.translatable(TOOLTIP+"bindingY"))
            .setSaveConsumer(d -> config.binding.cameraY = d)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(Text.translatable(OPTION+"bindingZ"), config.binding.cameraZ)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(Text.translatable(TOOLTIP+"bindingZ"))
            .setSaveConsumer(d -> config.binding.cameraZ = d)
            .build());
        binding.addEntry(entryBuilder.startFloatField(Text.translatable(OPTION+"pitch"), config.binding.pitch)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setTooltip(Text.translatable(TOOLTIP+"pitch"))
            .setSaveConsumer(f -> config.binding.pitch = f)
            .build());
        binding.addEntry(entryBuilder.startFloatField(Text.translatable(OPTION+"yaw"), config.binding.yaw)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setTooltip(Text.translatable(TOOLTIP+"yaw"))
            .setSaveConsumer(f -> config.binding.yaw = f)
            .build());
        binding.addEntry(entryBuilder.startFloatField(Text.translatable(OPTION+"roll"), config.binding.roll)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setTooltip(Text.translatable(TOOLTIP+"roll"))
            .setSaveConsumer(f -> config.binding.roll = f)
            .build());
        
        classic.addEntry(entryBuilder.startEnumSelector(Text.translatable(OPTION+"classicAdjustMode"), ModConfig.Classic.AdjustMode.class, config.classic.adjustMode)
            .setDefaultValue(ModConfig.Classic.AdjustMode.CAMERA)
            .setTooltip(Text.translatable(TOOLTIP+"classicAdjustMode"))
            .setSaveConsumer(e -> config.classic.adjustMode = e)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(Text.translatable(OPTION+"classicX"), config.classic.cameraX)
            .setDefaultValue(3.25D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(Text.translatable(TOOLTIP+"classicX"))
            .setSaveConsumer(d -> config.classic.cameraX = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(Text.translatable(OPTION+"classicY"), config.classic.cameraY)
            .setDefaultValue(2.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(Text.translatable(TOOLTIP+"classicY"))
            .setSaveConsumer(d -> config.classic.cameraY = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(Text.translatable(OPTION+"classicZ"), config.classic.cameraZ)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(Text.translatable(TOOLTIP+"classicZ"))
            .setSaveConsumer(d -> config.classic.cameraZ = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(Text.translatable(OPTION+"centerX"), config.classic.centerX)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(Text.translatable(TOOLTIP+"centerX"))
            .setSaveConsumer(d -> config.classic.centerX = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(Text.translatable(OPTION+"centerY"), config.classic.centerY)
            .setDefaultValue(-3.4D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(Text.translatable(TOOLTIP+"centerY"))
            .setSaveConsumer(d -> config.classic.centerY = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(Text.translatable(OPTION+"centerZ"), config.classic.centerZ)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setTooltip(Text.translatable(TOOLTIP+"centerZ"))
            .setSaveConsumer(d -> config.classic.centerZ = d)
            .build());
        classic.addEntry(entryBuilder.startFloatField(Text.translatable(OPTION+"pitch"), config.classic.pitch)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setTooltip(Text.translatable(TOOLTIP+"pitch"))
            .setSaveConsumer(f -> config.classic.pitch = f)
            .build());
        classic.addEntry(entryBuilder.startFloatField(Text.translatable(OPTION+"yaw"), config.classic.yaw)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setTooltip(Text.translatable(TOOLTIP+"yaw"))
            .setSaveConsumer(f -> config.classic.yaw = f)
            .build());
        classic.addEntry(entryBuilder.startFloatField(Text.translatable(OPTION+"roll"), config.classic.roll)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setTooltip(Text.translatable(TOOLTIP+"roll"))
            .setSaveConsumer(f -> config.classic.roll = f)
            .build());
        
        compats.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION+"useModModel"), config.compats.useModModel)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(TOOLTIP+"useModModel"))
            .setSaveConsumer(b -> config.compats.useModModel = b)
            .build());
        compats.addEntry(entryBuilder.startStrField(Text.translatable(OPTION+"modelModID"), config.compats.modelModID)
            .setDefaultValue("minecraft")
            .setTooltip(Text.translatable(TOOLTIP+"modelModID"))
            .setSaveConsumer(s -> config.compats.modelModID = s)
            .build());
        compats.addEntry(entryBuilder.startStrField(Text.translatable(OPTION+"modModelPart"), config.compats.modModelPart)
            .setDefaultValue("head")
            .setTooltip(Text.translatable(TOOLTIP+"modModelPart"))
            .setSaveConsumer(s -> config.compats.modModelPart = s)
            .build());
        if (DoABarrelRollCompat.loaded)
        compats.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION+"doABarrelRoll"), config.compats.doABarrelRoll)
            .setDefaultValue(true)
            .setTooltip(Text.translatable(TOOLTIP+"doABarrelRoll"))
            .setSaveConsumer(b -> config.compats.doABarrelRoll = b)
            .build());
        if (PehkuiCompat.loaded)
        compats.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION+"pehkui"), config.compats.pehkui)
            .setDefaultValue(true)
            .setTooltip(Text.translatable(TOOLTIP+"pehkui"))
            .setSaveConsumer(b -> config.compats.pehkui = b)
            .build());
        
        disables.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION+"fallFlying"), config.disables.fallFlying)
            .setDefaultValue(true)
            .setTooltip(Text.translatable(TOOLTIP+"fallFlying"))
            .setSaveConsumer(b -> config.disables.fallFlying = b)
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION+"swiming"), config.disables.swiming)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(TOOLTIP+"swiming"))
            .setSaveConsumer(b -> config.disables.swiming = b)
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION+"crawling"), config.disables.crawling)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(TOOLTIP+"crawling"))
            .setSaveConsumer(b -> config.disables.crawling = b)
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION+"sneaking"), config.disables.sneaking)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(TOOLTIP+"sneaking"))
            .setSaveConsumer(b -> config.disables.sneaking = b)
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION+"sleeping"), config.disables.sleeping)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(TOOLTIP+"sleeping"))
            .setSaveConsumer(b -> config.disables.sleeping = b)
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION+"scoping"), config.disables.scoping)
            .setDefaultValue(true)
            .setTooltip(Text.translatable(TOOLTIP+"scoping"))
            .setSaveConsumer(b -> config.disables.scoping = b)
            .build());

        return builder.build();
    }
}
