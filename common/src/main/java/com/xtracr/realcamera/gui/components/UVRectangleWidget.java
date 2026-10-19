package com.xtracr.realcamera.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import com.xtracr.realcamera.config.UVRectangle;
import com.xtracr.realcamera.gui.util.GUIHelper;
import com.xtracr.realcamera.gui.util.TextureViewport;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.world.phys.Vec2;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class UVRectangleWidget extends AbstractWidget {
    private static final int SELECTION_COLOR = 0x4F3333CC, SELECTION_HOVER_COLOR = 0x2F3333CC, OUTLINE_COLOR = 0xAAFFFFFF;

    public float uMin, vMin, uMax, vMax;
    @Nullable
    private TextureViewport viewport;
    private Runnable onDelete = () -> {};
    private Runnable onFocusUpdate = () -> {};

    public UVRectangleWidget(float uMin, float vMin, float uMax, float vMax) {
        super(0, 0, 16, 16, CommonComponents.EMPTY);
        this.uMin = uMin;
        this.vMin = vMin;
        this.uMax = uMax;
        this.vMax = vMax;
    }

    public void setOnDelete(@NonNull Runnable onDelete) {
        this.onDelete = onDelete;
    }

    public void setOnFocusUpdate(@NonNull Runnable onFocusUpdate) {
        this.onFocusUpdate = onFocusUpdate;
    }

    public UVRectangleWidget setViewport(@NonNull TextureViewport viewport) {
        this.viewport = viewport;
        return this;
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

    @Override
    public void extractWidgetRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (viewport == null) return;
        Vec2 minXY = viewport.uvToXY(uMin, vMin), maxXY = viewport.uvToXY(uMax, vMax);
        float x1 = minXY.x, y1 = minXY.y, x2 = maxXY.x, y2 = maxXY.y, width = x2 - x1, height = y2 - y1;
        setX((int) x1);
        setY((int) y1);
        setWidth((int) width);
        setHeight((int) height);
        GUIHelper.enableScissor(graphics, viewport.area());
        GUIHelper.fill(graphics, x1, y1, x2, y2, SELECTION_COLOR);
        if (isHoveredOrFocused()) GUIHelper.fill(graphics, x1, y1, x2, y2, SELECTION_HOVER_COLOR);
        if (isFocused()) GUIHelper.outline(graphics, x1, y1, width, height, OUTLINE_COLOR);
        graphics.disableScissor();
    }
    
    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (event.input() == InputConstants.KEY_DELETE) {
            onDelete.run();
            return true;
        }
        if (event.isSelection() && uMin < uMax && vMin < vMax) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (focused) onFocusUpdate.run();
    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput narrationElementOutput) {
    }
}