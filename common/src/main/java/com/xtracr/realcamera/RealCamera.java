package com.xtracr.realcamera;

import com.xtracr.realcamera.config.ConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealCamera {
    public static final String MODID = "realcamera";
    public static final String FULL_ID = "xtracr_" + MODID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static void setup() {
        ConfigFile.load();
    }
}
