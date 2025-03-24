package com.xtracr.realcamera.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Lighting;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.util.IVertexRecorder;
import com.xtracr.realcamera.util.LocUtil;
import com.xtracr.realcamera.util.MathUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ModelViewScreen extends Screen {
    protected int xSize = 406, ySize = 206, widgetWidth = (xSize - ySize) / 4 - 8, widgetHeight = 18;
    protected int x, y;
    private boolean initialized;
    private int entitySize = 80, layers = 0, category = 0, page = 0;
    private double entityX, entityY;
    private float xRot, yRot;
    private String focusedTextureId;
    private Vec2 focusedUV;
    private EditBox textureIdField, nameField, disabledIdField;
    private NumberField<Float> forwardUField, forwardVField, upwardUField, upwardVField, posUField, posVField, scaleField, depthField;
    private NumberField<Integer> priorityField;
    private List<String> idsInClipBoard = new ArrayList<>();
    private final List<String> disabledIds = new ArrayList<>();
    private final CycleButton<Integer> selectingButton = createCyclingButton(Map.of(
                    0, LocUtil.MODEL_VIEW_WIDGET("forwardMode").withStyle(s -> s.withColor(ChatFormatting.GREEN)),
                    1, LocUtil.MODEL_VIEW_WIDGET("upwardMode").withStyle(s -> s.withColor(ChatFormatting.RED)),
                    2, LocUtil.MODEL_VIEW_WIDGET("posMode").withStyle(s -> s.withColor(ChatFormatting.BLUE))),
            widgetWidth * 2 + 4, LocUtil.MODEL_VIEW_WIDGET("selectMode"));
    private final CyclingTexturedButton pauseButton = new CyclingTexturedButton(0, 16, 0, 2);
    private final CyclingTexturedButton bindXButton = new CyclingTexturedButton(16, 16, 1, 2);
    private final CyclingTexturedButton bindYButton = new CyclingTexturedButton(16, 16, 0, 2);
    private final CyclingTexturedButton bindZButton = new CyclingTexturedButton(16, 16, 1, 2);
    private final CyclingTexturedButton bindRotButton = new CyclingTexturedButton(16, 16, 1, 2);
    private final CyclingTexturedButton showDisabled = new CyclingTexturedButton(32, 16, 0, 2);
    private final DoubleSlider entityPitchSlider = createSlider("pitch", widgetWidth * 2 + 4, -90.0, 90.0);
    private final DoubleSlider entityYawSlider = createSlider("yaw", widgetWidth * 2 + 4, -60.0, 60.0);
    private final DoubleSlider offsetXSlider = createSlider("offsetX", widgetWidth * 2 - 18, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
    private final DoubleSlider offsetYSlider = createSlider("offsetY", widgetWidth * 2 - 18, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
    private final DoubleSlider offsetZSlider = createSlider("offsetZ", widgetWidth * 2 - 18, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
    private final DoubleSlider pitchSlider = createSlider("pitch", widgetWidth * 2 - 18, -180.0, 180.0);
    private final DoubleSlider yawSlider = createSlider("yaw", widgetWidth * 2 - 18, -180.0, 180.0);
    private final DoubleSlider rollSlider = createSlider("roll", widgetWidth * 2 - 18, -180.0, 180.0);

    public ModelViewScreen() {
        super(LocUtil.MODEL_VIEW_TITLE());
    }

    @Override
    protected void init() {
        super.init();
        x = (width - xSize) / 2;
        y = (height - ySize) / 2;
        initWidgets(category, page);
        if (!initialized) loadBindingTarget(RealCameraCore.currentTarget);
        initialized = true;
    }

    private void initWidgets(int category, int page) {
        this.category = category;
        this.page = page;
        clearWidgets();
        initLeftWidgets(category);
        addRenderableWidget(pauseButton).setPosition(x + (xSize - ySize) / 2 + 4, y + 4);
        addRenderableWidget(new TexturedButton(x + (xSize - ySize) / 2 + 22, y + 4, 16, 16, 0, 0, button -> {
            entitySize = 80;
            entityYawSlider.setValue(0);
            entityPitchSlider.setValue(0);
            entityX = entityY = 0;
            xRot = yRot = 0;
            layers = 0;
        }));
        initRightWidgets(category, page);
    }

    private void initLeftWidgets(final int category) {
        forwardUField = createFloatField(widgetWidth, 0, forwardUField);
        forwardVField = createFloatField(widgetWidth, 0, forwardVField);
        upwardUField = createFloatField(widgetWidth, 0, upwardUField);
        upwardVField = createFloatField(widgetWidth, 0, upwardVField);
        posUField = createFloatField(widgetWidth, 0, posUField);
        posVField = createFloatField(widgetWidth, 0, posVField);
        String textureId = textureIdField != null ? textureIdField.getValue() : "";
        textureIdField = createTextField(widgetWidth * 2 + 4, null);
        textureIdField.setMaxLength(1024);
        textureIdField.setValue(textureId);
        scaleField = createFloatField(widgetWidth, 1.0f, scaleField).setMax(64.0f);
        depthField = createFloatField(widgetWidth, 0.2f, depthField).setMax(16.0f);
        GridLayout grid = new GridLayout();
        grid.defaultCellSetting().padding(4, 2, 0, 0);
        LayoutSettings smallSettings = grid.newCellSettings().padding(5, 3, 1, 1);
        GridLayout.RowHelper rows = grid.createRowHelper(2);
        rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("settings"), widgetWidth, button -> initWidgets(0, page)));
        rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("preview"), widgetWidth, button -> initWidgets(category | 0b01, page)));
        if ((category & 0b1) == 0) {
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
            ConfigFile.config().putTarget(generateBindingTarget());
            ConfigFile.save();
            initWidgets(category, page);
        }));
        rows.addChild(priorityField = NumberField.ofInt(font, widgetWidth - 2, widgetHeight - 2, 0, priorityField), 1, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("priority"));
        rows.addChild(nameField = createTextField(widgetWidth * 2 + 4, nameField), 2, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("targetName"));
        nameField.setMaxLength(20);
        grid.arrangeElements();
        FrameLayout.alignInRectangle(grid, x, y + 2, x + (xSize - ySize) / 2 - 4, y + ySize, 0, 0);
        grid.visitWidgets(this::addRenderableWidget);
    }

    private void initRightWidgets(final int category, final int page) {
        disabledIdField = createTextField(widgetWidth * 2 + 4, disabledIdField);
        disabledIdField.setMaxLength(1024);
        GridLayout grid = new GridLayout();
        grid.defaultCellSetting().padding(4, 2, 0, 0);
        LayoutSettings smallSettings = grid.newCellSettings().padding(5, 3, 1, 1);
        GridLayout.RowHelper rows = grid.createRowHelper(4);
        rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("configs"), widgetWidth, button -> initWidgets(category & 0b01, 0)), 2).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("configs"));
        rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("disable"), widgetWidth, button -> initWidgets(0b11, 0)), 2).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("disable"));
        final int widgetsPerPage, size;
        if ((category & 0b10) == 0) {
            widgetsPerPage = 8;
            List<BindingTarget> targetList = ConfigFile.config().getTargetList();
            size = targetList.size();
            for (int i = page * widgetsPerPage; i < Math.min((page + 1) * widgetsPerPage, size); i++) {
                BindingTarget target = targetList.get(i);
                String name = target.name;
                rows.addChild(createButton(LocUtil.literal(name), widgetWidth * 2 - 18, button -> loadBindingTarget(target)), 3).setTooltip(Tooltip.create(LocUtil.literal(name)));
                rows.addChild(new TexturedButton(48, 0, button -> {
                    targetList.remove(target);
                    ConfigFile.save();
                    initWidgets(category, page * widgetsPerPage > size - 2 && size > 1 ? page - 1 : page);
                }), 1, smallSettings);
            }
        } else {
            widgetsPerPage = 5;
            size = disabledIds.size();
            rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("copy"), widgetWidth, button -> idsInClipBoard = List.copyOf(disabledIds)), 2).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("copy"));
            rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("paste"), widgetWidth, button -> {
                idsInClipBoard.stream().filter(textureId -> !disabledIds.contains(textureId)).forEach(disabledIds::add);
                initWidgets(category, 0);
            }), 2);
            rows.addChild(disabledIdField, 4, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("disabledIdField"));
            rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("clear"), widgetWidth, button -> {
                disabledIds.clear();
                initWidgets(category, 0);
            }), 2);
            rows.addChild(showDisabled, 1, grid.newCellSettings().padding(7, 3, 1, 1));
            rows.addChild(new TexturedButton(64, 0, button -> {
                String disabledId = disabledIdField.getValue();
                if (disabledId.isBlank() || disabledIds.contains(disabledId)) return;
                disabledIds.add(disabledId);
                initWidgets(category, page);
            }), 1, smallSettings);
            for (int i = page * widgetsPerPage; i < Math.min((page + 1) * widgetsPerPage, size); i++) {
                String textureId = disabledIds.get(i);
                rows.addChild(createButton(LocUtil.literal(textureId), widgetWidth * 2 - 18, button -> disabledIdField.setValue(textureId)), 3).setTooltip(Tooltip.create(LocUtil.literal(textureId)));
                rows.addChild(new TexturedButton(48, 0, button -> {
                    disabledIds.remove(textureId);
                    initWidgets(category, page * widgetsPerPage > size - 2 && size > 1 ? page - 1 : page);
                }), 1, smallSettings);
            }
        }
        grid.arrangeElements();
        FrameLayout.alignInRectangle(grid, x + (xSize + ySize) / 2 + 4, y + 2, x + xSize, y + ySize, 0, 0);
        grid.visitWidgets(this::addRenderableWidget);
        final int pages = (size - 1) / widgetsPerPage + 1;
        addRenderableWidget(new TexturedButton(x + (xSize + ySize) / 2 + 8, y + ySize - 20, 16, 16, 16, 0, button -> initWidgets(category, (page - 1 + pages) % pages)));
        addRenderableWidget(new StringWidget(x + (xSize + ySize) / 2 + 30, y + ySize - 20, widgetWidth * 2 - 40, widgetHeight, LocUtil.literal((page + 1) + " / " + pages), font));
        addRenderableWidget(new TexturedButton(x + xSize - 21, y + ySize - 20, 16, 16, 32, 0, button -> initWidgets(category, (page + 1) % pages)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        renderEntityInViewArea(graphics, x + (xSize - ySize) / 2, y, x + (xSize + ySize) / 2, y + ySize, mouseX, mouseY, minecraft.player);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderBackground(graphics, mouseX, mouseY, delta);
        graphics.fill(x, y, x + (xSize - ySize) / 2 - 4, y + ySize, 0xFF444444);
        graphics.fill(x + (xSize - ySize) / 2, y, x + (xSize + ySize) / 2, y + ySize, 0xFF222222);
        graphics.fill(x + (xSize + ySize) / 2 + 4, y, x + xSize, y + ySize, 0xFF444444);
    }

    protected void renderEntityInViewArea(GuiGraphics graphics, int x1, int y1, int x2, int y2, int mouseX, int mouseY, LivingEntity entity) {
        float centerX = (float) (x1 + x2) / 2.0f;
        float centerY = (float) (y1 + y2) / 2.0f;
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
        renderEntityWithAnalyser(graphics, centerX, centerY, mouseX, mouseY, vector3f, quaternionf, entity);
        entity.yBodyRot = entityBodyYaw;
        entity.setYRot(entityYaw);
        entity.setXRot(entityPitch);
        entity.yHeadRotO = entityPrevHeadYaw;
        entity.yHeadRot = entityHeadYaw;
        graphics.disableScissor();
    }

    protected void renderEntityWithAnalyser(GuiGraphics graphics, float x, float y, int mouseX, int mouseY, Vector3f offset, Quaternionf quaternionf, LivingEntity entity) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().mulPose(new Matrix4f().scaling(entitySize, entitySize, -entitySize));
        graphics.pose().translate(offset.x(), offset.y(), offset.z());
        graphics.pose().mulPose(quaternionf);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        ModelAnalyser analyser = new ModelAnalyser(generateBindingTarget());
        analyser.updateModel(Minecraft.getInstance(), entity, 0, -entity.getBbHeight() / 2.0f, 0, 0.0f, 1.0f, graphics.pose(), 0xF000f0);
        analyser.forEachCatcher(catcher -> analyser.records().add(IVertexRecorder.buildVertices(catcher.collectVertices(), catcher.renderType())));
        analyser.analyse(entitySize, mouseX, mouseY, layers, showDisabled.getValue() == 1, disabledIdField.getValue());
        focusedUV = analyser.getFocusedUV();
        focusedTextureId = analyser.focusedTextureId();
        analyser.records().forEach(record -> IVertexRecorder.renderVertices(record.vertices(), graphics.bufferSource().getBuffer(record.renderType())));
        graphics.flush();
        if ((category & 0b1) == 0) analyser.drawNormals(graphics, entitySize);
        else analyser.previewEffect(graphics, entitySize, (category & 0b10) == 2);
        dispatcher.setRenderShadow(true);
        graphics.pose().popPose();
        Lighting.setupFor3DItems();
    }

    protected BindingTarget generateBindingTarget() {
        return new BindingTarget(nameField.getValue(), textureIdField.getValue()).priority(priorityField.getNumber())
                .forwardU(forwardUField.getNumber()).forwardV(forwardVField.getNumber())
                .upwardU(upwardUField.getNumber()).upwardV(upwardVField.getNumber())
                .posU(posUField.getNumber()).posV(posVField.getNumber())
                .disablingDepth(depthField.getNumber())
                .bindX(bindXButton.getValue() == 0).bindY(bindYButton.getValue() == 0).bindZ(bindZButton.getValue() == 0).bindRotation(bindRotButton.getValue() == 0)
                .scale(scaleField.getNumber()).offsetX(offsetXSlider.getValue()).offsetY(offsetYSlider.getValue()).offsetZ(offsetZSlider.getValue())
                .pitch((float) pitchSlider.getValue()).yaw((float) yawSlider.getValue()).roll((float) rollSlider.getValue())
                .disabledTextureIds(List.copyOf(disabledIds));
    }

    protected void loadBindingTarget(BindingTarget target) {
        if (target.isEmpty()) return;
        nameField.setValue(target.name);
        textureIdField.setValue(target.textureId);
        priorityField.setNumber(target.priority);
        forwardUField.setNumber(target.forwardU);
        forwardVField.setNumber(target.forwardV);
        upwardUField.setNumber(target.upwardU);
        upwardVField.setNumber(target.upwardV);
        posUField.setNumber(target.posU);
        posVField.setNumber(target.posV);
        depthField.setNumber(target.disablingDepth);
        scaleField.setNumber((float) target.scale);
        bindXButton.setValue(target.bindX ? 0 : 1);
        offsetXSlider.setValue(target.offsetX);
        bindYButton.setValue(target.bindY ? 0 : 1);
        offsetYSlider.setValue(target.offsetY);
        bindZButton.setValue(target.bindZ ? 0 : 1);
        offsetZSlider.setValue(target.offsetZ);
        bindRotButton.setValue(target.bindRotation ? 0 : 1);
        pitchSlider.setValue(target.pitch);
        yawSlider.setValue(target.yaw);
        rollSlider.setValue(target.roll);
        disabledIds.clear();
        disabledIds.addAll(target.disabledTextureIds);
    }

    private Button createButton(Component message, int width, Button.OnPress onPress) {
        return Button.builder(message, onPress).size(width, widgetHeight).build();
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

    private EditBox createTextField(int width, @Nullable EditBox copyFrom) {
        return new EditBox(font, 0, 0, width - 2, widgetHeight - 2, copyFrom, Component.empty());
    }

    protected boolean mouseInViewArea(double mouseX, double mouseY) {
        return mouseX >= x + (double) (xSize - ySize) / 2 && mouseX <= x + (double) (xSize + ySize) / 2 && mouseY >= y && mouseY <= y + ySize;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseInViewArea(mouseX, mouseY) && focusedUV != null && button == GLFW.GLFW_MOUSE_BUTTON_LEFT &&
                InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_ALT)) {
            if (category == 0) {
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
            } else if (category == 0b11) {
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
                yRot -= (float) (Math.PI * deltaX / ySize);
                return true;
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                entityX += deltaX / entitySize;
                entityY += deltaY / entitySize;
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
                entitySize = Mth.clamp(entitySize + (int) verticalAmount * entitySize / 16, 16, 1024);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean isPauseScreen() {
        return pauseButton.getValue() == 1;
    }
}
