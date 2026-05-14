package com.xtracr.realcamera.gui.components;

import com.xtracr.realcamera.util.LocUtil;
import com.xtracr.realcamera.util.MathUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import org.jspecify.annotations.NonNull;

public final class NumberWidgetPair extends AbstractWidget {
    private final DoubleSlider slider;
    private final NumberField<Float> field;
    private boolean useSlider;

    public NumberWidgetPair(Font font, String translationKey, int width, int height, float min, float max) {
        super(0, 0, width, height, CommonComponents.EMPTY);
        this.slider = new DoubleSlider(width, height, 0, min, max, d -> LocUtil.MODEL_VIEW_WIDGET(translationKey, MathUtil.round(d, 2)));
        this.field = NumberField.ofFloat(font, width - 2, height - 2, 0, null).setMin(min).setMax(max);
        setUseSlider(true);
    }

    public void setNumber(float value) {
        slider.setNumber(value);
        field.setNumber(value);
    }

    public float getNumber() {
        return isUseSlider() ? (float) slider.getNumber() : field.getNumber();
    }

    public void setUseSlider(boolean useSlider) {
        this.useSlider = useSlider;
        slider.active = useSlider;
        field.active = !useSlider;
    }

    public boolean isUseSlider() {
        return useSlider;
    }

    public void syncAndSwitch(boolean useSlider) {
        setUseSlider(useSlider);
        if (useSlider) slider.setNumber(field.getNumber());
        else field.setNumber((float) slider.getNumber());
    }

    private AbstractWidget getActiveWidget() {
        return isUseSlider() ? slider : field;
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        slider.setX(x);
        field.setX(x);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        slider.setY(y);
        field.setY(y);
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        slider.setWidth(width);
        field.setWidth(width - 2);
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        slider.setHeight(height);
        field.setHeight(height - 2);
    }

    @Override
    public void extractWidgetRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        getActiveWidget().extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        return getActiveWidget().mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dx, double dy) {
        return getActiveWidget().mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        return getActiveWidget().mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return getActiveWidget().mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        return getActiveWidget().keyPressed(event);
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        return getActiveWidget().charTyped(event);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (focused) getActiveWidget().setFocused(true);
        else {
            slider.setFocused(false);
            field.setFocused(false);
        }
    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput output) {
        getActiveWidget().updateNarration(output);
    }
}
