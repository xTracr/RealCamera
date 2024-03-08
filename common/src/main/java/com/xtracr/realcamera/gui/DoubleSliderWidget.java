package com.xtracr.realcamera.gui;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.function.Function;

public class DoubleSliderWidget extends SliderWidget {
    private final Function<Double, Text> textFactory;
    private final double min, max;

    public DoubleSliderWidget(int width, int height, double value, double min, double max, Function<Double, Text> textFactory) {
        super(0, 0, width, height, textFactory.apply(value), MathHelper.clamp(0, (value - min) / (max - min), 1));
        this.textFactory = textFactory;
        this.min = min;
        this.max = max;
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
        setMessage(textFactory.apply(getValue()));
    }

    @Override
    protected void applyValue() {
        updateMessage();
    }
}
