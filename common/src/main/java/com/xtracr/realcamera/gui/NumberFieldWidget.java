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

    NumberFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height,
                      @Nullable NumberFieldWidget<T> copyFrom, Text text, T defaultValue, T maximum, T minimum) {
        super(textRenderer, x, y, width, height, text);
        this.defaultValue = defaultValue;
        this.maximum = maximum;
        this.minimum = minimum;
        setValue(defaultValue);
        if (copyFrom != null) setValue(copyFrom.getValue());
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
        setTooltip(null);
        setRenderTextProvider((string, firstCharacterIndex) -> OrderedText.styledForwardsVisitedString(string, Style.EMPTY));
        try {
            T value = getValueInternal();
            if (value.compareTo(minimum) < 0) throw new Exception("<" + minimum + " !");
            if (value.compareTo(maximum) > 0) throw new Exception(">" + maximum + " !");
        } catch (Exception exception) {
            setTooltip(Tooltip.of(Text.literal("Invalid number: " + exception.getMessage()).styled(s -> s.withColor(Formatting.RED))));
            setRenderTextProvider((string, firstCharacterIndex) -> OrderedText.styledForwardsVisitedString(string, Style.EMPTY.withColor(Formatting.RED)));
        }
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
}
