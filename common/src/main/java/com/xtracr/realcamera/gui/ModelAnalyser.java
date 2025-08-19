package com.xtracr.realcamera.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.mixin.accessor.GuiGraphicsAccessor;
import com.xtracr.realcamera.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class ModelAnalyser extends VertexRecorder {
    private static final Set<RenderType> UNFOCUSABLE_RENDER_TYPES = Set.of(RenderType.armorEntityGlint(), RenderType.glintTranslucent(), RenderType.glint(), RenderType.entityGlint());
    private static final int primitiveArgb = 0x6F3333CC, forwardArgb = 0xFF00CC00, upwardArgb = 0xFFCC0000, leftArgb = 0xFF0000CC;
    private static final int focusedArgb = 0x7FFFFFFF, sideArgb = 0x3FFFFFFF;
    public final PoseStack poseStack = new PoseStack();
    private BindingContext bindingContext = BindingContext.EMPTY;
    private BindingTarget target = new BindingTarget();
    @Nullable
    private BuiltRecord focusedRecord, currentRecord;
    @Nullable
    private ScreenRectangle scissorArea;
    private int focusedIndex = -1;

    public ModelAnalyser() { }

    private static boolean intersects(VertexData[] p1, List<VertexData[]> primitives) {
        final float precision = 1.0E-05f;
        for (VertexData[] p2 : primitives) for (VertexData v1 : p1) for (VertexData v2 : p2) if (v1.pos().distanceToSqr(v2.pos()) < precision) return true;
        return false;
    }

    public void setup(BindingTarget target, GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        this.target = target;
        bindingContext = BindingContext.EMPTY;
        focusedRecord = currentRecord = null;
        scissorArea = new ScreenRectangle(x1, y1, x2 - x1, y2 - y1).transformAxisAligned(graphics.pose());
        focusedIndex = -1;
        if (!poseStack.isEmpty()) poseStack.popPose();
        poseStack.pushPose();
    }

    public String focusedTextureId() {
        if (focusedRecord == null) return null;
        return focusedRecord.textureId();
    }

    public Vec2 getFocusedUV() {
        if (focusedIndex == -1 || focusedRecord == null) return null;
        float u = 0, v = 0;
        VertexData[] primitive = focusedRecord.primitives()[focusedIndex];
        for (VertexData vertex : primitive) {
            u += vertex.u();
            v += vertex.v();
        }
        return new Vec2(u / primitive.length, v / primitive.length);
    }

    public void previewEffect(GuiGraphics graphics, int entitySize, boolean canSelect) {
        if (canSelect) drawFocused(graphics);
        Vec3 start = bindingContext.getPosition();
        Matrix3f normal = bindingContext.normal;
        if (normal.m00() == 0 && normal.m11() == 0 && normal.m22() == 0) return;
        drawNormal(graphics, start, new Vec3(normal.m20(), normal.m21(), normal.m22()), entitySize / 3, forwardArgb);
        drawNormal(graphics, start, new Vec3(normal.m10(), normal.m11(), normal.m12()), entitySize / 6, upwardArgb);
        drawNormal(graphics, start, new Vec3(normal.m00(), normal.m01(), normal.m02()), entitySize / 6, leftArgb);
    }

    public void drawSelected(GuiGraphics graphics, int entitySize) {
        drawFocusedPolyhedron(graphics);
        drawFocused(graphics);
        if (currentRecord == null) return;
        currentRecord.findPrimitive(target.getPosU(), target.getPosV()).ifPresent(primitive -> drawPrimitive(graphics, primitive, primitiveArgb, 1000));
        currentRecord.findPrimitive(target.getForwardU(), target.getForwardV()).ifPresent(primitive ->
                drawNormal(graphics, getPosition(primitive, target.getForwardU(), target.getForwardV()), primitive[0].normal(), entitySize / 2, forwardArgb));
        currentRecord.findPrimitive(target.getUpwardU(), target.getUpwardV()).ifPresent(primitive ->
                drawNormal(graphics, getPosition(primitive, target.getUpwardU(), target.getUpwardV()), primitive[0].normal(), entitySize / 2, upwardArgb));
    }

    public void analyse(int entitySize, int mouseX, int mouseY, int layers, boolean hideDisabled, String idInField) {
        target.setScale(target.getScale() * entitySize);
        genContext();
        records.removeIf(record -> {
            boolean isIdInField = !idInField.isBlank() && record.textureId().contains(idInField);
            return (hideDisabled && isIdInField) || (!isIdInField && target.getDisabledTextureIds().stream().anyMatch(record.textureId()::contains));
        });
        List<Triple> sortByDepth = new ArrayList<>();
        records.stream().filter(record -> !UNFOCUSABLE_RENDER_TYPES.contains(record.renderType())).forEach(record -> {
            VertexData[][] primitives = record.primitives();
            for (int i = 0, primitiveCount = primitives.length; i < primitiveCount; i++) {
                VertexData[] primitive = primitives[i];
                int[] xs = new int[primitive.length], ys = new int[primitive.length];
                for (int j = 0; j < primitive.length; j++) {
                    xs[j] = (int) primitive[j].x();
                    ys[j] = (int) primitive[j].y();
                }
                if (!new Polygon(xs, ys, primitive.length).contains(mouseX, mouseY)) continue;
                VertexData point = primitive[0];
                double deltaZ = point.normalZ() == 0 ? 0 : (point.normalX() * (mouseX - point.x()) + point.normalY() * (mouseY - point.y())) / point.normalZ();
                sortByDepth.add(new Triple(point.z() - deltaZ, record, i));
            }
        });
        if (!sortByDepth.isEmpty()) {
            sortByDepth.sort(Comparator.comparingDouble(triple -> -triple.depth));
            Triple result = sortByDepth.get(Math.min(sortByDepth.size() - 1, layers));
            focusedRecord = result.record;
            focusedIndex = result.index;
        }
    }

    @Override
    public void updateModel(Minecraft client, Entity entity, float deltaTick, PoseStack poseStack) {
        MultiVertexCatcher catcher = new SimpleMultiVertexCatcher();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        client.gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        dispatcher.setRenderShadow(false);
        dispatcher.render(entity, 0, 0, 0, deltaTick, poseStack, catcher, 0xF000f0);
        dispatcher.setRenderShadow(true);
        catcher.sendVertices(this);
    }

    @Override
    public BindingContext genContext() {
        for (BuiltRecord record : records) {
            BindingContext context = new BindingContext(target, true);
            record.setupContext(context);
            if (!context.available()) continue;
            bindingContext = context;
            currentRecord = record;
            bindingContext.init();
            return context;
        }
        return BindingContext.EMPTY;
    }

    private void drawPrimitive(GuiGraphics graphics, VertexData[] primitive, int argb, int offset) {
        if (primitive.length < 3) return;
        int x0 = (int) primitive[0].x(), y0 = (int) primitive[0].y(), z0 = (int) primitive[0].z() + offset;
        int x1 = (int) primitive[1].x(), y1 = (int) primitive[1].y(), z1 = (int) primitive[1].z() + offset;
        int x2 = (int) primitive[2].x(), y2 = (int) primitive[2].y(), z2 = (int) primitive[2].z() + offset;
        int x3 = x2, y3 = y2, z3 = z2;
        if (primitive.length > 3) {
            x3 = (int) primitive[3].x();
            y3 = (int) primitive[3].y();
            z3 = (int) primitive[3].z() + offset;
        }
        ((GuiGraphicsAccessor) graphics).getGuiRenderState().submitGuiElement(new ColoredQuadRenderState(
                RenderPipelines.GUI, TextureSetup.noTexture(),
                x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, argb, scissorArea));
    }

    private void drawNormal(GuiGraphics graphics, Vec3 start, Vec3 normal, int length, int argb) {
        Vec3 end = normal.scale(length).add(start);
        final double width = 0.65;
        int x0 = (int) (start.x() - width * normal.y()), y0 = (int) (start.y() + width * normal.x()), z0 = (int) start.z() + 1200;
        int x1 = (int) (end.x() - width * normal.y()), y1 = (int) (end.y() + width * normal.x()), z1 = (int) end.z() + 1200;
        int x2 = (int) (end.x() + width * normal.y()), y2 = (int) (end.y() - width * normal.x()), z2 = (int) end.z() + 1200;
        int x3 = (int) (start.x() + width * normal.y()), y3 = (int) (start.y() - width * normal.x()), z3 = (int) start.z() + 1200;
        ((GuiGraphicsAccessor) graphics).getGuiRenderState().submitGuiElement(new ColoredQuadRenderState(
                RenderPipelines.GUI, TextureSetup.noTexture(),
                x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, argb, scissorArea));
//        ((GuiGraphicsAccessor) graphics).getGuiRenderState().submitGuiElement(new ColoredLineRenderState(RenderPipelines.LINES, TextureSetup.noTexture(),
//                (int) start.x(), (int) start.y(), (int) start.z() + 9999, (int) end.x(), (int) end.y(), (int) end.z() + 9999,
//                (float) normal.x(), (float) normal.y(), (float) normal.z(), argb, null));
    }

    private void drawFocused(GuiGraphics graphics) {
        if (focusedIndex == -1 || focusedRecord == null) return;
        VertexData[] focused = focusedRecord.primitives()[focusedIndex];
        drawPrimitive(graphics, focused, focusedArgb, 1100);
        int length = focused.length;
        VertexData[] reversed = new VertexData[length];
        for (int i = 0; i < length; i++) reversed[i] = focused[length - 1 - i];
        drawPrimitive(graphics, reversed, focusedArgb, 1100);
    }

    private void drawFocusedPolyhedron(GuiGraphics graphics) {
        if (focusedIndex == -1 || focusedRecord == null) return;
        List<VertexData[]> polyhedron = new ArrayList<>();
        polyhedron.add(focusedRecord.primitives()[focusedIndex]);
        List<Integer> indexes = new ArrayList<>(List.of(focusedIndex));
        VertexData[][] primitives = focusedRecord.primitives();
        final int primitiveCount = primitives.length;
        boolean added;
        do {
            added = false;
            for (int i = 0; i < primitiveCount; i++) {
                VertexData[] primitive = primitives[i];
                if (indexes.contains(i) | !intersects(primitive, polyhedron)) continue;
                polyhedron.add(primitive);
                indexes.add(i);
                added = true;
            }
        } while (added);
        List<Integer> resultIndexes = new ArrayList<>(List.of(focusedIndex));
        for (int i = focusedIndex + 1; i < primitiveCount; i++) {
            if (!indexes.contains(i)) break;
            resultIndexes.add(i);
        }
        for (int i = focusedIndex - 1; i >= 0; i--) {
            if (!indexes.contains(i)) break;
            resultIndexes.add(i);
        }
        resultIndexes.forEach(i -> drawPrimitive(graphics, primitives[i], sideArgb, 1000));
    }

    record Triple(double depth, BuiltRecord record, int index) {}
}
