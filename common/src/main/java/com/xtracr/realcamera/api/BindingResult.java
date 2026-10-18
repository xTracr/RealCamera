package com.xtracr.realcamera.api;

import com.xtracr.realcamera.util.ReferenceFrame;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/** A successful, unadjusted frame relative to the camera entity's render origin. */
public record BindingResult(Identifier id, ReferenceFrame referenceFrame) {
    public BindingResult {
        Objects.requireNonNull(id);
        Objects.requireNonNull(referenceFrame);
    }
}
