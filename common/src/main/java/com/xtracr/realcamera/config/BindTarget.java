package com.xtracr.realcamera.config;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.xtracr.realcamera.renderer.VertexData;
import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.floats.FloatOpenHashSet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public record BindTarget(
        String name, String textureId, int priority, float disablingDepth,
        TargetConfig targetConfig,
        BindConfig bindConfig,
        OffsetConfig offsets,
        DisableConfig[] disableConfigs) {
    public static final List<BindTarget> defaultTargets;
    public static final BindTarget EMPTY = blank(null, null);
    private static final short serialVersion = 703;

    static {
        defaultTargets = List.of(
                BindTarget.vanillaTarget("minecraft_head", 5, false),
                BindTarget.vanillaTarget("skin_head", 5, false),
                BindTarget.vanillaTarget("minecraft_head_2", 1, true),
                BindTarget.vanillaTarget("skin_head_2", 1, true)
        );
    }

    public static BindTarget blank(String name, String textureId) {
        TargetConfig targetConfig = new TargetConfig(0, 0, 0, 0, 0, 0);
        BindConfig bindConfig = new BindConfig(false, true, false, false);
        return new BindTarget(name, textureId, 0, 0.2f, targetConfig, bindConfig, new OffsetConfig(), new DisableConfig[0]);
    }

    public static BindTarget read(FriendlyByteBuf byteBuf) throws IllegalArgumentException {
        short version = byteBuf.readShort();
        if (version != serialVersion) throw new IllegalArgumentException("Invalid version: " + version + ", expected " + serialVersion);
        String name = byteBuf.readUtf();
        String textureId = byteBuf.readUtf();
        int priority = byteBuf.readVarInt();
        float disablingDepth = byteBuf.readFloat();
        TargetConfig targetConfig = TargetConfig.read(byteBuf);
        BindConfig bindConfig = BindConfig.read(byteBuf);
        OffsetConfig offsets = OffsetConfig.read(byteBuf);
        DisableConfig[] disableConfigs = new DisableConfig[byteBuf.readVarInt()];
        for (int i = 0; i < disableConfigs.length; i++) {
            disableConfigs[i] = DisableConfig.read(byteBuf);
        }
        return new BindTarget(name, textureId, priority, disablingDepth, targetConfig, bindConfig, offsets, disableConfigs);
    }

    private static BindTarget vanillaTarget(String name, int priority, boolean shouldBind) {
        String textureId = name.contains("skin") ? "minecraft:skins/" : "minecraft:textures/entity/player/";
        TargetConfig targetConfig = new TargetConfig(0.1875f, 0.2f, 0.1875f, 0.075f, 0.1875f, 0.2f);
        BindConfig bindConfig = new BindConfig(shouldBind, true, shouldBind, shouldBind);
        OffsetConfig offsets = new OffsetConfig().setX(-0.1f);
        DisableConfig playerHead = new DisableConfig("player_head", textureId, false, new UVRectangle[]{new UVRectangle(0, 0, 1.0f, 0.25f)});
        DisableConfig dragonHead = new DisableConfig("dragon_head", "minecraft:textures/entity/enderdragon/dragon.png", true, new UVRectangle[0]);
        DisableConfig[] disableConfigs = new DisableConfig[]{playerHead, dragonHead};
        return new BindTarget(name, textureId, priority, 0.1f, targetConfig, bindConfig, offsets, disableConfigs);
    }

    public boolean isEmpty() {
        return name == null || textureId == null || targetConfig == null || bindConfig == null || offsets == null || disableConfigs == null;
    }

    public DisableConfig[] filteredDisableConfigs(Predicate<DisableConfig> filter) {
        return Arrays.stream(disableConfigs).filter(filter).toArray(DisableConfig[]::new);
    }

    public void write(FriendlyByteBuf byteBuf) {
        byteBuf.writeShort(serialVersion);
        byteBuf.writeUtf(name);
        byteBuf.writeUtf(textureId);
        byteBuf.writeVarInt(priority);
        byteBuf.writeFloat(disablingDepth);
        targetConfig.write(byteBuf);
        bindConfig.write(byteBuf);
        offsets.write(byteBuf);
        byteBuf.writeVarInt(disableConfigs.length);
        for (DisableConfig disableConfig : disableConfigs) {
            disableConfig.write(byteBuf);
        }
    }

    public record TargetConfig(float forwardU, float forwardV, float upwardU, float upwardV, float posU, float posV) {
        public static TargetConfig read(FriendlyByteBuf byteBuf) {
            return new TargetConfig(byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat());
        }

        public void write(FriendlyByteBuf byteBuf) {
            byteBuf.writeFloat(forwardU);
            byteBuf.writeFloat(forwardV);
            byteBuf.writeFloat(upwardU);
            byteBuf.writeFloat(upwardV);
            byteBuf.writeFloat(posU);
            byteBuf.writeFloat(posV);
        }
    }

    public record BindConfig(boolean bindX, boolean bindY, boolean bindZ, boolean bindRotation) {
        public static BindConfig read(FriendlyByteBuf byteBuf) {
            byte bindFlags = byteBuf.readByte();
            boolean bindX = (bindFlags & 0x01) != 0;
            boolean bindY = (bindFlags & 0x02) != 0;
            boolean bindZ = (bindFlags & 0x04) != 0;
            boolean bindRotation = (bindFlags & 0x08) != 0;
            return new BindConfig(bindX, bindY, bindZ, bindRotation);
        }

        public void write(FriendlyByteBuf byteBuf) {
            byte bindFlags = 0;
            if (bindX) bindFlags |= 0x01;
            if (bindY) bindFlags |= 0x02;
            if (bindZ) bindFlags |= 0x04;
            if (bindRotation) bindFlags |= 0x08;
            byteBuf.writeByte(bindFlags);
        }
    }

    public static class OffsetConfig {
        private float scale = 1, x = 0, y = 0, z = 0, pitch = 0, yaw = 0, roll = 0;

        public static OffsetConfig read(FriendlyByteBuf byteBuf) {
            return new OffsetConfig().setScale(byteBuf.readFloat()).setX(byteBuf.readFloat()).setY(byteBuf.readFloat()).setZ(byteBuf.readFloat()).setPitch(byteBuf.readFloat()).setYaw(byteBuf.readFloat()).setRoll(byteBuf.readFloat());
        }

        public float getScale() {
            return scale;
        }

        public OffsetConfig setScale(float scale) {
            this.scale = scale;
            return this;
        }

        public float getX() {
            return x;
        }

        public OffsetConfig setX(float x) {
            this.x = Mth.clamp(x, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
            return this;
        }

        public float getY() {
            return y;
        }

        public OffsetConfig setY(float y) {
            this.y = Mth.clamp(y, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
            return this;
        }

        public float getZ() {
            return z;
        }

        public OffsetConfig setZ(float z) {
            this.z = Mth.clamp(z, ModConfig.MIN_OFFSET_F, ModConfig.MAX_OFFSET_F);
            return this;
        }

        public float getPitch() {
            return pitch;
        }

        public OffsetConfig setPitch(float pitch) {
            this.pitch = Mth.wrapDegrees(pitch);
            return this;
        }

        public float getYaw() {
            return yaw;
        }

        public OffsetConfig setYaw(float yaw) {
            this.yaw = Mth.wrapDegrees(yaw);
            return this;
        }

        public float getRoll() {
            return roll;
        }

        public OffsetConfig setRoll(float roll) {
            this.roll = Mth.wrapDegrees(roll);
            return this;
        }

        public void write(FriendlyByteBuf byteBuf) {
            byteBuf.writeFloat(scale);
            byteBuf.writeFloat(x);
            byteBuf.writeFloat(y);
            byteBuf.writeFloat(z);
            byteBuf.writeFloat(pitch);
            byteBuf.writeFloat(yaw);
            byteBuf.writeFloat(roll);
        }
    }

    @JsonAdapter(DisableConfig.Adapter.class)
    public static class DisableConfig {
        private final Float2ObjectOpenHashMap<FloatOpenHashSet> disableCacheMap = new Float2ObjectOpenHashMap<>();
        private final String name;
        private final String textureId;
        private final boolean disableAll;
        private final UVRectangle[] rectangles;

        public DisableConfig(String name, String textureId, boolean disableAll, UVRectangle[] rectangles) {
            this.name = name;
            this.textureId = textureId;
            this.disableAll = disableAll;
            this.rectangles = rectangles;
            disableCacheMap.defaultReturnValue(FloatOpenHashSet.of());
        }

        public static DisableConfig read(FriendlyByteBuf byteBuf) {
            String name = byteBuf.readUtf();
            String textureId = byteBuf.readUtf();
            boolean disableAll = byteBuf.readBoolean();
            UVRectangle[] rectangles = new UVRectangle[byteBuf.readVarInt()];
            for (int i = 0; i < rectangles.length; i++) {
                rectangles[i] = UVRectangle.read(byteBuf);
            }
            return new DisableConfig(name, textureId, disableAll, rectangles);
        }

        public String name() {
            return name;
        }

        public String textureId() {
            return textureId;
        }

        public boolean disableAll() {
            return disableAll;
        }

        public UVRectangle[] rectangles() {
            return rectangles;
        }

        public void write(FriendlyByteBuf byteBuf) {
            byteBuf.writeUtf(name);
            byteBuf.writeUtf(textureId);
            byteBuf.writeBoolean(disableAll);
            byteBuf.writeVarInt(rectangles.length);
            for (UVRectangle rect : rectangles) {
                rect.write(byteBuf);
            }
        }

        public boolean disable(VertexData vertex) {
            final float u = vertex.u(), v = vertex.v();
            final FloatOpenHashSet cachedVs = disableCacheMap.get(u);
            if (!cachedVs.isEmpty()) {
                if (cachedVs.contains(v)) return true;
                if (cachedVs.contains(-v)) return false;
            }
            for (UVRectangle rect : rectangles) {
                if (!rect.contains(u, v)) continue;
                disableCacheMap.computeIfAbsent(u, k -> new FloatOpenHashSet()).add(v);
                return true;
            }
            disableCacheMap.computeIfAbsent(u, k -> new FloatOpenHashSet()).add(-v);
            return false;
        }

        public static class Adapter extends TypeAdapter<DisableConfig> {
            @Override
            public void write(JsonWriter out, DisableConfig value) throws IOException {
                out.beginObject();
                out.name("name").value(value.name());
                out.name("textureId").value(value.textureId());
                out.name("disableAll").value(value.disableAll());
                out.name("rectangles");
                out.beginArray();
                for (UVRectangle rect : value.rectangles()) {
                    out.beginObject();
                    out.name("uMin").value(rect.uMin());
                    out.name("vMin").value(rect.vMin());
                    out.name("uMax").value(rect.uMax());
                    out.name("vMax").value(rect.vMax());
                    out.endObject();
                }
                out.endArray();
                out.endObject();
            }

            @Override
            public DisableConfig read(JsonReader in) throws IOException {
                in.beginObject();
                in.nextName();
                String name = in.nextString();
                in.nextName();
                String textureId = in.nextString();
                in.nextName();
                boolean disableAll = in.nextBoolean();
                in.nextName();
                in.beginArray();
                ArrayList<UVRectangle> rectangles = new ArrayList<>();
                while (in.hasNext()) {
                    in.beginObject();
                    in.nextName();
                    float uMin = (float) in.nextDouble();
                    in.nextName();
                    float vMin = (float) in.nextDouble();
                    in.nextName();
                    float uMax = (float) in.nextDouble();
                    in.nextName();
                    float vMax = (float) in.nextDouble();
                    in.endObject();
                    rectangles.add(new UVRectangle(uMin, vMin, uMax, vMax));
                }
                in.endArray();
                in.endObject();
                return new DisableConfig(name, textureId, disableAll, rectangles.toArray(UVRectangle[]::new));
            }
        }
    }

    public record UVRectangle(float uMin, float vMin, float uMax, float vMax) {
        public static UVRectangle read(FriendlyByteBuf byteBuf) {
            return new UVRectangle(byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat());
        }

        public boolean contains(float u, float v) {
            return u >= uMin && u <= uMax && v >= vMin && v <= vMax;
        }

        public void write(FriendlyByteBuf byteBuf) {
            byteBuf.writeFloat(uMin);
            byteBuf.writeFloat(vMin);
            byteBuf.writeFloat(uMax);
            byteBuf.writeFloat(vMax);
        }
    }
}
