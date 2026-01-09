package com.xtracr.realcamera;

import com.xtracr.realcamera.compat.CompatibilityHelper;
import com.xtracr.realcamera.compat.PlatformHelper;
import com.xtracr.realcamera.config.ConfigFile;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface RealCamera extends PlatformHelper {
    String MODID = "realcamera";
    String FULL_ID = "xtracr_" + MODID;
    Logger LOGGER = LoggerFactory.getLogger(MODID);

    default void initialize() {
        ConfigFile.load();
        CompatibilityHelper.initialize(this);
    }
}
