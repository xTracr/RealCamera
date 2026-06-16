package com.xtracr.realcamera.gui;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.DataResult;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.compat.CompatibilityHelper;
import com.xtracr.realcamera.config.*;
import com.xtracr.realcamera.config.BindTarget.BindConfig;
import com.xtracr.realcamera.config.BindTarget.TargetConfig;
import com.xtracr.realcamera.config.codec.ConfigCodec;
import com.xtracr.realcamera.gui.components.*;
import com.xtracr.realcamera.renderer.state.BuiltModelRecord;
import com.xtracr.realcamera.renderer.state.VertexData;
import com.xtracr.realcamera.util.LocUtil;
import com.xtracr.realcamera.util.MathUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec2;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

public final class ModelViewScreen extends Screen {
    private static final int SELECTION_COLOR = 0x4F3333CC, SELECTION_HOVER_COLOR = 0x2F3333CC, OUTLINE_COLOR = 0xAAFFFFFF;
    private static final int SIDE_PANEL_BG = 0xFF444444, CENTER_PANEL_BG = 0xFF222222;
    private static final int DEFAULT_SCALE = 80, MIN_SCALE = 16, MAX_SCALE = 1024;
    private final int xSize = 450, ySize = 206, middleWidth = xSize - 200, widgetWidth = (xSize - middleWidth) / 4 - 8, widgetHeight = 18, wideWidgetWidth = widgetWidth * 2 + 4, compactWidgetWidth = widgetWidth * 2 - 18;
    private int x, y, page = 0;
    private InputConstants.Key modifierKey = InputConstants.getKey(ConfigFile.config().binding.screenModifierKey);
    private boolean initialized;
    private int modelScale = DEFAULT_SCALE, textureScale = DEFAULT_SCALE, layers = 0, selectionRadius = 10;
    private double modelX, modelY, textureX, textureY, clickedX = -1, clickedY = -1;
    private float xRot, yRot;
    private String focusedTextureId;
    private ScreenRectangle modelViewArea;
    private Pose currentPosture = Pose.STANDING;
    @Nullable
    private ScreenRectangle textureViewArea;
    private VertexData[][] focusedPolyhedron = new VertexData[0][];
    @Nullable
    private UVRectangleWidget focusedRectWidget;
    private final StringWidget rectWidgetsSizeWidget = new StringWidget(widgetWidth - 22, widgetHeight, CommonComponents.EMPTY, font);
    private final EditBox nameField = createTextField(wideWidgetWidth, 20);
    private final EditBox textureIdField = createTextField(wideWidgetWidth, 1024);
    private final EditBox disabledNameField = createTextField(compactWidgetWidth, 20);
    private final EditBox disabledIdField = createTextField(wideWidgetWidth, 1024);
    private final NumberField<Integer> priorityField = NumberField.ofInt(font, widgetWidth - 2, widgetHeight - 2, 0, null);
    private final NumberField<Integer> focusedRectWidgetNumberField = NumberField.ofInt(font, widgetWidth - 2, widgetHeight - 2, 0, null).setMin(0);
    private final NumberField<Float> forwardUField = createFloatField(widgetWidth, 0);
    private final NumberField<Float> forwardVField = createFloatField(widgetWidth, 0);
    private final NumberField<Float> upwardUField = createFloatField(widgetWidth, 0);
    private final NumberField<Float> upwardVField = createFloatField(widgetWidth, 0);
    private final NumberField<Float> posUField = createFloatField(widgetWidth, 0);
    private final NumberField<Float> posVField = createFloatField(widgetWidth, 0);
    private final NumberField<Float> uMinField = createFloatField(widgetWidth * 2 - 24, 0).setMin(-1f).setMax(2f);
    private final NumberField<Float> uMaxField = createFloatField(widgetWidth * 2 - 24, 0).setMin(-1f).setMax(2f);
    private final NumberField<Float> vMinField = createFloatField(widgetWidth * 2 - 24, 0).setMin(-1f).setMax(2f);
    private final NumberField<Float> vMaxField = createFloatField(widgetWidth * 2 - 24, 0).setMin(-1f).setMax(2f);
    private final NumberField<Float> scaleField = createFloatField(widgetWidth, 1.0f).setMax(64.0f);
    private final NumberField<Float> depthField = createFloatField(widgetWidth, 0.2f).setMax(16.0f);
    private final CycleIconButton showTextureButton = new CycleIconButton(48, 16, 0, 2).setOnValueChange(_ -> initWidgets(page));
    private final CycleIconButton pauseButton = new CycleIconButton(0, 16, 0, 2);
    private final CycleIconButton bindXButton = new CycleIconButton(16, 16, 1, 2);
    private final CycleIconButton bindYButton = new CycleIconButton(16, 16, 0, 2);
    private final CycleIconButton bindZButton = new CycleIconButton(16, 16, 1, 2);
    private final CycleIconButton bindRotButton = new CycleIconButton(16, 16, 1, 2);
    private final DoubleSlider entityPitchSlider = createSlider("pitch", wideWidgetWidth, -90.0, 90.0);
    private final DoubleSlider entityYawSlider = createSlider("yaw", wideWidgetWidth, -60.0, 60.0);
    private final NumberWidgetPair offsetXPair = new NumberWidgetPair(font, "offsetX", compactWidgetWidth, widgetHeight, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
    private final NumberWidgetPair offsetYPair = new NumberWidgetPair(font, "offsetY", compactWidgetWidth, widgetHeight, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
    private final NumberWidgetPair offsetZPair = new NumberWidgetPair(font, "offsetZ", compactWidgetWidth, widgetHeight, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
    private final NumberWidgetPair offsetStandingPitchPair = new NumberWidgetPair(font, "pitch", compactWidgetWidth, widgetHeight, -180.0f, 180.0f);
    private final NumberWidgetPair offsetStandingYawPair = new NumberWidgetPair(font, "yaw", compactWidgetWidth, widgetHeight, -180.0f, 180.0f);
    private final NumberWidgetPair offsetStandingRollPair = new NumberWidgetPair(font, "roll", compactWidgetWidth, widgetHeight, -180.0f, 180.0f);
    private final NumberWidgetPair offsetCrouchingPitchPair = new NumberWidgetPair(font, "pitch", compactWidgetWidth, widgetHeight, -180.0f, 180.0f);
    private final NumberWidgetPair offsetCrouchingYawPair = new NumberWidgetPair(font, "yaw", compactWidgetWidth, widgetHeight, -180.0f, 180.0f);
    private final NumberWidgetPair offsetCrouchingRollPair = new NumberWidgetPair(font, "roll", compactWidgetWidth, widgetHeight, -180.0f, 180.0f);
    private final NumberWidgetPair offsetSwimmingPitchPair = new NumberWidgetPair(font, "pitch", compactWidgetWidth, widgetHeight, -180.0f, 180.0f);
    private final NumberWidgetPair offsetFlyingPitchPair = new NumberWidgetPair(font, "pitch", compactWidgetWidth, widgetHeight, -180.0f, 180.0f);
    private final NumberWidgetPair offsetSwimmingYawPair = new NumberWidgetPair(font, "yaw", compactWidgetWidth, widgetHeight, -180.0f, 180.0f);
    private final NumberWidgetPair offsetSwimmingRollPair = new NumberWidgetPair(font, "roll", compactWidgetWidth, widgetHeight, -180.0f, 180.0f);
    private final NumberWidgetPair offsetFlyingYawPair = new NumberWidgetPair(font, "yaw", compactWidgetWidth, widgetHeight, -180.0f, 180.0f);
    private final NumberWidgetPair offsetFlyingRollPair = new NumberWidgetPair(font, "roll", compactWidgetWidth, widgetHeight, -180.0f, 180.0f);
    private final List<DisableConfig> disableConfigs = new ArrayList<>();
    private final List<UVRectangleWidget> rectWidgets = new ArrayList<>();
    private final Map<String, Set<String>> hiddenNameMap = new HashMap<>();
    private final List<NumberWidgetPair> widgetPairs = List.of(offsetXPair, offsetYPair, offsetZPair,
            offsetStandingPitchPair, offsetStandingYawPair, offsetStandingRollPair,
            offsetCrouchingPitchPair, offsetCrouchingYawPair, offsetCrouchingRollPair,
            offsetSwimmingPitchPair, offsetSwimmingYawPair, offsetSwimmingRollPair,
            offsetFlyingPitchPair, offsetFlyingYawPair, offsetFlyingRollPair);
    private final CycleButton<Integer> selectingButton = createCyclingButtonBuilder(ImmutableSortedMap.of(
            0, LocUtil.MODEL_VIEW_WIDGET("forwardVector").withStyle(ChatFormatting.GREEN),
            1, LocUtil.MODEL_VIEW_WIDGET("upwardVector").withStyle(ChatFormatting.RED),
            2, LocUtil.MODEL_VIEW_WIDGET("position").withStyle(ChatFormatting.BLUE)), 0)
            .withTooltip(_ -> createTooltip("selecting", modifierKey.getDisplayName(), modifierKey.getDisplayName()))
            .create(0, 0, wideWidgetWidth, widgetHeight, LocUtil.MODEL_VIEW_WIDGET("selecting"));
    private final CycleButton<Integer> disableModeButton = createCyclingButtonBuilder(ImmutableMap.of(
            0, LocUtil.MODEL_VIEW_WIDGET("all").withStyle(ChatFormatting.GREEN),
            1, LocUtil.MODEL_VIEW_WIDGET("part").withStyle(ChatFormatting.BLUE)), 0)
            .withTooltip(_ -> createTooltip("disableMode", modifierKey.getDisplayName()))
            .create(0, 0, wideWidgetWidth, widgetHeight, LocUtil.MODEL_VIEW_WIDGET("disableMode"));
    private final CycleButton<Integer> selectionModeButton = createCyclingButtonBuilder(ImmutableSortedMap.of(
            0, LocUtil.MODEL_VIEW_WIDGET("single"),
            1, LocUtil.MODEL_VIEW_WIDGET("multiple"),
            2, LocUtil.MODEL_VIEW_WIDGET("range").withStyle(ChatFormatting.BLUE)), 0)
            .withTooltip(_ -> createTooltip("selectionMode", modifierKey.getDisplayName(), modifierKey.getDisplayName()))
            .create(0, 0, wideWidgetWidth, widgetHeight, LocUtil.MODEL_VIEW_WIDGET("selectionMode"));
    private final CycleButton<Integer> toggleSliderButton = createCyclingButtonBuilder(ImmutableMap.of(
            0, LocUtil.MODEL_VIEW_WIDGET("toggleSliderToField"),
            1, LocUtil.MODEL_VIEW_WIDGET("toggleFieldToSlider")), 0)
            .displayOnlyValue()
            .create(0, 0, widgetWidth, widgetHeight, CommonComponents.EMPTY, (_, i) -> {
                boolean useSlider = i == 0;
                for (NumberWidgetPair pair : widgetPairs) pair.syncAndSwitch(useSlider);
                initWidgets(page);
            });
    private final CycleButton<Pose> posturePreviewButton = createCyclingButtonBuilder(ImmutableMap.of(
            Pose.STANDING, LocUtil.MODEL_VIEW_WIDGET("standingPosture"),
            Pose.CROUCHING, LocUtil.MODEL_VIEW_WIDGET("crouchingPosture"),
            Pose.SWIMMING, LocUtil.MODEL_VIEW_WIDGET("swimmingPosture"),
            Pose.FALL_FLYING, LocUtil.MODEL_VIEW_WIDGET("flyingPosture")), Pose.STANDING)
            .withTooltip(_ -> createTooltip("posture"))
            .displayOnlyValue()
            .create(0, 0, widgetWidth, widgetHeight, LocUtil.MODEL_VIEW_WIDGET("posture"),
                    (_, posture) -> {
                        currentPosture = posture;
                        initWidgets(page);
                    });
    private final CycleButton<Category> toggleCategoryButton = createCyclingButtonBuilder(ImmutableSortedMap.of(
            Category.CONFIGS, LocUtil.MODEL_VIEW_WIDGET(Category.CONFIGS.next().id),
            Category.PREVIEW, LocUtil.MODEL_VIEW_WIDGET(Category.PREVIEW.next().id),
            Category.DISABLE, LocUtil.MODEL_VIEW_WIDGET(Category.DISABLE.next().id)), Category.CONFIGS)
            .withTooltip(category -> createTooltip(category.next().id))
            .displayOnlyValue()
            .create(0, 0, compactWidgetWidth, widgetHeight, CommonComponents.EMPTY, (_, _) -> initWidgets(0));

    public ModelViewScreen() {
        super(LocUtil.MODEL_VIEW_TITLE());
    }

    @Override
    protected void init() {
        super.init();
        x = (width - xSize) / 2;
        y = (height - ySize) / 2;
        modifierKey = InputConstants.getKey(ConfigFile.config().binding.screenModifierKey);
        initWidgets(page);
        if (!initialized) loadBindTarget(RealCameraCore.currentTarget());
        initialized = true;
    }

    private void initWidgets(int page) {
        this.page = page;
        if (toggleCategoryButton.getValue() == Category.DISABLE && showTextureButton.getValue() == 0) {
            modelViewArea = new ScreenRectangle(x + xSize / 2, y, middleWidth / 2, ySize);
            textureViewArea = new ScreenRectangle(x + (xSize - middleWidth) / 2, y, middleWidth / 2, ySize);
        } else {
            modelViewArea = new ScreenRectangle(x + (xSize - middleWidth) / 2, y, middleWidth, ySize);
            textureViewArea = null;
            if (focusedRectWidget != null && focusedRectWidget.isFocused()) setFocused(null);
            focusedRectWidget = null;
        }
        clearWidgets();
        initLeftWidgets();
        if (textureViewArea != null) rectWidgets.forEach(this::addRenderableWidget);
        if (toggleCategoryButton.getValue() == Category.DISABLE)
            addRenderableWidget(showTextureButton).setPosition(x + (xSize - middleWidth) / 2 + 4, y + 4);
        addRenderableWidget(pauseButton).setPosition(x + (xSize + middleWidth) / 2 - 38, y + 4);
        addRenderableWidget(new SimpleIconButton(x + (xSize + middleWidth) / 2 - 20, y + 4, 16, 16, 0, 0, _ -> {
            modelScale = textureScale = DEFAULT_SCALE;
            entityYawSlider.setNumber(0);
            entityPitchSlider.setNumber(0);
            modelX = modelY = textureX = textureY = 0;
            xRot = yRot = 0;
            layers = 0;
        }));
        initRightWidgets();
    }

    private void initLeftWidgets() {
        GridLayout grid = new GridLayout();
        grid.defaultCellSetting().padding(4, 2, 0, 0);
        LayoutSettings smallSettings = grid.newCellSettings().padding(5, 3, 1, 1);
        GridLayout.RowHelper rows = grid.createRowHelper(2);
        switch (toggleCategoryButton.getValue()) {
            case CONFIGS -> {
                rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("import"), widgetWidth, this::importBindTarget)).setTooltip(createTooltip("import"));
                rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("export"), widgetWidth, this::exportBindTarget)).setTooltip(createTooltip("export"));
                rows.addChild(entityPitchSlider, 2);
                rows.addChild(entityYawSlider, 2);
                rows.addChild(selectingButton, 2);
                rows.addChild(forwardUField, smallSettings);
                rows.addChild(forwardVField, smallSettings);
                rows.addChild(upwardUField, smallSettings);
                rows.addChild(upwardVField, smallSettings);
                rows.addChild(posUField, smallSettings);
                rows.addChild(posVField, smallSettings);
                rows.addChild(textureIdField, 2, smallSettings).setTooltip(createTooltip("textureId"));
            }
            case PREVIEW -> {
                rows.addChild(toggleSliderButton, 1);
                rows.addChild(posturePreviewButton, 1);
                LayoutSettings numericControlSettings = grid.newCellSettings().padding(-20, 2, 0, 0);
                rows.addChild(bindXButton, smallSettings).setTooltip(createTooltip("bindButtons"));
                rows.addChild(offsetXPair, numericControlSettings);
                rows.addChild(bindYButton, smallSettings).setTooltip(createTooltip("bindButtons"));
                rows.addChild(offsetYPair, numericControlSettings);
                rows.addChild(bindZButton, smallSettings).setTooltip(createTooltip("bindButtons"));
                rows.addChild(offsetZPair, numericControlSettings);
                rows.addChild(bindRotButton, smallSettings).setTooltip(createTooltip("bindButtons"));
                rows.addChild(currentPitchPair(), numericControlSettings);
                rows.addChild(currentYawPair(), 2, grid.newCellSettings().padding(26, 2, 0, 0));
                rows.addChild(new SimpleIconButton(0, 0, _ -> widgetPairs.forEach(pair -> pair.setNumber(0))), smallSettings);
                rows.addChild(currentRollPair(), numericControlSettings);
                rows.addChild(scaleField, smallSettings).setTooltip(createTooltip("scale"));
                rows.addChild(depthField, smallSettings).setTooltip(createTooltip("depth"));
            }
            case DISABLE -> {
                LayoutSettings offsetXSettings = grid.newCellSettings().padding(-13, 3, 1, 1);
                rows.addChild(disableModeButton, 2);
                rows.addChild(disabledIdField, 2, smallSettings).setTooltip(createTooltip("textureId"));
                rows.addChild(selectionModeButton, 2);
                rows.addChild(focusedRectWidgetNumberField.setMax(rectWidgets.size()), smallSettings).setOnValueChange(index -> {
                    if (index == 0) focusedRectWidget = null;
                    else if (index > 0 && index <= rectWidgets.size()) {
                        focusedRectWidget = rectWidgets.get(index - 1);
                        uMinField.setNumber(focusedRectWidget.uMin);
                        vMinField.setNumber(focusedRectWidget.vMin);
                        uMaxField.setNumber(focusedRectWidget.uMax);
                        vMaxField.setNumber(focusedRectWidget.vMax);
                    }
                }).setTooltip(createTooltip("focusedRectangleNumber"));
                addRenderableWidget(new StringWidget(x + 4 + widgetWidth + 3, y + 4 + (widgetHeight + 2) * 3, 6, widgetHeight, LocUtil.literal("/"), font));
                rectWidgetsSizeWidget.setPosition(x + 4 + widgetWidth + 5 + font.width("/"), y + 4 + (widgetHeight + 2) * 3);
                rectWidgetsSizeWidget.setMessage(LocUtil.literal(String.valueOf(rectWidgets.size())));
                addRenderableWidget(rectWidgetsSizeWidget);
                rows.addChild(new SimpleIconButton(48, 0, _ -> deleteFocusedRectWidget()), grid.newCellSettings().padding(5 + widgetWidth - 18, 3, 1, 1))
                        .setTooltip(createTooltip("deleteSelectedRectangle"));
                rows.addChild(new StringWidget(26, widgetHeight, LocUtil.literal("uMin:"), font));
                rows.addChild(uMinField, offsetXSettings).setOnValueChange(f -> {
                    if (focusedRectWidget != null) focusedRectWidget.uMin = f;
                });
                rows.addChild(new StringWidget(26, widgetHeight, LocUtil.literal("vMin:"), font));
                rows.addChild(vMinField, offsetXSettings).setOnValueChange(f -> {
                    if (focusedRectWidget != null) focusedRectWidget.vMin = f;
                });
                rows.addChild(new StringWidget(26, widgetHeight, LocUtil.literal("uMax:"), font));
                rows.addChild(uMaxField, offsetXSettings).setOnValueChange(f -> {
                    if (focusedRectWidget != null) focusedRectWidget.uMax = f;
                });
                rows.addChild(new StringWidget(26, widgetHeight, LocUtil.literal("vMax:"), font));
                rows.addChild(vMaxField, offsetXSettings).setOnValueChange(f -> {
                    if (focusedRectWidget != null) focusedRectWidget.vMax = f;
                });
            }
            default -> throw new IllegalStateException("Unexpected value: " + toggleCategoryButton.getValue());
        }
        rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("save"), widgetWidth, button -> {
            if (nameField.getValue().isBlank()) {
                button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("emptyName").withStyle(ChatFormatting.RED)));
                return;
            }
            button.setTooltip(null);
            BindTarget bindTarget = genBindTarget();
            ConfigFile.config().putBindTarget(bindTarget);
            ConfigFile.save();
            loadBindTarget(bindTarget);
            initWidgets(page);
        }));
        rows.addChild(priorityField, smallSettings).setTooltip(createTooltip("priority"));
        rows.addChild(nameField, 2, smallSettings).setTooltip(createTooltip("targetName"));
        grid.arrangeElements();
        FrameLayout.alignInRectangle(grid, x, y + 2, x + (xSize - middleWidth) / 2 - 4, y + ySize, 0, 0);
        grid.visitWidgets(this::addRenderableWidget);
    }

    private void initRightWidgets() {
        GridLayout grid = new GridLayout();
        grid.defaultCellSetting().padding(4, 2, 0, 0);
        LayoutSettings smallSettings = grid.newCellSettings().padding(5, 3, 1, 1);
        GridLayout.RowHelper rows = grid.createRowHelper(4);
        rows.addChild(toggleCategoryButton, 3);
        rows.addChild(new SimpleIconButton(80, 0, _ -> {
            if (CompatibilityHelper.isModLoaded("cloth-config")) minecraft.setScreen(ConfigScreen.create(this));
        }), smallSettings).setTooltip(createTooltip("toConfigScreen"));
        final int widgetsPerPage, size;
        if (toggleCategoryButton.getValue() == Category.DISABLE) {
            widgetsPerPage = 7;
            size = disableConfigs.size();
            rows.addChild(disabledNameField, 3, smallSettings).setTooltip(createTooltip("disabledName"));
            rows.addChild(new SimpleIconButton(64, 0, button -> {
                String name = disabledNameField.getValue(), textureId = disabledIdField.getValue();
                if (name.isBlank()) {
                    button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("emptyName").withStyle(ChatFormatting.RED)));
                } else if (textureId.isBlank()) {
                    button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("emptyTextureId").withStyle(ChatFormatting.RED)));
                } else {
                    button.setTooltip(createTooltip("saveAs"));
                    DisableConfig disableConfig = new DisableConfig(name, textureId, disableModeButton.getValue() == 0, rectWidgets.stream().map(UVRectangleWidget::toUVRectangle).toList());
                    for (int i = 0; i < disableConfigs.size(); i++) {
                        if (disableConfigs.get(i).name().equals(name)) {
                            disableConfigs.set(i, disableConfig);
                            initWidgets(page);
                            return;
                        }
                    }
                    disableConfigs.add(disableConfig);
                    initWidgets(page);
                }
            }), smallSettings).setTooltip(createTooltip("saveAs"));
            for (int i = page * widgetsPerPage; i < Math.min((page + 1) * widgetsPerPage, size); i++) {
                DisableConfig config = disableConfigs.get(i);
                String targetName = nameField.getValue();
                Set<String> hiddenNames = hiddenNameMap.computeIfAbsent(targetName, _ -> new HashSet<>());
                addRenderableWidget(new CycleIconButton(32, 16, hiddenNames.contains(config.name()) ? 1 : 0, 2))
                        .setOnValueChange(value -> {
                            if (value == 0) hiddenNames.remove(config.name());
                            else hiddenNames.add(config.name());
                        })
                        .setPosition(x + (xSize + middleWidth) / 2 - 20, y + 5 + (widgetHeight + 2) * (2 + i % widgetsPerPage));
                rows.addChild(createButton(LocUtil.literal(config.name()), compactWidgetWidth, _ -> {
                    disabledNameField.setValue(config.name());
                    disabledIdField.setValue(config.textureId());
                    disableModeButton.setValue(config.disableAll() ? 0 : 1);
                    rectWidgets.clear();
                    for (UVRectangle rect : config.rectangles()) rectWidgets.add(createRectWidget(rect));
                    initWidgets(page);
                }), 3).setTooltip(Tooltip.create(LocUtil.literal(config.name())));
                rows.addChild(new SimpleIconButton(48, 0, _ -> {
                    disableConfigs.removeIf(disableConfig -> disableConfig.name().equals(config.name()));
                    if (disabledNameField.getValue().equals(config.name())) rectWidgets.clear();
                    initWidgets(page * widgetsPerPage > size - 2 && size > 1 ? page - 1 : page);
                }), smallSettings);
            }
        } else {
            widgetsPerPage = 8;
            List<BindTarget> fixedTargetList = ConfigFile.config().binding.fixedTargetList.stream().filter(target -> target.name().equals(RealCameraCore.currentTarget().name())).toList();
            List<BindTarget> targetList = ConfigFile.config().binding.targetList;
            final int fixedTargetCount = fixedTargetList.size();
            size = fixedTargetCount + targetList.size();
            for (int i = page * widgetsPerPage; i < Math.min((page + 1) * widgetsPerPage, size); i++) {
                BindTarget target = i < fixedTargetCount ? fixedTargetList.get(i) : targetList.get(i - fixedTargetCount);
                String name = target.name();
                rows.addChild(createButton(LocUtil.literal(name), compactWidgetWidth, _ -> loadBindTarget(target)), 3)
                        .setTooltip(Tooltip.create(name.equals(RealCameraCore.currentTarget().name()) ?
                                LocUtil.literal(name + "\n").append(LocUtil.MODEL_VIEW_WIDGET("currentConfig")) :
                                LocUtil.literal(name))
                        );
                if (i < fixedTargetCount) continue;
                rows.addChild(new SimpleIconButton(48, 0, _ -> {
                    targetList.remove(target);
                    ConfigFile.save();
                    initWidgets(page * widgetsPerPage > size - 2 && size > 1 ? page - 1 : page);
                }), smallSettings);
            }
        }
        grid.arrangeElements();
        FrameLayout.alignInRectangle(grid, x + (xSize + middleWidth) / 2 + 4, y + 2, x + xSize, y + ySize, 0, 0);
        grid.visitWidgets(this::addRenderableWidget);
        final int pages = (size - 1) / widgetsPerPage + 1;
        addRenderableWidget(new SimpleIconButton(x + (xSize + middleWidth) / 2 + 8, y + ySize - 20, 16, 16, 16, 0, _ -> initWidgets((page - 1 + pages) % pages)));
        Component pageInfoText = LocUtil.literal((page + 1) + " / " + pages);
        addRenderableWidget(new StringWidget(x + (3 * xSize + middleWidth) / 4 + 2 - font.width(pageInfoText) / 2, y + ySize - 20, font.width(pageInfoText), widgetHeight, pageInfoText, font));
        addRenderableWidget(new SimpleIconButton(x + xSize - 21, y + ySize - 20, 16, 16, 32, 0, _ -> initWidgets((page + 1) % pages)));
    }

    public boolean deleteFocusedRectWidget() {
        if (focusedRectWidget == null) return false;
        rectWidgets.remove(focusedRectWidget);
        removeWidget(focusedRectWidget);
        focusedRectWidgetNumberField.setNumber(0);
        focusedRectWidgetNumberField.setMax(rectWidgets.size());
        rectWidgetsSizeWidget.setMessage(LocUtil.literal(String.valueOf(rectWidgets.size())));
        return true;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        if (toggleCategoryButton.getValue() == Category.DISABLE && selectionModeButton.getValue() == 2 && inModelViewArea(mouseX, mouseY)) {
            GUIHelper.enableScissor(graphics, modelViewArea);
            GUIHelper.fill(graphics, mouseX - selectionRadius, mouseY - selectionRadius, mouseX + selectionRadius, mouseY + selectionRadius, 400, SELECTION_COLOR);
            graphics.disableScissor();
        }
        if (textureViewArea != null) {
            GUIHelper.enableScissor(graphics, textureViewArea);
            if (clickedX >= 0 && clickedY >= 0)
                graphics.fill(Math.min((int) clickedX, mouseX), Math.min((int) clickedY, mouseY), Math.max((int) clickedX, mouseX), Math.max((int) clickedY, mouseY), SELECTION_COLOR);
            if (disableModeButton.getValue() == 0)
                new UVRectangleWidget(0f, 0f, 1f, 1f).extractWidgetRenderState(graphics, mouseX, mouseY, partialTicks);
            graphics.disableScissor();
        }
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);
        graphics.fill(x, y, x + (xSize - middleWidth) / 2 - 4, y + ySize, SIDE_PANEL_BG);
        graphics.fill(x + (xSize - middleWidth) / 2, y, x + (xSize + middleWidth) / 2, y + ySize, CENTER_PANEL_BG);
        graphics.fill(x + (xSize + middleWidth) / 2 + 4, y, x + xSize, y + ySize, SIDE_PANEL_BG);

        ModelAnalyser analyser = new ModelAnalyser();
        BindTarget target = genBindTarget();
        target.offsets().scale *= modelScale;
        String textureId = toggleCategoryButton.getValue() == Category.DISABLE ? disabledIdField.getValue() : "";
        Set<String> hiddenNames = hiddenNameMap.getOrDefault(nameField.getValue(), Set.of());
        List<BuiltModelRecord> modelRecords = captureRotatedEntity(analyser, target, minecraft.player);
        List<BuiltModelRecord> textureRecords = modelRecords.stream().filter(record -> record.containsTextureId(textureId)).toList();
        ModelAnalyser.applyDisableConfigs(modelRecords, target, textureId, hiddenNames);
        computeFocusedPrimitives(analyser, modelRecords, textureRecords, mouseX, mouseY);
        renderCulledModels(graphics, analyser, target, modelRecords);
        if (textureViewArea != null) renderFlattenedModels(graphics, analyser, textureRecords);
    }

    private List<BuiltModelRecord> captureRotatedEntity(ModelAnalyser analyser, BindTarget target, LivingEntity entity) {
        float entityBodyYaw = entity.yBodyRot;
        float entityYaw = entity.getYRot();
        float entityPitch = entity.getXRot();
        float entityPrevHeadYaw = entity.yHeadRotO;
        float entityHeadYaw = entity.yHeadRot;
        entity.yBodyRot = 180.0f;
        entity.setYRot(180.0f + (float) entityYawSlider.getNumber());
        entity.setXRot((float) entityPitchSlider.getNumber());
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();
        try {
            int x1 = modelViewArea.left(), y1 = modelViewArea.top(), x2 = modelViewArea.right(), y2 = modelViewArea.bottom();
            Quaternionf rotation = new Quaternionf().rotateX((float) Math.PI / 6 + xRot).rotateY((float) Math.PI / 6 + yRot).rotateZ((float) Math.PI);
            PoseStack modelPose = new PoseStack();
            modelPose.translate((x1 + x2) / 2.0f, (y1 + y2) / 2.0f, 0);
            modelPose.scale(modelScale, modelScale, -modelScale);
            modelPose.translate(modelX, modelY, 0);
            modelPose.mulPose(rotation);
            modelPose.translate(0, -entity.getBbHeight() / 2.0f, 0);
            return analyser.captureModel(minecraft, entity, 1.0f, modelPose, target, currentPosture);
        } finally {
            entity.yBodyRot = entityBodyYaw;
            entity.setYRot(entityYaw);
            entity.setXRot(entityPitch);
            entity.yHeadRotO = entityPrevHeadYaw;
            entity.yHeadRot = entityHeadYaw;
        }
    }

    private void computeFocusedPrimitives(ModelAnalyser analyser, List<BuiltModelRecord> modelRecords, List<BuiltModelRecord> textureRecords, int mouseX, int mouseY) {
        if (toggleCategoryButton.getValue() == Category.DISABLE && selectionModeButton.getValue() == 2 && inModelViewArea(mouseX, mouseY))
            analyser.computeFocusedOnModel(modelRecords, mouseX - selectionRadius, mouseY - selectionRadius, mouseX + selectionRadius, mouseY + selectionRadius);
        if (inTextureViewArea(mouseX, mouseY)) {
            Vec2 mouseUV = translateXYToUV(mouseX, mouseY, textureViewArea);
            analyser.computeFocusedOnTexture(textureRecords, mouseUV.x, mouseUV.y);
        }
        if (inModelViewArea(mouseX, mouseY)) analyser.computeFocusedOnModel(modelRecords, mouseX, mouseY, layers);
        if (toggleCategoryButton.getValue() == Category.CONFIGS || selectionModeButton.getValue() == 1) analyser.computeFocusedPolyhedron();
        focusedPolyhedron = analyser.getFocusedPolyhedron();
        focusedTextureId = analyser.getFocusedTextureId();
    }

    private void renderCulledModels(GuiGraphicsExtractor graphics, ModelAnalyser analyser, BindTarget target, List<BuiltModelRecord> modelRecords) {
        int x1 = modelViewArea.left(), y1 = modelViewArea.top(), x2 = modelViewArea.right(), y2 = modelViewArea.bottom();
        GUIHelper.enableScissor(graphics, modelViewArea);
        float invScale = 1.0f / modelScale;
        Matrix4f modelTransform = new Matrix4f();
        modelTransform.scale(invScale, invScale, -invScale);
        modelTransform.translate(-(x1 + x2) / 2.0f, -(y1 + y2) / 2.0f, 0);
        GUIHelper.culledModels(graphics, modelRecords, modelScale, modelTransform, x1, y1, x2, y2);
        if (toggleCategoryButton.getValue() != Category.PREVIEW) analyser.drawFocusedInModelArea(graphics);
        if (toggleCategoryButton.getValue() == Category.CONFIGS) analyser.drawBindTarget(graphics, target, modelScale);
        else analyser.drawCameraDirections(graphics, modelScale);
        graphics.disableScissor();
    }

    private void renderFlattenedModels(GuiGraphicsExtractor graphics, ModelAnalyser analyser, List<BuiltModelRecord> textureRecords) {
        int x1 = textureViewArea.left(), y1 = textureViewArea.top(), x2 = textureViewArea.right(), y2 = textureViewArea.bottom();
        GUIHelper.enableScissor(graphics, textureViewArea);
        float scale = (textureScale * textureViewArea.width()) / (float) DEFAULT_SCALE;
        Vector3f offset = new Vector3f((float) textureX - 0.5f, (float) textureY - 0.5f, 0);
        GUIHelper.flattenedModels(graphics, textureRecords, offset, x1, y1, x2, y2, scale);
        Matrix4f texturePose = new Matrix4f();
        texturePose.translate((x1 + x2) / 2.0f, (y1 + y2) / 2.0f, 0);
        texturePose.scale(scale, scale, -scale);
        texturePose.translate(offset);
        analyser.drawFocusedInTextureArea(graphics, texturePose);
        graphics.disableScissor();
    }

    private Vec2 translateUVToXY(float u, float v, ScreenRectangle screenArea) {
        int left = screenArea.left(), width = screenArea.width(), top = screenArea.top(), height = screenArea.height();
        float scale = (textureScale * width) / (float) DEFAULT_SCALE;
        float x = (u + (float) textureX - 0.5f) * scale + left + width / 2.0f;
        float y = (v + (float) textureY - 0.5f) * scale + top + height / 2.0f;
        return new Vec2(x, y);
    }

    private Vec2 translateXYToUV(float x, float y, ScreenRectangle screenArea) {
        int left = screenArea.left(), width = screenArea.width(), top = screenArea.top(), height = screenArea.height();
        float invScale = (float) DEFAULT_SCALE / (textureScale * width);
        float u = (x - left - width / 2.0f) * invScale - (float) textureX + 0.5f;
        float v = (y - top - height / 2.0f) * invScale - (float) textureY + 0.5f;
        return new Vec2(u, v);
    }

    private void importBindTarget(Button button) {
        String base64 = minecraft.keyboardHandler.getClipboard().trim();
        DataResult<BindTarget> result = ConfigCodec.fromCompressedBase64(base64);
        switch (result) {
            case DataResult.Success<BindTarget> success -> {
                BindTarget target = success.value();
                loadBindTarget(target);
                button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("importSucceeded", LocUtil.literal("'" + target.name() + "'").withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GREEN)));
            }
            case DataResult.Error<BindTarget> error -> {
                String message = error.message();
                button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("importFailed", message).withStyle(ChatFormatting.RED)));
            }
        }
    }

    private void exportBindTarget(Button button) {
        BindTarget target = genBindTarget();
        DataResult<String> result = ConfigCodec.toCompressedBase64(target);
        switch (result) {
            case DataResult.Success<String> success -> {
                String base64 = success.value();
                minecraft.keyboardHandler.setClipboard(base64);
                button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("exportSucceeded").withStyle(ChatFormatting.GREEN)));
            }
            case DataResult.Error<String> error -> {
                String message = error.message();
                button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("exportFailed", message).withStyle(ChatFormatting.RED)));
            }
        }
    }

    private void loadBindTarget(BindTarget target) {
        if (target.isEmpty()) return;
        nameField.setValue(target.name());
        textureIdField.setValue(target.textureId());
        priorityField.setNumber(target.priority());
        depthField.setNumber(target.disablingDepth());
        forwardUField.setNumber(target.targetConfig().forwardU());
        forwardVField.setNumber(target.targetConfig().forwardV());
        upwardUField.setNumber(target.targetConfig().upwardU());
        upwardVField.setNumber(target.targetConfig().upwardV());
        posUField.setNumber(target.targetConfig().posU());
        posVField.setNumber(target.targetConfig().posV());
        bindXButton.setValue(target.bindConfig().bindX() ? 0 : 1);
        bindYButton.setValue(target.bindConfig().bindY() ? 0 : 1);
        bindZButton.setValue(target.bindConfig().bindZ() ? 0 : 1);
        bindRotButton.setValue(target.bindConfig().bindRotation() ? 0 : 1);
        OffsetConfig offsets = target.offsets();
        scaleField.setNumber(offsets.scale);
        offsetXPair.setNumber(offsets.x);
        offsetYPair.setNumber(offsets.y);
        offsetZPair.setNumber(offsets.z);
        offsetStandingPitchPair.setNumber(offsets.standing.pitch);
        offsetStandingYawPair.setNumber(offsets.standing.yaw);
        offsetStandingRollPair.setNumber(offsets.standing.roll);
        offsetCrouchingPitchPair.setNumber(offsets.crouching.pitch);
        offsetCrouchingYawPair.setNumber(offsets.crouching.yaw);
        offsetCrouchingRollPair.setNumber(offsets.crouching.roll);
        offsetSwimmingPitchPair.setNumber(offsets.swimming.pitch);
        offsetSwimmingYawPair.setNumber(offsets.swimming.yaw);
        offsetSwimmingRollPair.setNumber(offsets.swimming.roll);
        offsetFlyingPitchPair.setNumber(offsets.flying.pitch);
        offsetFlyingYawPair.setNumber(offsets.flying.yaw);
        offsetFlyingRollPair.setNumber(offsets.flying.roll);
        disableConfigs.clear();
        disableConfigs.addAll(target.disableConfigs());
    }

    private BindTarget genBindTarget() {
        TargetConfig targetConfig = new TargetConfig(forwardUField.getNumber(), forwardVField.getNumber(), upwardUField.getNumber(), upwardVField.getNumber(), posUField.getNumber(), posVField.getNumber());
        BindConfig bindConfig = new BindConfig(bindXButton.getValue() == 0, bindYButton.getValue() == 0, bindZButton.getValue() == 0, bindRotButton.getValue() == 0);
        OffsetConfig offsets = new OffsetConfig(scaleField.getNumber(), offsetXPair.getNumber(), offsetYPair.getNumber(), offsetZPair.getNumber(),
                new Posture(offsetStandingPitchPair.getNumber(), offsetStandingYawPair.getNumber(), offsetStandingRollPair.getNumber()),
                new Posture(offsetCrouchingPitchPair.getNumber(), offsetCrouchingYawPair.getNumber(), offsetCrouchingRollPair.getNumber()),
                new Posture(offsetSwimmingPitchPair.getNumber(), offsetSwimmingYawPair.getNumber(), offsetSwimmingRollPair.getNumber()),
                new Posture(offsetFlyingPitchPair.getNumber(), offsetFlyingYawPair.getNumber(), offsetFlyingRollPair.getNumber()));
        DisableConfig currentDisableConfig = new DisableConfig(disabledNameField.getValue(), disabledIdField.getValue(), disableModeButton.getValue() == 0, rectWidgets.stream().map(UVRectangleWidget::toUVRectangle).toList());
        List<DisableConfig> newDisableConfigs = new ArrayList<>(disableConfigs);
        for (int i = 0; i < newDisableConfigs.size(); i++) {
            if (newDisableConfigs.get(i).name().equals(currentDisableConfig.name()))
                newDisableConfigs.set(i, currentDisableConfig);
        }
        return new BindTarget(nameField.getValue(), textureIdField.getValue(), priorityField.getNumber(), depthField.getNumber(), targetConfig, bindConfig, offsets, newDisableConfigs);
    }

    private UVRectangleWidget addRectWidget(UVRectangleWidget rectWidget) {
        UVRectangleWidget foundRectWidget = rectWidgets.stream().filter(r -> r.contains(rectWidget)).findFirst().orElse(null);
        if (foundRectWidget == null) {
            rectWidgets.add(rectWidget);
            focusedRectWidgetNumberField.setMax(rectWidgets.size());
            rectWidgetsSizeWidget.setMessage(LocUtil.literal(String.valueOf(rectWidgets.size())));
            if (textureViewArea != null) addRenderableWidget(rectWidget);
            return rectWidget;
        }
        return foundRectWidget;
    }

    private NumberWidgetPair currentPitchPair() {
        return switch (currentPosture) {
            case CROUCHING -> offsetCrouchingPitchPair;
            case SWIMMING -> offsetSwimmingPitchPair;
            case FALL_FLYING -> offsetFlyingPitchPair;
            default -> offsetStandingPitchPair;
        };
    }

    private NumberWidgetPair currentYawPair() {
        return switch (currentPosture) {
            case CROUCHING -> offsetCrouchingYawPair;
            case SWIMMING -> offsetSwimmingYawPair;
            case FALL_FLYING -> offsetFlyingYawPair;
            default -> offsetStandingYawPair;
        };
    }

    private NumberWidgetPair currentRollPair() {
        return switch (currentPosture) {
            case CROUCHING -> offsetCrouchingRollPair;
            case SWIMMING -> offsetSwimmingRollPair;
            case FALL_FLYING -> offsetFlyingRollPair;
            default -> offsetStandingRollPair;
        };
    }

    private Tooltip createTooltip(String key, Object... args) {
        return Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP(key, args));
    }

    private UVRectangleWidget createRectWidget(UVRectangle rect) {
        return new UVRectangleWidget(rect.uMin(), rect.vMin(), rect.uMax(), rect.vMax());
    }

    private Button createButton(Component message, int width, Button.OnPress onPress) {
        return Button.builder(message, onPress).size(width, widgetHeight).build();
    }

    private <T> CycleButton.Builder<T> createCyclingButtonBuilder(Map<T, Component> messages, T defaultValue) {
        return new CycleButton.Builder<>(messages::get, () -> defaultValue).withValues(messages.keySet());
    }

    private DoubleSlider createSlider(String key, int width, double min, double max) {
        return new DoubleSlider(width, widgetHeight, 0, min, max, d -> LocUtil.MODEL_VIEW_WIDGET(key, MathUtil.round(d, 2)));
    }

    private NumberField<Float> createFloatField(int width, float defaultValue) {
        return NumberField.ofFloat(font, width - 2, widgetHeight - 2, defaultValue, null).setMax(1.0f).setMin(0f);
    }

    private EditBox createTextField(int width, int maxLength) {
        EditBox editBox = new EditBox(font, 0, 0, width - 2, widgetHeight - 2, CommonComponents.EMPTY);
        editBox.setMaxLength(maxLength);
        return editBox;
    }

    private boolean inModelViewArea(double x, double y) {
        return modelViewArea.containsPoint((int) x, (int) y);
    }

    private boolean inTextureViewArea(double x, double y) {
        return textureViewArea != null && textureViewArea.containsPoint((int) x, (int) y);
    }

    public boolean leftClickedWithModifier(double mouseX, double mouseY) {
        if (focusedPolyhedron.length == 0) return false;
        if (inModelViewArea(mouseX, mouseY) && toggleCategoryButton.getValue() == Category.CONFIGS) {
            float u = 0, v = 0;
            for (VertexData vertex : focusedPolyhedron[0]) {
                u += vertex.u();
                v += vertex.v();
            }
            u /= focusedPolyhedron[0].length;
            v /= focusedPolyhedron[0].length;
            if (selectingButton.getValue() == 0) {
                forwardUField.setNumber(u);
                forwardVField.setNumber(v);
            } else if (selectingButton.getValue() == 1) {
                upwardUField.setNumber(u);
                upwardVField.setNumber(v);
            } else {
                posUField.setNumber(u);
                posVField.setNumber(v);
            }
            textureIdField.setValue(focusedTextureId);
            return true;
        } else if ((inModelViewArea(mouseX, mouseY) || inTextureViewArea(mouseX, mouseY)) && toggleCategoryButton.getValue() == Category.DISABLE) {
            String disabledId = disabledIdField.getValue();
            if (disabledId.isBlank() || !focusedTextureId.contains(disabledId)) {
                disabledIdField.setValue(focusedTextureId);
                return true;
            }
            if (selectionModeButton.getValue() == 2) {
                List<UVRectangleWidget> rectWidgets = new ArrayList<>();
                for (VertexData[] primitive : focusedPolyhedron) {
                    float uMin = 1f, vMin = 1f, uMax = 0, vMax = 0;
                    for (VertexData vertex : primitive) {
                        if (vertex.u() < uMin) uMin = vertex.u();
                        if (vertex.v() < vMin) vMin = vertex.v();
                        if (vertex.u() > uMax) uMax = vertex.u();
                        if (vertex.v() > vMax) vMax = vertex.v();
                    }
                    rectWidgets.add(new UVRectangleWidget(uMin, vMin, uMax, vMax));
                }
                boolean merged;
                do {
                    merged = false;
                    for (int i = 0; i < rectWidgets.size(); i++) {
                        UVRectangleWidget rectWidget = rectWidgets.get(i);
                        for (int j = i + 1; j < rectWidgets.size(); ) {
                            if (rectWidget.mergeWith(rectWidgets.get(j))) {
                                rectWidgets.remove(j);
                                merged = true;
                            } else {
                                j++;
                            }
                        }
                    }
                } while (merged);
                rectWidgets.forEach(this::addRectWidget);
            } else {
                float uMin = 1f, vMin = 1f, uMax = 0, vMax = 0;
                for (VertexData[] primitive : focusedPolyhedron) {
                    for (VertexData vertex : primitive) {
                        if (vertex.u() < uMin) uMin = vertex.u();
                        if (vertex.v() < vMin) vMin = vertex.v();
                        if (vertex.u() > uMax) uMax = vertex.u();
                        if (vertex.v() > vMax) vMax = vertex.v();
                    }
                }
                setFocused(addRectWidget(new UVRectangleWidget(uMin, vMin, uMax, vMax)));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        double storedX = clickedX, storedY = clickedY;
        clickedX = clickedY = -1;
        if (event.input() == InputConstants.MOUSE_BUTTON_LEFT && toggleCategoryButton.getValue() != Category.PREVIEW) {
            if (InputConstants.isKeyDown(minecraft.getWindow(), modifierKey.getValue())) {
                if (leftClickedWithModifier(event.x(), event.y())) return true;
            } else if (inTextureViewArea(event.x(), event.y())) {
                if (super.mouseClicked(event, doubleClick)) return true;
                if (storedX >= 0 && storedY >= 0) {
                    float xMin = (float) Math.min(storedX, event.x()), yMin = (float) Math.min(storedY, event.y()), xMax = (float) Math.max(storedX, event.x()), yMax = (float) Math.max(storedY, event.y());
                    setFocused(addRectWidget(new UVRectangleWidget(xMin, yMin, xMax, yMax, textureViewArea)));
                    return true;
                }
                clickedX = (int) event.x();
                clickedY = (int) event.y();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dx, double dy) {
        if (event.input() == InputConstants.MOUSE_BUTTON_LEFT && !InputConstants.isKeyDown(minecraft.getWindow(), modifierKey.getValue())) {
            if (inModelViewArea(event.x(), event.y())) {
                xRot += (float) (Math.PI * dy / ySize);
                yRot -= (float) (Math.PI * dx / middleWidth);
                return true;
            }
        } else if (event.input() == InputConstants.MOUSE_BUTTON_RIGHT) {
            if (inModelViewArea(event.x(), event.y())) {
                modelX += dx / modelScale;
                modelY += dy / modelScale;
                return true;
            }
            if (inTextureViewArea(event.x(), event.y())) {
                textureX += dx / textureScale;
                textureY += dy / textureScale;
                return true;
            }
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (inModelViewArea(mouseX, mouseY)) {
            if (InputConstants.isKeyDown(minecraft.getWindow(), modifierKey.getValue())) {
                if (toggleCategoryButton.getValue() == Category.DISABLE && selectionModeButton.getValue() == 2)
                    selectionRadius = Mth.clamp(selectionRadius + (int) verticalAmount * 2, 2, 48);
                else layers = Math.max(0, layers + (int) verticalAmount);
            } else {
                modelScale = Mth.clamp(modelScale + (int) verticalAmount * modelScale / 16, MIN_SCALE, MAX_SCALE);
            }
            return true;
        } else if (inTextureViewArea(mouseX, mouseY)) {
            textureScale = Mth.clamp(textureScale + (int) verticalAmount * textureScale / 16, MIN_SCALE, MAX_SCALE);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (event.isSelection()) {
            GuiEventListener focused = getFocused();
            if (focused != null && !focused.isFocused()) setFocused(focusedRectWidget);
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return pauseButton.getValue() == 1;
    }

    private enum Category {
        CONFIGS,
        PREVIEW,
        DISABLE;

        public final String id = name().toLowerCase();

        public Category next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private final class UVRectangleWidget extends AbstractWidget {
        private float uMin, vMin, uMax, vMax;

        public UVRectangleWidget(float uMin, float vMin, float uMax, float vMax) {
            super(0, 0, 16, 16, CommonComponents.EMPTY);
            this.uMin = uMin;
            this.vMin = vMin;
            this.uMax = uMax;
            this.vMax = vMax;
        }

        public UVRectangleWidget(float xMin, float yMin, float xMax, float yMax, @NonNull ScreenRectangle screenArea) {
            super((int) xMin, (int) yMin, (int) (xMax - xMin), (int) (yMax - yMin), CommonComponents.EMPTY);
            Vec2 minUV = translateXYToUV(xMin, yMin, screenArea), maxUV = translateXYToUV(xMax, yMax, screenArea);
            this.uMin = minUV.x;
            this.vMin = minUV.y;
            this.uMax = maxUV.x;
            this.vMax = maxUV.y;
        }

        public UVRectangle toUVRectangle() {
            return new UVRectangle(uMin, vMin, uMax, vMax);
        }

        public boolean contains(UVRectangleWidget other) {
            return uMin <= other.uMin && vMin <= other.vMin && uMax >= other.uMax && vMax >= other.vMax;
        }

        public boolean mergeWith(UVRectangleWidget other) {
            if (uMin == other.uMin && uMax == other.uMax && vMin <= other.vMax && vMax >= other.vMin) {
                vMin = Math.min(vMin, other.vMin);
                vMax = Math.max(vMax, other.vMax);
                return true;
            }
            if (vMin == other.vMin && vMax == other.vMax && uMin <= other.uMax && uMax >= other.uMin) {
                uMin = Math.min(uMin, other.uMin);
                uMax = Math.max(uMax, other.uMax);
                return true;
            }
            return false;
        }

        @Override
        protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
            Vec2 minXY = translateUVToXY(uMin, vMin, textureViewArea), maxXY = translateUVToXY(uMax, vMax, textureViewArea);
            float x1 = minXY.x, y1 = minXY.y, x2 = maxXY.x, y2 = maxXY.y, width = x2 - x1, height = y2 - y1;
            setX((int) x1);
            setY((int) y1);
            setWidth((int) width);
            setHeight((int) height);
            GUIHelper.enableScissor(graphics, textureViewArea);
            GUIHelper.fill(graphics, x1, y1, x2, y2, SELECTION_COLOR);
            if (isHoveredOrFocused()) GUIHelper.fill(graphics, x1, y1, x2, y2, SELECTION_HOVER_COLOR);
            if (isFocused() || this == focusedRectWidget)
                GUIHelper.outline(graphics, x1, y1, width, height, OUTLINE_COLOR);
            graphics.disableScissor();
        }

        @Override
        public boolean keyPressed(@NonNull KeyEvent event) {
            if (textureViewArea == null) return false;
            if (event.input() == InputConstants.KEY_DELETE && deleteFocusedRectWidget()) return true;
            if (event.isSelection() && uMin < uMax && vMin < vMax) {
                textureX = 0.5 * (1 - uMin - uMax);
                textureY = 0.5 * (1 - vMin - vMax);
                textureScale = (int) Mth.clamp(32d * DEFAULT_SCALE / (textureViewArea.width() * Math.sqrt((uMax - uMin) * (vMax - vMin))), MIN_SCALE, MAX_SCALE);
                return true;
            }
            return super.keyPressed(event);
        }

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);
            if (focused) {
                focusedRectWidget = this;
                focusedRectWidgetNumberField.setNumber(rectWidgets.indexOf(this) + 1);
                uMinField.setNumber(uMin);
                vMinField.setNumber(vMin);
                uMaxField.setNumber(uMax);
                vMaxField.setNumber(vMax);
            }
        }

        @Override
        protected void updateWidgetNarration(@NonNull NarrationElementOutput narrationElementOutput) {
        }
    }
}
