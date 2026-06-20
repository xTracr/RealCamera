package com.xtracr.realcamera;

import com.xtracr.realcamera.compat.CompatibilityHelper;
import com.xtracr.realcamera.compat.PlatformHelper;
import com.xtracr.realcamera.config.ConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface RealCamera extends PlatformHelper {
    String MOD_ID = "realcamera";
    String FULL_ID = "xtracr_" + MOD_ID;
    Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    default void initialize() {
        ConfigFile.load();
        CompatibilityHelper.initialize(this);
    }
}
