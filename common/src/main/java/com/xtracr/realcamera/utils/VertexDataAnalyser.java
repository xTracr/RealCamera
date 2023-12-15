package com.xtracr.realcamera.utils;

import com.xtracr.realcamera.RealCamera;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class VertexDataAnalyser {
    private static final String KEY_ANALYSER = "message.xtracr_" + RealCamera.MODID + "_analyser_";
    private static final Map<Integer, Pair<Integer, Float>> indexResult = new HashMap<>();
    private static final Map<Integer, Pair<Integer, Integer>> autoModeResult = new HashMap<>();
    private static final Map<Integer, Float> orthogonalVectors = new HashMap<>();
    public static final VertexDataCatcher catcher = new VertexDataCatcher();

    private static boolean readyToBind;
    private static boolean analysing;
    private static int count;
    private static int mode;
    private static int ticks;

    public static boolean isAnalysing() {
        return analysing;
    }

    public static MatrixStack getMatrixStack() {
        catcher.clear();
        return catcher.matrixStack;
    }

    public static void tick() {
        if (readyToBind) {
            ticks--;    // analysing must be false here
            if (ticks < -600) {
                readyToBind = false;
                printGameMessage(Text.translatable(KEY_ANALYSER + "willNotBind"));
            }
        }
        if (!analysing) return;
        ticks--;
        if (ticks > 0) return;
        if (mode == 0) {
            int target = getResult(1, false).get(0);
            start(-1 - target, 80);
            return;
        }
        analysing = false;
        showResult(12, false);
    }

    public static void start(int mode, int ticks) {
        if (readyToBind && mode == 0) {
            //TODO: set binding mode config
            readyToBind = false;
            int frontIndex = autoModeResult.get(-1).getLeft();
            int upIndex = autoModeResult.get(-2).getLeft();
            int leftIndex = autoModeResult.get(-2).getRight();
            printGameMessage(Text.translatable(KEY_ANALYSER + "autoBind", frontIndex, upIndex, leftIndex));
            return;
        }
        indexResult.clear();
        autoModeResult.clear();
        orthogonalVectors.clear();
        readyToBind = false;
        analysing = true;
        count = 0;
        VertexDataAnalyser.mode = mode;
        VertexDataAnalyser.ticks = ticks;
        if (mode >= 0) printGameMessage(Text.translatable(KEY_ANALYSER + "start"));
    }

    public static void showResult(int number, boolean detail) {
        printGameMessage(Text.translatable(KEY_ANALYSER + "showResult"));
        List<Integer> sorted = getResult(number, true);
        StringBuilder buffer = new StringBuilder();
        for (int i : sorted) {
            buffer.append(" [").append(i).append("]");
            if (!detail) continue;
            float f = Math.round(1000 * indexResult.get(i).getLeft() / (float) count) / 1000f;
            buffer.append(": ").append(f);
        }
        sorted = getResult(number, false);
        printGameMessage(Text.translatable(KEY_ANALYSER + "byFrequency", buffer.toString()));
        buffer = new StringBuilder();
        for (int i : sorted) {
            buffer.append(" [").append(i).append("]");
            if (!detail) continue;
            float c = Math.round(1000 * indexResult.get(i).getRight() / (float) count) / 1000f;
            buffer.append(": ").append(c);
        }
        printGameMessage(Text.translatable(KEY_ANALYSER + "byCorrelation", buffer.toString()));
        if (mode != 1 && !orthogonalVectors.isEmpty()) {
            buffer = new StringBuilder();
            sorted = new ArrayList<>(orthogonalVectors.keySet());
            sorted.sort((i, j) -> (int)((orthogonalVectors.get(i) - orthogonalVectors.get(j)) * 1000000));
            sorted = sorted.subList(0, Math.min(number, sorted.size()));
            for (int i : sorted) {
                buffer.append(" [").append(i).append("]");
                if (!detail) continue;
                float d = Math.round(1000 * orthogonalVectors.get(i) / (float) count) / 1000f;
                buffer.append(": ").append(d);
            }
            printGameMessage(Text.translatable(KEY_ANALYSER + "orthogonalVectors", buffer.toString()));
        }
        printGameMessage(Text.translatable(KEY_ANALYSER + "showResultEnd",
                Math.min(number, indexResult.keySet().size()), indexResult.keySet().size()));
        if (mode < 0 && orthogonalVectors.size() >= 2) {
            int upIndex;
            int leftIndex;
            if (!autoModeResult.containsKey(-1)) {
                sorted = new ArrayList<>(autoModeResult.keySet());
                sorted.sort((i, j) -> autoModeResult.get(j).getLeft() - autoModeResult.get(i).getLeft());
                upIndex = sorted.get(0);
                sorted.sort((i, j) -> autoModeResult.get(j).getRight() - autoModeResult.get(i).getRight());
                leftIndex = sorted.get(0);
                autoModeResult.put(-1, new Pair<>(-1 - mode, 0));
                autoModeResult.put(-2, new Pair<>(upIndex, leftIndex));
            } else {
                upIndex = autoModeResult.get(-2).getLeft();
                leftIndex = autoModeResult.get(-2).getRight();
            }
            readyToBind = true;
            count = 0;
            printGameMessage(Text.translatable(KEY_ANALYSER + "autoModeResult", Text.literal("'autoBind'")
                    .styled(s -> s.withColor(Formatting.GREEN)), Text.literal("30s").styled(s -> s.withColor(Formatting.YELLOW)),
                    -1 - mode, upIndex, leftIndex));
        }
    }

    public static void analyse(float accuracy, ClientPlayerEntity player, float tickDelta) {
        if (!analysing) return;
        count++;
        Vec3d viewVector = player.getRotationVec(tickDelta);
        List<Pair<Integer, Float>> indexCash = new ArrayList<>();
        if (mode == 1) poseMode(accuracy, viewVector, indexCash);
        else vertexMode(accuracy, viewVector, indexCash);
        for (Pair<Integer, Float> pair : indexCash) {
            int index = pair.getLeft();
            float dot = pair.getRight();
            if (indexResult.containsKey(index)) {
                Pair<Integer, Float> value = indexResult.get(index);
                value.setLeft(value.getLeft() + 1);
                value.setRight(value.getRight() + dot);
            } else {
                indexResult.put(index, new Pair<>(1, dot));
            }
        }
        if (mode < 0 && orthogonalVectors.size() >=2) {
            List<Integer> sorted = new ArrayList<>(orthogonalVectors.keySet());
            sorted.sort((i, j) -> (int)((orthogonalVectors.get(i) - orthogonalVectors.get(j)) * 1000000));
            sorted = sorted.subList(0, 2);
            int index0 = sorted.get(0);
            int index1 = sorted.get(1);
            Matrix3f viewRotation = new Matrix3f().rotate(RotationAxis.POSITIVE_X.rotationDegrees(-player.getPitch(tickDelta)))
                    .rotate(RotationAxis.POSITIVE_Y.rotationDegrees(player.getYaw(tickDelta)));
            Matrix3f rotation = new Matrix3f(catcher.normalRecorder.get(index0).toVector3f(),
                    catcher.normalRecorder.get(index1).toVector3f(),
                    catcher.normalRecorder.get(-1 - mode).toVector3f());
            Pair<Integer, Integer> pair0 = autoModeResult.getOrDefault(index0, new Pair<>(0, 0));
            Pair<Integer, Integer> pair1 = autoModeResult.getOrDefault(index1, new Pair<>(0, 0));
            if (viewRotation.mul(rotation, new Matrix3f()).determinant() > 0.5f) {
                rotation.mul(new Matrix3f(0, 1, 0, 1, 0, 0, 0, 0, 1));
                pair0.setRight(pair0.getRight() + 1);
                pair1.setLeft(pair1.getLeft() + 1);
            } else {
                pair0.setLeft(pair0.getLeft() + 1);
                pair1.setRight(pair1.getRight() + 1);
            }
            autoModeResult.putIfAbsent(index0, pair0);
            autoModeResult.putIfAbsent(index1, pair1);
            Vec3d eulerAngle = MathUtils.getEulerAngleYXZ(rotation).multiply(180.0D / Math.PI);
            eulerAngle.multiply(1);
        }
    }

    private static List<Integer> getResult(int number , boolean byFrequency) {
        List<Integer> indexList = new ArrayList<>(indexResult.keySet());
        if (byFrequency) indexList.sort((i, j) -> indexResult.get(j).getLeft() - indexResult.get(i).getLeft());
        else indexList.sort((i, j) -> (int)((indexResult.get(j).getRight() - indexResult.get(i).getRight()) * 1000000));
        return indexList.subList(0, Math.min(number, indexList.size()));
    }

    private static void poseMode(float accuracy, Vec3d viewVector, List<Pair<Integer, Float>> indexCash) {
        List<Matrix3f> recorder = catcher.matrixRecorder;
        List<Matrix3f> cash = new ArrayList<>();
        Matrix3f element0 = recorder.get(0);
        cash.add(element0);
        double dotWithView0 = new Vec3d(element0.m20(), element0.m21(), element0.m22()).multiply(-1d).dotProduct(viewVector);
        indexCash.add(new Pair<>(0, (float) dotWithView0));
        for (int i = 1; i < recorder.size(); i++) {
            boolean skip = false;
            Matrix3f element = recorder.get(i);
            float tr = element.m00() + element.m11() + element.m22();
            for (Matrix3f matrix : cash) {
                float cashTr = matrix.m00() + matrix.m11() + matrix.m22();
                float dif = Math.abs(tr - cashTr);
                if (dif <= accuracy) skip = true;
            }
            if (skip) continue;
            cash.add(element);
            double dotWithView = new Vec3d(element.m20(), element.m21(), element.m22()).multiply(-1d).dotProduct(viewVector);
            indexCash.add(new Pair<>(i, (float) dotWithView));
        }
    }

    private static void vertexMode(float accuracy, Vec3d viewVector, List<Pair<Integer, Float>> indexCash) {
        List<Vec3d> recorder = catcher.normalRecorder;
        List<Vec3d> cash = new ArrayList<>();
        int index0 = 0;
        if (mode >= 2) index0 = mode - 2 < recorder.size() ? mode - 2 : 0;
        else if (mode <= -1) index0 = -1 - mode < recorder.size() ? -1 - mode : 0;
        Vec3d element0 = recorder.get(index0);
        cash.add(element0);
        double dotWithView0 = element0.dotProduct(viewVector);
        indexCash.add(new Pair<>(index0, (float) dotWithView0));
        for (int i = 0; i < recorder.size(); i++) {
            if (i == index0) continue;
            boolean skip = false;
            Vec3d element = recorder.get(i);
            for (Vec3d vec : cash) {
                double dotAbs = Math.abs(vec.dotProduct(element));
                if (dotAbs >=  1-accuracy) skip = true;
            }
            if (skip) continue;
            cash.add(element);
            double dotWithView = element.dotProduct(viewVector);
            indexCash.add(new Pair<>(i, (float) dotWithView));
            double dotWith0 = Math.abs(element.dotProduct(element0));
            if (dotWith0 > accuracy) continue;
            boolean shouldPut = true;
            if (!orthogonalVectors.isEmpty()) for (int index : orthogonalVectors.keySet()) {
                double dotWith1 = Math.abs(element.dotProduct(recorder.get(index)));
                if (i == index || dotWith1 <= accuracy) continue;
                dotWith0 += dotWith1;
                shouldPut = false;
            }
            if (shouldPut) orthogonalVectors.put(i, orthogonalVectors.getOrDefault(i, 0f) + (float) dotWith0);
        }
    }

    private static void printGameMessage(Text text) {
        MinecraftClient.getInstance().getMessageHandler().onGameMessage(text, false);
    }
}
