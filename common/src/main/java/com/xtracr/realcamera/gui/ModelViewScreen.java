package com.xtracr.realcamera.gui;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.mojang.blaze3d.platform.InputConstants;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.BindTarget.*;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.renderer.VertexData;
import com.xtracr.realcamera.util.LocUtil;
import com.xtracr.realcamera.util.MathUtil;
import io.netty.buffer.Unpooled;
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
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class ModelViewScreen extends Screen {
    public final ModelAnalyser analyser = new ModelAnalyser();
    protected int xSize = 450, ySize = 206, middleWidth = xSize - 200, widgetWidth = (xSize - middleWidth) / 4 - 8, widgetHeight = 18;
    protected int x, y, page = 0;
    protected InputConstants.Key modifierKey = ConfigFile.config().getScreenModifierKey();
    private boolean initialized;
    private int  modelScale = 80, textureScale = 80, layers = 0, selectionRadius = 10;
    private double modelX, modelY, textureX, textureY, clickedX = -1, clickedY = -1;
    private float xRot, yRot;
    private String focusedTextureId;
    private ScreenRectangle modelViewArea;
    @Nullable
    private ScreenRectangle textureViewArea;
    private VertexData[][] focusedPolyhedron = new VertexData[0][];
    private List<DisableConfig> configsInClipBoard = new ArrayList<>();
    @Nullable
    private UVRectangleWidget focusedRectWidget;
    private StringWidget rectWidgetsSizeWidget;
    private EditBox textureIdField, nameField, disabledNameField, disabledIdField;
    private NumberField<Integer> priorityField, focusedRectWidgetNumberField;
    private NumberField<Float> forwardUField, forwardVField, upwardUField, upwardVField, posUField, posVField;
    private NumberField<Float> uMinField, vMinField, uMaxField, vMaxField;
    private NumberField<Float> offsetXField, offsetYField, offsetZField, offsetPitchField, offsetYawField, offsetRollField, scaleField, depthField;
    private final List<DisableConfig> disableConfigs = new ArrayList<>();
    private final List<UVRectangleWidget> rectWidgets = new ArrayList<>();
    private final Map<String, Set<String>> hiddenNameMap = new HashMap<>();
    private final CyclingTexturedButton showTextureButton = new CyclingTexturedButton(48, 16, 0, 2).setOnValueChange(_ -> initWidgets(page));
    private final CyclingTexturedButton pauseButton = new CyclingTexturedButton(0, 16, 0, 2);
    private final CyclingTexturedButton bindXButton = new CyclingTexturedButton(16, 16, 1, 2);
    private final CyclingTexturedButton bindYButton = new CyclingTexturedButton(16, 16, 0, 2);
    private final CyclingTexturedButton bindZButton = new CyclingTexturedButton(16, 16, 1, 2);
    private final CyclingTexturedButton bindRotButton = new CyclingTexturedButton(16, 16, 1, 2);
    private final DoubleSlider entityPitchSlider = createSlider("pitch", widgetWidth * 2 + 4, -90.0, 90.0);
    private final DoubleSlider entityYawSlider = createSlider("yaw", widgetWidth * 2 + 4, -60.0, 60.0);
    private final DoubleSlider offsetXSlider = createSlider("offsetX", widgetWidth * 2 - 18, ModConfig.MIN_OFFSET_D, ModConfig.MAX_OFFSET_D);
    private final DoubleSlider offsetYSlider = createSlider("offsetY", widgetWidth * 2 - 18, ModConfig.MIN_OFFSET_D, ModConfig.MAX_OFFSET_D);
    private final DoubleSlider offsetZSlider = createSlider("offsetZ", widgetWidth * 2 - 18, ModConfig.MIN_OFFSET_D, ModConfig.MAX_OFFSET_D);
    private final DoubleSlider offsetPitchSlider = createSlider("pitch", widgetWidth * 2 - 18, -180.0, 180.0);
    private final DoubleSlider offsetYawSlider = createSlider("yaw", widgetWidth * 2 - 18, -180.0, 180.0);
    private final DoubleSlider offsetRollSlider = createSlider("roll", widgetWidth * 2 - 18, -180.0, 180.0);
    private final CycleButton<Integer> selectingButton = createCyclingButtonBuilder(ImmutableSortedMap.of(
            0, LocUtil.MODEL_VIEW_WIDGET("forwardVector").withStyle(ChatFormatting.GREEN),
            1, LocUtil.MODEL_VIEW_WIDGET("upwardVector").withStyle(ChatFormatting.RED),
            2, LocUtil.MODEL_VIEW_WIDGET("position").withStyle(ChatFormatting.BLUE)), 0)
            .withTooltip(_ -> createTooltip("selecting", modifierKey.getDisplayName(), modifierKey.getDisplayName()))
            .create(0, 0, widgetWidth * 2 + 4, widgetHeight, LocUtil.MODEL_VIEW_WIDGET("selecting"));
    private final CycleButton<Integer> disableModeButton = createCyclingButtonBuilder(ImmutableMap.of(
            0, LocUtil.MODEL_VIEW_WIDGET("all").withStyle(ChatFormatting.GREEN),
            1, LocUtil.MODEL_VIEW_WIDGET("part").withStyle(ChatFormatting.BLUE)), 0)
            .withTooltip(_ -> createTooltip("disableMode", modifierKey.getDisplayName()))
            .create(0, 0, widgetWidth * 2 + 4, widgetHeight, LocUtil.MODEL_VIEW_WIDGET("disableMode"));
    private final CycleButton<Integer> selectionModeButton = createCyclingButtonBuilder(ImmutableSortedMap.of(
            0, LocUtil.MODEL_VIEW_WIDGET("single"),
            1, LocUtil.MODEL_VIEW_WIDGET("multiple"),
            2, LocUtil.MODEL_VIEW_WIDGET("range").withStyle(ChatFormatting.BLUE)), 0)
            .withTooltip(_ -> createTooltip("selectionMode", modifierKey.getDisplayName(), modifierKey.getDisplayName()))
            .create(0, 0, widgetWidth * 2 + 4, widgetHeight, LocUtil.MODEL_VIEW_WIDGET("selectionMode"));
    private final CycleButton<Integer> toggleSliderButton = createCyclingButtonBuilder(ImmutableMap.of(
            0, LocUtil.MODEL_VIEW_WIDGET("toggleSliderToField"),
            1, LocUtil.MODEL_VIEW_WIDGET("toggleFieldToSlider")), 0)
            .withTooltip(_ -> createTooltip("toggleSlider"))
            .displayOnlyValue()
            .create(0, 0, widgetWidth * 2 + 4, widgetHeight, CommonComponents.EMPTY, (_, i) -> {
                if (i == 1) {
                    offsetXField.setNumber((float) offsetXSlider.getValue());
                    offsetYField.setNumber((float) offsetYSlider.getValue());
                    offsetZField.setNumber((float) offsetZSlider.getValue());
                    offsetPitchField.setNumber((float) offsetPitchSlider.getValue());
                    offsetYawField.setNumber((float) offsetYawSlider.getValue());
                    offsetRollField.setNumber((float) offsetRollSlider.getValue());
                } else {
                    offsetXSlider.setValue(offsetXField.getNumber());
                    offsetYSlider.setValue(offsetYField.getNumber());
                    offsetZSlider.setValue(offsetZField.getNumber());
                    offsetPitchSlider.setValue(offsetPitchField.getNumber());
                    offsetYawSlider.setValue(offsetYawField.getNumber());
                    offsetRollSlider.setValue(offsetRollField.getNumber());
                }
                initWidgets(page);
            });
    private final CycleButton<Category> toggleCategoryButton = createCyclingButtonBuilder(ImmutableSortedMap.of(
            Category.CONFIGS, LocUtil.MODEL_VIEW_WIDGET(Category.CONFIGS.next().id),
            Category.PREVIEW, LocUtil.MODEL_VIEW_WIDGET(Category.PREVIEW.next().id),
            Category.DISABLE, LocUtil.MODEL_VIEW_WIDGET(Category.DISABLE.next().id)), Category.CONFIGS)
            .withTooltip(category -> createTooltip(category.next().id))
            .displayOnlyValue()
            .create(0, 0, widgetWidth * 2 - 18, widgetHeight, CommonComponents.EMPTY, (_, _) -> initWidgets(0));

    public ModelViewScreen() {
        super(LocUtil.MODEL_VIEW_TITLE());
    }

    @Override
    protected void init() {
        super.init();
        x = (width - xSize) / 2;
        y = (height - ySize) / 2;
        modifierKey = ConfigFile.config().getScreenModifierKey();
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
        if (toggleCategoryButton.getValue() == Category.DISABLE) addRenderableWidget(showTextureButton).setPosition(x + (xSize - middleWidth) / 2 + 4, y + 4);
        addRenderableWidget(pauseButton).setPosition(x + (xSize + middleWidth) / 2 - 38, y + 4);
        addRenderableWidget(new TexturedButton(x + (xSize + middleWidth) / 2 - 20, y + 4, 16, 16, 0, 0, _ -> {
            modelScale = textureScale = 80;
            entityYawSlider.setValue(0);
            entityPitchSlider.setValue(0);
            modelX = modelY = textureX = textureY = 0;
            xRot = yRot = 0;
            layers = 0;
        }));
        initRightWidgets();
    }

    private void initLeftWidgets() {
        rectWidgetsSizeWidget = new StringWidget(x + 4 + widgetWidth + 5, y + 4 + (widgetHeight + 2) * 3, widgetWidth - 22, widgetHeight, LocUtil.literal(String.valueOf(rectWidgets.size())), font);
        forwardUField = createFloatField(widgetWidth, 0, forwardUField);
        forwardVField = createFloatField(widgetWidth, 0, forwardVField);
        upwardUField = createFloatField(widgetWidth, 0, upwardUField);
        upwardVField = createFloatField(widgetWidth, 0, upwardVField);
        posUField = createFloatField(widgetWidth, 0, posUField);
        posVField = createFloatField(widgetWidth, 0, posVField);
        textureIdField = createTextField(widgetWidth * 2 + 4, 1024, textureIdField);
        disabledIdField = createTextField(widgetWidth * 2 + 4, 1024, disabledIdField);
        focusedRectWidgetNumberField = NumberField.ofInt(font, widgetWidth - 2, widgetHeight - 2, 0, focusedRectWidgetNumberField).setMin(0).setMax(rectWidgets.size());
        uMinField = createFloatField(widgetWidth * 2 - 24, 0, uMinField).setMin(-1f).setMax(2f);
        uMaxField = createFloatField(widgetWidth * 2 - 24, 0, uMaxField).setMin(-1f).setMax(2f);
        vMinField = createFloatField(widgetWidth * 2 - 24, 0, vMinField).setMin(-1f).setMax(2f);
        vMaxField = createFloatField(widgetWidth * 2 - 24, 0, vMaxField).setMin(-1f).setMax(2f);
        offsetXField = createFloatField(widgetWidth * 2 - 20, 0, offsetXField).setMin(ModConfig.MIN_OFFSET_F).setMax(ModConfig.MAX_OFFSET_F);
        offsetYField = createFloatField(widgetWidth * 2 - 20, 0, offsetYField).setMin(ModConfig.MIN_OFFSET_F).setMax(ModConfig.MAX_OFFSET_F);
        offsetZField = createFloatField(widgetWidth * 2 - 20, 0, offsetZField).setMin(ModConfig.MIN_OFFSET_F).setMax(ModConfig.MAX_OFFSET_F);
        offsetPitchField = createFloatField(widgetWidth * 2 - 20, 0, offsetPitchField).setMin(-180.0f).setMax(180.0f);
        offsetYawField = createFloatField(widgetWidth * 2 - 20, 0, offsetYawField).setMin(-180.0f).setMax(180.0f);
        offsetRollField = createFloatField(widgetWidth * 2 - 20, 0, offsetRollField).setMin(-180.0f).setMax(180.0f);
        scaleField = createFloatField(widgetWidth, 1.0f, scaleField).setMax(64.0f);
        depthField = createFloatField(widgetWidth, 0.2f, depthField).setMax(16.0f);
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
                boolean useSliderOffset = toggleSliderButton.getValue() == 0;
                rows.addChild(toggleSliderButton, 2);
                LayoutSettings sliderSettings = grid.newCellSettings().padding(-20, 2, 0, 0);
                LayoutSettings fieldSettings = grid.newCellSettings().padding(-17, 3, 1, 1);
                rows.addChild(bindXButton, smallSettings).setTooltip(createTooltip("bindButtons"));
                if (useSliderOffset) rows.addChild(offsetXSlider, sliderSettings);
                else rows.addChild(offsetXField, fieldSettings);
                rows.addChild(bindYButton, smallSettings).setTooltip(createTooltip("bindButtons"));
                if (useSliderOffset) rows.addChild(offsetYSlider, sliderSettings);
                else rows.addChild(offsetYField, fieldSettings);
                rows.addChild(bindZButton, smallSettings).setTooltip(createTooltip("bindButtons"));
                if (useSliderOffset) rows.addChild(offsetZSlider, sliderSettings);
                else rows.addChild(offsetZField, fieldSettings);
                rows.addChild(bindRotButton, smallSettings).setTooltip(createTooltip("bindButtons"));
                if (useSliderOffset) rows.addChild(offsetPitchSlider, sliderSettings);
                else rows.addChild(offsetPitchField, fieldSettings);
                if (useSliderOffset) rows.addChild(offsetYawSlider, 2, grid.newCellSettings().padding(26, 2, 0, 0));
                else rows.addChild(offsetYawField, 2, grid.newCellSettings().padding(29, 3, 1, 1));
                rows.addChild(new TexturedButton(0, 0, _ -> {
                    offsetXField.setNumber(0f);
                    offsetYField.setNumber(0f);
                    offsetZField.setNumber(0f);
                    offsetPitchField.setNumber(0f);
                    offsetYawField.setNumber(0f);
                    offsetRollField.setNumber(0f);
                    offsetXSlider.setValue(0);
                    offsetYSlider.setValue(0);
                    offsetZSlider.setValue(0);
                    offsetPitchSlider.setValue(0);
                    offsetYawSlider.setValue(0);
                    offsetRollSlider.setValue(0);
                }), smallSettings);
                if (useSliderOffset) rows.addChild(offsetRollSlider, sliderSettings);
                else rows.addChild(offsetRollField, fieldSettings);
                rows.addChild(scaleField, smallSettings).setTooltip(createTooltip("scale"));
                rows.addChild(depthField, smallSettings).setTooltip(createTooltip("depth"));
            }
            case DISABLE -> {
                LayoutSettings offsetXSettings = grid.newCellSettings().padding(-13, 3, 1, 1);
                rows.addChild(disableModeButton, 2);
                rows.addChild(disabledIdField, 2, smallSettings).setTooltip(createTooltip("textureId"));
                rows.addChild(selectionModeButton, 2);
                rows.addChild(focusedRectWidgetNumberField, smallSettings).setOnValueChange(index -> {
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
                addRenderableWidget(rectWidgetsSizeWidget);
                rows.addChild(new TexturedButton(48, 0, _ -> deleteFocusedRectWidget()), grid.newCellSettings().padding(5 + widgetWidth - 18, 3, 1, 1))
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
        rows.addChild(priorityField = NumberField.ofInt(font, widgetWidth - 2, widgetHeight - 2, 0, priorityField), smallSettings).setTooltip(createTooltip("priority"));
        rows.addChild(nameField = createTextField(widgetWidth * 2 + 4, 20, nameField), 2, smallSettings).setTooltip(createTooltip("targetName"));
        grid.arrangeElements();
        FrameLayout.alignInRectangle(grid, x, y + 2, x + (xSize - middleWidth) / 2 - 4, y + ySize, 0, 0);
        grid.visitWidgets(this::addRenderableWidget);
    }

    private void initRightWidgets() {
        disabledNameField = createTextField(widgetWidth * 2 - 18, 20, disabledNameField);
        GridLayout grid = new GridLayout();
        grid.defaultCellSetting().padding(4, 2, 0, 0);
        LayoutSettings smallSettings = grid.newCellSettings().padding(5, 3, 1, 1);
        GridLayout.RowHelper rows = grid.createRowHelper(4);
        rows.addChild(toggleCategoryButton, 3);
        rows.addChild(new TexturedButton(80, 0, _ -> {
            // if (CompatibilityHelper.isModLoaded("cloth-config")) minecraft.setScreen(ConfigScreen.create(this)); TODO
        }), smallSettings).setTooltip(createTooltip("toConfigScreen"));
        final int widgetsPerPage, size;
        if (toggleCategoryButton.getValue() == Category.DISABLE) {
            widgetsPerPage = 6;
            size = disableConfigs.size();
            rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("copy"), widgetWidth, _ -> configsInClipBoard = List.copyOf(disableConfigs)), 2).setTooltip(createTooltip("copy"));
            rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("paste"), widgetWidth, _ -> {
                configsInClipBoard.stream().filter(config -> disableConfigs.stream().noneMatch(c -> c.name().equals(config.name()))).forEach(disableConfigs::add);
                initWidgets(0);
            }), 2).setTooltip(createTooltip("paste"));
            rows.addChild(disabledNameField, 3, smallSettings).setTooltip(createTooltip("disabledName"));
            rows.addChild(new TexturedButton(64, 0, button -> {
                String name = disabledNameField.getValue(), textureId = disabledIdField.getValue();
                if (name.isBlank()) {
                    button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("emptyName").withStyle(ChatFormatting.RED)));
                } else if (textureId.isBlank()) {
                    button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("emptyTextureId").withStyle(ChatFormatting.RED)));
                } else {
                    button.setTooltip(createTooltip("saveAs"));
                    DisableConfig disableConfig = new DisableConfig(name, textureId, disableModeButton.getValue() == 0, rectWidgets.stream().map(UVRectangleWidget::toUVRectangle).toArray(UVRectangle[]::new));
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
                addRenderableWidget(new CyclingTexturedButton(32, 16, hiddenNames.contains(config.name()) ? 1 : 0, 2))
                        .setOnValueChange(value -> {
                            if (value == 0) hiddenNames.remove(config.name());
                            else hiddenNames.add(config.name());
                        })
                        .setPosition(x + (xSize + middleWidth) / 2 - 20, y + 5 + (widgetHeight + 2) * (3 + i % widgetsPerPage));
                rows.addChild(createButton(LocUtil.literal(config.name()), widgetWidth * 2 - 18, _ -> {
                    disabledNameField.setValue(config.name());
                    disabledIdField.setValue(config.textureId());
                    disableModeButton.setValue(config.disableAll() ? 0 : 1);
                    rectWidgets.clear();
                    for (UVRectangle rect : config.rectangles()) rectWidgets.add(createRectWidget(rect));
                    initWidgets(page);
                }), 3).setTooltip(Tooltip.create(LocUtil.literal(config.name())));
                rows.addChild(new TexturedButton(48, 0, _ -> {
                    disableConfigs.removeIf(disableConfig -> disableConfig.name().equals(config.name()));
                    if (disabledNameField.getValue().equals(config.name())) rectWidgets.clear();
                    initWidgets(page * widgetsPerPage > size - 2 && size > 1 ? page - 1 : page);
                }), smallSettings);
            }
        } else {
            widgetsPerPage = 8;
            List<BindTarget> fixedTargetList = ConfigFile.config().getFixedTargetList().stream().filter(target -> target.name().equals(RealCameraCore.currentTarget().name())).toList();
            List<BindTarget> targetList = ConfigFile.config().getBindTargetList();
            final int fixedTargetCount = fixedTargetList.size();
            size = fixedTargetCount + targetList.size();
            for (int i = page * widgetsPerPage; i < Math.min((page + 1) * widgetsPerPage, size); i++) {
                BindTarget target = i < fixedTargetCount ? fixedTargetList.get(i) : targetList.get(i - fixedTargetCount);
                String name = target.name();
                rows.addChild(createButton(LocUtil.literal(name), widgetWidth * 2 - 18, _ -> loadBindTarget(target)), 3).setTooltip(Tooltip.create(LocUtil.literal(name)));
                if (i < fixedTargetCount) continue;
                rows.addChild(new TexturedButton(48, 0, _ -> {
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
        addRenderableWidget(new TexturedButton(x + (xSize + middleWidth) / 2 + 8, y + ySize - 20, 16, 16, 16, 0, _ -> initWidgets((page - 1 + pages) % pages)));
        addRenderableWidget(new StringWidget(x + (xSize + middleWidth) / 2 + 30, y + ySize - 20, widgetWidth * 2 - 40, widgetHeight, LocUtil.literal((page + 1) + " / " + pages), font));
        addRenderableWidget(new TexturedButton(x + xSize - 21, y + ySize - 20, 16, 16, 32, 0, _ -> initWidgets((page + 1) % pages)));
    }

    public @NotNull UVRectangleWidget addRectWidget(@NotNull UVRectangleWidget rectWidget) {
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
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        if (toggleCategoryButton.getValue() == Category.DISABLE && selectionModeButton.getValue() == 2 && inModelViewArea(mouseX, mouseY)) {
            GUIHelper.enableScissor(graphics, modelViewArea);
            GUIHelper.fill(graphics, mouseX - selectionRadius, mouseY - selectionRadius, mouseX + selectionRadius, mouseY + selectionRadius, 400, 0x4F3333CC);
            graphics.disableScissor();
        }
        if (textureViewArea != null) {
            GUIHelper.enableScissor(graphics, textureViewArea);
            if (clickedX >= 0 && clickedY >= 0)
                graphics.fill(Math.min((int) clickedX, mouseX), Math.min((int) clickedY, mouseY), Math.max((int) clickedX, mouseX), Math.max((int) clickedY, mouseY), 0x4F3333CC);
            if (disableModeButton.getValue() == 0)
                new UVRectangleWidget(0f, 0f, 1f, 1f).extractWidgetRenderState(graphics, mouseX, mouseY, partialTicks);
            graphics.disableScissor();
        }
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);
        graphics.fill(x, y, x + (xSize - middleWidth) / 2 - 4, y + ySize, 0xFF444444);
        graphics.fill(x + (xSize - middleWidth) / 2, y, x + (xSize + middleWidth) / 2, y + ySize, 0xFF222222);
        graphics.fill(x + (xSize + middleWidth) / 2 + 4, y, x + xSize, y + ySize, 0xFF444444);
        analyser.initialize(genBindTarget(), modelScale);
        renderModelViewArea(graphics, minecraft.player);
        renderTextureViewArea(graphics, minecraft.player);
        applyAnalyser(graphics, mouseX, mouseY);
    }

    protected void applyAnalyser(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        String textureId = toggleCategoryButton.getValue() == Category.DISABLE ? disabledIdField.getValue() : "";
        Set<String> hiddenNames = hiddenNameMap.getOrDefault(nameField.getValue(), Set.of());
        analyser.applyDisableConfigs(textureId, hiddenNames);
        if (toggleCategoryButton.getValue() == Category.DISABLE && selectionModeButton.getValue() == 2 && inModelViewArea(mouseX, mouseY))
            analyser.computeFocusedOnModel(mouseX - selectionRadius, mouseY - selectionRadius, mouseX + selectionRadius, mouseY + selectionRadius);
        if (inTextureViewArea(mouseX, mouseY)) analyser.computeFocusedOnTexture(mouseX, mouseY);
        if (inModelViewArea(mouseX, mouseY)) analyser.computeFocusedOnModel(mouseX, mouseY, layers);
        if (toggleCategoryButton.getValue() == Category.CONFIGS || selectionModeButton.getValue() == 1) analyser.computeFocusedPolyhedron();
        focusedPolyhedron = analyser.focusedPolyhedron.toArray(new VertexData[0][]);
        focusedTextureId = analyser.getFocusedTextureId();
        GUIHelper.enableScissor(graphics, modelViewArea);
        if (toggleCategoryButton.getValue() != Category.PREVIEW) analyser.drawFocusedInModelArea(graphics);
        if (toggleCategoryButton.getValue() == Category.CONFIGS) analyser.drawBindTarget(graphics);
        else analyser.drawCameraDirections(graphics);
        graphics.disableScissor();
        if (textureViewArea != null) {
            GUIHelper.enableScissor(graphics, textureViewArea);
            analyser.drawFocusedInTextureArea(graphics);
            graphics.disableScissor();
        }
    }

    protected void renderModelViewArea(GuiGraphicsExtractor graphics, LivingEntity entity) {
        int x1 = modelViewArea.left(), y1 = modelViewArea.top(), x2 = modelViewArea.right(), y2 = modelViewArea.bottom();
        Quaternionf quaternionf = new Quaternionf().rotateX((float) Math.PI / 6 + xRot).rotateY((float) Math.PI / 6 + yRot).rotateZ((float) Math.PI);
        float entityBodyYaw = entity.yBodyRot;
        float entityYaw = entity.getYRot();
        float entityPitch = entity.getXRot();
        float entityPrevHeadYaw = entity.yHeadRotO;
        float entityHeadYaw = entity.yHeadRot;
        entity.yBodyRot = 180.0f;
        entity.setYRot(180.0f + (float) entityYawSlider.getValue());
        entity.setXRot((float) entityPitchSlider.getValue());
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();
        Vector3f offset = new Vector3f((float) modelX, (float) modelY, 0);
        renderEntityWithAnalyser(graphics, x1, y1, x2, y2, modelScale, offset, quaternionf, entity);
        entity.yBodyRot = entityBodyYaw;
        entity.setYRot(entityYaw);
        entity.setXRot(entityPitch);
        entity.yHeadRotO = entityPrevHeadYaw;
        entity.yHeadRot = entityHeadYaw;
    }

    protected void renderEntityWithAnalyser(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, float scale, Vector3f offset, Quaternionf quaternionf, LivingEntity entity) {
        analyser.modelPose.translate((float) (x1 + x2) / 2.0f, (float) (y1 + y2) / 2.0f, 0);
        analyser.modelPose.scale(scale, scale, -scale);
        analyser.modelPose.translate(offset.x(), offset.y(), offset.z());
        analyser.modelPose.mulPose(quaternionf);
        analyser.modelPose.translate(0, -entity.getBbHeight() / 2.0f, 0);
        analyser.updateModel(minecraft, entity, 1.0f, analyser.modelPose);
        EntityRenderState entityRenderState = minecraft.getEntityRenderDispatcher().getRenderer(entity).createRenderState(entity, 1.0F);
        graphics.entity(entityRenderState, scale, offset, quaternionf, new Quaternionf(), x1, y1, x2, y2);
    }

    protected void renderTextureViewArea(GuiGraphicsExtractor graphics, LivingEntity entity) {
        if (textureViewArea == null) return;
        int x1 = textureViewArea.left(), y1 = textureViewArea.top(), x2 = textureViewArea.right(), y2 = textureViewArea.bottom();
        Vector3f offset = new Vector3f((float) textureX - 0.5f, (float) textureY - 0.5f, 0);
        renderTextureWithAnalyser(graphics, x1, y1, x2, y2, (float) (textureScale * textureViewArea.width()) / 80, offset, entity);
    }

    protected void renderTextureWithAnalyser(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, float scale, Vector3f offset, LivingEntity entity) {
        analyser.texturePose.translate((float) (x1 + x2) / 2.0f, (float) (y1 + y2) / 2.0f, 0);
        analyser.texturePose.scale(scale, scale, -scale);
        analyser.texturePose.translate(offset.x(), offset.y(), offset.z());
        EntityRenderState entityRenderState = minecraft.getEntityRenderDispatcher().getRenderer(entity).createRenderState(entity, 1.0F);
        graphics.entity(entityRenderState, scale, offset, new Quaternionf(), null, x1, y1, x2, y2);
    }

    protected void importBindTarget(Button button) {
        FriendlyByteBuf byteBuf = null;
        try {
            String base64 = minecraft.keyboardHandler.getClipboard();
            byte[] compressed = Base64.getDecoder().decode(base64);
            InflaterInputStream inflaterStream = new InflaterInputStream(new ByteArrayInputStream(compressed));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inflaterStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            byte[] bytes = outputStream.toByteArray();
            byteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
            BindTarget target = BindTarget.read(byteBuf);
            if (target.isEmpty()) throw new IllegalArgumentException("Invalid config format");
            loadBindTarget(target);
            button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("importSucceeded", LocUtil.literal("'" + target.name() + "'").withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GREEN)));
        } catch (Exception e) {
            String message = e.getClass().getSimpleName();
            if (e instanceof IllegalArgumentException) message += ": " + e.getMessage();
            button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("importFailed", message).withStyle(ChatFormatting.RED)));
        } finally {
            if (byteBuf != null) byteBuf.release();
        }
    }

    protected void exportBindTarget(Button button) {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            genBindTarget().write(byteBuf);
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.getBytes(byteBuf.readerIndex(), bytes);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (DeflaterOutputStream deflaterStream = new DeflaterOutputStream(outputStream, new Deflater(Deflater.BEST_COMPRESSION))) {
                deflaterStream.write(bytes);
            }
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            minecraft.keyboardHandler.setClipboard(base64);
            button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("exportSucceeded").withStyle(ChatFormatting.GREEN)));
        } catch (Exception e) {
            String message = e.getClass().getSimpleName() + ": " + e.getMessage();
            button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("exportFailed", message).withStyle(ChatFormatting.RED)));
        } finally {
            byteBuf.release();
        }
    }

    protected void loadBindTarget(BindTarget target) {
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
        scaleField.setNumber(target.offsets().getScale());
        offsetXSlider.setValue(target.offsets().getX());
        offsetXField.setNumber(target.offsets().getX());
        offsetYSlider.setValue(target.offsets().getY());
        offsetYField.setNumber(target.offsets().getY());
        offsetZSlider.setValue(target.offsets().getZ());
        offsetZField.setNumber(target.offsets().getZ());
        offsetPitchSlider.setValue(target.offsets().getPitch());
        offsetPitchField.setNumber(target.offsets().getPitch());
        offsetYawSlider.setValue(target.offsets().getYaw());
        offsetYawField.setNumber(target.offsets().getYaw());
        offsetRollSlider.setValue(target.offsets().getRoll());
        offsetRollField.setNumber(target.offsets().getRoll());
        disableConfigs.clear();
        disableConfigs.addAll(List.of(target.disableConfigs()));
    }

    protected BindTarget genBindTarget() {
        TargetConfig targetConfig = new TargetConfig(forwardUField.getNumber(), forwardVField.getNumber(), upwardUField.getNumber(), upwardVField.getNumber(), posUField.getNumber(), posVField.getNumber());
        BindConfig bindConfig = new BindConfig(bindXButton.getValue() == 0, bindYButton.getValue() == 0, bindZButton.getValue() == 0, bindRotButton.getValue() == 0);
        OffsetConfig offsets = new OffsetConfig()
                .setScale(scaleField.getNumber())
                .setX(toggleSliderButton.getValue() == 0 ? (float) offsetXSlider.getValue() : offsetXField.getNumber())
                .setY(toggleSliderButton.getValue() == 0 ? (float) offsetYSlider.getValue() : offsetYField.getNumber())
                .setZ(toggleSliderButton.getValue() == 0 ? (float) offsetZSlider.getValue() : offsetZField.getNumber())
                .setPitch(toggleSliderButton.getValue() == 0 ? (float) offsetPitchSlider.getValue() : offsetPitchField.getNumber())
                .setYaw(toggleSliderButton.getValue() == 0 ? (float) offsetYawSlider.getValue() : offsetYawField.getNumber())
                .setRoll(toggleSliderButton.getValue() == 0 ? (float) offsetRollSlider.getValue() : offsetRollField.getNumber());
        DisableConfig currentDisableConfig = new DisableConfig(disabledNameField.getValue(), disabledIdField.getValue(), disableModeButton.getValue() == 0, rectWidgets.stream().map(UVRectangleWidget::toUVRectangle).toArray(UVRectangle[]::new));
        DisableConfig[] disableConfigArray = disableConfigs.toArray(new DisableConfig[0]);
        for (int i = 0; i < disableConfigArray.length; i++) {
            if (disableConfigArray[i].name().equals(currentDisableConfig.name()))
                disableConfigArray[i] = currentDisableConfig;
        }
        return new BindTarget(nameField.getValue(), textureIdField.getValue(), priorityField.getNumber(), depthField.getNumber(), targetConfig, bindConfig, offsets, disableConfigArray);
    }

    protected Tooltip createTooltip(String key, Object... args) {
        return Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP(key, args));
    }

    protected UVRectangleWidget createRectWidget(UVRectangle rect) {
        return new UVRectangleWidget(rect.uMin(), rect.vMin(), rect.uMax(), rect.vMax());
    }

    protected Button createButton(Component message, int width, Button.OnPress onPress) {
        return Button.builder(message, onPress).size(width, widgetHeight).build();
    }

    protected <T> CycleButton.Builder<T> createCyclingButtonBuilder(Map<T, Component> messages, T defaultValue) {
        return new CycleButton.Builder<>(messages::get, () -> defaultValue).withValues(messages.keySet());
    }

    protected DoubleSlider createSlider(String key, int width, double min, double max) {
        return new DoubleSlider(width, widgetHeight, 0, min, max, d -> LocUtil.MODEL_VIEW_WIDGET(key, MathUtil.round(d, 2)));
    }

    protected NumberField<Float> createFloatField(int width, float defaultValue, @Nullable NumberField<Float> copyFrom) {
        return NumberField.ofFloat(font, width - 2, widgetHeight - 2, defaultValue, copyFrom).setMax(1.0f).setMin(0f);
    }

    protected EditBox createTextField(int width, int maxLength, @Nullable EditBox copyFrom) {
        EditBox editBox = new EditBox(font, 0, 0, width - 2, widgetHeight - 2, CommonComponents.EMPTY);
        editBox.setMaxLength(maxLength);
        if (copyFrom != null) editBox.setValue(copyFrom.getValue());
        return editBox;
    }

    protected boolean inModelViewArea(double x, double y) {
        return modelViewArea.containsPoint((int) x, (int) y);
    }

    protected boolean inTextureViewArea(double x, double y) {
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
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
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
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dx, double dy) {
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
                modelScale = Mth.clamp(modelScale + (int) verticalAmount * modelScale / 16, 16, 1024);
            }
            return true;
        } else if (inTextureViewArea(mouseX, mouseY)) {
            textureScale = Mth.clamp(textureScale + (int) verticalAmount * textureScale / 16, 16, 1024);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
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

    public class UVRectangleWidget extends AbstractWidget {
        protected float uMin, vMin, uMax, vMax;

        public UVRectangleWidget(float uMin, float vMin, float uMax, float vMax) {
            super(0, 0, 16, 16, CommonComponents.EMPTY);
            this.uMin = uMin;
            this.vMin = vMin;
            this.uMax = uMax;
            this.vMax = vMax;
        }

        public UVRectangleWidget(float xMin, float yMin, float xMax, float yMax, @NotNull ScreenRectangle screenArea) {
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

        private Vec2 translateUVToXY(float u, float v, ScreenRectangle screenArea) {
            int x1 = screenArea.left(), y1 = screenArea.top(), x2 = screenArea.right(), y2 = screenArea.bottom();
            Vector3f vector3f = new Vector3f(u, v, 0)
                    .add((float) textureX - 0.5f, (float) textureY - 0.5f, 0)
                    .mul((float) (textureScale * screenArea.width()) / 80)
                    .add((float) (x1 + x2) / 2.0f, (float) (y1 + y2) / 2.0f, 0);
            return new Vec2(vector3f.x(), vector3f.y());
        }

        private Vec2 translateXYToUV(float x, float y, ScreenRectangle screenArea) {
            int x1 = screenArea.left(), y1 = screenArea.top(), x2 = screenArea.right(), y2 = screenArea.bottom();
            Vector3f vector3f = new Vector3f(x, y, 0)
                    .add(-(float) (x1 + x2) / 2.0f, -(float) (y1 + y2) / 2.0f, 0)
                    .mul((float) 80 / (textureScale * screenArea.width()))
                    .add((float) -textureX + 0.5f, (float) -textureY + 0.5f, 0);
            return new Vec2(vector3f.x(), vector3f.y());
        }

        @Override
        protected void extractWidgetRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
            Vec2 minXY = translateUVToXY(uMin, vMin, textureViewArea), maxXY = translateUVToXY(uMax, vMax, textureViewArea);
            float x1 = minXY.x, y1 = minXY.y, x2 = maxXY.x, y2 = maxXY.y, width = x2 - x1, height = y2 - y1;
            setX((int) x1);
            setY((int) y1);
            setWidth((int) width);
            setHeight((int) height);
            GUIHelper.enableScissor(graphics, textureViewArea);
            GUIHelper.fill(graphics, x1, y1, x2, y2, 0x4F3333CC);
            if (isHoveredOrFocused()) GUIHelper.fill(graphics, x1, y1, x2, y2, 0x2F3333CC);
            if (isFocused() || this == focusedRectWidget)
                GUIHelper.renderOutline(graphics, x1, y1, width, height, 0xAAFFFFFF);
            graphics.disableScissor();
        }

        @Override
        public boolean keyPressed(@NotNull KeyEvent event) {
            if (textureViewArea == null) return false;
            if (event.input() == InputConstants.KEY_DELETE && deleteFocusedRectWidget()) return true;
            if (event.isSelection() && uMin < uMax && vMin < vMax) {
                textureX = 0.5 * (1 - uMin - uMax);
                textureY = 0.5 * (1 - vMin - vMax);
                textureScale = (int) Mth.clamp(2560d / (textureViewArea.width() * Math.sqrt((uMax - uMin) * (vMax - vMin))), 16, 1024);
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
        protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) { }
    }








}
