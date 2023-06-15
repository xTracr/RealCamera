package com.xtracr.realcamera.config;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.api.VirtualRenderer;
import com.xtracr.realcamera.compat.DoABarrelRollCompat;
import com.xtracr.realcamera.compat.PehkuiCompat;
import com.xtracr.realcamera.compat.PhysicsModCompat;
import com.xtracr.realcamera.utils.Triple;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public class ConfigScreen {

    private static final String CATEGORY = "config.category.xtracr_"+RealCamera.MODID+"_";
    private static final String OPTION = "config.option.xtracr_"+RealCamera.MODID+"_";
    private static final String TOOLTIP = "config.tooltip.xtracr_"+RealCamera.MODID+"_";

    public static Screen create(Screen parent) {

        ConfigFile.load();
        final ModConfig config = ConfigFile.modConfig;

        final ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .transparentBackground()
            .setSavingRunnable(ConfigFile::save)
            .setTitle(new TranslatableText("config.title.xtracr_"+RealCamera.MODID));
        builder.setGlobalized(true);
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(new TranslatableText(CATEGORY+"general"));
        ConfigCategory binding = builder.getOrCreateCategory(new TranslatableText(CATEGORY+"binding"));
        ConfigCategory classic = builder.getOrCreateCategory(new TranslatableText(CATEGORY+"classic"));
        ConfigCategory compats = builder.getOrCreateCategory(new TranslatableText(CATEGORY+"compats"));
        ConfigCategory disable = builder.getOrCreateCategory(new TranslatableText(CATEGORY+"disable"));

        general.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"enabled"), config.general.enabled)
            .setSaveConsumer(b -> config.general.enabled = b)
            .build());
        general.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"classic"), config.general.classic)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(TOOLTIP+"classic"))
            .setSaveConsumer(b -> config.general.classic = b)
            .build());
        general.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"clipToSpace"), config.general.clipToSpace)
            .setDefaultValue(true)
            .setTooltip(new TranslatableText(TOOLTIP+"clipToSpace"))
            .setSaveConsumer(b -> config.general.clipToSpace = b)
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
        SubCategoryBuilder bindingCameraOffset = entryBuilder.startSubCategory(new TranslatableText(CATEGORY+"cameraOffset"))
            .setTooltip(new TranslatableText(TOOLTIP+"bindingOffset"), new TranslatableText(TOOLTIP+"referOffset"), new TranslatableText(TOOLTIP+"bindingOffset_n"));
        bindingCameraOffset.add(entryBuilder.startDoubleField(new TranslatableText(OPTION+"cameraOffset", "X"), config.binding.cameraX)
            .setDefaultValue(3.25D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setSaveConsumer(d -> config.binding.cameraX = d)
            .build());
        bindingCameraOffset.add(entryBuilder.startDoubleField(new TranslatableText(OPTION+"cameraOffset", "Y"), config.binding.cameraY)
            .setDefaultValue(2.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setSaveConsumer(d -> config.binding.cameraY = d)
            .build());
        bindingCameraOffset.add(entryBuilder.startDoubleField(new TranslatableText(OPTION+"cameraOffset", "Z"), config.binding.cameraZ)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setSaveConsumer(d -> config.binding.cameraZ = d)
            .build());
        bindingCameraOffset.add(entryBuilder.startDoubleField(new TranslatableText(OPTION+"referOffset", "X"), config.binding.referX)
            .setDefaultValue(3.25D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setSaveConsumer(d -> config.binding.referX = d)
            .build());
        bindingCameraOffset.add(entryBuilder.startDoubleField(new TranslatableText(OPTION+"referOffset", "Y"), config.binding.referY)
            .setDefaultValue(2.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setSaveConsumer(d -> config.binding.referY = d)
            .build());
        bindingCameraOffset.add(entryBuilder.startDoubleField(new TranslatableText(OPTION+"referOffset", "Z"), config.binding.referZ)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setSaveConsumer(d -> config.binding.referZ = d)
            .build());
        binding.addEntry(bindingCameraOffset.build());
        SubCategoryBuilder bindingCameraRotation = entryBuilder.startSubCategory(new TranslatableText(CATEGORY+"cameraRotation"))
            .setTooltip(new TranslatableText(TOOLTIP+"cameraRotation"), new TranslatableText(TOOLTIP+"cameraRotation_n"));
        bindingCameraRotation.add(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"bindPitching"), config.binding.bindPitching)
            .setDefaultValue(true)
            .setSaveConsumer(b -> config.binding.bindPitching = b)
            .build());
        bindingCameraRotation.add(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"bindYawing"), config.binding.bindYawing)
            .setDefaultValue(true)
            .setSaveConsumer(b -> config.binding.bindYawing = b)
            .build());
        bindingCameraRotation.add(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"bindRolling"), config.binding.bindRolling)
            .setDefaultValue(true)
            .setSaveConsumer(b -> config.binding.bindRolling = b)
            .build());
        bindingCameraRotation.add(entryBuilder.startFloatField(new TranslatableText(OPTION+"pitch"), config.binding.pitch)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setSaveConsumer(f -> config.binding.pitch = f)
            .build());
        bindingCameraRotation.add(entryBuilder.startFloatField(new TranslatableText(OPTION+"yaw"), config.binding.yaw)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setSaveConsumer(f -> config.binding.yaw = f)
            .build());
        bindingCameraRotation.add(entryBuilder.startFloatField(new TranslatableText(OPTION+"roll"), config.binding.roll)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setSaveConsumer(f -> config.binding.roll = f)
            .build());
        binding.addEntry(bindingCameraRotation.build());

        classic.addEntry(entryBuilder.startEnumSelector(new TranslatableText(OPTION+"classicAdjustMode"), ModConfig.Classic.AdjustMode.class, config.classic.adjustMode)
            .setDefaultValue(ModConfig.Classic.AdjustMode.CAMERA)
            .setTooltip(new TranslatableText(TOOLTIP+"classicAdjustMode"))
            .setSaveConsumer(e -> config.classic.adjustMode = e)
            .build());
        SubCategoryBuilder classicCameraOffset = entryBuilder.startSubCategory(new TranslatableText(CATEGORY+"cameraOffset"))
            .setTooltip(new TranslatableText(TOOLTIP+"classicOffset"), new TranslatableText(TOOLTIP+"referOffset"), new TranslatableText(TOOLTIP+"classicOffset_n"));
        classicCameraOffset.add(entryBuilder.startDoubleField(new TranslatableText(OPTION+"cameraOffset", "X"), config.classic.cameraX)
            .setDefaultValue(-60.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setSaveConsumer(d -> config.classic.cameraX = d)
            .build());
        classicCameraOffset.add(entryBuilder.startDoubleField(new TranslatableText(OPTION+"cameraOffset", "Y"), config.classic.cameraY)
            .setDefaultValue(2.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setSaveConsumer(d -> config.classic.cameraY = d)
            .build());
        classicCameraOffset.add(entryBuilder.startDoubleField(new TranslatableText(OPTION+"cameraOffset", "Z"), config.classic.cameraZ)
            .setDefaultValue(-16.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setSaveConsumer(d -> config.classic.cameraZ = d)
            .build());
        classicCameraOffset.add(entryBuilder.startDoubleField(new TranslatableText(OPTION+"referOffset", "X"), config.classic.referX)
            .setDefaultValue(3.25D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setSaveConsumer(d -> config.classic.referX = d)
            .build());
        classicCameraOffset.add(entryBuilder.startDoubleField(new TranslatableText(OPTION+"referOffset", "Y"), config.classic.referY)
            .setDefaultValue(2.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setSaveConsumer(d -> config.classic.referY = d)
            .build());
        classicCameraOffset.add(entryBuilder.startDoubleField(new TranslatableText(OPTION+"referOffset", "Z"), config.classic.referZ)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setSaveConsumer(d -> config.classic.referZ = d)
            .build());
        classic.addEntry(classicCameraOffset.build());
        SubCategoryBuilder classicCenterOffset = entryBuilder.startSubCategory(new TranslatableText(CATEGORY+"centerOffset"))
            .setTooltip(new TranslatableText(TOOLTIP+"centerOffset"));
        classicCenterOffset.add(entryBuilder.startDoubleField(new TranslatableText(OPTION+"centerOffset", "X"), config.classic.centerX)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setSaveConsumer(d -> config.classic.centerX = d)
            .build());
        classicCenterOffset.add(entryBuilder.startDoubleField(new TranslatableText(OPTION+"centerOffset", "Y"), config.classic.centerY)
            .setDefaultValue(-3.4D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setSaveConsumer(d -> config.classic.centerY = d)
            .build());
        classicCenterOffset.add(entryBuilder.startDoubleField(new TranslatableText(OPTION+"centerOffset", "Z"), config.classic.centerZ)
            .setDefaultValue(0.0D)
            .setMin(ModConfig.MIN_DOUBLE)
            .setMax(ModConfig.MAX_DOUBLE)
            .setSaveConsumer(d -> config.classic.centerZ = d)
            .build());
        classic.addEntry(classicCenterOffset.build());
        SubCategoryBuilder classicCameraRotation = entryBuilder.startSubCategory(new TranslatableText(CATEGORY+"cameraRotation"))
            .setTooltip(new TranslatableText(TOOLTIP+"cameraRotation"), new TranslatableText(TOOLTIP+"cameraRotation_n"));
        classicCameraRotation.add(entryBuilder.startFloatField(new TranslatableText(OPTION+"pitch"), config.classic.pitch)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setSaveConsumer(f -> config.classic.pitch = f)
            .build());
        classicCameraRotation.add(entryBuilder.startFloatField(new TranslatableText(OPTION+"yaw"), config.classic.yaw)
            .setDefaultValue(18.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setSaveConsumer(f -> config.classic.yaw = f)
            .build());
        classicCameraRotation.add(entryBuilder.startFloatField(new TranslatableText(OPTION+"roll"), config.classic.roll)
            .setDefaultValue(0.0F)
            .setMin(-180.0F)
            .setMax(180.0F)
            .setSaveConsumer(f -> config.classic.roll = f)
            .build());
        classic.addEntry(classicCameraRotation.build());

        compats.addEntry(entryBuilder.startTextDescription(new TranslatableText(OPTION+"compatsText_1",
            new TranslatableText(OPTION+"compatsText_2").styled(s -> s.withColor(Formatting.BLUE)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("https://github.com/xTracr/RealCamera/wiki/Configuration")))
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/xTracr/RealCamera/wiki/Configuration#mod-model-compat")))))
            .build());
        compats.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"useModModel"), config.compats.useModModel)
            .setDefaultValue(false)
            .setTooltip(new TranslatableText(TOOLTIP+"useModModel", new LiteralText((config.compats.useModModel ? RealCameraCore.status : "Disabled")).styled(
                s -> s.withColor((config.compats.useModModel ? (RealCameraCore.status.equals("Successful") ? Formatting.GREEN : Formatting.RED) : Formatting.YELLOW)))))
            .setSaveConsumer(b -> config.compats.useModModel = b)
            .build());
        compats.addEntry(entryBuilder.startSelector(new TranslatableText(OPTION+"modelModID"), VirtualRenderer.getModidList(), config.compats.modelModID)
            .setDefaultValue("minecraft")
            .setSaveConsumer(s -> config.compats.modelModID = s)
            .build());
        compats.addEntry(entryBuilder.startStrField(new TranslatableText(OPTION+"modModelPart"), config.compats.modModelPart)
            .setDefaultValue("head")
            .setTooltip(new TranslatableText(TOOLTIP+"modModelPart"))
            .setSaveConsumer(s -> config.compats.modModelPart = s)
            .build());
        SubCategoryBuilder compatSwitches = entryBuilder.startSubCategory(new TranslatableText(CATEGORY+"compatSwitches"))
            .setTooltip(new TranslatableText(TOOLTIP+"compatSwitches"));
        if (DoABarrelRollCompat.loaded)
        compatSwitches.add(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"doABarrelRoll"), config.compats.doABarrelRoll)
            .setDefaultValue(true)
            .setSaveConsumer(b -> config.compats.doABarrelRoll = b)
            .build());
        if (PehkuiCompat.loaded)
        compatSwitches.add(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"pehkui"), config.compats.pehkui)
            .setDefaultValue(true)
            .setSaveConsumer(b -> config.compats.pehkui = b)
            .build());
        if (PhysicsModCompat.loaded)
        compatSwitches.add(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"physicsMod"), config.compats.physicsMod)
            .setDefaultValue(true)
            .setSaveConsumer(b -> config.compats.physicsMod = b)
            .build());
        compats.addEntry(compatSwitches.build());

        disable.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"onlyInBinding"), config.disable.onlyInBinding)
            .setDefaultValue(true)
            .setSaveConsumer(b -> config.disable.onlyInBinding = b)
            .build());
        disable.addEntry(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"renderModelPart"), config.disable.renderModelPart)
            .setDefaultValue(false)
            .setSaveConsumer(b -> config.disable.renderModelPart = b)
            .build());
        disable.addEntry(entryBuilder.startStrList(new TranslatableText(OPTION+"disabledModelParts"), config.disable.disabledModelParts)
            .setDefaultValue(ModConfig.Disable.defaultParts)
            .setTooltip(new TranslatableText(TOOLTIP+"disabledModelParts", ModConfig.Disable.optionalParts))
            .setSaveConsumer(l -> config.disable.disabledModelParts = l)
            .build());
        disable.addEntry(new NestedListListEntry<Triple<String, List<String>, List<String>>, MultiElementListEntry<Triple<String, List<String>, List<String>>>>(
            new TranslatableText(OPTION+"customConditions"), 
            config.disable.customConditions, 
            false, 
            () -> Optional.empty(), 
            l -> config.disable.customConditions = l, 
            () -> ModConfig.Disable.defaultConditions, 
            entryBuilder.getResetButtonKey(), 
            true, 
            false, 
            (element, entry) -> {
                ModConfig.resetTripleIfNull(element, ModConfig.Disable.defaultTriple);
                return new MultiElementListEntry<>(new LiteralText(element.getMiddle().get(0)), element, Arrays.asList(
                    entryBuilder.startSelector(new TranslatableText(OPTION+"customConditions_behavior"), ModConfig.Disable.behaviors, element.getLeft())
                        .setSaveConsumer(element::setLeft)
                        .build(), 
                    entryBuilder.startStrList(new TranslatableText(OPTION+"customConditions_id"), element.getMiddle())
                        .setSaveConsumer(element::setMiddle)
                        .build(), 
                    entryBuilder.startStrList(new TranslatableText(OPTION+"customConditions_actions"), element.getRight())
                        .setTooltip(new TranslatableText(TOOLTIP+"customConditions_actions"))
                        .setSaveConsumer(element::setRight)
                        .build()), 
                    false);
            }));
        SubCategoryBuilder disableModWhen = entryBuilder.startSubCategory(new TranslatableText(CATEGORY+"disableModWhen"));
        disableModWhen.add(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"fallFlying"), config.disable.fallFlying)
            .setDefaultValue(true)
            .setSaveConsumer(b -> config.disable.fallFlying = b)
            .build());
        disableModWhen.add(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"swiming"), config.disable.swiming)
            .setDefaultValue(false)
            .setSaveConsumer(b -> config.disable.swiming = b)
            .build());
        disableModWhen.add(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"crawling"), config.disable.crawling)
            .setDefaultValue(false)
            .setSaveConsumer(b -> config.disable.crawling = b)
            .build());
        disableModWhen.add(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"sneaking"), config.disable.sneaking)
            .setDefaultValue(false)
            .setSaveConsumer(b -> config.disable.sneaking = b)
            .build());
        disableModWhen.add(entryBuilder.startBooleanToggle(new TranslatableText(OPTION+"sleeping"), config.disable.sleeping)
            .setDefaultValue(false)
            .setSaveConsumer(b -> config.disable.sleeping = b)
            .build());
        disable.addEntry(disableModWhen.build());

        return builder.build();
    }
}
