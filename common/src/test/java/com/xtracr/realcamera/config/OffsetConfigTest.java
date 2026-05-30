package com.xtracr.realcamera.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OffsetConfigTest {
    @Test
    void rotationsAreIndependentForEachPosture() {
        OffsetConfig offsets = new OffsetConfig(1.0f, 0.0f, 0.0f, 0.0f, 10.0f, 20.0f, 30.0f);

        offsets.adjustPitch(CameraPosture.SNEAKING, 5.0f);
        offsets.adjustYaw(CameraPosture.SWIMMING, 15.0f);
        offsets.adjustRoll(CameraPosture.CRAWLING, 25.0f);

        assertRotation(offsets, CameraPosture.STAND, 10.0f, 20.0f, 30.0f);
        assertRotation(offsets, CameraPosture.SNEAKING, 15.0f, 20.0f, 30.0f);
        assertRotation(offsets, CameraPosture.SWIMMING, 10.0f, 35.0f, 30.0f);
        assertRotation(offsets, CameraPosture.CRAWLING, 10.0f, 20.0f, 55.0f);
    }

    @Test
    void rotationsWrapPerPosture() {
        OffsetConfig offsets = new OffsetConfig(1.0f, 0.0f, 0.0f, 0.0f, 179.0f, -179.0f, 170.0f);

        offsets.adjustPitch(CameraPosture.SWIMMING, 10.0f);
        offsets.adjustYaw(CameraPosture.SWIMMING, -10.0f);
        offsets.adjustRoll(CameraPosture.SWIMMING, 20.0f);

        assertRotation(offsets, CameraPosture.STAND, 179.0f, -179.0f, 170.0f);
        assertRotation(offsets, CameraPosture.SWIMMING, -171.0f, 171.0f, -170.0f);
    }

    private static void assertRotation(OffsetConfig offsets, CameraPosture posture, float pitch, float yaw, float roll) {
        assertEquals(pitch, offsets.pitch(posture), 0.0001f);
        assertEquals(yaw, offsets.yaw(posture), 0.0001f);
        assertEquals(roll, offsets.roll(posture), 0.0001f);
    }
}
