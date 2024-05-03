package com.xtracr.realcamera.config;

import com.xtracr.realcamera.util.LocUtil;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Formatting;

public class ConfigScreen {
    public static Screen create(Screen parent) {
        ConfigFile.load();
        final ModConfig config = ConfigFile.config();
        final ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .transparentBackground()
                .setSavingRunnable(ConfigFile::save)
                .setTitle(LocUtil.MOD_NAME());
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(LocUtil.CONFIG_CATEGORY("general"));
        ConfigCategory classic = builder.getOrCreateCategory(LocUtil.CONFIG_CATEGORY("classic"));
        ConfigCategory binding = builder.getOrCreateCategory(LocUtil.CONFIG_CATEGORY("binding"));

        general.addEntry(entryBuilder.startBooleanToggle(LocUtil.CONFIG_OPTION("enabled"), config.enabled)
                .setSaveConsumer(b -> config.enabled = b)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(LocUtil.CONFIG_OPTION("isClassic"), config.isClassic)
                .setDefaultValue(false)
                .setTooltip(LocUtil.CONFIG_TOOLTIP("isClassic"))
                .setSaveConsumer(b -> config.isClassic = b)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(LocUtil.CONFIG_OPTION("dynamicCrosshair"), config.dynamicCrosshair)
                .setDefaultValue(false)
                .setTooltip(LocUtil.CONFIG_TOOLTIP("dynamicCrosshair"))
                .setSaveConsumer(b -> config.dynamicCrosshair = b)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(LocUtil.CONFIG_OPTION("renderModel"), config.renderModel)
                .setDefaultValue(true)
                .setTooltip(LocUtil.CONFIG_TOOLTIP("renderModel"))
                .setSaveConsumer(b -> config.renderModel = b)
                .build());
        general.addEntry(entryBuilder.startDoubleField(LocUtil.CONFIG_OPTION("adjustStep"), config.adjustStep)
                .setDefaultValue(0.01)
                .setMin(0.0)
                .setMax(ModConfig.MAX_DOUBLE)
                .setTooltip(LocUtil.CONFIG_TOOLTIP("adjustStep"))
                .setSaveConsumer(d -> config.adjustStep = d)
                .build());

        classic.addEntry(entryBuilder.startEnumSelector(LocUtil.CONFIG_OPTION("classicAdjustMode"), ModConfig.Classic.AdjustMode.class, config.classic.adjustMode)
                .setDefaultValue(ModConfig.Classic.AdjustMode.CAMERA)
                .setTooltip(LocUtil.CONFIG_TOOLTIP("classicAdjustMode"))
                .setSaveConsumer(e -> config.classic.adjustMode = e)
                .build());
        classic.addEntry(entryBuilder.startDoubleField(LocUtil.CONFIG_OPTION("scale"), config.classic.scale)
                .setDefaultValue(8.0)
                .setMin(0.0)
                .setMax(64.0)
                .setTooltip(LocUtil.CONFIG_TOOLTIP("scale"))
                .setSaveConsumer(d -> config.classic.scale = d)
                .build());
        SubCategoryBuilder classicCameraOffset = entryBuilder.startSubCategory(LocUtil.CONFIG_CATEGORY("cameraOffset"))
                .setTooltip(LocUtil.CONFIG_TOOLTIP("classicOffset"), LocUtil.CONFIG_TOOLTIP("classicOffset_n"));
        classicCameraOffset.add(entryBuilder.startDoubleField(LocUtil.CONFIG_OPTION("cameraOffset", "X"), config.classic.cameraX)
                .setDefaultValue(-0.5)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.classic.cameraX = d)
                .build());
        classicCameraOffset.add(entryBuilder.startDoubleField(LocUtil.CONFIG_OPTION("cameraOffset", "Y"), config.classic.cameraY)
                .setDefaultValue(0.04)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.classic.cameraY = d)
                .build());
        classicCameraOffset.add(entryBuilder.startDoubleField(LocUtil.CONFIG_OPTION("cameraOffset", "Z"), config.classic.cameraZ)
                .setDefaultValue(-0.15)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.classic.cameraZ = d)
                .build());
        classic.addEntry(classicCameraOffset.build());
        SubCategoryBuilder classicCenterOffset = entryBuilder.startSubCategory(LocUtil.CONFIG_CATEGORY("centerOffset")).setTooltip(LocUtil.CONFIG_TOOLTIP("centerOffset"));
        classicCenterOffset.add(entryBuilder.startDoubleField(LocUtil.CONFIG_OPTION("centerOffset", "X"), config.classic.centerX)
                .setDefaultValue(0.0)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.classic.centerX = d)
                .build());
        classicCenterOffset.add(entryBuilder.startDoubleField(LocUtil.CONFIG_OPTION("centerOffset", "Y"), config.classic.centerY)
                .setDefaultValue(0.0)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.classic.centerY = d)
                .build());
        classicCenterOffset.add(entryBuilder.startDoubleField(LocUtil.CONFIG_OPTION("centerOffset", "Z"), config.classic.centerZ)
                .setDefaultValue(0.0)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.classic.centerZ = d)
                .build());
        classic.addEntry(classicCenterOffset.build());
        SubCategoryBuilder classicCameraRotation = entryBuilder.startSubCategory(LocUtil.CONFIG_CATEGORY("cameraRotation"))
                .setTooltip(LocUtil.CONFIG_TOOLTIP("cameraRotation"), LocUtil.CONFIG_TOOLTIP("cameraRotation_n"));
        classicCameraRotation.add(entryBuilder.startFloatField(LocUtil.CONFIG_OPTION("pitch"), config.classic.pitch)
                .setDefaultValue(0.0f)
                .setMin(-180.0f)
                .setMax(180.0f)
                .setSaveConsumer(f -> config.classic.pitch = f)
                .build());
        classicCameraRotation.add(entryBuilder.startFloatField(LocUtil.CONFIG_OPTION("yaw"), config.classic.yaw)
                .setDefaultValue(18.0f)
                .setMin(-180.0f)
                .setMax(180.0f)
                .setSaveConsumer(f -> config.classic.yaw = f)
                .build());
        classicCameraRotation.add(entryBuilder.startFloatField(LocUtil.CONFIG_OPTION("roll"), config.classic.roll)
                .setDefaultValue(0.0f)
                .setMin(-180.0f)
                .setMax(180.0f)
                .setSaveConsumer(f -> config.classic.roll = f)
                .build());
        classic.addEntry(classicCameraRotation.build());

        binding.addEntry(entryBuilder.startTextDescription(LocUtil.CONFIG_OPTION("toModelViewScreen",
                        LocUtil.MODEL_VIEW_TITLE().styled(s -> s.withColor(Formatting.BLUE))))
                .build());
        binding.addEntry(entryBuilder.startBooleanToggle(LocUtil.CONFIG_OPTION("adjustOffset"), config.binding.adjustOffset)
                .setDefaultValue(true)
                .setTooltip(LocUtil.CONFIG_TOOLTIP("adjustOffset"))
                .setSaveConsumer(b -> config.binding.adjustOffset = b)
                .build());
        binding.addEntry(entryBuilder.startBooleanToggle(LocUtil.CONFIG_OPTION("renderStuckObjects"), config.renderModel)
                .setDefaultValue(true)
                .setTooltip(LocUtil.CONFIG_TOOLTIP("renderStuckObjects"))
                .setSaveConsumer(b -> config.renderModel = b)
                .build());
        binding.addEntry(entryBuilder.startStrList(LocUtil.CONFIG_OPTION("disableRenderItems"), config.binding.disableRenderItems)
                .setDefaultValue(ModConfig.Binding.defaultDisableRenderItems)
                .setSaveConsumer(l -> config.binding.disableRenderItems = l)
                .build());

        return builder.build();
    }
}
