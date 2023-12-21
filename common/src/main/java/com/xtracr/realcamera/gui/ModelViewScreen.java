package com.xtracr.realcamera.gui;

import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.MathUtil;
import com.xtracr.realcamera.util.ModelAnalyser;
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

import java.util.ArrayList;
import java.util.List;

public class ModelViewScreen extends Screen {
    private static final String KEY_SCREEN = "screen.xtracr_" + RealCamera.MODID + "_modelView_";
    private static final String KEY_WIDGET = "screen.widget.xtracr_" + RealCamera.MODID + "_modelView_";
    private static final String KEY_TOOLTIP = "screen.tooltip.xtracr_" + RealCamera.MODID + "_modelView_";
    protected int xSize = 400, ySize = 220;
    protected int x, y;
    private int entitySize = 80;
    private double entityX, entityY;
    private float yaw, pitch, xRot, yRot;
    private boolean selectingFront, selectingUp, selectingPos;
    private List<Integer> focusedPolygon, posPolygon;
    private int layers, frontIndex, upIndex, posIndex;
    private IntFieldWidget frontIndexWidget, upIndexWidget, posIndexWidget;
    private TextFieldWidget nameWidget;
    private final ButtonWidget selectFrontButton = ButtonWidget.builder(Text.literal("OFF"), button -> changeSelectionTarget(0)).size(25, 18).build();
    private final ButtonWidget selectUpButton = ButtonWidget.builder(Text.literal("OFF"), button -> changeSelectionTarget(1)).size(25, 18).build();
    private final ButtonWidget selectPosButton = ButtonWidget.builder(Text.literal("OFF"), button -> changeSelectionTarget(2)).size(25, 18).build();
    private final ButtonWidget saveButton = ButtonWidget.builder(Text.translatable(KEY_WIDGET + "save"), button -> saveCurrent()).size((xSize - ySize)/4 - 10, 18).build();
    private final ButtonWidget loadButton = ButtonWidget.builder(Text.translatable(KEY_WIDGET + "load"), button -> loadToCurrent()).size((xSize - ySize)/4 - 10, 18).build();
    private final DoubleValueSlider yawWidget = new DoubleValueSlider((xSize - ySize)/2 - 15, 18, 0.5D,
            -60.0D, 60.0D, d -> Text.translatable(KEY_WIDGET + "yaw", MathUtil.round(d, 2)), d -> yaw = (float) d);
    private final DoubleValueSlider pitchWidget = new DoubleValueSlider((xSize - ySize)/2 - 15, 18, 0.5D,
            -90.0D, 90.0D, d -> Text.translatable(KEY_WIDGET + "pitch", MathUtil.round(d, 2)), d -> pitch = (float) d);
    private final ButtonWidget resetWidget = ButtonWidget.builder(Text.translatable(KEY_WIDGET + "reset"), button -> reset()).size((xSize - ySize)/2 - 15, 18).build();

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
        nameWidget = new TextFieldWidget(textRenderer, x + 5, y + 158, (xSize - ySize)/2 - 15, 18, Text.translatable(KEY_WIDGET + "listName"));
        nameWidget.setMaxLength(20);
        selectFrontButton.setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "selectFront")));
        selectUpButton.setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "selectUp")));
        selectPosButton.setTooltip(Tooltip.of(Text.translatable(KEY_TOOLTIP + "selectPos")));
        addDrawableChild(resetWidget).setPosition(x + 5, y + 4);
        addDrawableChild(yawWidget).setPosition(x + 5, y + 26);
        addDrawableChild(pitchWidget).setPosition(x + 5, y + 48);
        addDrawableChild(frontIndexWidget).setTooltip(Tooltip.of(Text.translatable(KEY_WIDGET + "frontIndex")));
        addDrawableChild(selectFrontButton).setPosition(x + (xSize - ySize)/2 - 35, y + 70);
        addDrawableChild(upIndexWidget).setTooltip(Tooltip.of(Text.translatable(KEY_WIDGET + "upIndex")));
        addDrawableChild(selectUpButton).setPosition(x + (xSize - ySize)/2 - 35, y + 92);
        addDrawableChild(posIndexWidget).setTooltip(Tooltip.of(Text.translatable(KEY_WIDGET + "posIndex")));
        addDrawableChild(selectPosButton).setPosition(x + (xSize - ySize)/2 - 35, y + 114);
        addDrawableChild(saveButton).setPosition(x + 5, y + 136);
        addDrawableChild(loadButton).setPosition(x + (xSize - ySize)/4, y + 136);
        addDrawableChild(nameWidget).setTooltip(Tooltip.of(Text.translatable(KEY_WIDGET + "listName")));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(x, y, x + (xSize - ySize) / 2 - 5, y + ySize, 0xEE505050);
        context.fill(x + (xSize - ySize) / 2, y, x + (xSize + ySize) / 2, y + ySize, 0xFF222222);
        context.fill(x + (xSize + ySize) / 2 + 5, y, x + xSize, y + ySize, 0xEE505050);
        super.render(context, mouseX, mouseY, delta);
        drawEntity(context, x + (xSize - ySize) / 2, y, x + (xSize + ySize) / 2, y + ySize, mouseX, mouseY, this.client.player);
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
        drawEntity(context, centerX, centerY, entitySize, mouseX, mouseY, vector3f, quaternionf, entity);
        entity.bodyYaw = entityBodyYaw;
        entity.setYaw(entityYaw);
        entity.setPitch(entityPitch);
        entity.prevHeadYaw = entityPrevHeadYaw;
        entity.headYaw = entityHeadYaw;
        context.disableScissor();
    }

    protected void drawEntity(DrawContext context, float x, float y, int size, int mouseX, int mouseY, Vector3f offset, Quaternionf quaternionf, LivingEntity entity) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().multiplyPositionMatrix(new Matrix4f().scaling(size, size, -size));
        context.getMatrices().translate(offset.x(), offset.y(), offset.z());
        context.getMatrices().multiply(quaternionf);
        DiffuseLighting.method_34742();
        EntityRenderDispatcher entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        entityRenderDispatcher.setRenderShadows(false);
        entityRenderDispatcher.render(entity, 0, -entity.getHeight() / 2.0f, 0, 0.0f, 1.0f, context.getMatrices(), context.getVertexConsumers(), 0xF000F0);
        ModelAnalyser analyser = new ModelAnalyser();
        entityRenderDispatcher.render(entity, 0, -entity.getHeight() / 2.0f, 0, 0.0f, 1.0f, context.getMatrices(), analyser, 0xF000F0);
        context.draw();
        focusedPolygon = analyser.getFocusedPolygon(mouseX, mouseY, layers);
        if (focusedPolygon != null) analyser.drawPolygon(context, focusedPolygon.get(0), 0x6FFFFFFF);
        analyser.drawNormal(context, frontIndex, entitySize / 2, 0xFF00CC00);
        analyser.drawNormal(context, upIndex, entitySize / 2, 0xFFCC0000);
        analyser.drawPolygon(context, posIndex, 0x6F3333CC);
        posPolygon = analyser.getPolygon(posIndex);
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
        List<Integer> list = new ArrayList<>(List.of(frontIndex, upIndex));
        list.addAll(posPolygon);
        ConfigFile.modConfig.binding.indexListMap.put(name, list);
        ConfigFile.save();
    }

    private void loadToCurrent() {
        String name = nameWidget.getText();
        List<Integer> list = ConfigFile.modConfig.binding.indexListMap.get(name);
        try {
            frontIndexWidget.setValue(list.get(0));
            upIndexWidget.setValue(list.get(1));
            posIndexWidget.setValue(list.get(2));
        } catch (Exception ignored) {
        }
    }

    private void changeSelectionTarget(int target) {
        Text ON = Text.literal("ON").styled(style -> style.withColor(Formatting.GREEN));
        Text OFF = Text.literal("OFF");
        boolean front = selectingFront;
        boolean up = selectingUp;
        boolean pos = selectingPos;
        selectFrontButton.setMessage(OFF);
        selectUpButton.setMessage(OFF);
        selectPosButton.setMessage(OFF);
        selectingFront = selectingUp = selectingPos = false;
        switch (target) {
            case 0:
                if (front) break;
                selectFrontButton.setMessage(ON);
                selectingFront = true;
                break;
            case 1:
                if (up) break;
                selectUpButton.setMessage(ON);
                selectingUp = true;
                break;
            case 2:
                if (pos) break;
                selectPosButton.setMessage(ON);
                selectingPos = true;
                break;
        }
    }

    protected boolean mouseInViewArea(double mouseX, double mouseY) {
        return mouseX >= x + (double) (xSize - ySize) / 2 && mouseX <= x + (double) (xSize + ySize) / 2 && mouseY >= y && mouseY <= y + (double) ySize;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseInViewArea(mouseX, mouseY) && focusedPolygon != null  && button == GLFW.GLFW_MOUSE_BUTTON_LEFT&&
                InputUtil.isKeyPressed(this.client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_ALT)) {
            if (selectingFront) frontIndexWidget.setValue(focusedPolygon.get(0));
            if (selectingUp) upIndexWidget.setValue(focusedPolygon.get(0));
            if (selectingPos) posIndexWidget.setValue(focusedPolygon.get(0));
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (mouseInViewArea(mouseX, mouseY)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !InputUtil.isKeyPressed(this.client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_ALT)) {
                xRot = MathHelper.wrapDegrees(xRot + (float) deltaY / 90f);
                yRot = MathHelper.wrapDegrees(yRot - (float) deltaX / 90f);
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                entityX = entityX + deltaX / entitySize;
                entityY = entityY + deltaY / entitySize;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) { // 1.20.1
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
        return false;
    }
}
