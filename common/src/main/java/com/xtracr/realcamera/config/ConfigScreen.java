package com.xtracr.realcamera.config;

import com.xtracr.realcamera.RealCamera;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ConfigScreen {
    private static final String CATEGORY = "config.category." + RealCamera.FULL_ID + ".";
    private static final String OPTION = "config.option." + RealCamera.FULL_ID + ".";
    private static final String TOOLTIP = "config.tooltip." + RealCamera.FULL_ID + ".";

    public static Screen create(Screen parent) {
        ConfigFile.load();
        final ModConfig config = ConfigFile.modConfig;
        final ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .transparentBackground()
                .setSavingRunnable(ConfigFile::save)
                .setTitle(Text.translatable("config.title." + RealCamera.FULL_ID));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable(CATEGORY + "general"));
        ConfigCategory classic = builder.getOrCreateCategory(Text.translatable(CATEGORY + "classic"));
        ConfigCategory binding = builder.getOrCreateCategory(Text.translatable(CATEGORY + "binding"));

        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "enabled"), config.enabled)
                .setSaveConsumer(b -> config.enabled = b)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "isClassic"), config.isClassic)
                .setDefaultValue(false)
                .setTooltip(Text.translatable(TOOLTIP + "isClassic"))
                .setSaveConsumer(b -> config.isClassic = b)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "dynamicCrosshair"), config.dynamicCrosshair)
                .setDefaultValue(false)
                .setTooltip(Text.translatable(TOOLTIP + "dynamicCrosshair"))
                .setSaveConsumer(b -> config.dynamicCrosshair = b)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "renderModel"), config.renderModel)
                .setDefaultValue(true)
                .setTooltip(Text.translatable(TOOLTIP + "renderModel"))
                .setSaveConsumer(b -> config.renderModel = b)
                .build());
        general.addEntry(entryBuilder.startDoubleField(Text.translatable(OPTION + "adjustStep"), config.adjustStep)
                .setDefaultValue(0.01)
                .setMin(0.0)
                .setMax(ModConfig.MAX_DOUBLE)
                .setTooltip(Text.translatable(TOOLTIP + "adjustStep"))
                .setSaveConsumer(d -> config.adjustStep = d)
                .build());

        classic.addEntry(entryBuilder.startEnumSelector(Text.translatable(OPTION + "classicAdjustMode"), ModConfig.Classic.AdjustMode.class, config.classic.adjustMode)
                .setDefaultValue(ModConfig.Classic.AdjustMode.CAMERA)
                .setTooltip(Text.translatable(TOOLTIP + "classicAdjustMode"))
                .setSaveConsumer(e -> config.classic.adjustMode = e)
                .build());
        classic.addEntry(entryBuilder.startDoubleField(Text.translatable(OPTION + "scale"), config.classic.scale)
                .setDefaultValue(8.0)
                .setMin(0.0)
                .setMax(64.0)
                .setTooltip(Text.translatable(TOOLTIP + "scale"))
                .setSaveConsumer(d -> config.classic.scale = d)
                .build());
        SubCategoryBuilder classicCameraOffset = entryBuilder.startSubCategory(Text.translatable(CATEGORY + "cameraOffset"))
                .setTooltip(Text.translatable(TOOLTIP + "classicOffset"), Text.translatable(TOOLTIP + "classicOffset_n"));
        classicCameraOffset.add(entryBuilder.startDoubleField(Text.translatable(OPTION + "cameraOffset", "X"), config.classic.cameraX)
                .setDefaultValue(-0.5)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.classic.cameraX = d)
                .build());
        classicCameraOffset.add(entryBuilder.startDoubleField(Text.translatable(OPTION + "cameraOffset", "Y"), config.classic.cameraY)
                .setDefaultValue(0.04)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.classic.cameraY = d)
                .build());
        classicCameraOffset.add(entryBuilder.startDoubleField(Text.translatable(OPTION + "cameraOffset", "Z"), config.classic.cameraZ)
                .setDefaultValue(-0.15)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.classic.cameraZ = d)
                .build());
        classic.addEntry(classicCameraOffset.build());
        SubCategoryBuilder classicCenterOffset = entryBuilder.startSubCategory(Text.translatable(CATEGORY + "centerOffset"))
                .setTooltip(Text.translatable(TOOLTIP + "centerOffset"));
        classicCenterOffset.add(entryBuilder.startDoubleField(Text.translatable(OPTION + "centerOffset", "X"), config.classic.centerX)
                .setDefaultValue(0.0)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.classic.centerX = d)
                .build());
        classicCenterOffset.add(entryBuilder.startDoubleField(Text.translatable(OPTION + "centerOffset", "Y"), config.classic.centerY)
                .setDefaultValue(0.0)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.classic.centerY = d)
                .build());
        classicCenterOffset.add(entryBuilder.startDoubleField(Text.translatable(OPTION + "centerOffset", "Z"), config.classic.centerZ)
                .setDefaultValue(0.0)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.classic.centerZ = d)
                .build());
        classic.addEntry(classicCenterOffset.build());
        SubCategoryBuilder classicCameraRotation = entryBuilder.startSubCategory(Text.translatable(CATEGORY + "cameraRotation"))
                .setTooltip(Text.translatable(TOOLTIP + "cameraRotation"), Text.translatable(TOOLTIP + "cameraRotation_n"));
        classicCameraRotation.add(entryBuilder.startFloatField(Text.translatable(OPTION + "pitch"), config.classic.pitch)
                .setDefaultValue(0.0f)
                .setMin(-180.0f)
                .setMax(180.0f)
                .setSaveConsumer(f -> config.classic.pitch = f)
                .build());
        classicCameraRotation.add(entryBuilder.startFloatField(Text.translatable(OPTION + "yaw"), config.classic.yaw)
                .setDefaultValue(18.0f)
                .setMin(-180.0f)
                .setMax(180.0f)
                .setSaveConsumer(f -> config.classic.yaw = f)
                .build());
        classicCameraRotation.add(entryBuilder.startFloatField(Text.translatable(OPTION + "roll"), config.classic.roll)
                .setDefaultValue(0.0f)
                .setMin(-180.0f)
                .setMax(180.0f)
                .setSaveConsumer(f -> config.classic.roll = f)
                .build());
        classic.addEntry(classicCameraRotation.build());

        binding.addEntry(entryBuilder.startTextDescription(Text.translatable(OPTION + "toModelViewGui",
                        Text.translatable("screen." + RealCamera.FULL_ID + ".modelView_title").styled(s -> s.withColor(Formatting.BLUE))))
                .build());
        binding.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "adjustOffset"), config.binding.adjustOffset)
                .setDefaultValue(true)
                .setTooltip(Text.translatable(TOOLTIP + "adjustOffset"))
                .setSaveConsumer(b -> config.binding.adjustOffset = b)
                .build());

        return builder.build();
    }
}
