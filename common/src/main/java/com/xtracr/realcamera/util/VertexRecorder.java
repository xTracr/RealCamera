package com.xtracr.realcamera.util;

import java.util.ArrayList;
import java.util.List;

public class VertexRecorder implements IVertexRecorder {
    protected final List<BuiltRecord> records = new ArrayList<>();

    @Override
    public List<BuiltRecord> records() {
        return records;
    }
}
