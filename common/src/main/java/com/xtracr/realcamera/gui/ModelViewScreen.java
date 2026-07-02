package com.xtracr.realcamera.gui;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.DataResult;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.compat.CompatibilityHelper;
import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.BindTarget.*;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ConfigScreen;
import com.xtracr.realcamera.config.DisableConfig;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.config.OffsetConfig;
import com.xtracr.realcamera.config.UVRectangle;
import com.xtracr.realcamera.config.codec.ConfigCodec;
import com.xtracr.realcamera.gui.components.CycleIconButton;
import com.xtracr.realcamera.gui.components.DoubleSlider;
import com.xtracr.realcamera.gui.components.NumberField;
import com.xtracr.realcamera.gui.components.NumberWidgetPair;
import com.xtracr.realcamera.gui.components.SimpleIconButton;
import com.xtracr.realcamera.util.LocUtil;
import com.xtracr.realcamera.util.MathUtil;
import com.xtracr.realcamera.renderer.state.VertexData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public class ModelViewScreen extends Screen {
    private static final int CONFIG_NAME_MAX_LENGTH = 20;
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
    @Nullable
    private UVRectangleWidget focusedRectWidget;
    private StringWidget rectWidgetsSizeWidget;
    private EditBox textureIdField, nameField, disabledNameField, disabledIdField;
    private NumberField<Integer> priorityField, focusedRectWidgetNumberField;
    private NumberField<Float> forwardUField, forwardVField, upwardUField, upwardVField, posUField, posVField;
    private NumberField<Float> uMinField, vMinField, uMaxField, vMaxField;
    private NumberField<Float> scaleField, depthField;
    private NumberWidgetPair offsetXPair, offsetYPair, offsetZPair, offsetPitchPair, offsetYawPair, offsetRollPair;
    private List<NumberWidgetPair> widgetPairs = List.of();
    private final List<DisableConfig> disableConfigs = new ArrayList<>();
    private final List<UVRectangleWidget> rectWidgets = new ArrayList<>();
    private final Map<String, Set<String>> hiddenNameMap = new HashMap<>();
    private final CycleIconButton showTextureButton = new CycleIconButton(48, 16, 0, 2).setOnValueChange(i -> initWidgets(page));
    private final CycleIconButton pauseButton = new CycleIconButton(0, 16, 0, 2);
    private final CycleIconButton bindXButton = new CycleIconButton(16, 16, 1, 2);
    private final CycleIconButton bindYButton = new CycleIconButton(16, 16, 0, 2);
    private final CycleIconButton bindZButton = new CycleIconButton(16, 16, 1, 2);
    private final CycleIconButton bindRotButton = new CycleIconButton(16, 16, 1, 2);
    private final DoubleSlider entityPitchSlider = createSlider("pitch", widgetWidth * 2 + 4, -90.0, 90.0);
    private final DoubleSlider entityYawSlider = createSlider("yaw", widgetWidth * 2 + 4, -60.0, 60.0);
    private final CycleButton<Integer> selectingButton = createCyclingButtonBuilder(ImmutableSortedMap.of(
                    0, LocUtil.MODEL_VIEW_WIDGET("forwardVector").withStyle(ChatFormatting.GREEN),
                    1, LocUtil.MODEL_VIEW_WIDGET("upwardVector").withStyle(ChatFormatting.RED),
                    2, LocUtil.MODEL_VIEW_WIDGET("position").withStyle(ChatFormatting.BLUE)))
            .withInitialValue(0)
            .withTooltip(i -> createTooltip("selecting", modifierKey.getDisplayName(), modifierKey.getDisplayName()))
            .create(0, 0, widgetWidth * 2 + 4, widgetHeight, LocUtil.MODEL_VIEW_WIDGET("selecting"));
    private final CycleButton<Integer> disableModeButton = createCyclingButtonBuilder(ImmutableMap.of(
                    0, LocUtil.MODEL_VIEW_WIDGET("all").withStyle(ChatFormatting.GREEN),
                    1, LocUtil.MODEL_VIEW_WIDGET("part").withStyle(ChatFormatting.BLUE)))
            .withInitialValue(0)
            .withTooltip(i -> createTooltip("disableMode", modifierKey.getDisplayName()))
            .create(0, 0, widgetWidth * 2 + 4, widgetHeight, LocUtil.MODEL_VIEW_WIDGET("disableMode"));
    private final CycleButton<Integer> selectionModeButton = createCyclingButtonBuilder(ImmutableSortedMap.of(
                    0, LocUtil.MODEL_VIEW_WIDGET("single"),
                    1, LocUtil.MODEL_VIEW_WIDGET("multiple"),
                    2, LocUtil.MODEL_VIEW_WIDGET("range").withStyle(ChatFormatting.BLUE)))
            .withInitialValue(0)
            .withTooltip(i -> createTooltip("selectionMode", modifierKey.getDisplayName(), modifierKey.getDisplayName()))
            .create(0, 0, widgetWidth * 2 + 4, widgetHeight, LocUtil.MODEL_VIEW_WIDGET("selectionMode"));
    private final CycleButton<Integer> toggleSliderButton = createCyclingButtonBuilder(ImmutableMap.of(
                    0, LocUtil.MODEL_VIEW_WIDGET("toggleSliderToField"),
                    1, LocUtil.MODEL_VIEW_WIDGET("toggleFieldToSlider")))
            .withInitialValue(0)
            .withTooltip(i -> createTooltip("toggleSlider"))
            .displayOnlyValue()
            .create(0, 0, widgetWidth * 2 + 4, widgetHeight, LocUtil.MODEL_VIEW_WIDGET("toggleSlider"), (button, i) -> {
                boolean useSlider = i == 0;
                widgetPairs.forEach(pair -> pair.syncAndSwitch(useSlider));
                initWidgets(page);
            });
    private final CycleButton<Category> toggleCategoryButton = createCyclingButtonBuilder(ImmutableSortedMap.of(
                    Category.CONFIGS, LocUtil.MODEL_VIEW_WIDGET(Category.CONFIGS.next().id),
                    Category.PREVIEW, LocUtil.MODEL_VIEW_WIDGET(Category.PREVIEW.next().id),
                    Category.DISABLE, LocUtil.MODEL_VIEW_WIDGET(Category.DISABLE.next().id)))
            .withInitialValue(Category.CONFIGS)
            .withTooltip(category -> createTooltip(category.next().id))
            .displayOnlyValue()
            .create(0, 0, widgetWidth * 2 - 18, widgetHeight, LocUtil.MODEL_VIEW_WIDGET("toggleCategory"), (button, i) -> initWidgets(0));

    public ModelViewScreen() {
        super(LocUtil.MODEL_VIEW_TITLE());
    }

    @Override
    protected void init() {
        super.init();
        xSize = Mth.clamp(width - 10, 10, 450);
        ySize = Mth.clamp(height - 10, 10, 206);
        middleWidth = Math.max(10, xSize - 200);
        x = (width - xSize) / 2;
        y = (height - ySize) / 2;
        modifierKey = ConfigFile.config().getScreenModifierKey();
        initWidgets(page);
        if (!initialized) loadBindTarget(RealCameraCore.currentTarget());
        initialized = true;
    }

    private void initOffsetPairs() {
        if (offsetXPair != null) return;
        offsetXPair = new NumberWidgetPair(font, "offsetX", widgetWidth * 2 - 18, widgetHeight, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
        offsetYPair = new NumberWidgetPair(font, "offsetY", widgetWidth * 2 - 18, widgetHeight, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
        offsetZPair = new NumberWidgetPair(font, "offsetZ", widgetWidth * 2 - 18, widgetHeight, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
        offsetPitchPair = new NumberWidgetPair(font, "pitch", widgetWidth * 2 - 18, widgetHeight, -180.0f, 180.0f);
        offsetYawPair = new NumberWidgetPair(font, "yaw", widgetWidth * 2 - 18, widgetHeight, -180.0f, 180.0f);
        offsetRollPair = new NumberWidgetPair(font, "roll", widgetWidth * 2 - 18, widgetHeight, -180.0f, 180.0f);
        widgetPairs = List.of(offsetXPair, offsetYPair, offsetZPair, offsetPitchPair, offsetYawPair, offsetRollPair);
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
        addRenderableWidget(new SimpleIconButton(x + (xSize + middleWidth) / 2 - 20, y + 4, 16, 16, 0, 0, button -> {
            modelScale = textureScale = 80;
            entityYawSlider.setNumber(0);
            entityPitchSlider.setNumber(0);
            modelX = modelY = textureX = textureY = 0;
            xRot = yRot = 0;
            layers = 0;
        }));
        initRightWidgets();
    }

    private void initLeftWidgets() {
        initOffsetPairs();
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
                rows.addChild(toggleSliderButton, 2);
                LayoutSettings numericControlSettings = grid.newCellSettings().padding(-20, 2, 0, 0);
                rows.addChild(bindXButton, smallSettings).setTooltip(createTooltip("bindButtons"));
                rows.addChild(offsetXPair, numericControlSettings);
                rows.addChild(bindYButton, smallSettings).setTooltip(createTooltip("bindButtons"));
                rows.addChild(offsetYPair, numericControlSettings);
                rows.addChild(bindZButton, smallSettings).setTooltip(createTooltip("bindButtons"));
                rows.addChild(offsetZPair, numericControlSettings);
                rows.addChild(bindRotButton, smallSettings).setTooltip(createTooltip("bindButtons"));
                rows.addChild(offsetPitchPair, numericControlSettings);
                rows.addChild(offsetYawPair, 2, grid.newCellSettings().padding(26, 2, 0, 0));
                rows.addChild(new SimpleIconButton(0, 0, button -> widgetPairs.forEach(pair -> pair.setNumber(0))), smallSettings);
                rows.addChild(offsetRollPair, numericControlSettings);
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
                rows.addChild(new SimpleIconButton(48, 0, button -> deleteFocusedRectWidget()), grid.newCellSettings().padding(5 + widgetWidth - 18, 3, 1, 1))
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
            if (textureIdField.getValue().isBlank()) {
                button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("emptyTextureId").withStyle(ChatFormatting.RED)));
                return;
            }
            if (toggleCategoryButton.getValue() == Category.DISABLE && !syncDisableDraft(button, true)) return;
            button.setTooltip(null);
            BindTarget bindTarget = genBindTarget();
            ConfigFile.config().putBindTarget(bindTarget);
            ConfigFile.save();
            initWidgets(page);
        }));
        rows.addChild(priorityField = NumberField.ofInt(font, widgetWidth - 2, widgetHeight - 2, 0, priorityField), smallSettings).setTooltip(createTooltip("priority"));
        nameField = createTextField(widgetWidth * 2 + 4, 20, nameField);
        boolean editableName = toggleCategoryButton.getValue() == Category.CONFIGS;
        nameField.setEditable(editableName);
        if (!editableName && nameField.isFocused()) nameField.setFocused(false);
        rows.addChild(nameField, 2, smallSettings).setTooltip(createTooltip("targetName"));
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
        rows.addChild(new SimpleIconButton(80, 0, button -> {
            if (CompatibilityHelper.isModLoaded("cloth-config")) minecraft.setScreen(ConfigScreen.create(this));
        }), smallSettings).setTooltip(createTooltip("toConfigScreen"));
        final int widgetsPerPage, size;
        if (toggleCategoryButton.getValue() == Category.DISABLE) {
            widgetsPerPage = 7;
            size = disableConfigs.size();
            rows.addChild(disabledNameField, 3, smallSettings).setTooltip(createTooltip("disabledName"));
            rows.addChild(new SimpleIconButton(64, 0, button -> {
                if (syncDisableDraft(button, false)) {
                    button.setTooltip(createTooltip("saveAs"));
                    initWidgets(page);
                }
                }), smallSettings).setTooltip(createTooltip("saveAs"));
            for (int i = page * widgetsPerPage; i < Math.min((page + 1) * widgetsPerPage, size); i++) {
                DisableConfig config = disableConfigs.get(i);
                String targetName = nameField.getValue();
                Set<String> hiddenNames = hiddenNameMap.computeIfAbsent(targetName, k -> new HashSet<>());
                addRenderableWidget(new CycleIconButton(32, 16, hiddenNames.contains(config.name()) ? 1 : 0, 2))
                        .setOnValueChange(value -> {
                            if (value == 0) hiddenNames.remove(config.name());
                            else hiddenNames.add(config.name());
                        })
                        .setPosition(x + (xSize + middleWidth) / 2 - 20, y + 5 + (widgetHeight + 2) * (3 + i % widgetsPerPage));
                rows.addChild(createButton(LocUtil.literal(config.name()), widgetWidth * 2 - 18, button -> {
                    loadDisableConfig(config);
                    initWidgets(page);
                }), 3).setTooltip(Tooltip.create(LocUtil.literal(config.name())));
                rows.addChild(new SimpleIconButton(48, 0, button -> {
                    disableConfigs.removeIf(disableConfig -> disableConfig.name().equals(config.name()));
                    hiddenNameMap.values().forEach(names -> names.remove(config.name()));
                    if (disabledNameField.getValue().equals(config.name())) clearDisableDraft();
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
                final int targetIndex = i;
                String name = target.name();
                rows.addChild(createButton(LocUtil.literal(name), widgetWidth * 2 - 18, button -> loadBindTarget(target)), 3)
                        .setTooltip(Tooltip.create(name.equals(RealCameraCore.currentTarget().name()) ?
                                LocUtil.literal(name + "\n").append(LocUtil.MODEL_VIEW_WIDGET("currentConfig")) :
                                LocUtil.literal(name)));
                if (i < fixedTargetCount) continue;
                rows.addChild(new SimpleIconButton(48, 0, button -> {
                    targetList.remove(target);
                    ConfigFile.save();
                    if (nameField.getValue().equals(target.name())) loadAdjacentBindTarget(fixedTargetList, targetList, targetIndex);
                    initWidgets(page * widgetsPerPage > size - 2 && size > 1 ? page - 1 : page);
                }), smallSettings);
            }
        }
        grid.arrangeElements();
        FrameLayout.alignInRectangle(grid, x + (xSize + middleWidth) / 2 + 4, y + 2, x + xSize, y + ySize, 0, 0);
        grid.visitWidgets(this::addRenderableWidget);
        final int pages = (size - 1) / widgetsPerPage + 1;
        addRenderableWidget(new SimpleIconButton(x + (xSize + middleWidth) / 2 + 8, y + ySize - 20, 16, 16, 16, 0, button -> initWidgets((page - 1 + pages) % pages)));
        addRenderableWidget(new StringWidget(x + (xSize + middleWidth) / 2 + 30, y + ySize - 20, widgetWidth * 2 - 40, widgetHeight, LocUtil.literal((page + 1) + " / " + pages), font));
        addRenderableWidget(new SimpleIconButton(x + xSize - 21, y + ySize - 20, 16, 16, 32, 0, button -> initWidgets((page + 1) % pages)));
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

    private boolean syncDisableDraft(AbstractButton button, boolean allowEmptyName) {
        String name = disabledNameField.getValue().trim();
        String textureId = disabledIdField.getValue().trim();
        if (name.isBlank()) {
            if (!allowEmptyName) {
                button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("emptyName").withStyle(ChatFormatting.RED)));
                return false;
            }
            if (!hasMeaningfulDisableDraft()) {
                if (textureId.isBlank() && !rectWidgets.isEmpty()) {
                    button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("emptyTextureId").withStyle(ChatFormatting.RED)));
                    return false;
                }
                return true;
            }
            name = generateDisableConfigName(textureId);
            disabledNameField.setValue(name);
        }
        if (textureId.isBlank()) {
            button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("emptyTextureId").withStyle(ChatFormatting.RED)));
            return false;
        }
        upsertDisableConfig(createDisableDraft());
        return true;
    }

    private DisableConfig createDisableDraft() {
        return new DisableConfig(disabledNameField.getValue().trim(), disabledIdField.getValue().trim(), disableModeButton.getValue() == 0,
                rectWidgets.stream().map(UVRectangleWidget::toUVRectangle).toList());
    }

    private boolean hasMeaningfulDisableDraft() {
        return !disabledIdField.getValue().isBlank() && (disableModeButton.getValue() == 0 || !rectWidgets.isEmpty());
    }

    private void upsertDisableConfig(DisableConfig disableConfig) {
        for (int i = 0; i < disableConfigs.size(); i++) {
            if (disableConfigs.get(i).name().equals(disableConfig.name())) {
                disableConfigs.set(i, disableConfig);
                return;
            }
        }
        disableConfigs.add(disableConfig);
    }

    private String generateDisableConfigName(String textureId) {
        String base = textureId.trim();
        int separatorIndex = Math.max(base.lastIndexOf('/'), base.lastIndexOf(':'));
        if (separatorIndex >= 0 && separatorIndex < base.length() - 1) base = base.substring(separatorIndex + 1);
        int extensionIndex = base.lastIndexOf('.');
        if (extensionIndex > 0) base = base.substring(0, extensionIndex);
        base = sanitizeDisableConfigName(base);
        if (base.length() > CONFIG_NAME_MAX_LENGTH) base = base.substring(0, CONFIG_NAME_MAX_LENGTH);

        String candidate = base;
        for (int suffix = 2; disableConfigNameExists(candidate); suffix++) {
            String suffixText = "_" + suffix;
            int baseLength = Mth.clamp(CONFIG_NAME_MAX_LENGTH - suffixText.length(), 1, base.length());
            candidate = base.substring(0, baseLength) + suffixText;
        }
        return candidate;
    }

    private String sanitizeDisableConfigName(String name) {
        StringBuilder builder = new StringBuilder();
        boolean lastWasSeparator = false;
        for (int i = 0; i < name.length(); i++) {
            char c = Character.toLowerCase(name.charAt(i));
            if (Character.isLetterOrDigit(c)) {
                builder.append(c);
                lastWasSeparator = false;
            } else if ((c == '_' || c == '-') && builder.length() > 0 && !lastWasSeparator) {
                builder.append(c);
                lastWasSeparator = true;
            } else if (builder.length() > 0 && !lastWasSeparator) {
                builder.append('_');
                lastWasSeparator = true;
            }
        }
        while (builder.length() > 0 && (builder.charAt(builder.length() - 1) == '_' || builder.charAt(builder.length() - 1) == '-')) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.length() == 0 ? "disable" : builder.toString();
    }

    private boolean disableConfigNameExists(String name) {
        return disableConfigs.stream().anyMatch(disableConfig -> disableConfig.name().equals(name));
    }

    private void loadAdjacentBindTarget(List<BindTarget> fixedTargetList, List<BindTarget> targetList, int deletedIndex) {
        List<BindTarget> targets = new ArrayList<>(fixedTargetList);
        targets.addAll(targetList);
        if (targets.isEmpty()) loadBindTarget(BindTarget.blank("", ""));
        else loadBindTarget(targets.get(Math.min(deletedIndex, targets.size() - 1)));
    }

    private void loadDisableConfig(DisableConfig config) {
        disabledNameField.setValue(config.name());
        disabledIdField.setValue(config.textureId());
        disableModeButton.setValue(config.disableAll() ? 0 : 1);
        focusedRectWidget = null;
        rectWidgets.clear();
        for (UVRectangle rect : config.rectangles()) rectWidgets.add(createRectWidget(rect));
        focusedRectWidgetNumberField.setMax(rectWidgets.size());
        focusedRectWidgetNumberField.setNumber(0);
        rectWidgetsSizeWidget.setMessage(LocUtil.literal(String.valueOf(rectWidgets.size())));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float deltaTick) {
        renderBackground(graphics);
        analyser.initialize(toggleCategoryButton.getValue() == Category.DISABLE ? genPreviewBindTarget() : genBindTarget(), modelScale);
        renderModelViewArea(graphics, minecraft.player);
        renderTextureViewArea(graphics);
        applyAnalyser(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, deltaTick);
        if (toggleCategoryButton.getValue() == Category.DISABLE && selectionModeButton.getValue() == 2 && inModelViewArea(mouseX, mouseY)) {
            GUIHelper.enableScissor(graphics, modelViewArea);
            graphics.fill(mouseX - selectionRadius, mouseY - selectionRadius, mouseX + selectionRadius, mouseY + selectionRadius, 400, 0x4F3333CC);
            graphics.disableScissor();
        }
        if (textureViewArea != null) {
            GUIHelper.enableScissor(graphics, textureViewArea);
            if (clickedX >= 0 && clickedY >= 0)
                graphics.fill(Math.min((int) clickedX, mouseX), Math.min((int) clickedY, mouseY), Math.max((int) clickedX, mouseX), Math.max((int) clickedY, mouseY), 0x4F3333CC);
            if (disableModeButton.getValue() == 0)
                new UVRectangleWidget(0f, 0f, 1f, 1f).renderWidget(graphics, mouseX, mouseY, deltaTick);
            graphics.disableScissor();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        super.renderBackground(graphics);
        graphics.fill(x, y, x + (xSize - middleWidth) / 2 - 4, y + ySize, 0xFF444444);
        graphics.fill(x + (xSize - middleWidth) / 2, y, x + (xSize + middleWidth) / 2, y + ySize, 0xFF222222);
        graphics.fill(x + (xSize + middleWidth) / 2 + 4, y, x + xSize, y + ySize, 0xFF444444);
    }

    protected void applyAnalyser(GuiGraphics graphics, int mouseX, int mouseY) {
        String textureId = toggleCategoryButton.getValue() == Category.DISABLE ? disabledIdField.getValue() : "";
        Set<String> hiddenNames = hiddenNameMap.getOrDefault(nameField.getValue(), Set.of());
        analyser.applyDisableConfigs(textureId, hiddenNames);
        if (toggleCategoryButton.getValue() == Category.DISABLE && selectionModeButton.getValue() == 2 && inModelViewArea(mouseX, mouseY))
            analyser.computeFocusedOnModel(mouseX - selectionRadius, mouseY - selectionRadius, mouseX + selectionRadius, mouseY + selectionRadius);
        if (inTextureViewArea(mouseX, mouseY)) analyser.computeFocusedOnTexture(mouseX, mouseY);
        if (inModelViewArea(mouseX, mouseY)) analyser.computeFocusedOnModel(mouseX, mouseY, layers);
        if (toggleCategoryButton.getValue() == Category.CONFIGS || selectionModeButton.getValue() == 1) analyser.computeFocusedPolyhedron();
        focusedPolyhedron = analyser.getFocusedPolyhedron();
        focusedTextureId = analyser.getFocusedTextureId();
        GUIHelper.enableScissor(graphics, modelViewArea);
        analyser.drawModel(graphics, analyser.modelPose);
        if (toggleCategoryButton.getValue() != Category.PREVIEW) analyser.drawFocusedInModelArea(graphics);
        if (toggleCategoryButton.getValue() == Category.CONFIGS) analyser.drawBindTarget(graphics);
        else analyser.drawCameraDirections(graphics);
        graphics.disableScissor();
        if (textureViewArea != null) {
            GUIHelper.enableScissor(graphics, textureViewArea);
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
        try {
            entity.yBodyRot = 180.0f;
            entity.setYRot(180.0f + (float) entityYawSlider.getNumber());
            entity.setXRot((float) entityPitchSlider.getNumber());
            entity.yHeadRot = entity.getYRot();
            entity.yHeadRotO = entity.getYRot();
            Vector3f offset = new Vector3f((float) modelX, (float) modelY, 0);
            renderEntityWithAnalyser(graphics, x1, y1, x2, y2, modelScale, offset, quaternionf, entity);
        } finally {
            entity.yBodyRot = entityBodyYaw;
            entity.setYRot(entityYaw);
            entity.setXRot(entityPitch);
            entity.yHeadRotO = entityPrevHeadYaw;
            entity.yHeadRot = entityHeadYaw;
        }
    }

    protected void renderEntityWithAnalyser(GuiGraphics graphics, int x1, int y1, int x2, int y2, float scale, Vector3f offset, Quaternionf quaternionf, LivingEntity entity) {
        analyser.modelPose.translate((float) (x1 + x2) / 2.0f, (float) (y1 + y2) / 2.0f, 0);
        analyser.modelPose.mulPoseMatrix(new Matrix4f().scaling(scale, scale, -scale));
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
        analyser.texturePose.mulPoseMatrix(new Matrix4f().scaling(scale, scale, -scale));
        analyser.texturePose.translate(offset.x(), offset.y(), offset.z());
    }

    protected void importBindTarget(Button button) {
        DataResult<BindTarget> result = ConfigCodec.fromCompressedBase64(minecraft.keyboardHandler.getClipboard());
        result.result().ifPresentOrElse(target -> {
            loadBindTarget(target);
            button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("importSucceeded", LocUtil.literal("'" + target.name() + "'").withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GREEN)));
        }, () -> {
            String message = result.error().map(error -> error.message()).orElse("Unknown error");
            button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("importFailed", message).withStyle(ChatFormatting.RED)));
        });
    }

    protected void exportBindTarget(Button button) {
        DataResult<String> result = ConfigCodec.toCompressedBase64(genBindTarget());
        result.result().ifPresentOrElse(base64 -> {
            minecraft.keyboardHandler.setClipboard(base64);
            button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("exportSucceeded").withStyle(ChatFormatting.GREEN)));
        }, () -> {
            String message = result.error().map(error -> error.message()).orElse("Unknown error");
            button.setTooltip(Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP("exportFailed", message).withStyle(ChatFormatting.RED)));
        });
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
        initOffsetPairs();
        offsetXPair.setNumber(target.offsets().getX());
        offsetYPair.setNumber(target.offsets().getY());
        offsetZPair.setNumber(target.offsets().getZ());
        offsetPitchPair.setNumber(target.offsets().getPitch());
        offsetYawPair.setNumber(target.offsets().getYaw());
        offsetRollPair.setNumber(target.offsets().getRoll());
        disableConfigs.clear();
        disableConfigs.addAll(target.disableConfigs());
        clearDisableDraft();
    }

    protected BindTarget genBindTarget() {
        return genBindTarget(new ArrayList<>(disableConfigs));
    }

    private BindTarget genPreviewBindTarget() {
        return genBindTarget(previewDisableConfigs(disableConfigs, createDisableDraft()));
    }

    static List<DisableConfig> previewDisableConfigs(List<DisableConfig> disableConfigs, DisableConfig draft) {
        List<DisableConfig> previewConfigs = new ArrayList<>(disableConfigs);
        for (int i = 0; i < previewConfigs.size(); i++) {
            if (previewConfigs.get(i).name().equals(draft.name())) {
                previewConfigs.set(i, draft);
                break;
            }
        }
        return previewConfigs;
    }

    private BindTarget genBindTarget(List<DisableConfig> disableConfigs) {
        initOffsetPairs();
        TargetConfig targetConfig = new TargetConfig( forwardUField.getNumber(), forwardVField.getNumber(), upwardUField.getNumber(), upwardVField.getNumber(), posUField.getNumber(), posVField.getNumber());
        BindConfig bindConfig = new BindConfig( bindXButton.getValue() == 0, bindYButton.getValue() == 0, bindZButton.getValue() == 0, bindRotButton.getValue() == 0);
        OffsetConfig offsets = new OffsetConfig()
                .setScale(scaleField.getNumber())
                .setX(offsetXPair.getNumber())
                .setY(offsetYPair.getNumber())
                .setZ(offsetZPair.getNumber())
                .setPitch(offsetPitchPair.getNumber())
                .setYaw(offsetYawPair.getNumber())
                .setRoll(offsetRollPair.getNumber());
        return new BindTarget(nameField.getValue(), textureIdField.getValue(), priorityField.getNumber(), depthField.getNumber(), targetConfig, bindConfig, offsets, disableConfigs);
    }

    private void clearDisableDraft() {
        disabledNameField.setValue("");
        disabledIdField.setValue("");
        disableModeButton.setValue(0);
        rectWidgets.clear();
        focusedRectWidget = null;
        focusedRectWidgetNumberField.setMax(0);
        focusedRectWidgetNumberField.setNumber(0);
        rectWidgetsSizeWidget.setMessage(LocUtil.literal("0"));
        uMinField.setNumber(0f);
        vMinField.setNumber(0f);
        uMaxField.setNumber(0f);
        vMaxField.setNumber(0f);
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

    protected <T> CycleButton.Builder<T> createCyclingButtonBuilder(Map<T, Component> messages) {
        return new CycleButton.Builder<T>(messages::get).withValues(messages.keySet());
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
        return x >= modelViewArea.left() && y >= modelViewArea.top() && x <= modelViewArea.right() && y <= modelViewArea.bottom();
    }

    protected boolean inTextureViewArea(double x, double y) {
        return textureViewArea != null && x >= textureViewArea.left() && y >= textureViewArea.top() && x <= textureViewArea.right() && y <= textureViewArea.bottom();
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double storedX = clickedX, storedY = clickedY;
        clickedX = clickedY = -1;
        if (button == InputConstants.MOUSE_BUTTON_LEFT && toggleCategoryButton.getValue() != Category.PREVIEW) {
            if (InputConstants.isKeyDown(minecraft.getWindow().getWindow(), modifierKey.getValue())) {
                if (leftClickedWithModifier(mouseX, mouseY)) return true;
            } else if (inTextureViewArea(mouseX, mouseY)) {
                if (super.mouseClicked(mouseX, mouseY, button)) return true;
                if (storedX >= 0 && storedY >= 0) {
                    float xMin = (float) Math.min(storedX, mouseX), yMin = (float) Math.min(storedY,  mouseY), xMax = (float) Math.max(storedX, mouseX), yMax = (float) Math.max(storedY, mouseY);
                    setFocused(addRectWidget(new UVRectangleWidget(xMin, yMin, xMax, yMax, textureViewArea)));
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
        if (button == InputConstants.MOUSE_BUTTON_LEFT && !InputConstants.isKeyDown(minecraft.getWindow().getWindow(), modifierKey.getValue())) {
            if (inModelViewArea(mouseX, mouseY)) {
                xRot += (float) (Math.PI * deltaY / ySize);
                yRot -= (float) (Math.PI * deltaX / middleWidth);
                return true;
            }
        } else if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
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
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (inModelViewArea(mouseX, mouseY)) {
            if (InputConstants.isKeyDown(minecraft.getWindow().getWindow(), modifierKey.getValue())) {
                if (toggleCategoryButton.getValue() == Category.DISABLE && selectionModeButton.getValue() == 2)
                    selectionRadius = Mth.clamp(selectionRadius + (int) amount * 2, 2, 48);
                else layers = Math.max(0, layers + (int) amount);
            } else {
                modelScale = Mth.clamp(modelScale + (int) amount * modelScale / 16, 16, 1024);
            }
            return true;
        } else if (inTextureViewArea(mouseX, mouseY)) {
            textureScale = Mth.clamp(textureScale + (int) amount * textureScale / 16, 16, 1024);
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (CommonInputs.selected(keyCode)) {
            GuiEventListener focused = getFocused();
            if (focused != null && !focused.isFocused()) setFocused(focusedRectWidget);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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

        public UVRectangleWidget(float xMin, float yMin, float xMax, float yMax, @NotNull ScreenRectangle screenArea) {
            super((int) xMin, (int) yMin, (int) (xMax - xMin), (int) (yMax - yMin), Component.empty());
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
                    .add(- (float) (x1 + x2) / 2.0f, - (float) (y1 + y2) / 2.0f, 0)
                    .mul((float) 80 / (textureScale * screenArea.width()))
                    .add((float) -textureX + 0.5f, (float) -textureY + 0.5f, 0);
            return new Vec2(vector3f.x(), vector3f.y());
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float deltaTick) {
            Vec2 minXY = translateUVToXY(uMin, vMin, textureViewArea), maxXY = translateUVToXY(uMax, vMax, textureViewArea);
            float x1 = minXY.x, y1 = minXY.y, x2 = maxXY.x, y2 = maxXY.y, width = x2 - x1, height = y2 - y1;
            setX((int) x1);
            setY((int) y1);
            setWidth((int) width);
            this.height = (int) height;
            GUIHelper.enableScissor(graphics, textureViewArea);
            GUIHelper.fill(graphics, x1, y1, x2, y2, 0x4F3333CC);
            if (isHoveredOrFocused()) GUIHelper.fill(graphics, x1, y1, x2, y2, 0x2F3333CC);
            if (isFocused() || this == focusedRectWidget) GUIHelper.outline(graphics, x1, y1, width, height, 0xAAFFFFFF);
            graphics.disableScissor();
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (textureViewArea == null) return false;
            if (keyCode == InputConstants.KEY_DELETE && deleteFocusedRectWidget()) return true;
            if (CommonInputs.selected(keyCode) && uMin < uMax && vMin < vMax) {
                textureX = 0.5 * (1 - uMin - uMax);
                textureY = 0.5 * (1 - vMin - vMax);
                textureScale = (int) Mth.clamp(2560d / (textureViewArea.width() * Math.sqrt((uMax - uMin) * (vMax - vMin))), 16, 1024);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
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
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) { }
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
}
