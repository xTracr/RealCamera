package com.xtracr.realcamera.gui;

public enum Category {
    CONFIGS,
    PREVIEW,
    DISABLE;

    public final String id = name().toLowerCase();

    Category next() {
        return values()[(ordinal() + 1) % values().length];
    }
}