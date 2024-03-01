package com.xtracr.realcamera.gui;

import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.config.ConfigFile;
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

import java.util.LinkedHashMap;
import java.util.Map;

public class ModelViewScreen extends Screen {
    private static final String KEY_WIDGET = "screen.widget." + RealCamera.FULL_ID + ".modelView_";
    private static final String KEY_TOOLTIP = "screen.tooltip." + RealCamera.FULL_ID + ".modelView_";
    protected int xSize = 420, ySize = 220, widgetWidth = (xSize - ySize) / 4 - 8, widgetHeight = 18;
    protected int x, y;
    private boolean initialized;
    private int entitySize = 80, layers = 0, category = 0, page = 0;
    private double entityX, entityY;
    private float xRot, yRot;
    private String focusedTextureId;
    private Vec2f focusedUV;
    private TextFieldWidget textureIdField, nameField;
    private NumberFieldWidget<Float> forwardUField, forwardVField, upwardUField, upwardVField, posUField, posVField, scaleField, depthField;
    private final CyclingButtonWidget<Integer> selectingButton = createCyclingButton(Map.of(
                    0, Text.translatable(KEY_WIDGET + "forwardMode").styled(s -> s.withColor(Formatting.GREEN)),
                    1, Text.translatable(KEY_WIDGET + "upwardMode").styled(s -> s.withColor(Formatting.RED)),
                    2, Text.translatable(KEY_WIDGET + "posMode").styled(s -> s.withColor(Formatting.BLUE))),
            widgetWidth * 2 + 4, Text.translatable(KEY_WIDGET + "selectMode"));
    private final CyclingTexturedButton pauseButton = new CyclingTexturedButton(0, 0, 2);
    private final CyclingTexturedButton bindXButton = new CyclingTexturedButton(16, 0, 2);
    private final CyclingTexturedButton bindYButton = new CyclingTexturedButton(16, 0, 2);
    private final CyclingTexturedButton bindZButton = new CyclingTexturedButton(16, 0, 2);
    private final CyclingTexturedButton bindRotButton = new CyclingTexturedButton(16, 0, 2);
    private final DoubleSliderWidget entityPitchSlider = createSlider("pitch", widgetWidth * 2 + 4, -90.0d, 90.0d);
    private final DoubleSliderWidget entityYawSlider = createSlider("yaw", widgetWidth * 2 + 4, -60.0d, 60.0d);
    private final DoubleSliderWidget offsetXSlider = createSlider("offsetX", widgetWidth * 2 - 18, -1.0d, 1.0d);
    private final DoubleSliderWidget offsetYSlider = createSlider("offsetY", widgetWidth * 2 - 18, -1.0d, 1.0d);
    private final DoubleSliderWidget offsetZSlider = createSlider("offsetZ", widgetWidth * 2 - 18, -1.0d, 1.0d);
    private final DoubleSliderWidget pitchSlider = createSlider("pitch", widgetWidth * 2 - 18, -180.0d, 180.0d);
    private final DoubleSliderWidget yawSlider = createSlider("yaw", widgetWidth * 2 - 18, -180.0d, 180.0d);
    private final DoubleSliderWidget rollSlider = createSlider("roll", widgetWidth * 2 - 18, -180.0d, 180.0d);

    public ModelViewScreen() {
        super(Text.translatable("screen." + RealCamera.FULL_ID + ".modelView_title"));
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
        addDrawableChild(new TexturedButton(x + (xSize - ySize) / 2 + 22, y + 4, 16, 16, 32, 0, button -> {
            entitySize = 80;
            entityYawSlider.setValue(0);
            entityPitchSlider.setValue(0);
            entityX = entityY = 0;
            xRot = yRot = 0;
            layers = 0;
        }));
        initRightWidgets(page);
    }

    private void initLeftWidgets(final int category) {
        GridWidget gridWidget = new GridWidget();
        gridWidget.getMainPositioner().margin(4, 2, 0, 0);
        Positioner smallPositioner = gridWidget.copyPositioner().margin(5, 3, 1, 1);
        GridWidget.Adder adder = gridWidget.createAdder(2);
        adder.add(createButton(Text.translatable(KEY_WIDGET + "settings"), widgetWidth, button -> initWidgets(0, page)));
        adder.add(createButton(Text.translatable(KEY_WIDGET + "preview"), widgetWidth, button -> initWidgets(1, page)));
        forwardUField = createFloatField(widgetWidth, 0, forwardUField);
        forwardVField = createFloatField(widgetWidth, 0, forwardVField);
        upwardUField = createFloatField(widgetWidth, 0, upwardUField);
        upwardVField = createFloatField(widgetWidth, 0, upwardVField);
        posUField = createFloatField(widgetWidth, 0, posUField);
        posVField = createFloatField(widgetWidth, 0, posVField);
        textureIdField = createTextField(widgetWidth * 2 + 4, textureIdField);
        textureIdField.setMaxLength(1024);
        scaleField = createFloatField(widgetWidth, 1.0f, scaleField).setMax(64.0f).setMin(0.0f);
        depthField = createFloatField(widgetWidth, 0.2f, depthField).setMax(4.0f).setMin(0.0f);
        if (category == 0) {
            adder.add(entityPitchSlider, 2);
            adder.add(entityYawSlider, 2);
            adder.add(selectingButton, 2).setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "selectMode")));
            adder.add(forwardUField, 1, smallPositioner);
            adder.add(forwardVField, 1, smallPositioner);
            adder.add(upwardUField, 1, smallPositioner);
            adder.add(upwardVField, 1, smallPositioner);
            adder.add(posUField, 1, smallPositioner);
            adder.add(posVField, 1, smallPositioner);
            adder.add(textureIdField, 2, smallPositioner).setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "textureId")));
        } else if (category == 1) {
            Positioner sliderPositioner = gridWidget.copyPositioner().margin(-20, 2, 0, 0);
            adder.add(bindXButton, 1, smallPositioner).setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "bindButtons")));
            adder.add(offsetXSlider, 1, sliderPositioner);
            adder.add(bindYButton, 1, smallPositioner).setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "bindButtons")));
            adder.add(offsetYSlider, 1, sliderPositioner);
            adder.add(bindZButton, 1, smallPositioner).setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "bindButtons")));
            adder.add(offsetZSlider, 1, sliderPositioner);
            adder.add(bindRotButton, 1, smallPositioner).setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "bindButtons")));
            adder.add(pitchSlider, 1, sliderPositioner);
            adder.add(yawSlider, 2, gridWidget.copyPositioner().margin(26, 2, 0, 0));
            adder.add(rollSlider, 2, gridWidget.copyPositioner().margin(26, 2, 0, 0));
            adder.add(scaleField, 1, smallPositioner).setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "scale")));
            adder.add(depthField, 1, smallPositioner).setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "depth")));
        }
        adder.add(createButton(Text.translatable(KEY_WIDGET + "save"), widgetWidth, button -> {
            ConfigFile.modConfig.putTarget(generateBindingTarget());
            ConfigFile.save();
            initWidgets(category, page);
        }));
        adder.add(createButton(Text.translatable(KEY_WIDGET + "bind"), widgetWidth, button -> {
            ConfigFile.modConfig.binding.nameOfList = nameField.getText();
            ConfigFile.save();
            initWidgets(category, page);
        })).setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "bind", "Auto Bind")));
        adder.add(nameField = createTextField(widgetWidth * 2 + 4, nameField), 2, smallPositioner)
                .setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "listName")));
        nameField.setMaxLength(20);
        gridWidget.refreshPositions();
        SimplePositioningWidget.setPos(gridWidget, x, y + 2, x + (xSize - ySize) / 2 - 4, y + ySize, 0, 0);
        gridWidget.forEachChild(this::addDrawableChild);
    }

    private void initRightWidgets(final int page) {
        LinkedHashMap<String, BindingTarget> targetMap = ConfigFile.modConfig.binding.targetMap;
        final int widgetsPerPage = 6, pages = (targetMap.size() - 1) / widgetsPerPage + 1;
        GridWidget gridWidget = new GridWidget();
        gridWidget.getMainPositioner().margin(4, 2, 0, 0);
        Positioner smallPositioner = gridWidget.copyPositioner().margin(5, 3, 1, 1);
        GridWidget.Adder adder = gridWidget.createAdder(2);
        addDrawableChild(new TexturedButton(x + (xSize + ySize) / 2 + 8, y + 4, 16, 16, 0, 32, button -> initWidgets(category, (page - 1 + pages) % pages)));
        addDrawableChild(new TextWidget(x + (xSize + ySize) / 2 + 30, y + 4, widgetWidth * 2 - 40, widgetHeight, Text.of((page + 1) + " / " + pages), textRenderer));
        addDrawableChild(new TexturedButton(x + xSize - 21, y + 4, 16, 16, 16, 32, button -> initWidgets(category, (page + 1) % pages)));
        String[] names = targetMap.keySet().toArray(String[]::new);
        for (int i = page * widgetsPerPage; i < Math.min((page + 1) * widgetsPerPage, targetMap.size()); i++) {
            String name = names[i];
            BindingTarget target = targetMap.get(name);
            adder.add(createButton(Text.literal(name).styled(s -> name.equals(ConfigFile.modConfig.binding.nameOfList) ? s.withColor(Formatting.GREEN) : s),
                    widgetWidth * 2 - 18, button -> loadBindingTarget(target)));
            adder.add(new TexturedButton(32, 32, button -> {
                targetMap.remove(name);
                ConfigFile.save();
                initWidgets(category, page * widgetsPerPage >= targetMap.size() && !targetMap.isEmpty() ? page - 1 : page);
            }), 1, smallPositioner);
        }
        gridWidget.refreshPositions();
        SimplePositioningWidget.setPos(gridWidget, x + (xSize + ySize) / 2 + 4, y + 22, x + xSize, y + ySize, 0, 0);
        gridWidget.forEachChild(this::addDrawableChild);
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
        ModelAnalyser analyser = new ModelAnalyser();
        entityRenderDispatcher.render(entity, 0, -entity.getHeight() / 2.0f, 0, 0.0f, 1.0f, context.getMatrices(), analyser, 0xF000f0);
        analyser.buildLastRecord();
        analyser.setCurrent(renderLayer -> renderLayer.toString().contains(textureIdField.getText()));
        int focusedIndex = analyser.getFocusedIndex(mouseX, mouseY, layers);
        focusedUV = analyser.getCenterUV(focusedIndex);
        focusedTextureId = analyser.focusedTextureId();
        if (category == 0) {
            analyser.drawByAnother(context.getVertexConsumers());
            context.draw();
            analyser.drawQuad(context, posUField.getValue(), posVField.getValue(), 0x6F3333CC);
            if (focusedIndex != -1) analyser.drawPolyhedron(context, focusedIndex, 0x7FFFFFFF, 0x3FFFFFFF);
            analyser.drawNormal(context, forwardUField.getValue(), forwardVField.getValue(), entitySize / 2, 0xFF00CC00);
            analyser.drawNormal(context, upwardUField.getValue(), upwardVField.getValue(), entitySize / 2, 0xFFCC0000);
        } else analyser.preview(context, generateBindingTarget(), entitySize, 0xFF00CC00, 0xFFCC0000, 0xFF0000CC);
        entityRenderDispatcher.setRenderShadows(true);
        context.getMatrices().pop();
        DiffuseLighting.enableGuiDepthLighting();
    }

    protected BindingTarget generateBindingTarget() {
        return new BindingTarget(nameField.getText(), textureIdField.getText(), forwardUField.getValue(), forwardVField.getValue(), upwardUField.getValue(),
                upwardVField.getValue(), posUField.getValue(), posVField.getValue(), depthField.getValue(),
                bindXButton.getValue() == 0, bindYButton.getValue() == 0, bindZButton.getValue() == 0, bindRotButton.getValue() == 0,
                scaleField.getValue(), offsetXSlider.getValue(), offsetYSlider.getValue(), offsetZSlider.getValue(),
                (float) pitchSlider.getValue(), (float) yawSlider.getValue(), (float) rollSlider.getValue());
    }

    protected void loadBindingTarget(BindingTarget target) {
        if (target.isEmpty()) return;
        nameField.setText(target.name());
        textureIdField.setText(target.textureId());
        forwardUField.setValue(target.forwardU());
        forwardVField.setValue(target.forwardV());
        upwardUField.setValue(target.upwardU());
        upwardVField.setValue(target.upwardV());
        posUField.setValue(target.posU());
        posVField.setValue(target.posV());
        depthField.setValue(target.disablingDepth());
        scaleField.setValue((float) target.scale());
        bindXButton.setValue(target.bindX() ? 0 : 1);
        offsetXSlider.setValue(target.offsetX());
        bindYButton.setValue(target.bindY() ? 0 : 1);
        offsetYSlider.setValue(target.offsetY());
        bindZButton.setValue(target.bindZ() ? 0 : 1);
        offsetZSlider.setValue(target.offsetZ());
        bindRotButton.setValue(target.bindRotation() ? 0 : 1);
        pitchSlider.setValue(target.pitch());
        yawSlider.setValue(target.yaw());
        rollSlider.setValue(target.roll());
    }

    private ButtonWidget createButton(Text message, int width, ButtonWidget.PressAction onPress) {
        return ButtonWidget.builder(message, onPress).size(width, widgetHeight).build();
    }

    private CyclingButtonWidget<Integer> createCyclingButton(Map<Integer, Text> messages, int width, Text optionText) {
        return new CyclingButtonWidget.Builder<Integer>(messages::get).values(messages.keySet()).build(0, 0, width, widgetHeight, optionText);
    }

    private DoubleSliderWidget createSlider(String key, int width, double min, double max) {
        return new DoubleSliderWidget(width, widgetHeight, 0, min, max, d -> Text.translatable(KEY_WIDGET + key, MathUtil.round(d, 2)));
    }

    private NumberFieldWidget<Float> createFloatField(int width, float defaultValue, @Nullable NumberFieldWidget<Float> copyFrom) {
        return new FloatFieldWidget(textRenderer, width - 2, widgetHeight - 2, defaultValue, copyFrom, Text.empty()).setMax(1.0f).setMin(0f);
    }

    private TextFieldWidget createTextField(int width, @Nullable TextFieldWidget copyFrom) {
        return new TextFieldWidget(textRenderer, 0, 0, width - 2, widgetHeight - 2, copyFrom, Text.empty());
    }

    protected boolean mouseInViewArea(double mouseX, double mouseY) {
        return mouseX >= x + (double) (xSize - ySize) / 2 && mouseX <= x + (double) (xSize + ySize) / 2 && mouseY >= y && mouseY <= y + ySize;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseInViewArea(mouseX, mouseY) && category == 0 && focusedUV != null && button == GLFW.GLFW_MOUSE_BUTTON_LEFT &&
                InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_ALT)) {
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
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (mouseInViewArea(mouseX, mouseY)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_ALT)) {
                xRot = MathHelper.wrapDegrees(xRot + (float) deltaY / 90f);
                yRot = MathHelper.wrapDegrees(yRot - (float) deltaX / 90f);
                return true;
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                entityX = entityX + deltaX / entitySize;
                entityY = entityY + deltaY / entitySize;
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
