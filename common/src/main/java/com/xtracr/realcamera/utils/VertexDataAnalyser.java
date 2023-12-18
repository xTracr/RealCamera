package com.xtracr.realcamera.utils;

import com.xtracr.realcamera.RealCamera;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix2f;
import org.joml.Matrix3f;

import java.util.*;

public abstract class VertexDataAnalyser {
    private static final String KEY_ANALYSER = "message.xtracr_" + RealCamera.MODID + "_analyser_";
    private static final float defaultAccuracy = 0.00001f;
    private static final Map<Integer, Pair<Integer, Float>> indexResult = new HashMap<>();
    private static final Map<Integer, Pair<Integer, Integer>> orthogonalResult = new HashMap<>();
    private static final List<Integer> processedResults = new ArrayList<>();
    private static List<Integer> equivalenceClass = new ArrayList<>();
    private static boolean analysing;
    private static float accuracy = defaultAccuracy;
    private static int count;
    private static int mode;
    private static int ticks;
    public static final VertexDataCatcher catcher = new VertexDataCatcher(i -> analysing, i -> analysing);

    public static boolean isAnalysing() {
        return analysing;
    }

    public static boolean preAnalysing() {
        catcher.clear();
        return analysing;
    }

    public static List<Integer> getFinalResults(int index) {
        if (processedResults.isEmpty()) {
            printGameMessage(Text.translatable(KEY_ANALYSER + "notReady"));
            return null;
        }
        List<Integer> ret = new ArrayList<>(processedResults);
        final int target = index == -1 ? processedResults.get(0) : index;
        ret.add(target);
        if (equivalenceClass.contains(target)) {
            for (int i = 1; i <= Math.max(target, equivalenceClass.get(equivalenceClass.size() - 1) - target); i++) {
                boolean add = equivalenceClass.contains(target + i);
                if (add) ret.add(target + i);
                if (equivalenceClass.contains(target - i)) ret.add(target + i);
                else if (!add) break;
            }
        }
        return ret;
    }

    public static void tick() {
        if (!analysing) return;
        ticks--;
        if (ticks > 0) return;
        if (mode == 0) {
            int target = getResult(1, false).get(0);
            start(target + 1, 80, accuracy);
            return;
        }
        analysing = false;
        showResult(12, false);
    }

    public static void start(int mode, int ticks, float accuracy) {
        indexResult.clear();
        orthogonalResult.clear();
        processedResults.clear();
        equivalenceClass.clear();
        analysing = true;
        count = 0;
        VertexDataAnalyser.accuracy = accuracy;
        VertexDataAnalyser.mode = mode;
        VertexDataAnalyser.ticks = ticks;
        equivalenceClass.add(Math.max(0, mode - 1));
        if (mode >= 0) printGameMessage(Text.translatable(KEY_ANALYSER + "start"));
    }

    public static void showResult(int number, boolean detail) {
        if (indexResult.isEmpty()) {
            printGameMessage(Text.translatable(KEY_ANALYSER + "notReady"));
            return;
        }
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
        printGameMessage(Text.translatable(KEY_ANALYSER + "numberOfResults",
                Math.min(number, indexResult.keySet().size()), indexResult.keySet().size()));
        if (orthogonalResult.isEmpty()) return;
        if (processedResults.isEmpty()) {
            sorted = new ArrayList<>(orthogonalResult.keySet());
            sorted.sort(Comparator.comparingInt(i -> -Math.abs(orthogonalResult.get(i).getRight())));
            int leftIndex = sorted.get(0);
            sorted.sort(Comparator.comparingInt(i -> -Math.abs(orthogonalResult.get(i).getLeft())));
            int upIndex = sorted.get(0);
            processedResults.add(equivalenceClass.get(0));
            processedResults.add(orthogonalResult.get(upIndex).getLeft() > 0 ? upIndex : -upIndex - 1);
            processedResults.add(orthogonalResult.get(leftIndex).getLeft()> 0 ? leftIndex : -leftIndex - 1);
        }
        if (!equivalenceClass.contains(-1)) {
            equivalenceClass = simplifyList(equivalenceClass, Comparator.comparingInt(i -> i), (int) (count * 0.9));
            equivalenceClass = equivalenceClass.subList(0, Math.min(number, equivalenceClass.size()));
            equivalenceClass.add(-1);
        }
        if (detail) {
            buffer = new StringBuilder().append("[ ");
            for (int i : equivalenceClass) {
                if (i < 0) continue;
                buffer.append(i).append(" ");
            }
            printGameMessage(Text.translatable(KEY_ANALYSER + "equivalenceClass", buffer.append("]").toString()));
        }
        printGameMessage(Text.translatable(KEY_ANALYSER + "bindSuggestion", Text.literal("'autoBind'")
                .styled(s -> s.withColor(Formatting.GREEN)), processedResults.get(0), processedResults.get(1), processedResults.get(2)));
    }

    public static void analyse(ClientPlayerEntity player, float tickDelta) {
        if (!analysing) return;
        count++;
        Vec3d viewVector = player.getRotationVec(tickDelta);
        List<Pair<Integer, Float>> indexCash = new ArrayList<>();
        Matrix3f viewRotation = new Matrix3f().rotate(RotationAxis.POSITIVE_X.rotationDegrees(-player.getPitch(tickDelta)))
                .rotate(RotationAxis.POSITIVE_Y.rotationDegrees(player.getYaw(tickDelta)));
        analyseVertices(viewVector, viewRotation, indexCash);
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
    }

    private static List<Integer> getResult(int number , boolean byFrequency) {
        List<Integer> indexList = new ArrayList<>(indexResult.keySet());
        if (byFrequency) indexList.sort((i, j) -> indexResult.get(j).getLeft() - indexResult.get(i).getLeft());
        else indexList.sort(Comparator.comparingDouble(i -> -indexResult.get(i).getRight()));
        return indexList.subList(0, Math.min(number, indexList.size()));
    }

    private static <T> List<T> simplifyList(List<T> list, Comparator<T> comparator, int times) {
        Map<T, Integer> listCash = new HashMap<>();
        List<T> ret = new ArrayList<>();
        list.forEach(t -> listCash.put(t, listCash.getOrDefault(t, 0) + 1));
        listCash.forEach((t, i) -> { if (i >= times) ret.add(t); });
        ret.sort(comparator);
        return ret;
    }

    private static void analyseVertices(Vec3d viewVector, Matrix3f viewRotation, List<Pair<Integer, Float>> indexCash) {
        final float accuracy = mode == 0 ? defaultAccuracy : VertexDataAnalyser.accuracy;
        List<Vec3d> recorder = catcher.normalRecorder;
        List<Vec3d> cash = new ArrayList<>();
        List<Integer> orthogonalCash = new ArrayList<>();
        final int index0 = Math.min(equivalenceClass.get(0), recorder.size());
        if (equivalenceClass.size() == 1) {
            equivalenceClass.clear();
            equivalenceClass.add(index0);
        }
        Vec3d element0 = recorder.get(index0);
        cash.add(element0);
        double dotWithView0 = element0.dotProduct(viewVector);
        indexCash.add(new Pair<>(index0, (float) dotWithView0));
        for (int i = 0; i < recorder.size(); i++) {
            Vec3d element = recorder.get(i);
            double dotWith0 = element.dotProduct(element0);
            if (i == index0 || dotWith0 >= 1- defaultAccuracy) equivalenceClass.add(i);
            else if (dotWith0 < defaultAccuracy && -dotWith0 < defaultAccuracy) {
                boolean shouldAdd = true;
                if (!orthogonalCash.isEmpty()) for (int index : orthogonalCash) {
                    if (Math.abs(element.dotProduct(recorder.get(index))) > defaultAccuracy) shouldAdd = false;
                }
                if (shouldAdd) orthogonalCash.add(i);
            }

            double dotWithView = element.dotProduct(viewVector);
            if (i == index0 || -dotWithView > accuracy) continue;
            boolean skip = false;
            for (Vec3d vec : cash) {
                double dotAbs = Math.abs(vec.dotProduct(element));
                if (dotAbs >=  1-accuracy) skip = true;
            }
            if (skip) continue;
            cash.add(element);
            indexCash.add(new Pair<>(i, (float) dotWithView));
        }
        if (orthogonalCash.size() >= 2) {
            int index2 = orthogonalCash.get(1);
            int index1 = orthogonalCash.get(0);
            Matrix3f rotation = new Matrix3f(recorder.get(index2).toVector3f(), recorder.get(index1).toVector3f(),
                    recorder.get(equivalenceClass.get(0)).toVector3f());
            Pair<Integer, Integer> pair2 = orthogonalResult.getOrDefault(index2, new Pair<>(0, 0));
            Pair<Integer, Integer> pair1 = orthogonalResult.getOrDefault(index1, new Pair<>(0, 0));
            Matrix2f dotProduct = new Matrix2f(viewRotation.mul(rotation, new Matrix3f()));
            pair2.setRight(pair2.getRight() + Math.round(dotProduct.m00()));
            pair2.setLeft(pair2.getLeft() + Math.round(dotProduct.m01()));
            pair1.setRight(pair1.getRight() + Math.round(dotProduct.m10()));
            pair1.setLeft(pair1.getLeft() + Math.round(dotProduct.m11()));
            orthogonalResult.putIfAbsent(index2, pair2);
            orthogonalResult.putIfAbsent(index1, pair1);
        }
    }

    private static void printGameMessage(Text text) {
        MinecraftClient.getInstance().getMessageHandler().onGameMessage(text, false);
    }
}
