package com.xtracr.realcamera;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xtracr.realcamera.compat.CompatExample;
import com.xtracr.realcamera.config.ConfigFile;

public class RealCamera {

    public static final String MODID = "realcamera";
    public static final Logger LOGGER = LoggerFactory.getLogger("realcamera");

    public static void setup() {
        ConfigFile.setup();
        CompatExample.register();
    }
}
