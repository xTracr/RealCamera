package com.xtracr.realcamera;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xtracr.realcamera.api.CompatExample;
import com.xtracr.realcamera.config.ConfigFile;

public class RealCamera {

    public static final String MODID = "realcamera";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static void setup() {
        ConfigFile.load();
        CompatExample.register();
    }
}
