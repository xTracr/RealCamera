package com.xtracr.realcamera.config.binding;

public record UVRectangle(float uMin, float vMin, float uMax, float vMax) {
    public boolean contains(float u, float v) {
        return u >= uMin && u <= uMax && v >= vMin && v <= vMax;
    }
}
