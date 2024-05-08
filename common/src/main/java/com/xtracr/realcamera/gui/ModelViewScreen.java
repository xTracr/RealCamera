package com.xtracr.realcamera.gui;

import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.util.LocUtil;
import com.xtracr.realcamera.util.MathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
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
    private Vec2f focusedUV;
    private TextFieldWidget textureIdField, nameField, disabledIdField;
    private NumberFieldWidget<Float> forwardUField, forwardVField, upwardUField, upwardVField, posUField, posVField, scaleField, depthField;
    private NumberFieldWidget<Integer> priorityField;
    private List<String> idsInClipBoard = new ArrayList<>();
    private final List<String> disabledIds = new ArrayList<>();
    private final CyclingButtonWidget<Integer> selectingButton = createCyclingButton(Map.of(
                    0, LocUtil.MODEL_VIEW_WIDGET("forwardMode").styled(s -> s.withColor(Formatting.GREEN)),
                    1, LocUtil.MODEL_VIEW_WIDGET("upwardMode").styled(s -> s.withColor(Formatting.RED)),
                    2, LocUtil.MODEL_VIEW_WIDGET("posMode").styled(s -> s.withColor(Formatting.BLUE))),
            widgetWidth * 2 + 4, LocUtil.MODEL_VIEW_WIDGET("selectMode"));
    private final CyclingTexturedButton pauseButton = new CyclingTexturedButton(0, 16, 0, 2);
    private final CyclingTexturedButton bindXButton = new CyclingTexturedButton(16, 16, 1, 2);
    private final CyclingTexturedButton bindYButton = new CyclingTexturedButton(16, 16, 0, 2);
    private final CyclingTexturedButton bindZButton = new CyclingTexturedButton(16, 16, 1, 2);
    private final CyclingTexturedButton bindRotButton = new CyclingTexturedButton(16, 16, 1, 2);
    private final CyclingTexturedButton showDisabled = new CyclingTexturedButton(32, 16, 0, 2);
    private final DoubleSliderWidget entityPitchSlider = createSlider("pitch", widgetWidth * 2 + 4, -90.0, 90.0);
    private final DoubleSliderWidget entityYawSlider = createSlider("yaw", widgetWidth * 2 + 4, -60.0, 60.0);
    private final DoubleSliderWidget offsetXSlider = createSlider("offsetX", widgetWidth * 2 - 18, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
    private final DoubleSliderWidget offsetYSlider = createSlider("offsetY", widgetWidth * 2 - 18, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
    private final DoubleSliderWidget offsetZSlider = createSlider("offsetZ", widgetWidth * 2 - 18, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
    private final DoubleSliderWidget pitchSlider = createSlider("pitch", widgetWidth * 2 - 18, -180.0, 180.0);
    private final DoubleSliderWidget yawSlider = createSlider("yaw", widgetWidth * 2 - 18, -180.0, 180.0);
    private final DoubleSliderWidget rollSlider = createSlider("roll", widgetWidth * 2 - 18, -180.0, 180.0);

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
        clearChildren();
        initLeftWidgets(category);
        addDrawableChild(pauseButton).setPosition(x + (xSize - ySize) / 2 + 4, y + 4);
        addDrawableChild(new TexturedButton(x + (xSize - ySize) / 2 + 22, y + 4, 16, 16, 0, 0, button -> {
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
        String textureId = textureIdField != null ? textureIdField.getText() : "";
        textureIdField = createTextField(widgetWidth * 2 + 4, null);
        textureIdField.setMaxLength(1024);
        textureIdField.setText(textureId);
        scaleField = createFloatField(widgetWidth, 1.0f, scaleField).setMax(64.0f);
        depthField = createFloatField(widgetWidth, 0.2f, depthField).setMax(16.0f);
        GridWidget gridWidget = new GridWidget();
        gridWidget.getMainPositioner().margin(4, 2, 0, 0);
        Positioner smallPositioner = gridWidget.copyPositioner().margin(5, 3, 1, 1);
        GridWidget.Adder adder = gridWidget.createAdder(2);
        adder.add(createButton(LocUtil.MODEL_VIEW_WIDGET("settings"), widgetWidth, button -> initWidgets(0, page)));
        adder.add(createButton(LocUtil.MODEL_VIEW_WIDGET("preview"), widgetWidth, button -> initWidgets(category | 0b01, page)));
        if ((category & 0b1) == 0) {
            adder.add(entityPitchSlider, 2);
            adder.add(entityYawSlider, 2);
            adder.add(selectingButton, 2).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("selectMode"));
            adder.add(forwardUField, 1, smallPositioner);
            adder.add(forwardVField, 1, smallPositioner);
            adder.add(upwardUField, 1, smallPositioner);
            adder.add(upwardVField, 1, smallPositioner);
            adder.add(posUField, 1, smallPositioner);
            adder.add(posVField, 1, smallPositioner);
            adder.add(textureIdField, 2, smallPositioner).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("textureId"));
        } else {
            Positioner sliderPositioner = gridWidget.copyPositioner().margin(-20, 2, 0, 0);
            adder.add(bindXButton, 1, smallPositioner).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("bindButtons"));
            adder.add(offsetXSlider, 1, sliderPositioner);
            adder.add(bindYButton, 1, smallPositioner).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("bindButtons"));
            adder.add(offsetYSlider, 1, sliderPositioner);
            adder.add(bindZButton, 1, smallPositioner).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("bindButtons"));
            adder.add(offsetZSlider, 1, sliderPositioner);
            adder.add(bindRotButton, 1, smallPositioner).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("bindButtons"));
            adder.add(pitchSlider, 1, sliderPositioner);
            adder.add(yawSlider, 2, gridWidget.copyPositioner().margin(26, 2, 0, 0));
            adder.add(new TexturedButton(0, 0, button -> {
                offsetXSlider.setValue(0);
                offsetYSlider.setValue(0);
                offsetZSlider.setValue(0);
                pitchSlider.setValue(0);
                yawSlider.setValue(0);
                rollSlider.setValue(0);
            }), 1, smallPositioner);
            adder.add(rollSlider, 1, sliderPositioner);
            adder.add(scaleField, 1, smallPositioner).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("scale"));
            adder.add(depthField, 1, smallPositioner).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("depth"));
        }
        adder.add(createButton(LocUtil.MODEL_VIEW_WIDGET("save"), widgetWidth, button -> {
            ConfigFile.config().putTarget(generateBindingTarget());
            ConfigFile.save();
            initWidgets(category, page);
        }));
        adder.add(priorityField = NumberFieldWidget.ofInt(textRenderer, widgetWidth - 2, widgetHeight - 2, 0, priorityField), 1, smallPositioner).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("priority"));
        adder.add(nameField = createTextField(widgetWidth * 2 + 4, nameField), 2, smallPositioner).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("targetName"));
        nameField.setMaxLength(20);
        gridWidget.refreshPositions();
        SimplePositioningWidget.setPos(gridWidget, x, y + 2, x + (xSize - ySize) / 2 - 4, y + ySize, 0, 0);
        gridWidget.forEachChild(this::addDrawableChild);
    }

    private void initRightWidgets(final int category, final int page) {
        disabledIdField = createTextField(widgetWidth * 2 + 4, disabledIdField);
        disabledIdField.setMaxLength(1024);
        GridWidget gridWidget = new GridWidget();
        gridWidget.getMainPositioner().margin(4, 2, 0, 0);
        Positioner smallPositioner = gridWidget.copyPositioner().margin(5, 3, 1, 1);
        GridWidget.Adder adder = gridWidget.createAdder(4);
        adder.add(createButton(LocUtil.MODEL_VIEW_WIDGET("configs"), widgetWidth, button -> initWidgets(category & 0b01, 0)), 2).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("configs"));
        adder.add(createButton(LocUtil.MODEL_VIEW_WIDGET("disable"), widgetWidth, button -> initWidgets(0b11, 0)), 2).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("disable"));
        final int widgetsPerPage, size;
        if ((category & 0b10) == 0) {
            widgetsPerPage = 8;
            List<BindingTarget> targetList = ConfigFile.config().getTargetList();
            size = targetList.size();
            for (int i = page * widgetsPerPage; i < Math.min((page + 1) * widgetsPerPage, size); i++) {
                BindingTarget target = targetList.get(i);
                String name = target.name;
                adder.add(createButton(LocUtil.literal(name), widgetWidth * 2 - 18, button -> loadBindingTarget(target)), 3).setTooltip(Tooltip.of(LocUtil.literal(name)));
                adder.add(new TexturedButton(48, 0, button -> {
                    targetList.remove(target);
                    ConfigFile.save();
                    initWidgets(category, page * widgetsPerPage > size - 2 && size > 1 ? page - 1 : page);
                }), 1, smallPositioner);
            }
        } else {
            widgetsPerPage = 5;
            size = disabledIds.size();
            adder.add(createButton(LocUtil.MODEL_VIEW_WIDGET("copy"), widgetWidth, button -> idsInClipBoard = List.copyOf(disabledIds)), 2).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("copy"));
            adder.add(createButton(LocUtil.MODEL_VIEW_WIDGET("paste"), widgetWidth, button -> {
                idsInClipBoard.stream().filter(textureId -> !disabledIds.contains(textureId)).forEach(disabledIds::add);
                initWidgets(category, 0);
            }), 2);
            adder.add(disabledIdField, 4, smallPositioner).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("disabledIdField"));
            adder.add(createButton(LocUtil.MODEL_VIEW_WIDGET("clear"), widgetWidth, button -> {
                disabledIds.clear();
                initWidgets(category, 0);
            }), 2);
            adder.add(showDisabled, 1, gridWidget.copyPositioner().margin(7, 3, 1, 1));
            adder.add(new TexturedButton(64, 0, button -> {
                String disabledId = disabledIdField.getText();
                if (disabledId.isBlank() || disabledIds.contains(disabledId)) return;
                disabledIds.add(disabledId);
                initWidgets(category, page);
            }), 1, smallPositioner);
            for (int i = page * widgetsPerPage; i < Math.min((page + 1) * widgetsPerPage, size); i++) {
                String textureId = disabledIds.get(i);
                adder.add(createButton(LocUtil.literal(textureId), widgetWidth * 2 - 18, button -> disabledIdField.setText(textureId)), 3).setTooltip(Tooltip.of(LocUtil.literal(textureId)));
                adder.add(new TexturedButton(48, 0, button -> {
                    disabledIds.remove(textureId);
                    initWidgets(category, page * widgetsPerPage > size - 2 && size > 1 ? page - 1 : page);
                }), 1, smallPositioner);
            }
        }
        gridWidget.refreshPositions();
        SimplePositioningWidget.setPos(gridWidget, x + (xSize + ySize) / 2 + 4, y + 2, x + xSize, y + ySize, 0, 0);
        gridWidget.forEachChild(this::addDrawableChild);
        final int pages = (size - 1) / widgetsPerPage + 1;
        addDrawableChild(new TexturedButton(x + (xSize + ySize) / 2 + 8, y + ySize - 20, 16, 16, 16, 0, button -> initWidgets(category, (page - 1 + pages) % pages)));
        addDrawableChild(new TextWidget(x + (xSize + ySize) / 2 + 30, y + ySize - 20, widgetWidth * 2 - 40, widgetHeight, LocUtil.literal((page + 1) + " / " + pages), textRenderer));
        addDrawableChild(new TexturedButton(x + xSize - 21, y + ySize - 20, 16, 16, 32, 0, button -> initWidgets(category, (page + 1) % pages)));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);// 1.20.1 only
        drawEntity(context, x + (xSize - ySize) / 2, y, x + (xSize + ySize) / 2, y + ySize, mouseX, mouseY, client.player);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context) {
        super.renderBackground(context);
        context.fill(x, y, x + (xSize - ySize) / 2 - 4, y + ySize, 0xFF444444);
        context.fill(x + (xSize - ySize) / 2, y, x + (xSize + ySize) / 2, y + ySize, 0xFF222222);
        context.fill(x + (xSize + ySize) / 2 + 4, y, x + xSize, y + ySize, 0xFF444444);
    }

    protected void drawEntity(DrawContext context, int x1, int y1, int x2, int y2, int mouseX, int mouseY, LivingEntity entity) {
        float centerX = (float) (x1 + x2) / 2.0f;
        float centerY = (float) (y1 + y2) / 2.0f;
        context.enableScissor(x1, y1, x2, y2);
        Quaternionf quaternionf = new Quaternionf().rotateX((float) Math.PI / 6 + xRot).rotateY((float) Math.PI / 6 + yRot).rotateZ((float) Math.PI);
        float entityBodyYaw = entity.bodyYaw;
        float entityYaw = entity.getYaw();
        float entityPitch = entity.getPitch();
        float entityPrevHeadYaw = entity.prevHeadYaw;
        float entityHeadYaw = entity.headYaw;
        entity.bodyYaw = 180.0f;
        entity.setYaw(180.0f + (float) entityYawSlider.getValue());
        entity.setPitch((float) entityPitchSlider.getValue());
        entity.headYaw = entity.getYaw();
        entity.prevHeadYaw = entity.getYaw();
        Vector3f vector3f = new Vector3f((float) entityX, (float) entityY, -2.0f);
        drawEntity(context, centerX, centerY, mouseX, mouseY, vector3f, quaternionf, entity);
        entity.bodyYaw = entityBodyYaw;
        entity.setYaw(entityYaw);
        entity.setPitch(entityPitch);
        entity.prevHeadYaw = entityPrevHeadYaw;
        entity.headYaw = entityHeadYaw;
        context.disableScissor();
    }

    protected void drawEntity(DrawContext context, float x, float y, int mouseX, int mouseY, Vector3f offset, Quaternionf quaternionf, LivingEntity entity) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().multiplyPositionMatrix(new Matrix4f().scaling(entitySize, entitySize, -entitySize));
        context.getMatrices().translate(offset.x(), offset.y(), offset.z());
        context.getMatrices().multiply(quaternionf);
        DiffuseLighting.method_34742();
        EntityRenderDispatcher entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        entityRenderDispatcher.setRenderShadows(false);
        ModelAnalyser analyser = new ModelAnalyser(generateBindingTarget());
        entityRenderDispatcher.render(entity, 0, -entity.getHeight() / 2.0f, 0, 0.0f, 1.0f, context.getMatrices(), analyser, 0xF000f0);
        analyser.initialize(entitySize, mouseX, mouseY, layers, showDisabled.getValue() == 1, disabledIdField.getText());
        focusedUV = analyser.getFocusedUV();
        focusedTextureId = analyser.focusedTextureId();
        if ((category & 0b1) == 0) analyser.drawModelWithNormals(context, entitySize);
        else analyser.previewEffect(context, entitySize, (category & 0b10) == 2);
        entityRenderDispatcher.setRenderShadows(true);
        context.getMatrices().pop();
        DiffuseLighting.enableGuiDepthLighting();
    }

    protected BindingTarget generateBindingTarget() {
        return new BindingTarget(nameField.getText(), textureIdField.getText()).priority(priorityField.getValue())
                .forwardU(forwardUField.getValue()).forwardV(forwardVField.getValue())
                .upwardU(upwardUField.getValue()).upwardV(upwardVField.getValue())
                .posU(posUField.getValue()).posV(posVField.getValue())
                .disablingDepth(depthField.getValue())
                .bindX(bindXButton.getValue() == 0).bindY(bindYButton.getValue() == 0).bindZ(bindZButton.getValue() == 0).bindRotation(bindRotButton.getValue() == 0)
                .scale(scaleField.getValue()).offsetX(offsetXSlider.getValue()).offsetY(offsetYSlider.getValue()).offsetZ(offsetZSlider.getValue())
                .pitch((float) pitchSlider.getValue()).yaw((float) yawSlider.getValue()).roll((float) rollSlider.getValue())
                .disabledTextureIds(List.copyOf(disabledIds));
    }

    protected void loadBindingTarget(BindingTarget target) {
        if (target.isEmpty()) return;
        disabledIds.clear();
        disabledIds.addAll(target.disabledTextureIds);
        nameField.setText(target.name);
        textureIdField.setText(target.textureId);
        priorityField.setValue(target.priority);
        forwardUField.setValue(target.forwardU);
        forwardVField.setValue(target.forwardV);
        upwardUField.setValue(target.upwardU);
        upwardVField.setValue(target.upwardV);
        posUField.setValue(target.posU);
        posVField.setValue(target.posV);
        depthField.setValue(target.disablingDepth);
        scaleField.setValue((float) target.scale);
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
    }

    private ButtonWidget createButton(Text message, int width, ButtonWidget.PressAction onPress) {
        return ButtonWidget.builder(message, onPress).size(width, widgetHeight).build();
    }

    private CyclingButtonWidget<Integer> createCyclingButton(Map<Integer, Text> messages, int width, Text optionText) {
        return new CyclingButtonWidget.Builder<Integer>(messages::get).values(messages.keySet()).build(0, 0, width, widgetHeight, optionText);
    }

    private DoubleSliderWidget createSlider(String key, int width, double min, double max) {
        return new DoubleSliderWidget(width, widgetHeight, 0, min, max, d -> LocUtil.MODEL_VIEW_WIDGET(key, MathUtil.round(d, 2)));
    }

    private NumberFieldWidget<Float> createFloatField(int width, float defaultValue, @Nullable NumberFieldWidget<Float> copyFrom) {
        return NumberFieldWidget.ofFloat(textRenderer, width - 2, widgetHeight - 2, defaultValue, copyFrom).setMax(1.0f).setMin(0f);
    }

    private TextFieldWidget createTextField(int width, @Nullable TextFieldWidget copyFrom) {
        return new TextFieldWidget(textRenderer, 0, 0, width - 2, widgetHeight - 2, copyFrom, Text.empty());
    }

    protected boolean mouseInViewArea(double mouseX, double mouseY) {
        return mouseX >= x + (double) (xSize - ySize) / 2 && mouseX <= x + (double) (xSize + ySize) / 2 && mouseY >= y && mouseY <= y + ySize;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseInViewArea(mouseX, mouseY) && focusedUV != null && button == GLFW.GLFW_MOUSE_BUTTON_LEFT &&
                InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_ALT)) {
            if (category == 0) {
                if (selectingButton.getValue() == 0) {
                    forwardUField.setValue(focusedUV.x);
                    forwardVField.setValue(focusedUV.y);
                } else if (selectingButton.getValue() == 1) {
                    upwardUField.setValue(focusedUV.x);
                    upwardVField.setValue(focusedUV.y);
                } else {
                    posUField.setValue(focusedUV.x);
                    posVField.setValue(focusedUV.y);
                }
                textureIdField.setText(focusedTextureId);
                return true;
            } else if (category == 0b11) {
                disabledIdField.setText(focusedTextureId);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (mouseInViewArea(mouseX, mouseY)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_ALT)) {
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
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseInViewArea(mouseX, mouseY)) {
            if (InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_ALT)) {
                layers = Math.max(0, layers + (int) amount);
            } else {
                entitySize = MathHelper.clamp(entitySize + (int) amount * entitySize / 16, 16, 1024);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean shouldPause() {
        return pauseButton.getValue() == 1;
    }
}
