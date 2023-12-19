package com.xtracr.realcamera.config;

import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.api.VirtualRenderer;
import com.xtracr.realcamera.compat.PehkuiCompat;
import com.xtracr.realcamera.compat.PhysicsModCompat;
import com.xtracr.realcamera.util.Triple;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ConfigScreen {
    private static final String CATEGORY = "config.category.xtracr_" + RealCamera.MODID + "_";
    private static final String OPTION = "config.option.xtracr_" + RealCamera.MODID + "_";
    private static final String TOOLTIP = "config.tooltip.xtracr_" + RealCamera.MODID + "_";

    public static Screen create(Screen parent) {

        ConfigFile.load();
        final ModConfig config = ConfigFile.modConfig;

        final ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .transparentBackground()
                .setSavingRunnable(ConfigFile::save)
                .setTitle(Text.translatable("config.title.xtracr_" + RealCamera.MODID));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable(CATEGORY + "general"));
        ConfigCategory binding = builder.getOrCreateCategory(Text.translatable(CATEGORY + "binding"));
        ConfigCategory classic = builder.getOrCreateCategory(Text.translatable(CATEGORY + "classic"));
        ConfigCategory compats = builder.getOrCreateCategory(Text.translatable(CATEGORY + "compats"));
        ConfigCategory disable = builder.getOrCreateCategory(Text.translatable(CATEGORY + "disable"));
        ConfigCategory experimental = builder.getOrCreateCategory(Text.literal("Experimental"));

        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "enabled"), config.general.enabled)
                .setSaveConsumer(b -> config.general.enabled = b)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "classic"), config.general.classic)
                .setDefaultValue(false)
                .setTooltip(Text.translatable(TOOLTIP + "classic"))
                .setSaveConsumer(b -> config.general.classic = b)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "dynamicCrosshair"), config.general.dynamicCrosshair)
                .setDefaultValue(false)
                .setTooltip(Text.translatable(TOOLTIP + "dynamicCrosshair"))
                .setSaveConsumer(b -> config.general.dynamicCrosshair = b)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "renderModel"), config.general.renderModel)
                .setDefaultValue(true)
                .setTooltip(Text.translatable(TOOLTIP + "renderModel"))
                .setSaveConsumer(b -> config.general.renderModel = b)
                .build());
        general.addEntry(entryBuilder.startDoubleField(Text.translatable(OPTION + "adjustStep"), config.general.adjustStep)
                .setDefaultValue(0.25D)
                .setMin(0.0D)
                .setMax(ModConfig.MAX_DOUBLE)
                .setTooltip(Text.translatable(TOOLTIP + "adjustStep"))
                .setSaveConsumer(d -> config.general.adjustStep = d)
                .build());
        general.addEntry(entryBuilder.startDoubleField(Text.translatable(OPTION + "scale"), config.general.scale)
                .setDefaultValue(1.0D)
                .setMin(0.0D)
                .setMax(ModConfig.MAX_DOUBLE)
                .setTooltip(Text.translatable(TOOLTIP + "scale"))
                .setSaveConsumer(d -> config.general.scale = d)
                .build());

        binding.addEntry(entryBuilder.startEnumSelector(Text.translatable(OPTION + "vanillaModelPart"), VanillaModelPart.class, config.binding.vanillaModelPart)
                .setDefaultValue(VanillaModelPart.head)
                .setTooltip(Text.translatable(TOOLTIP + "vanillaModelPart"))
                .setSaveConsumer(e -> config.binding.vanillaModelPart = e)
                .build());
        binding.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "adjustOffset"), config.binding.adjustOffset)
                .setDefaultValue(true)
                .setTooltip(Text.translatable(TOOLTIP + "adjustOffset"))
                .setSaveConsumer(b -> config.binding.adjustOffset = b)
                .build());
        binding.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "offsetModel"), config.binding.offsetModel)
                .setDefaultValue(false)
                .setTooltip(Text.translatable(TOOLTIP + "offsetModel"))
                .setSaveConsumer(b -> config.binding.offsetModel = b)
                .build());
        SubCategoryBuilder bindingCameraOffset = entryBuilder.startSubCategory(Text.translatable(CATEGORY + "cameraOffset"))
                .setTooltip(Text.translatable(TOOLTIP + "bindingOffset"), Text.translatable(TOOLTIP + "bindingOffset_n"));
        bindingCameraOffset.add(entryBuilder.startDoubleField(Text.translatable(OPTION + "cameraOffset", "X"), config.binding.cameraX)
                .setDefaultValue(3.25D)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.binding.cameraX = d)
                .build());
        bindingCameraOffset.add(entryBuilder.startDoubleField(Text.translatable(OPTION + "cameraOffset", "Y"), config.binding.cameraY)
                .setDefaultValue(2.0D)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.binding.cameraY = d)
                .build());
        bindingCameraOffset.add(entryBuilder.startDoubleField(Text.translatable(OPTION + "cameraOffset", "Z"), config.binding.cameraZ)
                .setDefaultValue(0.0D)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.binding.cameraZ = d)
                .build());
        binding.addEntry(bindingCameraOffset.build());
        SubCategoryBuilder bindingCameraRotation = entryBuilder.startSubCategory(Text.translatable(CATEGORY + "cameraRotation"))
                .setTooltip(Text.translatable(TOOLTIP + "cameraRotation"), Text.translatable(TOOLTIP + "cameraRotation_n"));
        bindingCameraRotation.add(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "bindPitching"), config.binding.bindPitching)
                .setDefaultValue(true)
                .setSaveConsumer(b -> config.binding.bindPitching = b)
                .build());
        bindingCameraRotation.add(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "bindYawing"), config.binding.bindYawing)
                .setDefaultValue(true)
                .setSaveConsumer(b -> config.binding.bindYawing = b)
                .build());
        bindingCameraRotation.add(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "bindRolling"), config.binding.bindRolling)
                .setDefaultValue(true)
                .setSaveConsumer(b -> config.binding.bindRolling = b)
                .build());
        bindingCameraRotation.add(entryBuilder.startFloatField(Text.translatable(OPTION + "pitch"), config.binding.pitch)
                .setDefaultValue(0.0F)
                .setMin(-180.0F)
                .setMax(180.0F)
                .setSaveConsumer(f -> config.binding.pitch = f)
                .build());
        bindingCameraRotation.add(entryBuilder.startFloatField(Text.translatable(OPTION + "yaw"), config.binding.yaw)
                .setDefaultValue(0.0F)
                .setMin(-180.0F)
                .setMax(180.0F)
                .setSaveConsumer(f -> config.binding.yaw = f)
                .build());
        bindingCameraRotation.add(entryBuilder.startFloatField(Text.translatable(OPTION + "roll"), config.binding.roll)
                .setDefaultValue(0.0F)
                .setMin(-180.0F)
                .setMax(180.0F)
                .setSaveConsumer(f -> config.binding.roll = f)
                .build());
        binding.addEntry(bindingCameraRotation.build());

        classic.addEntry(entryBuilder.startEnumSelector(Text.translatable(OPTION + "classicAdjustMode"), ModConfig.Classic.AdjustMode.class, config.classic.adjustMode)
                .setDefaultValue(ModConfig.Classic.AdjustMode.CAMERA)
                .setTooltip(Text.translatable(TOOLTIP + "classicAdjustMode"))
                .setSaveConsumer(e -> config.classic.adjustMode = e)
                .build());
        SubCategoryBuilder classicCameraOffset = entryBuilder.startSubCategory(Text.translatable(CATEGORY + "cameraOffset"))
                .setTooltip(Text.translatable(TOOLTIP + "classicOffset"), Text.translatable(TOOLTIP + "classicOffset_n"));
        classicCameraOffset.add(entryBuilder.startDoubleField(Text.translatable(OPTION + "cameraOffset", "X"), config.classic.cameraX)
                .setDefaultValue(-60.0D)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.classic.cameraX = d)
                .build());
        classicCameraOffset.add(entryBuilder.startDoubleField(Text.translatable(OPTION + "cameraOffset", "Y"), config.classic.cameraY)
                .setDefaultValue(2.0D)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.classic.cameraY = d)
                .build());
        classicCameraOffset.add(entryBuilder.startDoubleField(Text.translatable(OPTION + "cameraOffset", "Z"), config.classic.cameraZ)
                .setDefaultValue(-16.0D)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.classic.cameraZ = d)
                .build());
        classic.addEntry(classicCameraOffset.build());
        SubCategoryBuilder classicCenterOffset = entryBuilder.startSubCategory(Text.translatable(CATEGORY + "centerOffset"))
                .setTooltip(Text.translatable(TOOLTIP + "centerOffset"));
        classicCenterOffset.add(entryBuilder.startDoubleField(Text.translatable(OPTION + "centerOffset", "X"), config.classic.centerX)
                .setDefaultValue(0.0D)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.classic.centerX = d)
                .build());
        classicCenterOffset.add(entryBuilder.startDoubleField(Text.translatable(OPTION + "centerOffset", "Y"), config.classic.centerY)
                .setDefaultValue(-3.4D)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.classic.centerY = d)
                .build());
        classicCenterOffset.add(entryBuilder.startDoubleField(Text.translatable(OPTION + "centerOffset", "Z"), config.classic.centerZ)
                .setDefaultValue(0.0D)
                .setMin(ModConfig.MIN_DOUBLE)
                .setMax(ModConfig.MAX_DOUBLE)
                .setSaveConsumer(d -> config.classic.centerZ = d)
                .build());
        classic.addEntry(classicCenterOffset.build());
        SubCategoryBuilder classicCameraRotation = entryBuilder.startSubCategory(Text.translatable(CATEGORY + "cameraRotation"))
                .setTooltip(Text.translatable(TOOLTIP + "cameraRotation"), Text.translatable(TOOLTIP + "cameraRotation_n"));
        classicCameraRotation.add(entryBuilder.startFloatField(Text.translatable(OPTION + "pitch"), config.classic.pitch)
                .setDefaultValue(0.0F)
                .setMin(-180.0F)
                .setMax(180.0F)
                .setSaveConsumer(f -> config.classic.pitch = f)
                .build());
        classicCameraRotation.add(entryBuilder.startFloatField(Text.translatable(OPTION + "yaw"), config.classic.yaw)
                .setDefaultValue(18.0F)
                .setMin(-180.0F)
                .setMax(180.0F)
                .setSaveConsumer(f -> config.classic.yaw = f)
                .build());
        classicCameraRotation.add(entryBuilder.startFloatField(Text.translatable(OPTION + "roll"), config.classic.roll)
                .setDefaultValue(0.0F)
                .setMin(-180.0F)
                .setMax(180.0F)
                .setSaveConsumer(f -> config.classic.roll = f)
                .build());
        classic.addEntry(classicCameraRotation.build());

        compats.addEntry(entryBuilder.startTextDescription(Text.translatable(OPTION + "compatsText_1",
                        Text.translatable(OPTION + "compatsText_2").styled(s -> s.withColor(Formatting.BLUE)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("https://github.com/xTracr/RealCamera/wiki/Configuration")))
                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/xTracr/RealCamera/wiki/Configuration#mod-model-compat")))))
                .build());
        compats.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "useModModel"), config.compats.useModModel)
                .setDefaultValue(false)
                .setTooltip(Text.translatable(TOOLTIP + "useModModel", Text.literal((config.compats.useModModel ? RealCameraCore.getStatus() : "Disabled")).styled(
                        s -> s.withColor((config.compats.useModModel ? (RealCameraCore.getStatus().equals("Successful") ? Formatting.GREEN : Formatting.RED) : Formatting.YELLOW)))))
                .setSaveConsumer(b -> config.compats.useModModel = b)
                .build());
        compats.addEntry(entryBuilder.startSelector(Text.translatable(OPTION + "modelModID"), VirtualRenderer.getModidList(), config.compats.modelModID)
                .setDefaultValue("minecraft")
                .setSaveConsumer(s -> config.compats.modelModID = s)
                .build());
        compats.addEntry(entryBuilder.startStrField(Text.translatable(OPTION + "modModelPart"), config.compats.modModelPart)
                .setDefaultValue("head")
                .setTooltip(Text.translatable(TOOLTIP + "modModelPart"))
                .setSaveConsumer(s -> config.compats.modModelPart = s)
                .build());
        SubCategoryBuilder compatSwitches = entryBuilder.startSubCategory(Text.translatable(CATEGORY + "compatSwitches"))
                .setTooltip(Text.translatable(TOOLTIP + "compatSwitches"));
        if (PehkuiCompat.loaded)
            compatSwitches.add(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "pehkui"), config.compats.pehkui)
                    .setDefaultValue(true)
                    .setSaveConsumer(b -> config.compats.pehkui = b)
                    .build());
        if (PhysicsModCompat.loaded)
            compatSwitches.add(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "physicsMod"), config.compats.physicsMod)
                    .setDefaultValue(true)
                    .setSaveConsumer(b -> config.compats.physicsMod = b)
                    .build());
        compats.addEntry(compatSwitches.build());

        disable.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "onlyInBinding"), config.disable.onlyInBinding)
                .setDefaultValue(true)
                .setSaveConsumer(b -> config.disable.onlyInBinding = b)
                .build());
        disable.addEntry(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "renderModelPart"), config.disable.renderModelPart)
                .setDefaultValue(false)
                .setSaveConsumer(b -> config.disable.renderModelPart = b)
                .build());
        disable.addEntry(entryBuilder.startStrList(Text.translatable(OPTION + "disabledModelParts"), config.disable.disabledModelParts)
                .setDefaultValue(ModConfig.Disable.defaultParts)
                .setTooltip(Text.translatable(TOOLTIP + "disabledModelParts", ModConfig.Disable.optionalParts))
                .setSaveConsumer(l -> config.disable.disabledModelParts = l)
                .build());
        disable.addEntry(new NestedListListEntry<Triple<String, List<String>, List<String>>, MultiElementListEntry<Triple<String, List<String>, List<String>>>>(
                Text.translatable(OPTION + "customConditions"),
                config.disable.customConditions,
                false,
                Optional::empty,
                l -> config.disable.customConditions = l,
                () -> ModConfig.Disable.defaultConditions,
                entryBuilder.getResetButtonKey(),
                true,
                false,
                (element, entry) -> {
                    ModConfig.resetTripleIfNull(element, ModConfig.Disable.defaultTriple);
                    return new MultiElementListEntry<>(Text.literal(element.getMiddle().get(0)), element, Arrays.asList(
                            entryBuilder.startSelector(Text.translatable(OPTION + "customConditions_behavior"), ModConfig.Disable.behaviors, element.getLeft())
                                    .setSaveConsumer(element::setLeft)
                                    .build(),
                            entryBuilder.startStrList(Text.translatable(OPTION + "customConditions_id"), element.getMiddle())
                                    .setSaveConsumer(element::setMiddle)
                                    .build(),
                            entryBuilder.startStrList(Text.translatable(OPTION + "customConditions_actions"), element.getRight())
                                    .setTooltip(Text.translatable(TOOLTIP + "customConditions_actions"))
                                    .setSaveConsumer(element::setRight)
                                    .build()),
                            false);
                }));
        SubCategoryBuilder disableModWhen = entryBuilder.startSubCategory(Text.translatable(CATEGORY + "disableModWhen"));
        disableModWhen.add(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "fallFlying"), config.disable.fallFlying)
                .setDefaultValue(true)
                .setSaveConsumer(b -> config.disable.fallFlying = b)
                .build());
        disableModWhen.add(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "swimming"), config.disable.swimming)
                .setDefaultValue(false)
                .setSaveConsumer(b -> config.disable.swimming = b)
                .build());
        disableModWhen.add(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "crawling"), config.disable.crawling)
                .setDefaultValue(false)
                .setSaveConsumer(b -> config.disable.crawling = b)
                .build());
        disableModWhen.add(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "sneaking"), config.disable.sneaking)
                .setDefaultValue(false)
                .setSaveConsumer(b -> config.disable.sneaking = b)
                .build());
        disableModWhen.add(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "sleeping"), config.disable.sleeping)
                .setDefaultValue(false)
                .setSaveConsumer(b -> config.disable.sleeping = b)
                .build());
        disableModWhen.add(entryBuilder.startBooleanToggle(Text.translatable(OPTION + "screenOpened"), config.disable.screenOpened)
                .setDefaultValue(false)
                .setSaveConsumer(b -> config.disable.screenOpened = b)
                .build());
        disable.addEntry(disableModWhen.build());

        experimental.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enabled"), config.binding.experimental)
                .setDefaultValue(false)
                .setSaveConsumer(b -> config.binding.experimental = b)
                .build());
        experimental.addEntry(entryBuilder.startStrField(Text.literal("Name Of List"), config.binding.nameOfList)
                .setDefaultValue("minecraft_head")
                .setSaveConsumer(s -> config.binding.nameOfList = s)
                .build());

        return builder.build();
    }
}
