package com.xtracr.realcamera.gui;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.function.Function;

public class DoubleSliderWidget extends SliderWidget {
    private final double min, max;
    private final Function<Double, Text> textFunction;

    public DoubleSliderWidget(int width, int height, double value, double min, double max, Function<Double, Text> textFunction) {
        super(0, 0, width, height, textFunction.apply(value), MathHelper.clamp(0, (value - min) / (max - min), 1));
        this.min = min;
        this.max = max;
        this.textFunction = textFunction;
    }

    public double getValue() {
        return min + (max - min) * value;
    }

    public void setValue(double value) {
        this.value = MathHelper.clamp(0, (value - min) / (max - min), 1);
        applyValue();
    }

    @Override
    protected void updateMessage() {
        setMessage(textFunction.apply(getValue()));
    }

    @Override
    protected void applyValue() {
        updateMessage();
    }
}
