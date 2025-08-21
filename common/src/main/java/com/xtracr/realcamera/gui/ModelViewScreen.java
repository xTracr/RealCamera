package com.xtracr.realcamera.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.config.BindingTarget.*;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.List;

public class ModelViewScreen extends Screen {
    public final ModelAnalyser analyser = new ModelAnalyser();
    protected int xSize = 450, ySize = 206, middleWidth = xSize - 200, widgetWidth = (xSize - middleWidth) / 4 - 8, widgetHeight = 18;
    protected int x, y;
    private boolean initialized;
    private int entityScale = 80, layers = 0, configCategory = 0, listCategory = 0, page = 0;
    private double entityX, entityY;
    private float xRot, yRot;
    private Vec2 focusedUV;
    private String focusedTextureId;
    private UVRectangleWidget focusedRectangle;
    private EditBox textureIdField, nameField, disabledNameField, disabledIdField;
    private NumberField<Integer> priorityField;
    private NumberField<Float> forwardUField, forwardVField, upwardUField, upwardVField, posUField, posVField;
    private NumberField<Float> uMinField, vMinField, uMaxField, vMaxField;
    private NumberField<Float> scaleField, depthField;
    private List<DisableConfig> configsInClipBoard = new ArrayList<>();
    private final List<DisableConfig> disableConfigs = new ArrayList<>();
    private final Map<String, Set<String>> hiddenNameMap = new HashMap<>();
    private final CycleButton<Integer> selectingButton = createCyclingButton(Map.of(
                    0, LocUtil.MODEL_VIEW_WIDGET("forwardMode").withStyle(ChatFormatting.GREEN),
                    1, LocUtil.MODEL_VIEW_WIDGET("upwardMode").withStyle(ChatFormatting.RED),
                    2, LocUtil.MODEL_VIEW_WIDGET("posMode").withStyle(ChatFormatting.BLUE)),
            widgetWidth * 2 + 4, LocUtil.MODEL_VIEW_WIDGET("selectMode"));
    private final CyclingTexturedButton pauseButton = new CyclingTexturedButton(0, 16, 0, 2);
    private final CyclingTexturedButton bindXButton = new CyclingTexturedButton(16, 16, 1, 2);
    private final CyclingTexturedButton bindYButton = new CyclingTexturedButton(16, 16, 0, 2);
    private final CyclingTexturedButton bindZButton = new CyclingTexturedButton(16, 16, 1, 2);
    private final CyclingTexturedButton bindRotButton = new CyclingTexturedButton(16, 16, 1, 2);
    private final CyclingTexturedButton disableAllButton = new CyclingTexturedButton(16, 16, 0, 2);
    private final DoubleSlider entityPitchSlider = createSlider("pitch", widgetWidth * 2 + 4, -90.0, 90.0);
    private final DoubleSlider entityYawSlider = createSlider("yaw", widgetWidth * 2 + 4, -60.0, 60.0);
    private final DoubleSlider offsetXSlider = createSlider("offsetX", widgetWidth * 2 - 18, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
    private final DoubleSlider offsetYSlider = createSlider("offsetY", widgetWidth * 2 - 18, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
    private final DoubleSlider offsetZSlider = createSlider("offsetZ", widgetWidth * 2 - 18, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
    private final DoubleSlider pitchSlider = createSlider("pitch", widgetWidth * 2 - 18, -180.0, 180.0);
    private final DoubleSlider yawSlider = createSlider("yaw", widgetWidth * 2 - 18, -180.0, 180.0);
    private final DoubleSlider rollSlider = createSlider("roll", widgetWidth * 2 - 18, -180.0, 180.0);
    private final List<UVRectangleWidget> rectangleWidgets = new ArrayList<>();

    public ModelViewScreen() {
        super(LocUtil.MODEL_VIEW_TITLE());
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
        initWidgets(configCategory, listCategory, page);
    }

    private void initWidgets(int configCategory, int listCategory, int page) {
        this.configCategory = configCategory;
        this.listCategory = listCategory;
        this.page = page;
        clearWidgets();
        initLeftWidgets();
        addRenderableWidget(pauseButton).setPosition(x + (xSize + middleWidth) / 2 - 38, y + 4);
        addRenderableWidget(new TexturedButton(x + (xSize + middleWidth) / 2 - 20, y + 4, 16, 16, 0, 0, button -> {
            entityScale = 80;
            entityYawSlider.setValue(0);
            entityPitchSlider.setValue(0);
            entityX = entityY = 0;
            xRot = yRot = 0;
            layers = 0;
        }));
        initRightWidgets();
    }

    private void initLeftWidgets() {
        forwardUField = createFloatField(widgetWidth, 0, forwardUField);
        forwardVField = createFloatField(widgetWidth, 0, forwardVField);
        upwardUField = createFloatField(widgetWidth, 0, upwardUField);
        upwardVField = createFloatField(widgetWidth, 0, upwardVField);
        posUField = createFloatField(widgetWidth, 0, posUField);
        posVField = createFloatField(widgetWidth, 0, posVField);
        textureIdField = createTextField(widgetWidth * 2 + 4, 1024, textureIdField);
        disabledIdField = createTextField(widgetWidth * 2 + 4, 1024, disabledIdField);
        uMinField = createFloatField(widgetWidth * 2 - 18, 0, uMinField);
        uMaxField = createFloatField(widgetWidth * 2 - 18, 0, uMaxField);
        vMinField = createFloatField(widgetWidth * 2 - 18, 0, vMinField);
        vMaxField = createFloatField(widgetWidth * 2 - 18, 0, vMaxField);
        scaleField = createFloatField(widgetWidth, 1.0f, scaleField).setMax(64.0f);
        depthField = createFloatField(widgetWidth, 0.2f, depthField).setMax(16.0f);
        GridLayout grid = new GridLayout();
        grid.defaultCellSetting().padding(4, 2, 0, 0);
        LayoutSettings smallSettings = grid.newCellSettings().padding(5, 3, 1, 1);
        GridLayout.RowHelper rows = grid.createRowHelper(2);
        rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("settings"), widgetWidth, button -> initWidgets(0, listCategory, page)));
        rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("preview"), widgetWidth, button -> initWidgets(1, listCategory, page)));
        if (configCategory == 0) {
            if (listCategory == 0) {
                rows.addChild(entityPitchSlider, 2);
                rows.addChild(entityYawSlider, 2);
                rows.addChild(selectingButton, 2).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("selectMode"));
                rows.addChild(forwardUField, 1, smallSettings);
                rows.addChild(forwardVField, 1, smallSettings);
                rows.addChild(upwardUField, 1, smallSettings);
                rows.addChild(upwardVField, 1, smallSettings);
                rows.addChild(posUField, 1, smallSettings);
                rows.addChild(posVField, 1, smallSettings);
                rows.addChild(textureIdField, 2, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("textureId"));
            } else {
                LayoutSettings offsetXSettings = grid.newCellSettings().padding(-19, 3, 1, 1);
                rows.addChild(new StringWidget(widgetWidth + 10, widgetHeight, LocUtil.MODEL_VIEW_WIDGET("disableAll"), font), 2);
                addRenderableWidget(disableAllButton).setPosition(x + 2 * widgetWidth + 8 - 17, y + 5 + (widgetHeight + 2));
                rows.addChild(disabledIdField, 2, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("textureId"));
                rows.addChild(new StringWidget(20, widgetHeight, LocUtil.literal("uMin:"), font));
                rows.addChild(uMinField, 1, offsetXSettings);
                rows.addChild(new StringWidget(20, widgetHeight, LocUtil.literal("uMax:"), font));
                rows.addChild(uMaxField, 1, offsetXSettings);
                rows.addChild(new StringWidget(20, widgetHeight, LocUtil.literal("vMin:"), font));
                rows.addChild(vMinField, 1, offsetXSettings);
                rows.addChild(new StringWidget(20, widgetHeight, LocUtil.literal("vMax:"), font));
                rows.addChild(vMaxField, 1, offsetXSettings);
                rows.addChild(new StringWidget(widgetWidth, widgetHeight, Component.empty(), font), 2);
                addRenderableWidget(new TexturedButton(48, 0, button -> {
                    rectangleWidgets.remove(focusedRectangle);
                    focusedRectangle = null;
                })).setPosition(x + 2 * widgetWidth + 8 - 17, y + 5 + 7 * (widgetHeight + 2));
            }
        } else {
            LayoutSettings sliderSettings = grid.newCellSettings().padding(-20, 2, 0, 0);
            rows.addChild(bindXButton, 1, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("bindButtons"));
            rows.addChild(offsetXSlider, 1, sliderSettings);
            rows.addChild(bindYButton, 1, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("bindButtons"));
            rows.addChild(offsetYSlider, 1, sliderSettings);
            rows.addChild(bindZButton, 1, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("bindButtons"));
            rows.addChild(offsetZSlider, 1, sliderSettings);
            rows.addChild(bindRotButton, 1, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("bindButtons"));
            rows.addChild(pitchSlider, 1, sliderSettings);
            rows.addChild(yawSlider, 2, grid.newCellSettings().padding(26, 2, 0, 0));
            rows.addChild(new TexturedButton(0, 0, button -> {
                offsetXSlider.setValue(0);
                offsetYSlider.setValue(0);
                offsetZSlider.setValue(0);
                pitchSlider.setValue(0);
                yawSlider.setValue(0);
                rollSlider.setValue(0);
            }), 1, smallSettings);
            rows.addChild(rollSlider, 1, sliderSettings);
            rows.addChild(scaleField, 1, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("scale"));
            rows.addChild(depthField, 1, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("depth"));
        }
        rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("save"), widgetWidth, button -> {
            ConfigFile.config().binding.putTarget(generateBindingTarget());
            ConfigFile.save();
            initWidgets(page);
        }));
        rows.addChild(priorityField = NumberField.ofInt(font, widgetWidth - 2, widgetHeight - 2, 0, priorityField), 1, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("priority"));
        rows.addChild(nameField = createTextField(widgetWidth * 2 + 4, 20, nameField), 2, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("targetName"));
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
        rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("configs"), widgetWidth, button -> initWidgets(configCategory, 0, 0)), 2).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("configs"));
        rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("disable"), widgetWidth, button -> initWidgets(configCategory, 1, 0)), 2).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("disable"));
        final int widgetsPerPage, size;
        if (listCategory == 0) {
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
                }), 1, smallSettings);
            }
        } else {
            widgetsPerPage = 6;
            size = disableConfigs.size();
            rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("copy"), widgetWidth, button -> configsInClipBoard = List.copyOf(disableConfigs)), 2).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("copy"));
            rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("paste"), widgetWidth, button -> {
                configsInClipBoard.stream().filter(config -> !disableConfigs.contains(config)).forEach(disableConfigs::add);
                initWidgets(0);
            }), 2);
            rows.addChild(disabledNameField, 3, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("disabledName"));
            rows.addChild(new TexturedButton(64, 0, button -> {
                String name = disabledNameField.getValue();
                String textureId = disabledIdField.getValue();
                if (name.isBlank() || textureId.isBlank()) return;
                DisableConfig disableConfig = new DisableConfig(name, textureId, disableAllButton.getValue() == 0, rectangleWidgets.stream().map(UVRectangleWidget::toRectangle).toArray(UVRectangle[]::new));
                for (int i = 0; i < disableConfigs.size(); i++) {
                    if (disableConfigs.get(i).name().equals(name)) {
                        disableConfigs.set(i, disableConfig);
                        initWidgets(page);
                        return;
                    }
                }
                disableConfigs.add(disableConfig);
                initWidgets(page);
            }), 1, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("addOrSaveAs"));
            for (int i = page * widgetsPerPage; i < Math.min((page + 1) * widgetsPerPage, size); i++) {
                DisableConfig config = disableConfigs.get(i);
                String targetName = nameField.getValue();
                Set<String> hiddenNames = hiddenNameMap.computeIfAbsent(targetName, k -> new HashSet<>());
                addRenderableWidget(new CyclingTexturedButton(32, 16, hiddenNames.contains(config.name()) ? 1 : 0, 2))
                        .setOnPress(value -> {
                            if (value == 0) hiddenNames.remove(config.name());
                            else hiddenNames.add(config.name());
                        })
                        .setPosition(x + (xSize + middleWidth) / 2 - 20, y + 5 + (widgetHeight + 2) * (3 + i % widgetsPerPage));
                rows.addChild(createButton(LocUtil.literal(config.name()), widgetWidth * 2 - 18, button -> {
                    disabledNameField.setValue(config.name());
                    disabledIdField.setValue(config.textureId());
                    disableAllButton.setValue(config.disableAll() ? 0 : 1);
                    rectangleWidgets.clear();
                    for (UVRectangle rectangle : config.rectangles()) rectangleWidgets.add(createRectangleWidget(rectangle));
                    initWidgets(page);
                }), 3).setTooltip(Tooltip.create(LocUtil.literal(config.name())));
                rows.addChild(new TexturedButton(48, 0, button -> {
                    disableConfigs.removeIf(disableConfig -> disableConfig.name().equals(config.name()));
                    initWidgets(page * widgetsPerPage > size - 2 && size > 1 ? page - 1 : page);
                }), 1, smallSettings);
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

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float deltaTick) {
        super.renderBackground(graphics, mouseX, mouseY, deltaTick);
        graphics.fill(x, y, x + (xSize - middleWidth) / 2 - 4, y + ySize, 0xFF444444);
        renderEntityInViewArea(graphics, x + (xSize - middleWidth) / 2, y, x + (xSize + middleWidth) / 2, y + ySize, mouseX, mouseY, minecraft.player);
        graphics.fill(x + (xSize + middleWidth) / 2 + 4, y, x + xSize, y + ySize, 0xFF444444);
    }

    protected void renderEntityInViewArea(GuiGraphics graphics, int x1, int y1, int x2, int y2, int mouseX, int mouseY, LivingEntity entity) {
        graphics.enableScissor(x1, y1, x2, y2);
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
        Vector3f vector3f = new Vector3f((float) entityX, (float) entityY, -2.0f);
        renderEntityWithAnalyser(graphics, x1, y1, x2, y2, mouseX, mouseY, vector3f, quaternionf, entity);
        entity.yBodyRot = entityBodyYaw;
        entity.setYRot(entityYaw);
        entity.setXRot(entityPitch);
        entity.yHeadRotO = entityPrevHeadYaw;
        entity.yHeadRot = entityHeadYaw;
        graphics.disableScissor();
    }

    public void renderEntityWithAnalyser(GuiGraphics graphics, int x1, int y1, int x2, int y2, int mouseX, int mouseY, Vector3f offset, Quaternionf quaternionf, LivingEntity entity) {
        int offsetZ = (int) (-8 * entityScale * entity.getBbHeight());
        graphics.fill(x1, y1, x2, y2, offsetZ, 0xFF222222);
        analyser.setup(generateBindingTarget(), entityScale, offsetZ);
        graphics.pose().pushPose();
        graphics.pose().translate((float) (x1 + x2) / 2.0f, (float) (y1 + y2) / 2.0f, 0);
        graphics.pose().scale(entityScale, entityScale, -entityScale);
        graphics.pose().translate(offset.x(), offset.y(), offset.z());
        graphics.pose().mulPose(quaternionf);
        graphics.pose().translate(0, -entity.getBbHeight() / 2.0f, 0);
        analyser.updateModel(minecraft, entity, 1.0f, graphics.pose());
        analyser.analyse(mouseX, mouseY, layers);
        analyser.drawModel(graphics, hiddenNameMap, 1);
        focusedUV = analyser.getFocusedUV();
        focusedTextureId = analyser.getFocusedTextureId();
        if (configCategory == 0 && listCategory == 0) analyser.drawSelected(graphics);
        else analyser.previewEffect(graphics, listCategory == 1);
        graphics.pose().popPose();
    }

    protected BindingTarget generateBindingTarget() {
        TargetConfig targetConfig = new TargetConfig( forwardUField.getNumber(), forwardVField.getNumber(), upwardUField.getNumber(), upwardVField.getNumber(), posUField.getNumber(), posVField.getNumber());
        BindConfig bindConfig = new BindConfig( bindXButton.getValue() == 0, bindYButton.getValue() == 0, bindZButton.getValue() == 0, bindRotButton.getValue() == 0);
        OffsetConfig offsets = new OffsetConfig().setScale(scaleField.getNumber()).setX(offsetXSlider.getValue()) .setY(offsetYSlider.getValue()).setZ(offsetZSlider.getValue()).setPitch((float) pitchSlider.getValue()).setYaw((float) yawSlider.getValue()).setRoll((float) rollSlider.getValue());
        return new BindingTarget(nameField.getValue(), textureIdField.getValue(), priorityField.getNumber(), depthField.getNumber(), targetConfig, bindConfig, offsets, disableConfigs.toArray(DisableConfig[]::new));
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
        offsetYSlider.setValue(target.offsets().getY());
        offsetZSlider.setValue(target.offsets().getZ());
        pitchSlider.setValue(target.offsets().getPitch());
        yawSlider.setValue(target.offsets().getYaw());
        rollSlider.setValue(target.offsets().getRoll());
        disableConfigs.clear();
        disableConfigs.addAll(List.of(target.disableConfigs()));
    }

    private Button createButton(Component message, int width, Button.OnPress onPress) {
        return Button.builder(message, onPress).size(width, widgetHeight).build();
    }

    public UVRectangleWidget createRectangleWidget(UVRectangle rectangle) {
        return new UVRectangleWidget(rectangle.uMin(), rectangle.vMin(), rectangle.uMax(), rectangle.vMax());
    }

    private CycleButton<Integer> createCyclingButton(Map<Integer, Component> messages, int width, Component optionText) {
        return new CycleButton.Builder<Integer>(messages::get).withValues(messages.keySet()).create(0, 0, width, widgetHeight, optionText);
    }

    private DoubleSlider createSlider(String key, int width, double min, double max) {
        return new DoubleSlider(width, widgetHeight, 0, min, max, d -> LocUtil.MODEL_VIEW_WIDGET(key, MathUtil.round(d, 2)));
    }

    private NumberField<Float> createFloatField(int width, float defaultValue, @Nullable NumberField<Float> copyFrom) {
        return NumberField.ofFloat(font, width - 2, widgetHeight - 2, defaultValue, copyFrom).setMax(1.0f).setMin(0f);
    }

    private EditBox createTextField(int width, int maxLength, @Nullable EditBox copyFrom) {
        EditBox editBox = new EditBox(font, 0, 0, width - 2, widgetHeight - 2, Component.empty());
        editBox.setMaxLength(maxLength);
        if (copyFrom != null) editBox.setValue(copyFrom.getValue());
        return editBox;
    }

    protected boolean mouseInViewArea(double mouseX, double mouseY) {
        return mouseX >= x + (double) (xSize - middleWidth) / 2 && mouseX <= x + (double) (xSize + middleWidth) / 2 && mouseY >= y && mouseY <= y + ySize;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseInViewArea(mouseX, mouseY) && focusedUV != null && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && configCategory == 0 && InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_ALT)) {
            if (listCategory == 0) {
                if (selectingButton.getValue() == 0) {
                    forwardUField.setNumber(focusedUV.x);
                    forwardVField.setNumber(focusedUV.y);
                } else if (selectingButton.getValue() == 1) {
                    upwardUField.setNumber(focusedUV.x);
                    upwardVField.setNumber(focusedUV.y);
                } else {
                    posUField.setNumber(focusedUV.x);
                    posVField.setNumber(focusedUV.y);
                }
                textureIdField.setValue(focusedTextureId);
                return true;
            } else if (listCategory == 1) {
                disabledIdField.setValue(focusedTextureId);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (mouseInViewArea(mouseX, mouseY)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_ALT)) {
                xRot += (float) (Math.PI * deltaY / ySize);
                yRot -= (float) (Math.PI * deltaX / middleWidth);
                return true;
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                entityX += deltaX / entityScale;
                entityY += deltaY / entityScale;
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseInViewArea(mouseX, mouseY)) {
            if (InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_ALT)) {
                layers = Math.max(0, layers + (int) verticalAmount);
            } else {
                entityScale = Mth.clamp(entityScale + (int) verticalAmount * entityScale / 16, 16, 1024);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean isPauseScreen() {
        return pauseButton.getValue() == 1;
    }

    public class UVRectangleWidget extends AbstractWidget {
        public float uMin, vMin, uMax, vMax;

        public UVRectangleWidget(float uMin, float vMin, float uMax, float vMax) {
            super(0, 0, 16, 16, Component.empty());
            this.uMin = uMin;
            this.vMin = vMin;
            this.uMax = uMax;
            this.vMax = vMax;
        }

        public UVRectangle toRectangle() {
            return new UVRectangle(uMin, vMin, uMax, vMax);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float deltaTick) {

        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

        }
    }
}
