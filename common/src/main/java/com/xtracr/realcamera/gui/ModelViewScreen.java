package com.xtracr.realcamera.gui;

import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
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
    private static final String KEY_WIDGET = "screen.widget.xtracr_" + RealCamera.MODID + "_modelView_";
    private static final String KEY_TOOLTIP = "screen.tooltip.xtracr_" + RealCamera.MODID + "_modelView_";
    protected int xSize = 420, ySize = 220, widgetWidth = (xSize - ySize) / 4 - 8, widgetHeight = 18;
    protected int x, y;
    private boolean shouldPause = false;
    private int entitySize = 80, layers = 0, currentPage = 0;
    private double entityX, entityY;
    private float xRot, yRot;
    private String focusedTextureId;
    private Vec2f focusedUV;
    private CyclingButtonWidget<Integer> selectingButton;
    private NumberFieldWidget<Float> forwardUField, forwardVField, upwardUField, upwardVField, posUField, posVField;
    private TextFieldWidget textureIdField, nameField;
    private final DoubleSliderWidget yawSlider = new DoubleSliderWidget(widgetWidth * 2 + 4, widgetHeight, 0,
            -60.0D, 60.0D, d -> Text.translatable(KEY_WIDGET + "yaw", MathUtil.round(d, 2)));
    private final DoubleSliderWidget pitchSlider = new DoubleSliderWidget(widgetWidth * 2 + 4, widgetHeight, 0,
                    -90.0D, 90.0D, d -> Text.translatable(KEY_WIDGET + "pitch", MathUtil.round(d, 2)));

    public ModelViewScreen() {
        super(Text.translatable("screen.xtracr_" + RealCamera.MODID + "_modelView_title"));
    }

    @Override
    protected void init() {
        super.init();
        x = (width - xSize) / 2;
        y = (height - ySize) / 2;
        initWidgets(currentPage);
    }

    private void initWidgets(int page) {
        this.currentPage = page;
        clearChildren();
        initLeftWidgets();
        addDrawableChild(new TexturedButton(x + (xSize - ySize) / 2 + 4, y + 4, 16, 16, 0, 0, button -> {
            shouldPause = !shouldPause;
            if (shouldPause) button.setUV(16, 0);
            else button.setUV(0, 0);
        }));
        addDrawableChild(new TexturedButton(x + (xSize - ySize) / 2 + 22, y + 4, 16, 16, 32, 0, button -> {
            entitySize = 80;
            yawSlider.setValue(0);
            pitchSlider.setValue(0);
            entityX = entityY = 0;
            xRot = yRot = 0;
            layers = 0;
        }));
        initRightWidgets(page);
    }

    private void initLeftWidgets() {
        GridWidget gridWidget = new GridWidget();
        gridWidget.getMainPositioner().margin(4, 2, 0, 0);
        Positioner fieldPositioner = gridWidget.copyPositioner().margin(5, 3, 1, 1);
        GridWidget.Adder adder = gridWidget.createAdder(2);
        adder.add(yawSlider, 2);
        adder.add(pitchSlider, 2);
        adder.add(selectingButton = createCyclingButton(Map.of(0 ,Text.translatable(KEY_WIDGET + "forwardMode").styled(s -> s.withColor(Formatting.GREEN)),
                        1, Text.translatable(KEY_WIDGET + "upwardMode").styled(s -> s.withColor(Formatting.RED)),
                        2, Text.translatable(KEY_WIDGET + "posMode").styled(s -> s.withColor(Formatting.BLUE))), widgetWidth * 2 + 4,
                Text.translatable(KEY_WIDGET + "selectMode")), 2).setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "selectMode")));
        adder.add(forwardUField = createFloatField(widgetWidth, forwardUField), 1, fieldPositioner);
        adder.add(forwardVField = createFloatField(widgetWidth, forwardVField), 1, fieldPositioner);
        adder.add(upwardUField = createFloatField(widgetWidth, upwardUField), 1, fieldPositioner);
        adder.add(upwardVField = createFloatField(widgetWidth, upwardVField), 1, fieldPositioner);
        adder.add(posUField = createFloatField(widgetWidth, posUField), 1, fieldPositioner);
        adder.add(posVField = createFloatField(widgetWidth, posVField), 1, fieldPositioner);
        adder.add(textureIdField = createTextField(widgetWidth * 2 + 4, textureIdField),2, fieldPositioner)
                .setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "textureId")));
        textureIdField.setMaxLength(1024);
        adder.add(createButton(Text.translatable(KEY_WIDGET + "save"), widgetWidth, button -> {
            String name = nameField.getText();
            if (name == null) return;
            ConfigFile.modConfig.binding.targetMap.put(name, new ModConfig.Binding.Target(textureIdField.getText(),
                    forwardUField.getValue(), forwardVField.getValue(), upwardUField.getValue(), upwardVField.getValue(),
                    posUField.getValue(), posVField.getValue()));
            ConfigFile.save();
            initWidgets(currentPage);
        }));
        adder.add(createButton(Text.translatable(KEY_WIDGET + "bind"), widgetWidth, button -> {
            ConfigFile.modConfig.binding.nameOfList = nameField.getText();
            initWidgets(currentPage);
        })).setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "bind", "Auto Bind")));
        adder.add(nameField = createTextField(widgetWidth * 2 + 4, nameField),2, fieldPositioner)
                .setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "listName")));
        nameField.setMaxLength(20);
        gridWidget.refreshPositions();
        SimplePositioningWidget.setPos(gridWidget, x, y + 2, x + (xSize - ySize) / 2 - 4, y + ySize, 0, 0);
        gridWidget.forEachChild(this::addDrawableChild);
    }

    private void initRightWidgets(final int page) {
        LinkedHashMap<String, ModConfig.Binding.Target> targetMap = ConfigFile.modConfig.binding.targetMap;
        int pageCount = (targetMap.size() - 1) / 6 + 1;
        GridWidget gridWidget = new GridWidget();
        gridWidget.getMainPositioner().margin(4, 2, 0, 0);
        Positioner texturedPositioner = gridWidget.copyPositioner().margin(5, 3, 1, 1);
        GridWidget.Adder adder = gridWidget.createAdder(3);
        adder.add(new TexturedButton(0, 16, button -> initWidgets((page - 1 + pageCount) % pageCount)), 1, texturedPositioner);
        adder.add(new TextWidget(18, widgetHeight, Text.of((page + 1) + " / " + pageCount), textRenderer));
        adder.add(new TexturedButton(16, 16, button -> initWidgets((page + 1) % pageCount)), 1, texturedPositioner);
        String[] names = targetMap.keySet().toArray(String[]::new);
        for (int i = page * 6; i < Math.min(page * 6 + 6, targetMap.size()); i++) {
            String name = names[i];
            ModConfig.Binding.Target target = targetMap.get(name);
            adder.add(createButton(Text.literal(name).styled(s -> name.equals(ConfigFile.modConfig.binding.nameOfList) ? s.withColor(Formatting.GREEN) : s),
                    widgetWidth * 2 - 18, button -> {
                nameField.setText(name);
                textureIdField.setText(target.textureId());
                forwardUField.setValue(target.forwardU());
                forwardVField.setValue(target.forwardV());
                upwardUField.setValue(target.upwardU());
                upwardVField.setValue(target.upwardV());
                posUField.setValue(target.posU());
                posVField.setValue(target.posV());
            }), 2);
            adder.add(new TexturedButton(32, 16, button -> {
                targetMap.remove(name);
                initWidgets(page * 6 >= targetMap.size() ? page - 1 : page);
            }), 1, texturedPositioner);
        }
        gridWidget.refreshPositions();
        SimplePositioningWidget.setPos(gridWidget, x + (xSize + ySize) / 2 + 4, y + 2, x + xSize, y + ySize, 0, 0);
        gridWidget.forEachChild(this::addDrawableChild);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);// 1.20.1 only
        drawEntity(context, x + (xSize - ySize) / 2, y, x + (xSize + ySize) / 2, y + ySize, mouseX, mouseY, this.client.player);
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
        float centerX = (float)(x1 + x2) / 2.0f;
        float centerY = (float)(y1 + y2) / 2.0f;
        context.enableScissor(x1, y1, x2, y2);
        Quaternionf quaternionf = new Quaternionf().rotateX((float) Math.PI/6 + xRot).rotateY((float) Math.PI/6 + yRot).rotateZ((float) Math.PI);
        float entityBodyYaw = entity.bodyYaw;
        float entityYaw = entity.getYaw();
        float entityPitch = entity.getPitch();
        float entityPrevHeadYaw = entity.prevHeadYaw;
        float entityHeadYaw = entity.headYaw;
        entity.bodyYaw = 180.0f;
        entity.setYaw(180.0f + (float) yawSlider.getValue());
        entity.setPitch((float) pitchSlider.getValue());
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
        entityRenderDispatcher.render(entity, 0, -entity.getHeight() / 2.0f, 0, 0.0f, 1.0f, context.getMatrices(), analyser, 0xF000F0);
        analyser.buildLastRecord();
        analyser.drawByAnother(context.getVertexConsumers(), renderLayer -> true, (renderLayer, vertices) -> true); // TODO
        context.draw();
        analyser.setCurrent(renderLayer -> renderLayer.toString().contains(textureIdField.getText()));
        int focusedIndex = analyser.getFocusedIndex(mouseX, mouseY, layers);
        focusedUV = analyser.getCenterUV(focusedIndex);
        focusedTextureId = analyser.focusedTextureId();
        analyser.drawQuad(context, posUField.getValue(), posVField.getValue(), 0x6F3333CC);
        if (focusedIndex != -1) analyser.drawPolyhedron(context, focusedIndex, 0x7FFFFFFF, 0x2FFFFFFF);
        analyser.drawNormal(context, forwardUField.getValue(), forwardVField.getValue(), entitySize / 2, 0xFF00CC00);
        analyser.drawNormal(context, upwardUField.getValue(), upwardVField.getValue(), entitySize / 2, 0xFFCC0000);
        entityRenderDispatcher.setRenderShadows(true);
        context.getMatrices().pop();
        DiffuseLighting.enableGuiDepthLighting();
    }

    private ButtonWidget createButton(Text message, int width, ButtonWidget.PressAction onPress) {
        return ButtonWidget.builder(message, onPress).size(width, widgetHeight).build();
    }

    private CyclingButtonWidget<Integer> createCyclingButton(Map<Integer, Text> messages, int width, Text optionText) {
        return new CyclingButtonWidget.Builder<Integer>(messages::get).values(messages.keySet()).build(0, 0, width, widgetHeight, optionText);
    }

    private NumberFieldWidget<Float> createFloatField(int width, @Nullable NumberFieldWidget<Float> copyFrom) {
        return new FloatFieldWidget(textRenderer, width - 2, widgetHeight - 2, copyFrom, Text.empty()).setMax(1.0f).setMin(0f);
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
                InputUtil.isKeyPressed(this.client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_ALT)) {
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
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !InputUtil.isKeyPressed(this.client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_ALT)) {
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
            if (InputUtil.isKeyPressed(this.client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_ALT)) {
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
        return shouldPause;
    }
}
