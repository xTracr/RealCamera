package com.xtracr.realcamera.config;

import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.compat.PehkuiCompat;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

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
            .setTitle(Text.translatable("config.title.xtracr_"+RealCamera.MODID));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable(Category+"general"));
        ConfigCategory binding = builder.getOrCreateCategory(Text.translatable(Category+"binding"));
        ConfigCategory classic = builder.getOrCreateCategory(Text.translatable(Category+"classic"));
        ConfigCategory compats = builder.getOrCreateCategory(Text.translatable(Category+"compats"));
        ConfigCategory disables = builder.getOrCreateCategory(Text.translatable(Category+"disables"));

        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"enabled"), config.general.enabled)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(Tooltip+"enabled"))
            .setSaveConsumer(b -> config.general.enabled = b)
            .build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"classic"), config.general.classic)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(Tooltip+"classic"))
            .setSaveConsumer(b -> config.general.classic = b)
            .build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"renderModel"), config.general.renderModel)
            .setDefaultValue(true)
            .setTooltip(Text.translatable(Tooltip+"renderModel"))
            .setSaveConsumer(b -> config.general.renderModel = b)
            .build());
        general.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"adjustStep"), config.general.adjustStep)
            .setDefaultValue(0.25D)
            .setMin(0.0D)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"adjustStep"))
            .setSaveConsumer(d -> config.general.adjustStep = d)
            .build());
        general.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"scale"), config.general.scale)
            .setDefaultValue(1.0D)
            .setMin(0.0D)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"scale"))
            .setSaveConsumer(d -> config.general.scale = d)
            .build());
        
        binding.addEntry(entryBuilder.startEnumSelector(Text.translatable(Option+"vanillaModelPart"), VanillaModelPart.class, config.binding.vanillaModelPart)
            .setDefaultValue(VanillaModelPart.head)
            .setTooltip(Text.translatable(Tooltip+"vanillaModelPart"))
            .setSaveConsumer(e -> config.binding.vanillaModelPart = e)
            .build());
        binding.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"bindDirection"), config.binding.bindDirection)
            .setDefaultValue(true)
            .setTooltip(Text.translatable(Tooltip+"bindDirection"))
            .setSaveConsumer(b -> config.binding.bindDirection = b)
            .build());
        binding.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"lockRolling"), config.binding.lockRolling)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(Tooltip+"lockRolling"))
            .setSaveConsumer(b -> config.binding.lockRolling = b)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"bindingX"), config.binding.bindingX)
            .setDefaultValue(3.25D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"bindingX"))
            .setSaveConsumer(d -> config.binding.bindingX = d)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"bindingY"), config.binding.bindingY)
            .setDefaultValue(2.0D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"bindingY"))
            .setSaveConsumer(d -> config.binding.bindingY = d)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"bindingZ"), config.binding.bindingZ)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"bindingZ"))
            .setSaveConsumer(d -> config.binding.bindingZ = d)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"pitch"), config.binding.pitch)
            .setDefaultValue(0.0D)
            .setMin(-180.0D)
            .setMax(180.0D)
            .setTooltip(Text.translatable(Tooltip+"pitch"))
            .setSaveConsumer(d -> config.binding.pitch = d)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"yaw"), config.binding.yaw)
            .setDefaultValue(0.0D)
            .setMin(-180.0D)
            .setMax(180.0D)
            .setTooltip(Text.translatable(Tooltip+"yaw"))
            .setSaveConsumer(d -> config.binding.yaw = d)
            .build());
        binding.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"roll"), config.binding.roll)
            .setDefaultValue(0.0D)
            .setMin(-180.0D)
            .setMax(180.0D)
            .setTooltip(Text.translatable(Tooltip+"roll"))
            .setSaveConsumer(d -> config.binding.roll = d)
            .build());
        
        classic.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"adjustCamera"), config.classic.adjustCamera)
            .setDefaultValue(true)
            .setTooltip(Text.translatable(Tooltip+"adjustCamera"))
            .setSaveConsumer(b -> config.classic.adjustCamera = b)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"cameraX"), config.classic.cameraX)
            .setDefaultValue(3.25D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"cameraX"))
            .setSaveConsumer(d -> config.classic.cameraX = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"cameraY"), config.classic.cameraY)
            .setDefaultValue(2.0D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"cameraY"))
            .setSaveConsumer(d -> config.classic.cameraY = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"cameraZ"), config.classic.cameraZ)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"cameraZ"))
            .setSaveConsumer(d -> config.classic.cameraZ = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"centerX"), config.classic.centerX)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"centerX"))
            .setSaveConsumer(d -> config.classic.centerX = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"centerY"), config.classic.centerY)
            .setDefaultValue(-3.4D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"centerY"))
            .setSaveConsumer(d -> config.classic.centerY = d)
            .build());
        classic.addEntry(entryBuilder.startDoubleField(Text.translatable(Option+"centerZ"), config.classic.centerZ)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.minVALUE)
            .setMax(ModConfig.maxVALUE)
            .setTooltip(Text.translatable(Tooltip+"centerZ"))
            .setSaveConsumer(d -> config.classic.centerZ = d)
            .build());
        
        compats.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"useModModel"), config.compats.useModModel)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(Tooltip+"useModModel"))
            .setSaveConsumer(b -> config.compats.useModModel = b)
            .build());
        compats.addEntry(entryBuilder.startStrField(Text.translatable(Option+"modelModID"), config.compats.modelModID)
            .setDefaultValue("minecraft")
            .setTooltip(Text.translatable(Tooltip+"modelModID"))
            .setSaveConsumer(s -> config.compats.modelModID = s)
            .build());
        compats.addEntry(entryBuilder.startStrField(Text.translatable(Option+"modModelPart"), config.compats.modModelPart)
            .setDefaultValue("head")
            .setTooltip(Text.translatable(Tooltip+"modModelPart"))
            .setSaveConsumer(s -> config.compats.modModelPart = s)
            .build());
        if (PehkuiCompat.loaded)
        compats.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"pehkui"), config.compats.pehkui)
            .setDefaultValue(true)
            .setTooltip(Text.translatable(Tooltip+"pehkui"))
            .setSaveConsumer(b -> config.compats.pehkui = b)
            .build());
        
        disables.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"fallFlying"), config.disables.fallFlying)
            .setDefaultValue(true)
            .setTooltip(Text.translatable(Tooltip+"fallFlying"))
            .setSaveConsumer(b -> config.disables.fallFlying = b)
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"swiming"), config.disables.swiming)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(Tooltip+"swiming"))
            .setSaveConsumer(b -> config.disables.swiming = b)
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"crawling"), config.disables.crawling)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(Tooltip+"crawling"))
            .setSaveConsumer(b -> config.disables.crawling = b)
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"sneaking"), config.disables.sneaking)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(Tooltip+"sneaking"))
            .setSaveConsumer(b -> config.disables.sneaking = b)
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"sleeping"), config.disables.sleeping)
            .setDefaultValue(false)
            .setTooltip(Text.translatable(Tooltip+"sleeping"))
            .setSaveConsumer(b -> config.disables.sleeping = b)
            .build());
        disables.addEntry(entryBuilder.startBooleanToggle(Text.translatable(Option+"scoping"), config.disables.scoping)
            .setDefaultValue(true)
            .setTooltip(Text.translatable(Tooltip+"scoping"))
            .setSaveConsumer(b -> config.disables.scoping = b)
            .build());

        return builder.build();
    }
}
