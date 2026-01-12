package com.xtracr.realcamera.gui;

import com.xtracr.realcamera.util.LocUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

public abstract class NumberField<T extends Comparable<T>> extends EditBox {
    private final T defaultValue;
    protected T maximum, minimum;
    private Tooltip tooltip;

    NumberField(Font font, int width, int height, T defaultValue, T maximum, T minimum, @Nullable NumberField<T> copyFrom) {
        super(font, 0, 0, width, height, CommonComponents.EMPTY);
        this.defaultValue = defaultValue;
        this.maximum = maximum;
        this.minimum = minimum;
        setNumber(defaultValue);
        if (copyFrom != null) setNumber(copyFrom.getNumber());
    }

    public static NumberField<Float> ofFloat(Font font, int width, int height, float defaultValue, @Nullable NumberField<Float> copyFrom) {
        return new FloatField(font, width, height, defaultValue, copyFrom);
    }

    public static NumberField<Integer> ofInt(Font font, int width, int height, int defaultValue, @Nullable NumberField<Integer> copyFrom) {
        return new IntField(font, width, height, defaultValue, copyFrom);
    }

    public T getNumber() {
        try {
            return getNumberInternal();
        } catch (Exception e) {
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

    public NumberField<T> setMax(T maximum) {
        this.maximum = maximum;
        return this;
    }

    public NumberField<T> setMin(T minimum) {
        this.minimum = minimum;
        return this;
    }

    public NumberField<T> setOnValueChange(Consumer<T> consumer) {
        super.setResponder(_ -> consumer.accept(getNumber()));
        return this;
    }

    abstract protected T getNumberInternal();

    protected void checkText() {
        super.setTooltip(tooltip);
        setTextColor(EditBox.DEFAULT_TEXT_COLOR);
        if (getValue().isEmpty()) return;
        try {
            T value = getNumberInternal();
            if (value.compareTo(minimum) < 0) throw new Exception("< " + minimum);
            if (value.compareTo(maximum) > 0) throw new Exception("> " + maximum);
        } catch (Exception e) {
            super.setTooltip(Tooltip.create(LocUtil.literal("Invalid number: " + e.getMessage()).withStyle(s -> s.withColor(ChatFormatting.RED))));
            setTextColor(0xFFFF5555);
        }
    }

    @Override
    public void setTooltip(Tooltip tooltip) {
        this.tooltip = tooltip;
        super.setTooltip(tooltip);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (event.isSelection()) {
            setFocused(false);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        char chr = (char) event.codepoint();
        if (chr != '-' && chr != '.' && (chr < '0' || chr > '9')) return false;
        return super.charTyped(event);
    }

    @Override
    public void renderWidget(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        checkText();
        super.renderWidget(graphics, mouseX, mouseY, partialTicks);
    }

    private static class FloatField extends NumberField<Float> {
        FloatField(Font font, int width, int height, float defaultValue, @Nullable NumberField<Float> copyFrom) {
            super(font, width, height, defaultValue, Float.MAX_VALUE, -Float.MAX_VALUE, copyFrom);
            setMaxLength(16);
        }

        @Override
        protected Float getNumberInternal() {
            return Float.parseFloat(getValue());
        }
    }

    private static class IntField extends NumberField<Integer> {
        IntField(Font font, int width, int height, int defaultValue, @Nullable NumberField<Integer> copyFrom) {
            super(font, width, height, defaultValue, Integer.MAX_VALUE, Integer.MIN_VALUE, copyFrom);
            setMaxLength(8);
        }

        @Override
        protected Integer getNumberInternal() {
            return Integer.parseInt(getValue());
        }
    }
}
