package com.xtracr.realcamera.gui;

import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.util.MathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class ModelViewScreen extends Screen {
    private static final String KEY_SCREEN = "screen.xtracr_" + RealCamera.MODID + "_modelView_";
    private static final String KEY_WIDGET = "screen.widget.xtracr_" + RealCamera.MODID + "_modelView_";
    private static final String KEY_TOOLTIP = "screen.tooltip.xtracr_" + RealCamera.MODID + "_modelView_";
    protected int xSize = 400, ySize = 220;
    protected int x, y;
    protected boolean shouldPause = false, showCube = false;
    private int entitySize = 80;
    private double entityX, entityY;
    private float yaw, pitch, xRot, yRot;
    private int select;
    private String focusedRenderTypeName;
    private int layers, frontIndex, upIndex, posIndex, focusedIndex = -1;
    private IntFieldWidget frontIndexWidget, upIndexWidget, posIndexWidget;
    private TextFieldWidget renderTypeWidget, nameWidget;
    private final ButtonWidget selectFrontButton = ButtonWidget.builder(Text.literal("OFF"), button -> changeSelectionTarget(0, button)).size(25, 18).build();
    private final ButtonWidget selectUpButton = ButtonWidget.builder(Text.literal("OFF"), button -> changeSelectionTarget(1, button)).size(25, 18).build();
    private final ButtonWidget selectPosButton = ButtonWidget.builder(Text.literal("OFF"), button -> changeSelectionTarget(2, button)).size(25, 18).build();
    private final ButtonWidget saveButton = ButtonWidget.builder(Text.translatable(KEY_WIDGET + "save"), button -> saveCurrent()).size((xSize - ySize)/4 - 10, 18).build();
    private final ButtonWidget loadButton = ButtonWidget.builder(Text.translatable(KEY_WIDGET + "load"), button -> loadToCurrent()).size((xSize - ySize)/4 - 10, 18).build();
    private final DoubleValueSlider yawWidget = new DoubleValueSlider((xSize - ySize)/2 - 15, 18, 0.5D,
            -60.0D, 60.0D, d -> Text.translatable(KEY_WIDGET + "yaw", MathUtil.round(d, 2)), d -> yaw = (float) d);
    private final DoubleValueSlider pitchWidget = new DoubleValueSlider((xSize - ySize)/2 - 15, 18, 0.5D,
            -90.0D, 90.0D, d -> Text.translatable(KEY_WIDGET + "pitch", MathUtil.round(d, 2)), d -> pitch = (float) d);
    private final ButtonWidget resetWidget = ButtonWidget.builder(Text.translatable(KEY_WIDGET + "reset"), button -> reset()).size((xSize - ySize)/2 - 15, 18).build();
    private final ButtonWidget pauseWidget = ButtonWidget.builder(Text.translatable(KEY_WIDGET + "pause"), button -> shouldPause = !shouldPause).size((xSize - ySize)/2 - 15, 18).build();
    private final ButtonWidget showCubeWidget = ButtonWidget.builder(Text.translatable(KEY_WIDGET + "showCube"), button -> showCube = !showCube).size((xSize - ySize)/2 - 15, 18).build();

    public ModelViewScreen() {
        super(Text.translatable(KEY_SCREEN + "title"));
    }

    @Override
    protected void init() {
        super.init();
        x = (width - xSize) / 2;
        y = (height - ySize) / 2;
        frontIndexWidget = new IntFieldWidget(textRenderer, x + 5, y + 70, (xSize - ySize)/2 - 45, 18, Text.translatable(KEY_WIDGET + "frontIndex"), frontIndex, i -> frontIndex = i);
        upIndexWidget = new IntFieldWidget(textRenderer, x + 5, y + 92, (xSize - ySize)/2 - 45, 18, Text.translatable(KEY_WIDGET + "upIndex"), upIndex, i -> upIndex = i);
        posIndexWidget = new IntFieldWidget(textRenderer, x + 5, y + 114, (xSize - ySize)/2 - 45, 18, Text.translatable(KEY_WIDGET + "posIndex"), posIndex, i -> posIndex = i);
        renderTypeWidget = new TextFieldWidget(textRenderer, x + 5, y + 136, (xSize - ySize)/2 - 15, 18, Text.translatable(KEY_WIDGET + "renderType"));
        nameWidget = new TextFieldWidget(textRenderer, x + 5, y + 180, (xSize - ySize)/2 - 15, 18, Text.translatable(KEY_WIDGET + "listName"));
        renderTypeWidget.setMaxLength(32768);
        nameWidget.setMaxLength(20);
        selectFrontButton.setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "selectFront")));
        selectUpButton.setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "selectUp")));
        selectPosButton.setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "selectPos")));
        pauseWidget.setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "pause")));
        showCubeWidget.setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "showCube")));
        addDrawableChild(resetWidget).setPosition(x + 5, y + 4);
        addDrawableChild(yawWidget).setPosition(x + 5, y + 26);
        addDrawableChild(pitchWidget).setPosition(x + 5, y + 48);
        addDrawableChild(frontIndexWidget).setTooltip(Tooltip.of(Text.translatable(KEY_WIDGET + "frontIndex")));
        addDrawableChild(selectFrontButton).setPosition(x + (xSize - ySize)/2 - 35, y + 70);
        addDrawableChild(upIndexWidget).setTooltip(Tooltip.of(Text.translatable(KEY_WIDGET + "upIndex")));
        addDrawableChild(selectUpButton).setPosition(x + (xSize - ySize)/2 - 35, y + 92);
        addDrawableChild(posIndexWidget).setTooltip(Tooltip.of(Text.translatable(KEY_WIDGET + "posIndex")));
        addDrawableChild(selectPosButton).setPosition(x + (xSize - ySize)/2 - 35, y + 114);
        addDrawableChild(saveButton).setPosition(x + 5, y + 158);
        addDrawableChild(loadButton).setPosition(x + (xSize - ySize)/4, y + 158);
        addDrawableChild(renderTypeWidget).setTooltip(Tooltip.of(Text.translatable(KEY_WIDGET + "renderType")));
        addDrawableChild(nameWidget).setTooltip(Tooltip.of(Text.translatable(KEY_WIDGET + "listName")));
        addDrawableChild(pauseWidget).setPosition(x + (xSize + ySize) / 2 + 10, y + 4);
        addDrawableChild(showCubeWidget).setPosition(x + (xSize + ySize) / 2 + 10, y + 26);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);// 1.20.1 only
        super.render(context, mouseX, mouseY, delta);
        drawEntity(context, x + (xSize - ySize) / 2, y, x + (xSize + ySize) / 2, y + ySize, mouseX, mouseY, this.client.player);
    }

    @Override
    public void renderBackground(DrawContext context) {
        super.renderBackground(context);
        context.fill(x, y, x + (xSize - ySize) / 2 - 5, y + ySize, 0xFF555555);
        context.fill(x + (xSize - ySize) / 2, y, x + (xSize + ySize) / 2, y + ySize, 0xFF222222);
        context.fill(x + (xSize + ySize) / 2 + 5, y, x + xSize, y + ySize, 0xFF555555);
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
        entity.setYaw(180.0f + yaw);
        entity.setPitch(pitch);
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
        analyser.drawByAnother(context.getVertexConsumers(), null, null); // TODO
        context.draw();
        analyser.setCurrent(renderLayer -> renderLayer.toString().equals(renderTypeWidget.getText()), 0);
        focusedIndex = analyser.getFocusedIndex(mouseX, mouseY, layers);
        focusedRenderTypeName = analyser.focusedRenderLayerName();
        analyser.drawQuad(context, posIndex, 0x6F3333CC, false);
        if (focusedIndex > -1) {
            if (showCube) analyser.drawPolyhedron(context, focusedIndex, 0x5FFFFFFF);
            else analyser.drawQuad(context, focusedIndex, 0x7FFFFFFF, true);
        }
        analyser.drawNormal(context, frontIndex, entitySize / 2, 0xFF00CC00);
        analyser.drawNormal(context, upIndex, entitySize / 2, 0xFFCC0000);
        entityRenderDispatcher.setRenderShadows(true);
        context.getMatrices().pop();
        DiffuseLighting.enableGuiDepthLighting();
    }

    private void reset() {
        entitySize = 80;
        yawWidget.setValue(0);
        pitchWidget.setValue(0);
        entityX = entityY = 0;
        xRot = yRot = 0;
        layers = 0;
    }

    private void saveCurrent() {
        String name = nameWidget.getText();
        if (name == null) return;
        ConfigFile.modConfig.binding.targetMap.put(name,
                new ModConfig.Binding.Target(renderTypeWidget.getText(), frontIndex, upIndex, posIndex));
        ConfigFile.save();
    }

    private void loadToCurrent() {
        String name = nameWidget.getText();
        ModConfig.Binding.Target target = ConfigFile.modConfig.binding.targetMap.get(name);
        try {
            renderTypeWidget.setText(target.renderTypeName());
            frontIndexWidget.setValue(target.frontIndex());
            upIndexWidget.setValue(target.upIndex());
            posIndexWidget.setValue(target.posIndex());
        } catch (Exception ignored) {
        }
    }

    private void changeSelectionTarget(int target, ButtonWidget button) {
        select ^= 1 << target;
        if ((select >> target & 1) != 0) button.setMessage(Text.literal("ON").styled(style -> style.withColor(Formatting.GREEN)));
        else button.setMessage(Text.literal("OFF"));
    }

    protected boolean mouseInViewArea(double mouseX, double mouseY) {
        return mouseX >= x + (double) (xSize - ySize) / 2 && mouseX <= x + (double) (xSize + ySize) / 2 && mouseY >= y && mouseY <= y + ySize;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseInViewArea(mouseX, mouseY) && focusedIndex > -1  && button == GLFW.GLFW_MOUSE_BUTTON_LEFT &&
                InputUtil.isKeyPressed(this.client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_ALT)) {
            if ((select & 1) != 0) frontIndexWidget.setValue(focusedIndex);
            if ((select >> 1 & 1) != 0) upIndexWidget.setValue(focusedIndex);
            if ((select >> 2 & 1) != 0) posIndexWidget.setValue(focusedIndex);
            if (select != 0) {
                renderTypeWidget.setText(focusedRenderTypeName);
                return true;
            }
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
