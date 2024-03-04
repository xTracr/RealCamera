package com.xtracr.realcamera.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

public abstract class NumberFieldWidget<T extends Comparable<T>> extends TextFieldWidget {
    private final T defaultValue;
    protected T maximum, minimum;
    private Tooltip tooltip;

    NumberFieldWidget(TextRenderer textRenderer, int width, int height, T defaultValue, T maximum, T minimum, @Nullable NumberFieldWidget<T> copyFrom) {
        super(textRenderer, 0, 0, width, height, Text.empty());
        this.defaultValue = defaultValue;
        this.maximum = maximum;
        this.minimum = minimum;
        setValue(defaultValue);
        if (copyFrom != null) setValue(copyFrom.getValue());
    }

    public static NumberFieldWidget<Float> ofFloat(TextRenderer textRenderer, int width, int height, float defaultValue, @Nullable NumberFieldWidget<Float> copyFrom) {
        return new FloatFieldWidget(textRenderer, width, height, defaultValue, copyFrom);
    }

    public static NumberFieldWidget<Integer> ofInt(TextRenderer textRenderer, int width, int height, int defaultValue, @Nullable NumberFieldWidget<Integer> copyFrom) {
        return new IntFieldWidget(textRenderer, width, height, defaultValue, copyFrom);
    }

    public T getValue() {
        try {
            return getValueInternal();
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    public void setValue(T value) {
        try {
            if (value.compareTo(minimum) < 0) value = minimum;
            else if (value.compareTo(maximum) > 0) value = maximum;
            setText(value.toString());
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

    abstract protected T getValueInternal();

    protected void checkText() {
        super.setTooltip(tooltip);
        setRenderTextProvider((string, firstCharacterIndex) -> OrderedText.styledForwardsVisitedString(string, Style.EMPTY));
        if (getText().isEmpty()) return;
        try {
            T value = getValueInternal();
            if (value.compareTo(minimum) < 0) throw new Exception("< " + minimum);
            if (value.compareTo(maximum) > 0) throw new Exception("> " + maximum);
        } catch (Exception exception) {
            super.setTooltip(Tooltip.of(Text.literal("Invalid number: " + exception.getMessage()).styled(s -> s.withColor(Formatting.RED))));
            setRenderTextProvider((string, firstCharacterIndex) -> OrderedText.styledForwardsVisitedString(string, Style.EMPTY.withColor(Formatting.RED)));
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
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        checkText();
        super.renderButton(context, mouseX, mouseY, delta);
    }

    private static class FloatFieldWidget extends NumberFieldWidget<Float> {
        FloatFieldWidget(TextRenderer textRenderer, int width, int height, float defaultValue, @Nullable NumberFieldWidget<Float> copyFrom) {
            super(textRenderer, width, height, defaultValue, Float.MAX_VALUE, -Float.MAX_VALUE, copyFrom);
            setMaxLength(8);
        }

        @Override
        protected Float getValueInternal() {
            return Float.parseFloat(getText());
        }
    }

    private static class IntFieldWidget extends NumberFieldWidget<Integer> {
        IntFieldWidget(TextRenderer textRenderer, int width, int height, int defaultValue, @Nullable NumberFieldWidget<Integer> copyFrom) {
            super(textRenderer, width, height, defaultValue, Integer.MAX_VALUE, Integer.MIN_VALUE, copyFrom);
            setMaxLength(8);
        }

        @Override
        protected Integer getValueInternal() {
            return Integer.parseInt(getText());
        }
    }
}
