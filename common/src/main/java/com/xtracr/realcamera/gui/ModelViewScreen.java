package com.xtracr.realcamera.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.compat.CompatibilityHelper;
import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.config.BindingTarget.*;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ConfigScreen;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.util.LocUtil;
import com.xtracr.realcamera.util.MathUtil;
import com.xtracr.realcamera.util.VertexData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class ModelViewScreen extends Screen {
    public final ModelAnalyser analyser = new ModelAnalyser();
    protected int xSize = 450, ySize = 206, middleWidth = xSize - 200, widgetWidth = (xSize - middleWidth) / 4 - 8, widgetHeight = 18;
    protected int x, y;
    private boolean initialized;
    private int modelScale = 80, textureScale = 80, layers = 0, page = 0, clickedX = -1, clickedY = -1;
    private double modelX, modelY, textureX, textureY;
    private float xRot, yRot;
    private String focusedTextureId;
    private ScreenRectangle modelViewArea;
    @Nullable
    private ScreenRectangle textureViewArea;
    private VertexData[][] focusedPolyhedron = new VertexData[0][];
    private List<DisableConfig> configsInClipBoard = new ArrayList<>();
    @Nullable
    private UVRectangleWidget focusedRectangle;
    private StringWidget rectangleWidgetsSizeWidget;
    private EditBox textureIdField, nameField, disabledNameField, disabledIdField;
    private NumberField<Integer> priorityField, focusedRectangleNumberField;
    private NumberField<Float> forwardUField, forwardVField, upwardUField, upwardVField, posUField, posVField;
    private NumberField<Float> uMinField, vMinField, uMaxField, vMaxField;
    private NumberField<Float> offsetXField, offsetYField, offsetZField, offsetPitchField, offsetYawField, offsetRollField, scaleField, depthField;
    private final List<DisableConfig> disableConfigs = new ArrayList<>();
    private final List<UVRectangleWidget> rectangleWidgets = new ArrayList<>();
    private final Map<String, Set<String>> hiddenNameMap = new HashMap<>();
    private final Map<Integer, String> toggleConfigMap = Map.of(0, "disable", 1, "configs");
    private final Map<Integer, String> togglePreviewMap = Map.of(0, "preview", 1, "settings");
    private final CyclingTexturedButton pauseButton = new CyclingTexturedButton(0, 16, 0, 2);
    private final CyclingTexturedButton bindXButton = new CyclingTexturedButton(16, 16, 1, 2);
    private final CyclingTexturedButton bindYButton = new CyclingTexturedButton(16, 16, 0, 2);
    private final CyclingTexturedButton bindZButton = new CyclingTexturedButton(16, 16, 1, 2);
    private final CyclingTexturedButton bindRotButton = new CyclingTexturedButton(16, 16, 1, 2);
    private final DoubleSlider entityPitchSlider = createSlider("pitch", widgetWidth * 2 + 4, -90.0, 90.0);
    private final DoubleSlider entityYawSlider = createSlider("yaw", widgetWidth * 2 + 4, -60.0, 60.0);
    private final DoubleSlider offsetXSlider = createSlider("offsetX", widgetWidth * 2 - 18, ModConfig.MIN_OFFSET, ModConfig.MAX_OFFSET);
    private final DoubleSlider offsetYSlider = createSlider("offsetY", widgetWidth * 2 - 18, ModConfig.MIN_OFFSET, ModConfig.MAX_OFFSET);
    private final DoubleSlider offsetZSlider = createSlider("offsetZ", widgetWidth * 2 - 18, ModConfig.MIN_OFFSET, ModConfig.MAX_OFFSET);
    private final DoubleSlider offsetPitchSlider = createSlider("pitch", widgetWidth * 2 - 18, -180.0, 180.0);
    private final DoubleSlider offsetYawSlider = createSlider("yaw", widgetWidth * 2 - 18, -180.0, 180.0);
    private final DoubleSlider offsetRollSlider = createSlider("roll", widgetWidth * 2 - 18, -180.0, 180.0);
    private final CycleButton<Integer> selectingButton = createCyclingButton(Map.of(
                    0, LocUtil.MODEL_VIEW_WIDGET("forwardMode").withStyle(ChatFormatting.GREEN),
                    1, LocUtil.MODEL_VIEW_WIDGET("upwardMode").withStyle(ChatFormatting.RED),
                    2, LocUtil.MODEL_VIEW_WIDGET("posMode").withStyle(ChatFormatting.BLUE)),
            widgetWidth * 2 + 4, LocUtil.MODEL_VIEW_WIDGET("selectMode"));
    private final CycleButton<Integer> disableModeButton = createCyclingButton(Map.of(
                    0, LocUtil.MODEL_VIEW_WIDGET("all").withStyle(ChatFormatting.GREEN),
                    1, LocUtil.MODEL_VIEW_WIDGET("part").withStyle(ChatFormatting.BLUE)),
            widgetWidth * 2 + 4, LocUtil.MODEL_VIEW_WIDGET("disableMode"));
    private final CycleButton<Integer> toggleSliderButton = createCyclingButton(Map.of(
                    0, LocUtil.MODEL_VIEW_WIDGET("toggleSliderToField"),
                    1, LocUtil.MODEL_VIEW_WIDGET("toggleFieldToSlider")),
            widgetWidth * 2 + 4, (button, i) -> {
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
    private final CycleButton<Integer> toggleConfigButton = createCyclingButton(Map.of(
                    0, LocUtil.MODEL_VIEW_WIDGET(toggleConfigMap.get(0)),
                    1, LocUtil.MODEL_VIEW_WIDGET(toggleConfigMap.get(1))),
            widgetWidth, (button, i) -> initWidgets(0));
    private final CycleButton<Integer> togglePreviewButton = createCyclingButton(Map.of(
                    0, LocUtil.MODEL_VIEW_WIDGET(togglePreviewMap.get(0)),
                    1, LocUtil.MODEL_VIEW_WIDGET(togglePreviewMap.get(1))),
            widgetWidth, (button, i) -> initWidgets(0));

    public ModelViewScreen() {
        super(LocUtil.MODEL_VIEW_TITLE());
    }

    public static void enableScissor(GuiGraphics graphics, ScreenRectangle rectangle) {
        graphics.enableScissor(rectangle.left(), rectangle.top(), rectangle.right(), rectangle.bottom());
    }

    @Override
    protected void init() {
        super.init();
        x = (width - xSize) / 2;
        y = (height - ySize) / 2;
        initWidgets(page);
        if (!initialized) loadBindingTarget(RealCameraCore.currentTarget());
        initialized = true;
    }

    private void initWidgets(int page) {
        this.page = page;
        if (toggleConfigButton.getValue() == 1 && togglePreviewButton.getValue() == 0) {
            modelViewArea = new ScreenRectangle(x + xSize / 2, y, middleWidth / 2, ySize);
            textureViewArea = new ScreenRectangle(x + (xSize - middleWidth) / 2, y, middleWidth / 2, ySize);
        } else {
            modelViewArea = new ScreenRectangle(x + (xSize - middleWidth) / 2, y, middleWidth, ySize);
            textureViewArea = null;
        }
        clearWidgets();
        initLeftWidgets();
        if (textureViewArea != null) rectangleWidgets.forEach(this::addRenderableWidget);
        addRenderableWidget(pauseButton).setPosition(x + (xSize + middleWidth) / 2 - 38, y + 4);
        addRenderableWidget(new TexturedButton(x + (xSize + middleWidth) / 2 - 20, y + 4, 16, 16, 0, 0, button -> {
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
        rectangleWidgetsSizeWidget = new StringWidget(x + 4 + widgetWidth + 5, y + 4 + (widgetHeight + 2) * 3, widgetWidth - 22, widgetHeight, LocUtil.literal(String.valueOf(rectangleWidgets.size())), font);
        forwardUField = createFloatField(widgetWidth, 0, forwardUField);
        forwardVField = createFloatField(widgetWidth, 0, forwardVField);
        upwardUField = createFloatField(widgetWidth, 0, upwardUField);
        upwardVField = createFloatField(widgetWidth, 0, upwardVField);
        posUField = createFloatField(widgetWidth, 0, posUField);
        posVField = createFloatField(widgetWidth, 0, posVField);
        textureIdField = createTextField(widgetWidth * 2 + 4, 1024, textureIdField);
        disabledIdField = createTextField(widgetWidth * 2 + 4, 1024, disabledIdField);
        focusedRectangleNumberField = NumberField.ofInt(font, widgetWidth - 2, widgetHeight - 2, 0, focusedRectangleNumberField).setMin(0).setMax(rectangleWidgets.size());
        uMinField = createFloatField(widgetWidth * 2 - 24, 0, uMinField).setMin(-1f).setMax(2f);
        uMaxField = createFloatField(widgetWidth * 2 - 24, 0, uMaxField).setMin(-1f).setMax(2f);
        vMinField = createFloatField(widgetWidth * 2 - 24, 0, vMinField).setMin(-1f).setMax(2f);
        vMaxField = createFloatField(widgetWidth * 2 - 24, 0, vMaxField).setMin(-1f).setMax(2f);
        offsetXField = createFloatField(widgetWidth * 2 - 20, 0, offsetXField).setMin((float) ModConfig.MIN_OFFSET).setMax((float) ModConfig.MAX_OFFSET);
        offsetYField = createFloatField(widgetWidth * 2 - 20, 0, offsetYField).setMin((float) ModConfig.MIN_OFFSET).setMax((float) ModConfig.MAX_OFFSET);
        offsetZField = createFloatField(widgetWidth * 2 - 20, 0, offsetZField).setMin((float) ModConfig.MIN_OFFSET).setMax((float) ModConfig.MAX_OFFSET);
        offsetPitchField = createFloatField(widgetWidth * 2 - 20, 0, offsetPitchField).setMin(-180.0f).setMax(180.0f);
        offsetYawField = createFloatField(widgetWidth * 2 - 20, 0, offsetYawField).setMin(-180.0f).setMax(180.0f);
        offsetRollField = createFloatField(widgetWidth * 2 - 20, 0, offsetRollField).setMin(-180.0f).setMax(180.0f);
        scaleField = createFloatField(widgetWidth, 1.0f, scaleField).setMax(64.0f);
        depthField = createFloatField(widgetWidth, 0.2f, depthField).setMax(16.0f);
        GridLayout grid = new GridLayout();
        grid.defaultCellSetting().padding(4, 2, 0, 0);
        LayoutSettings smallSettings = grid.newCellSettings().padding(5, 3, 1, 1);
        GridLayout.RowHelper rows = grid.createRowHelper(2);
        if (togglePreviewButton.getValue() == 0) {
            rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("toConfigScreen"), widgetWidth * 2 + 4, button -> {
                if (CompatibilityHelper.isModLoaded("cloth-config")) minecraft.setScreen(ConfigScreen.create(this));
            }), 2).setTooltip(createTooltip("toConfigScreen"));
            if (toggleConfigButton.getValue() == 0) {
                rows.addChild(entityPitchSlider, 2);
                rows.addChild(entityYawSlider, 2);
                rows.addChild(selectingButton, 2).setTooltip(createTooltip("selectMode"));
                rows.addChild(forwardUField, smallSettings);
                rows.addChild(forwardVField, smallSettings);
                rows.addChild(upwardUField, smallSettings);
                rows.addChild(upwardVField, smallSettings);
                rows.addChild(posUField, smallSettings);
                rows.addChild(posVField, smallSettings);
                rows.addChild(textureIdField, 2, smallSettings).setTooltip(createTooltip("textureId"));
            } else {
                LayoutSettings offsetXSettings = grid.newCellSettings().padding(-13, 3, 1, 1);
                rows.addChild(disableModeButton, 2).setTooltip(createTooltip("disableMode"));
                rows.addChild(disabledIdField, 2, smallSettings).setTooltip(createTooltip("textureId"));
                rows.addChild(focusedRectangleNumberField, smallSettings).setOnValueChange(index -> {
                    if (index == 0) focusedRectangle = null;
                    else if (index > 0 && index <= rectangleWidgets.size()) {
                        focusedRectangle = rectangleWidgets.get(index - 1);
                        uMinField.setNumber(focusedRectangle.uMin);
                        vMinField.setNumber(focusedRectangle.vMin);
                        uMaxField.setNumber(focusedRectangle.uMax);
                        vMaxField.setNumber(focusedRectangle.vMax);
                    }
                }).setTooltip(createTooltip("focusedRectangleNumber"));
                addRenderableWidget(new StringWidget(x + 4 + widgetWidth + 3, y + 4 + (widgetHeight + 2) * 3, 6, widgetHeight, LocUtil.literal("/"), font));
                addRenderableWidget(rectangleWidgetsSizeWidget);
                rows.addChild(new TexturedButton(48, 0, button -> deleteFocusedRectangle()), grid.newCellSettings().padding(5 + widgetWidth - 18, 3, 1, 1))
                        .setTooltip(createTooltip("deleteSelectedRectangle"));
                rows.addChild(new StringWidget(26, widgetHeight, LocUtil.literal("uMin:"), font));
                rows.addChild(uMinField, offsetXSettings).setOnValueChange(f -> {
                    if (focusedRectangle != null) focusedRectangle.uMin = f;
                });
                rows.addChild(new StringWidget(26, widgetHeight, LocUtil.literal("vMin:"), font));
                rows.addChild(vMinField, offsetXSettings).setOnValueChange(f -> {
                    if (focusedRectangle != null) focusedRectangle.vMin = f;
                });
                rows.addChild(new StringWidget(26, widgetHeight, LocUtil.literal("uMax:"), font));
                rows.addChild(uMaxField, offsetXSettings).setOnValueChange(f -> {
                    if (focusedRectangle != null) focusedRectangle.uMax = f;
                });
                rows.addChild(new StringWidget(26, widgetHeight, LocUtil.literal("vMax:"), font));
                rows.addChild(vMaxField, offsetXSettings).setOnValueChange(f -> {
                    if (focusedRectangle != null) focusedRectangle.vMax = f;
                });
            }
        } else {
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
            rows.addChild(new TexturedButton(0, 0, button -> {
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
        rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("save"), widgetWidth, button -> {
            if (nameField.getValue().isBlank()) {
                button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("emptyName").withStyle(ChatFormatting.RED)));
                return;
            }
            button.setTooltip(null);
            ConfigFile.config().binding.putTarget(genBindingTarget());
            ConfigFile.save();
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
        rows.addChild(toggleConfigButton, 2).setTooltip(createTooltip(toggleConfigMap.get(toggleConfigButton.getValue())));
        rows.addChild(togglePreviewButton, 2);
        final int widgetsPerPage, size;
        if (toggleConfigButton.getValue() == 0) {
            widgetsPerPage = 8;
            List<BindingTarget> fixedTargetList = ConfigFile.config().getFixedTargetList().stream().filter(target -> target.name().equals(RealCameraCore.currentTarget().name())).toList();
            List<BindingTarget> targetList = ConfigFile.config().getTargetList();
            final int fixedTargetCount = fixedTargetList.size();
            size = fixedTargetCount + targetList.size();
            for (int i = page * widgetsPerPage; i < Math.min((page + 1) * widgetsPerPage, size); i++) {
                BindingTarget target = i < fixedTargetCount ? fixedTargetList.get(i) : targetList.get(i - fixedTargetCount);
                String name = target.name();
                rows.addChild(createButton(LocUtil.literal(name), widgetWidth * 2 - 18, button -> loadBindingTarget(target)), 3).setTooltip(Tooltip.create(LocUtil.literal(name)));
                if (i < fixedTargetCount) continue;
                rows.addChild(new TexturedButton(48, 0, button -> {
                    targetList.remove(target);
                    ConfigFile.save();
                    initWidgets(page * widgetsPerPage > size - 2 && size > 1 ? page - 1 : page);
                }), smallSettings);
            }
        } else {
            widgetsPerPage = 6;
            size = disableConfigs.size();
            rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("copy"), widgetWidth, button -> configsInClipBoard = List.copyOf(disableConfigs)), 2).setTooltip(createTooltip("copy"));
            rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("paste"), widgetWidth, button -> {
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
                    DisableConfig disableConfig = new DisableConfig(name, textureId, disableModeButton.getValue() == 0, rectangleWidgets.stream().map(UVRectangleWidget::toRectangle).toArray(UVRectangle[]::new));
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
                Set<String> hiddenNames = hiddenNameMap.computeIfAbsent(targetName, k -> new HashSet<>());
                addRenderableWidget(new CyclingTexturedButton(32, 16, hiddenNames.contains(config.name()) ? 1 : 0, 2))
                        .setOnValueChange(value -> {
                            if (value == 0) hiddenNames.remove(config.name());
                            else hiddenNames.add(config.name());
                        })
                        .setPosition(x + (xSize + middleWidth) / 2 - 20, y + 5 + (widgetHeight + 2) * (3 + i % widgetsPerPage));
                rows.addChild(createButton(LocUtil.literal(config.name()), widgetWidth * 2 - 18, button -> {
                    disabledNameField.setValue(config.name());
                    disabledIdField.setValue(config.textureId());
                    disableModeButton.setValue(config.disableAll() ? 0 : 1);
                    rectangleWidgets.clear();
                    for (UVRectangle rectangle : config.rectangles()) rectangleWidgets.add(createRectangleWidget(rectangle));
                    initWidgets(page);
                }), 3).setTooltip(Tooltip.create(LocUtil.literal(config.name())));
                rows.addChild(new TexturedButton(48, 0, button -> {
                    disableConfigs.removeIf(disableConfig -> disableConfig.name().equals(config.name()));
                    initWidgets(page * widgetsPerPage > size - 2 && size > 1 ? page - 1 : page);
                }), smallSettings);
            }
        }
        grid.arrangeElements();
        FrameLayout.alignInRectangle(grid, x + (xSize + middleWidth) / 2 + 4, y + 2, x + xSize, y + ySize, 0, 0);
        grid.visitWidgets(this::addRenderableWidget);
        final int pages = (size - 1) / widgetsPerPage + 1;
        addRenderableWidget(new TexturedButton(x + (xSize + middleWidth) / 2 + 8, y + ySize - 20, 16, 16, 16, 0, button -> initWidgets((page - 1 + pages) % pages)));
        addRenderableWidget(new StringWidget(x + (xSize + middleWidth) / 2 + 30, y + ySize - 20, widgetWidth * 2 - 40, widgetHeight, LocUtil.literal((page + 1) + " / " + pages), font));
        addRenderableWidget(new TexturedButton(x + xSize - 21, y + ySize - 20, 16, 16, 32, 0, button -> initWidgets((page + 1) % pages)));
    }

    public void setFocusedRectangle(@NotNull UVRectangleWidget rectangle) {
        UVRectangleWidget foundRectangle = rectangleWidgets.stream().filter(r -> r.contains(rectangle)).findFirst().orElse(null);
        focusedRectangle = foundRectangle == null ? rectangle : foundRectangle;
        setFocused(focusedRectangle);
        if (foundRectangle == null) {
            rectangleWidgets.add(focusedRectangle);
            focusedRectangleNumberField.setMax(rectangleWidgets.size());
            rectangleWidgetsSizeWidget.setMessage(LocUtil.literal(String.valueOf(rectangleWidgets.size())));
            addRenderableWidget(focusedRectangle);
        }
        focusedRectangleNumberField.setNumber(rectangleWidgets.indexOf(focusedRectangle) + 1);
        uMinField.setNumber(focusedRectangle.uMin);
        vMinField.setNumber(focusedRectangle.vMin);
        uMaxField.setNumber(focusedRectangle.uMax);
        vMaxField.setNumber(focusedRectangle.vMax);
    }

    public void deleteFocusedRectangle() {
        if (focusedRectangle == null) return;
        rectangleWidgets.remove(focusedRectangle);
        removeWidget(focusedRectangle);
        focusedRectangleNumberField.setNumber(0);
        focusedRectangleNumberField.setMax(rectangleWidgets.size());
        rectangleWidgetsSizeWidget.setMessage(LocUtil.literal(String.valueOf(rectangleWidgets.size())));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float deltaTick) {
        super.render(graphics, mouseX, mouseY, deltaTick);
        if (textureViewArea != null) {
            enableScissor(graphics, textureViewArea);
            if (clickedX >= 0 && clickedY >= 0)
                graphics.fill(Math.min(clickedX, mouseX), Math.min(clickedY, mouseY), Math.max(clickedX, mouseX), Math.max(clickedY, mouseY), 0x4F3333CC);
            if (disableModeButton.getValue() == 0)
                new UVRectangleWidget(0f, 0f, 1f, 1f).renderWidget(graphics, mouseX, mouseY, deltaTick);
            graphics.disableScissor();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float deltaTick) {
        super.renderBackground(graphics, mouseX, mouseY, deltaTick);
        graphics.fill(x, y, x + (xSize - middleWidth) / 2 - 4, y + ySize, 0xFF444444);
        graphics.fill(x + (xSize - middleWidth) / 2, y, x + (xSize + middleWidth) / 2, y + ySize, 0xFF222222);
        graphics.fill(x + (xSize + middleWidth) / 2 + 4, y, x + xSize, y + ySize, 0xFF444444);
        analyser.setup(genBindingTarget(), modelScale);
        renderModelViewArea(graphics, minecraft.player);
        renderTextureViewArea(graphics);
        applyAnalyser(graphics, mouseX, mouseY);
    }

    protected void applyAnalyser(GuiGraphics graphics, int mouseX, int mouseY) {
        String textureId = textureViewArea == null ? "" : disabledIdField.getValue();
        Set<String> hiddenNames = hiddenNameMap.getOrDefault(nameField.getValue(), Set.of());
        analyser.genContext();
        analyser.applyDisableConfigs(textureId, hiddenNames);
        analyser.computeFocusedOnTexture(inTextureViewArea(mouseX, mouseY) ? mouseX : -1, mouseY);
        analyser.computeFocusedOnModel(inModelViewArea(mouseX, mouseY) ? mouseX : -1, mouseY, layers);
        analyser.computeFocusedPolyhedron();
        focusedPolyhedron = analyser.focusedPolyhedron.toArray(VertexData[][]::new);
        focusedTextureId = analyser.getFocusedTextureId();
        enableScissor(graphics, modelViewArea);
        analyser.drawModel(graphics, analyser.modelPose);
        if (togglePreviewButton.getValue() == 0) analyser.drawFocusedInModelArea(graphics);
        if (toggleConfigButton.getValue() == 0 && togglePreviewButton.getValue() == 0) analyser.drawBindingTarget(graphics);
        else analyser.drawCameraDirections(graphics);
        graphics.disableScissor();
        if (textureViewArea != null) {
            enableScissor(graphics, textureViewArea);
            analyser.drawTexture(graphics, analyser.texturePose);
            analyser.drawFocusedInTextureArea(graphics);
            graphics.disableScissor();
        }
    }

    protected void renderModelViewArea(GuiGraphics graphics, LivingEntity entity) {
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

    protected void renderEntityWithAnalyser(GuiGraphics graphics, int x1, int y1, int x2, int y2, float scale, Vector3f offset, Quaternionf quaternionf, LivingEntity entity) {
        analyser.modelPose.translate((float) (x1 + x2) / 2.0f, (float) (y1 + y2) / 2.0f, 0);
        analyser.modelPose.scale(scale, scale, -scale);
        analyser.modelPose.translate(offset.x(), offset.y(), offset.z());
        analyser.modelPose.mulPose(quaternionf);
        analyser.modelPose.translate(0, -entity.getBbHeight() / 2.0f, 0);
        analyser.updateModel(minecraft, entity, 1.0f, analyser.modelPose);
    }

    protected void renderTextureViewArea(GuiGraphics graphics) {
        if (textureViewArea == null) return;
        int x1 = textureViewArea.left(), y1 = textureViewArea.top(), x2 = textureViewArea.right(), y2 = textureViewArea.bottom();
        Vector3f offset = new Vector3f((float) textureX - 0.5f, (float) textureY - 0.5f, 0);
        renderTextureWithAnalyser(graphics, x1, y1, x2, y2, (float) (textureScale * textureViewArea.width()) / 80, offset);
    }

    protected void renderTextureWithAnalyser(GuiGraphics graphics, int x1, int y1, int x2, int y2, float scale, Vector3f offset) {
        analyser.texturePose.translate((float) (x1 + x2) / 2.0f, (float) (y1 + y2) / 2.0f, 0);
        analyser.texturePose.scale(scale, scale, -scale);
        analyser.texturePose.translate(offset.x(), offset.y(), offset.z());
    }

    protected BindingTarget genBindingTarget() {
        TargetConfig targetConfig = new TargetConfig( forwardUField.getNumber(), forwardVField.getNumber(), upwardUField.getNumber(), upwardVField.getNumber(), posUField.getNumber(), posVField.getNumber());
        BindConfig bindConfig = new BindConfig( bindXButton.getValue() == 0, bindYButton.getValue() == 0, bindZButton.getValue() == 0, bindRotButton.getValue() == 0);
        OffsetConfig offsets = new OffsetConfig()
                .setScale(scaleField.getNumber())
                .setX(toggleSliderButton.getValue() == 0 ? offsetXSlider.getValue() : offsetXField.getNumber())
                .setY(toggleSliderButton.getValue() == 0 ? offsetYSlider.getValue() : offsetYField.getNumber())
                .setZ(toggleSliderButton.getValue() == 0 ? offsetZSlider.getValue() : offsetZField.getNumber())
                .setPitch(toggleSliderButton.getValue() == 0 ? (float) offsetPitchSlider.getValue() : offsetPitchField.getNumber())
                .setYaw(toggleSliderButton.getValue() == 0 ? (float) offsetYawSlider.getValue() : offsetYawField.getNumber())
                .setRoll(toggleSliderButton.getValue() == 0 ? (float) offsetRollSlider.getValue() : offsetRollField.getNumber());
        DisableConfig currentDisableConfig = new DisableConfig(disabledNameField.getValue(), disabledIdField.getValue(), disableModeButton.getValue() == 0, rectangleWidgets.stream().map(UVRectangleWidget::toRectangle).toArray(UVRectangle[]::new));
        DisableConfig[] disableConfigArray = disableConfigs.toArray(DisableConfig[]::new);
        for (int i = 0; i < disableConfigArray.length; i++) {
            if (disableConfigArray[i].name().equals(currentDisableConfig.name())) disableConfigArray[i] = currentDisableConfig;
        }
        return new BindingTarget(nameField.getValue(), textureIdField.getValue(), priorityField.getNumber(), depthField.getNumber(), targetConfig, bindConfig, offsets, disableConfigArray);
    }

    protected void loadBindingTarget(BindingTarget target) {
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
        scaleField.setNumber((float) target.offsets().getScale());
        offsetXSlider.setValue(target.offsets().getX());
        offsetXField.setNumber((float) target.offsets().getX());
        offsetYSlider.setValue(target.offsets().getY());
        offsetYField.setNumber((float) target.offsets().getY());
        offsetZSlider.setValue(target.offsets().getZ());
        offsetZField.setNumber((float) target.offsets().getZ());
        offsetPitchSlider.setValue(target.offsets().getPitch());
        offsetPitchField.setNumber(target.offsets().getPitch());
        offsetYawSlider.setValue(target.offsets().getYaw());
        offsetYawField.setNumber(target.offsets().getYaw());
        offsetRollSlider.setValue(target.offsets().getRoll());
        offsetRollField.setNumber(target.offsets().getRoll());
        disableConfigs.clear();
        disableConfigs.addAll(List.of(target.disableConfigs()));
    }

    protected Tooltip createTooltip(String key, Object... args) {
        return Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP(key, args));
    }

    protected UVRectangleWidget createRectangleWidget(UVRectangle rectangle) {
        return new UVRectangleWidget(rectangle.uMin(), rectangle.vMin(), rectangle.uMax(), rectangle.vMax());
    }

    protected Button createButton(Component message, int width, Button.OnPress onPress) {
        return Button.builder(message, onPress).size(width, widgetHeight).build();
    }

    protected CycleButton<Integer> createCyclingButton(Map<Integer, Component> messages, int width, Component optionText) {
        return new CycleButton.Builder<Integer>(messages::get).withValues(messages.keySet()).withInitialValue(0).create(0, 0, width, widgetHeight, optionText);
    }

    protected CycleButton<Integer> createCyclingButton(Map<Integer, Component> messages, int width, CycleButton.OnValueChange<Integer> onValueChange) {
        return new CycleButton.Builder<Integer>(messages::get).withValues(messages.keySet()).withInitialValue(0).displayOnlyValue().create(0, 0, width, widgetHeight, Component.empty(), onValueChange);
    }

    protected DoubleSlider createSlider(String key, int width, double min, double max) {
        return new DoubleSlider(width, widgetHeight, 0, min, max, d -> LocUtil.MODEL_VIEW_WIDGET(key, MathUtil.round(d, 2)));
    }

    protected NumberField<Float> createFloatField(int width, float defaultValue, @Nullable NumberField<Float> copyFrom) {
        return NumberField.ofFloat(font, width - 2, widgetHeight - 2, defaultValue, copyFrom).setMax(1.0f).setMin(0f);
    }

    protected EditBox createTextField(int width, int maxLength, @Nullable EditBox copyFrom) {
        EditBox editBox = new EditBox(font, 0, 0, width - 2, widgetHeight - 2, Component.empty());
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int storedX = clickedX, storedY = clickedY;
        clickedX = clickedY = -1;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && togglePreviewButton.getValue() == 0) {
            if (InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_ALT)) {
                if (focusedPolyhedron.length == 0) return super.mouseClicked(mouseX, mouseY, button);
                if (inModelViewArea(mouseX, mouseY) && toggleConfigButton.getValue() == 0) {
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
                } else if ((inModelViewArea(mouseX, mouseY) || inTextureViewArea(mouseX, mouseY)) && toggleConfigButton.getValue() == 1) {
                    float uMin = 1f, vMin = 1f, uMax = 0, vMax = 0;
                    for (VertexData[] primitive : focusedPolyhedron) {
                        for (VertexData vertex : primitive) {
                            if (vertex.u() < uMin) uMin = vertex.u();
                            if (vertex.v() < vMin) vMin = vertex.v();
                            if (vertex.u() > uMax) uMax = vertex.u();
                            if (vertex.v() > vMax) vMax = vertex.v();
                        }
                    }
                    if (!disabledIdField.getValue().isBlank()) setFocusedRectangle(new UVRectangleWidget(uMin, vMin, uMax, vMax));
                    disabledIdField.setValue(focusedTextureId);
                    return true;
                }
            } else if (inTextureViewArea(mouseX, mouseY)) {
                if (storedX >= 0 && storedY >= 0) {
                    setFocusedRectangle(new UVRectangleWidget(Math.min(storedX, (int) mouseX), Math.min(storedY, (int) mouseY), Math.max(storedX, (int) mouseX), Math.max(storedY, (int) mouseY)));
                    return true;
                }
                for (UVRectangleWidget rectangleWidget : rectangleWidgets) {
                    if (!rectangleWidget.isHovered()) continue;
                    setFocusedRectangle(rectangleWidget);
                    return true;
                }
                clickedX = (int) mouseX;
                clickedY = (int) mouseY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_ALT)) {
            if (inModelViewArea(mouseX, mouseY)) {
                xRot += (float) (Math.PI * deltaY / ySize);
                yRot -= (float) (Math.PI * deltaX / middleWidth);
                return true;
            }
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (inModelViewArea(mouseX, mouseY)) {
                modelX += deltaX / modelScale;
                modelY += deltaY / modelScale;
                return true;
            }
            if (inTextureViewArea(mouseX, mouseY)) {
                textureX += deltaX / textureScale;
                textureY += deltaY / textureScale;
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (inModelViewArea(mouseX, mouseY)) {
            if (InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_ALT)) {
                layers = Math.max(0, layers + (int) verticalAmount);
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
    public boolean isPauseScreen() {
        return pauseButton.getValue() == 1;
    }

    public class UVRectangleWidget extends AbstractWidget {
        protected float uMin, vMin, uMax, vMax;

        public UVRectangleWidget(float uMin, float vMin, float uMax, float vMax) {
            super(0, 0, 16, 16, Component.empty());
            this.uMin = uMin;
            this.vMin = vMin;
            this.uMax = uMax;
            this.vMax = vMax;
        }

        public UVRectangleWidget(int x1, int y1, int x2, int y2) {
            super(x1, y1, x2 - x1, y2 - y1, Component.empty());
            Vec2 minUV = translateXYToUV(x1, y1), maxUV = translateXYToUV(x2, y2);
            this.uMin = minUV.x;
            this.vMin = minUV.y;
            this.uMax = maxUV.x;
            this.vMax = maxUV.y;
        }

        public UVRectangle toRectangle() {
            return new UVRectangle(uMin, vMin, uMax, vMax);
        }

        public boolean contains(UVRectangleWidget another) {
            return uMin <= another.uMin && vMin <= another.vMin && uMax >= another.uMax && vMax >= another.vMax;
        }

        private void update() {
            Vec2 minXY = translateUVToXY(uMin, vMin), sub = minXY.scale(-1).add(translateUVToXY(uMax, vMax));
            this.setX((int) minXY.x);
            this.setY((int) minXY.y);
            this.setWidth((int) sub.x);
            this.setHeight((int) sub.y);
        }

        private Vec2 translateUVToXY(float u, float v) {
            int x1 = textureViewArea.left(), y1 = textureViewArea.top(), x2 = textureViewArea.right(), y2 = textureViewArea.bottom();
            Vector3f vector3f = new Vector3f(u, v, 0)
                    .add((float) textureX - 0.5f, (float) textureY - 0.5f, 0)
                    .mul((float) (textureScale * textureViewArea.width()) / 80)
                    .add((float) (x1 + x2) / 2.0f, (float) (y1 + y2) / 2.0f, 0);
            return new Vec2(vector3f.x(), vector3f.y());
        }

        private Vec2 translateXYToUV(float x, float y) {
            int x1 = textureViewArea.left(), y1 = textureViewArea.top(), x2 = textureViewArea.right(), y2 = textureViewArea.bottom();
            Vector3f vector3f = new Vector3f(x, y, 0)
                    .add(- (float) (x1 + x2) / 2.0f, - (float) (y1 + y2) / 2.0f, 0)
                    .mul((float) 80 / (textureScale * textureViewArea.width()))
                    .add((float) -textureX + 0.5f, (float) -textureY + 0.5f, 0);
            return new Vec2(vector3f.x(), vector3f.y());
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_DELETE) deleteFocusedRectangle();
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float deltaTick) {
            update();
            enableScissor(graphics, textureViewArea);
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x4F3333CC);
            if (isHoveredOrFocused()) graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x2F3333CC);
            if (isFocused() || this == focusedRectangle) graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xAAFFFFFF);
            graphics.disableScissor();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
    }
}
