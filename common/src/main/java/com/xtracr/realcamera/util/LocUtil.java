package com.xtracr.realcamera.util;

import com.xtracr.realcamera.RealCamera;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class LocUtil {
    public static final String KEY_MOD_NAME = "general." + RealCamera.FULL_ID + ".modName";

    public static MutableText MOD_NAME() {
        return Text.translatable(KEY_MOD_NAME);
    }

    public static MutableText CONFIG_CATEGORY(String key) {
        return Text.translatable("config.category." + RealCamera.FULL_ID + "." + key);
    }

    public static MutableText CONFIG_OPTION(String key, Object... args) {
        return Text.translatable("config.option." + RealCamera.FULL_ID + "." + key, args);
    }

    public static MutableText CONFIG_TOOLTIP(String key) {
        return Text.translatable("config.tooltip." + RealCamera.FULL_ID + "." + key);
    }

    public static MutableText MESSAGE(String key, Object... args) {
        return Text.translatable("message." + RealCamera.FULL_ID + "." + key, args);
    }

    public static MutableText MODEL_VIEW_TITLE() {
        return Text.translatable("screen." + RealCamera.FULL_ID + ".modelView_title");
    }

    public static MutableText MODEL_VIEW_WIDGET(String key, Object... args) {
        return Text.translatable("screen.widget." + RealCamera.FULL_ID + ".modelView_" + key, args);
    }

    public static Tooltip MODEL_VIEW_TOOLTIP(String key, Object... args) {
        return Tooltip.of(Text.translatable("screen.tooltip." + RealCamera.FULL_ID + ".modelView_" + key, args));
    }

    public static MutableText literal(String string) {
        return Text.literal(string);
    }
}
