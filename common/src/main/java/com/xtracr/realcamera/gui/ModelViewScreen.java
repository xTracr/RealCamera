package com.xtracr.realcamera.gui;

import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.util.MathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
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
    private static final String KEY_SCREEN = "screen.xtracr_" + RealCamera.MODID + "_vertexSelection_";
    private static final String KEY_WIDGET = "screen.widget.xtracr_" + RealCamera.MODID + "_vertexSelection_";
    protected int xSize = 400, ySize = 220;
    protected int x, y;
    public double mouseX, mouseY;
    private int entitySize = 80;
    private double entityX, entityY;
    private float yaw, pitch, xRot, yRot;
    private boolean selectingFront, selectingUp, selectingPos;
    private int layers, frontIndex, upIndex;
    private IntFieldWidget frontIndexWidget, upIndexWidget;
    private final ButtonWidget selectFrontButton = ButtonWidget.builder(Text.literal("OFF"), button -> changeSelectionTarget(0)).size(25, 18).build();
    private final ButtonWidget selectUpButton = ButtonWidget.builder(Text.literal("OFF"), button -> changeSelectionTarget(1)).size(25, 18).build();
    private final ButtonWidget selectPosButton = ButtonWidget.builder(Text.literal("OFF"), button -> changeSelectionTarget(2)).size(25, 18).build();
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
        frontIndexWidget = new IntFieldWidget(textRenderer, (xSize - ySize)/2 - 45, 18, Text.translatable(KEY_WIDGET + "frontIndex"), frontIndex, i -> frontIndex = i);
        upIndexWidget = new IntFieldWidget(textRenderer, (xSize - ySize)/2 - 45, 18, Text.translatable(KEY_WIDGET + "upIndex"), upIndex, i -> upIndex = i);
        frontIndexWidget.setPosition(x + 5, y + 70);
        selectFrontButton.setPosition(x + (xSize - ySize)/2 - 35, y + 70);
        upIndexWidget.setPosition(x + 5, y + 92);
        selectUpButton.setPosition(x + (xSize - ySize)/2 - 35, y + 92);
        selectPosButton.setPosition(x + (xSize - ySize)/2 - 35, y + 114);
        yawWidget.setPosition(x + 5, y + 26);
        pitchWidget.setPosition(x + 5, y + 48);
        resetWidget.setPosition(x + 5, y + 4);
        addDrawableChild(frontIndexWidget);
        addDrawableChild(selectFrontButton);
        addDrawableChild(upIndexWidget);
        addDrawableChild(selectUpButton);
        addDrawableChild(selectPosButton);
        addDrawableChild(yawWidget);
        addDrawableChild(pitchWidget);
        addDrawableChild(resetWidget);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        drawEntity(context, x + (xSize - ySize) / 2, y, x + (xSize + ySize) / 2, y + ySize, this.client.player);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.fill(x, y, x + (xSize - ySize) / 2 - 5, y + ySize, 0xFF555555);
        context.fill(x + (xSize - ySize) / 2, y, x + (xSize + ySize) / 2, y + ySize, 0xFF222222);
        context.fill(x + (xSize + ySize) / 2 + 5, y, x + xSize, y + ySize, 0xFF555555);
    }

    protected void drawEntity(DrawContext context, int x1, int y1, int x2, int y2, LivingEntity entity) {
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
        drawEntity(context, centerX, centerY, entitySize, vector3f, quaternionf, entity);
        entity.bodyYaw = entityBodyYaw;
        entity.setYaw(entityYaw);
        entity.setPitch(entityPitch);
        entity.prevHeadYaw = entityPrevHeadYaw;
        entity.headYaw = entityHeadYaw;
        context.disableScissor();
    }

    protected static void drawEntity(DrawContext context, float x, float y, int size, Vector3f offset, Quaternionf quaternionf, LivingEntity entity) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().multiplyPositionMatrix(new Matrix4f().scaling(size, size, -size));
        context.getMatrices().translate(offset.x(), offset.y(), offset.z());
        context.getMatrices().multiply(quaternionf);
        DiffuseLighting.method_34742();
        EntityRenderDispatcher entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        entityRenderDispatcher.setRenderShadows(false);
        entityRenderDispatcher.render(entity, 0, -entity.getHeight() / 2.0f, 0, 0.0f, 1.0f, context.getMatrices(), context.getVertexConsumers(), 0xF000F0);
        //entityRenderDispatcher.render(entity, 0, -entity.getHeight() / 2.0f, 0, 0.0f, 1.0f, context.getMatrices(), layer -> null, 0xF000F0);
        context.draw();
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (mouseInViewArea(mouseX, mouseY)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
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
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseInViewArea(mouseX, mouseY)) {
            if (InputUtil.isKeyPressed(this.client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_ALT)) {
                layers = Math.max(0, layers + (int) verticalAmount);
            } else {
                entitySize = MathHelper.clamp(entitySize + (int) verticalAmount * entitySize / 16, 16, 1024);
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
