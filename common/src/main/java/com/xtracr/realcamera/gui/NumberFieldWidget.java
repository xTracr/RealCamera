package com.xtracr.realcamera.gui;

import com.xtracr.realcamera.util.LocUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public abstract class NumberFieldWidget<T extends Comparable<T>> extends EditBox {
    private final T defaultValue;
    protected T maximum, minimum;
    private Tooltip tooltip;

    NumberFieldWidget(Font textRenderer, int width, int height, T defaultValue, T maximum, T minimum, @Nullable NumberFieldWidget<T> copyFrom) {
        super(textRenderer, 0, 0, width, height, Component.empty());
        this.defaultValue = defaultValue;
        this.maximum = maximum;
        this.minimum = minimum;
        setNumber(defaultValue);
        if (copyFrom != null) setNumber(copyFrom.getNumber());
    }

    public static NumberFieldWidget<Float> ofFloat(Font textRenderer, int width, int height, float defaultValue, @Nullable NumberFieldWidget<Float> copyFrom) {
        return new FloatFieldWidget(textRenderer, width, height, defaultValue, copyFrom);
    }

    public static NumberFieldWidget<Integer> ofInt(Font textRenderer, int width, int height, int defaultValue, @Nullable NumberFieldWidget<Integer> copyFrom) {
        return new IntFieldWidget(textRenderer, width, height, defaultValue, copyFrom);
    }

    public T getNumber() {
        try {
            return getNumberInternal();
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    public void setNumber(T value) {
        try {
            if (value.compareTo(minimum) < 0) value = minimum;
            else if (value.compareTo(maximum) > 0) value = maximum;
            setValue(value.toString());
        } catch (Exception ignored) {
        }
    }

    public NumberFieldWidget<T> setMax(T maximum) {
        this.maximum = maximum;
        return this;
    }

    public NumberFieldWidget<T> setMin(T minimum) {
        this.minimum = minimum;
        return this;
    }

    abstract protected T getNumberInternal();

    protected void checkText() {
        super.setTooltip(tooltip);
        setFormatter((string, firstCharacterIndex) -> FormattedCharSequence.forward(string, Style.EMPTY));
        if (getValue().isEmpty()) return;
        try {
            T value = getNumberInternal();
            if (value.compareTo(minimum) < 0) throw new Exception("< " + minimum);
            if (value.compareTo(maximum) > 0) throw new Exception("> " + maximum);
        } catch (Exception exception) {
            super.setTooltip(Tooltip.create(LocUtil.literal("Invalid number: " + exception.getMessage()).withStyle(s -> s.withColor(ChatFormatting.RED))));
            setFormatter((string, firstCharacterIndex) -> FormattedCharSequence.forward(string, Style.EMPTY.withColor(ChatFormatting.RED)));
        }
    }

    @Override
    public void setTooltip(Tooltip tooltip) {
        this.tooltip = tooltip;
        super.setTooltip(tooltip);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr != '-' && chr != '.' && (chr < '0' || chr > '9')) return false;
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        checkText();
        super.renderWidget(context, mouseX, mouseY, delta);
    }

    private static class FloatFieldWidget extends NumberFieldWidget<Float> {
        FloatFieldWidget(Font textRenderer, int width, int height, float defaultValue, @Nullable NumberFieldWidget<Float> copyFrom) {
            super(textRenderer, width, height, defaultValue, Float.MAX_VALUE, -Float.MAX_VALUE, copyFrom);
            setMaxLength(8);
        }

        @Override
        protected Float getNumberInternal() {
            return Float.parseFloat(getValue());
        }
    }

    private static class IntFieldWidget extends NumberFieldWidget<Integer> {
        IntFieldWidget(Font textRenderer, int width, int height, int defaultValue, @Nullable NumberFieldWidget<Integer> copyFrom) {
            super(textRenderer, width, height, defaultValue, Integer.MAX_VALUE, Integer.MIN_VALUE, copyFrom);
            setMaxLength(8);
        }

        @Override
        protected Integer getNumberInternal() {
            return Integer.parseInt(getValue());
        }
    }
}
