package com.xtracr.realcamera.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ExcludedRegion;
import com.xtracr.realcamera.config.ModConfig;
import com.xtracr.realcamera.util.LocUtil;
import com.xtracr.realcamera.util.MathUtil;
import com.xtracr.realcamera.util.VertexData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ModelViewScreen extends Screen {
    public final ModelAnalyser analyser = new ModelAnalyser();
    protected int xSize = 440, ySize = 240, widgetWidth = (xSize - ySize) / 4 - 8, widgetHeight = 18;
    protected int x, y;
    private boolean initialized;
    private int entitySize = 80, layers = 0, category = 0, page = 0;
    private double entityX, entityY;
    private float xRot, yRot;
    private String focusedTextureId;
    private Vec2 focusedUV;
    private EditBox textureIdField, nameField, disabledIdField;
    private NumberField<Float> forwardUField, forwardVField, upwardUField, upwardVField, posUField, posVField, scaleField, depthField;
    private NumberField<Integer> priorityField;
    private List<String> idsInClipBoard = new ArrayList<>();
    private final List<String> disabledIds = new ArrayList<>();
    private final List<ExcludedRegion> selectedExclusions = new ArrayList<>();
    private boolean exclusionMode = false;
    
    // Brush mode for continuous deletion
    private boolean brushMode = false;
    private long lastDeletionTime = 0;
    private static final long DELETION_DELAY = 20; // 20ms interval = 50 actions per second for faster response
    private int brushRadius = 10; // Default brush radius in pixels
    private boolean batchUpdating = false; // Flag to batch UI updates during brush painting
    private int batchAddedCount = 0; // Count of faces added in current batch
    private final CycleButton<Integer> selectingButton = createCyclingButton(Map.of(
                    0, LocUtil.MODEL_VIEW_WIDGET("forwardMode").withStyle(s -> s.withColor(ChatFormatting.GREEN)),
                    1, LocUtil.MODEL_VIEW_WIDGET("upwardMode").withStyle(s -> s.withColor(ChatFormatting.RED)),
                    2, LocUtil.MODEL_VIEW_WIDGET("posMode").withStyle(s -> s.withColor(ChatFormatting.BLUE))),
            widgetWidth * 2 + 4, LocUtil.MODEL_VIEW_WIDGET("selectMode"));
    private final CyclingTexturedButton pauseButton = new CyclingTexturedButton(0, 16, 0, 2);
    private final CyclingTexturedButton bindXButton = new CyclingTexturedButton(16, 16, 1, 2);
    private final CyclingTexturedButton bindYButton = new CyclingTexturedButton(16, 16, 0, 2);
    private final CyclingTexturedButton bindZButton = new CyclingTexturedButton(16, 16, 1, 2);
    private final CyclingTexturedButton bindRotButton = new CyclingTexturedButton(16, 16, 1, 2);
    private final CyclingTexturedButton showDisabled = new CyclingTexturedButton(32, 16, 0, 2);
    private final DoubleSlider entityPitchSlider = createSlider("pitch", widgetWidth * 2 + 4, -90.0, 90.0);
    private final DoubleSlider entityYawSlider = createSlider("yaw", widgetWidth * 2 + 4, -60.0, 60.0);
    private final DoubleSlider offsetXSlider = createSlider("offsetX", widgetWidth * 2 - 18, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
    private final DoubleSlider offsetYSlider = createSlider("offsetY", widgetWidth * 2 - 18, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
    private final DoubleSlider offsetZSlider = createSlider("offsetZ", widgetWidth * 2 - 18, ModConfig.MIN_DOUBLE, ModConfig.MAX_DOUBLE);
    private final DoubleSlider pitchSlider = createSlider("pitch", widgetWidth * 2 - 18, -180.0, 180.0);
    private final DoubleSlider yawSlider = createSlider("yaw", widgetWidth * 2 - 18, -180.0, 180.0);
    private final DoubleSlider rollSlider = createSlider("roll", widgetWidth * 2 - 18, -180.0, 180.0);

    public ModelViewScreen() {
        super(LocUtil.MODEL_VIEW_TITLE());
    }

    @Override
    protected void init() {
        super.init();
        x = (width - xSize) / 2;
        y = (height - ySize) / 2;
        initWidgets(category, page);
        if (!initialized) loadBindingTarget(RealCameraCore.currentTarget());
        initialized = true;
    }

    private void initWidgets(int category, int page) {
        this.category = category;
        this.page = page;
        clearWidgets();
        initLeftWidgets(category);
        addRenderableWidget(pauseButton).setPosition(x + (xSize - ySize) / 2 + 4, y + 4);
        addRenderableWidget(new TexturedButton(x + (xSize - ySize) / 2 + 22, y + 4, 16, 16, 0, 0, button -> {
            entitySize = 80;
            entityYawSlider.setValue(0);
            entityPitchSlider.setValue(0);
            entityX = entityY = 0;
            xRot = yRot = 0;
            layers = 0;
        }));
        initRightWidgets(category, page);
    }

    private void initLeftWidgets(final int category) {
        forwardUField = createFloatField(widgetWidth, 0, forwardUField);
        forwardVField = createFloatField(widgetWidth, 0, forwardVField);
        upwardUField = createFloatField(widgetWidth, 0, upwardUField);
        upwardVField = createFloatField(widgetWidth, 0, upwardVField);
        posUField = createFloatField(widgetWidth, 0, posUField);
        posVField = createFloatField(widgetWidth, 0, posVField);
        String textureId = textureIdField != null ? textureIdField.getValue() : "";
        textureIdField = createTextField(widgetWidth * 2 + 4, null);
        textureIdField.setMaxLength(1024);
        textureIdField.setValue(textureId);
        scaleField = createFloatField(widgetWidth, 1.0f, scaleField).setMax(64.0f);
        depthField = createFloatField(widgetWidth, 0.2f, depthField).setMax(16.0f);
        GridLayout grid = new GridLayout();
        grid.defaultCellSetting().padding(4, 2, 0, 0);
        LayoutSettings smallSettings = grid.newCellSettings().padding(5, 3, 1, 1);
        GridLayout.RowHelper rows = grid.createRowHelper(2);
        rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("settings"), widgetWidth, button -> initWidgets(0, page)));
        rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("preview"), widgetWidth, button -> initWidgets(category | 0b01, page)));
        rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("exclusion"), widgetWidth * 2 + 4, button -> {
            exclusionMode = true;
            initWidgets(0b100, page);
        }), 2);
        if ((category & 0b100) != 0) {
            // Exclusion mode
            rows.addChild(entityPitchSlider, 2);
            rows.addChild(entityYawSlider, 2);
            rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("exclusionHelp"), widgetWidth * 2 + 4, button -> {}), 2)
                    .setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("exclusionHelp"));
            rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("resetExclusions"), widgetWidth * 2 + 4, button -> {
                selectedExclusions.clear();
                initWidgets(category, page);
            }), 2).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("resetExclusions"));
            
            // Add brush mode toggle button
            Component brushButtonText = brushMode ? 
                LocUtil.MODEL_VIEW_WIDGET("brushModeOn").withStyle(ChatFormatting.GREEN) :
                LocUtil.MODEL_VIEW_WIDGET("brushModeOff").withStyle(ChatFormatting.GRAY);
            rows.addChild(createButton(brushButtonText, widgetWidth * 2 + 4, button -> {
                brushMode = !brushMode;
                initWidgets(category, page); // Refresh to update button text
            }), 2);
            rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("saveExclusions"), widgetWidth * 2 + 4, button -> {
                BindingTarget target = generateBindingTarget();
                target.setExcludedRegions(new ArrayList<>(selectedExclusions));
                ConfigFile.config().binding.putTarget(target);
                ConfigFile.save();
                
                // Debug: Log what was saved
                System.out.println("[RealCamera] Saved target: " + target.name + 
                                 " with " + selectedExclusions.size() + " excluded regions");
                
                exclusionMode = false;
                initWidgets(0, page);
            }), 2).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("saveExclusions"));
            // Show count of selected exclusions
            rows.addChild(new StringWidget(0, 0, widgetWidth * 2 + 4, widgetHeight, 
                    LocUtil.literal("Selected: " + selectedExclusions.size()), font), 2);
        } else if ((category & 0b1) == 0) {
            rows.addChild(entityPitchSlider, 2);
            rows.addChild(entityYawSlider, 2);
            rows.addChild(selectingButton, 2).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("selectMode"));
            rows.addChild(forwardUField, 1, smallSettings);
            rows.addChild(forwardVField, 1, smallSettings);
            rows.addChild(upwardUField, 1, smallSettings);
            rows.addChild(upwardVField, 1, smallSettings);
            rows.addChild(posUField, 1, smallSettings);
            rows.addChild(posVField, 1, smallSettings);
            rows.addChild(textureIdField, 2, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("textureId"));
        } else {
            LayoutSettings sliderSettings = grid.newCellSettings().padding(-20, 2, 0, 0);
            rows.addChild(bindXButton, 1, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("bindButtons"));
            rows.addChild(offsetXSlider, 1, sliderSettings);
            rows.addChild(bindYButton, 1, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("bindButtons"));
            rows.addChild(offsetYSlider, 1, sliderSettings);
            rows.addChild(bindZButton, 1, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("bindButtons"));
            rows.addChild(offsetZSlider, 1, sliderSettings);
            rows.addChild(bindRotButton, 1, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("bindButtons"));
            rows.addChild(pitchSlider, 1, sliderSettings);
            rows.addChild(yawSlider, 2, grid.newCellSettings().padding(26, 2, 0, 0));
            rows.addChild(new TexturedButton(0, 0, button -> {
                offsetXSlider.setValue(0);
                offsetYSlider.setValue(0);
                offsetZSlider.setValue(0);
                pitchSlider.setValue(0);
                yawSlider.setValue(0);
                rollSlider.setValue(0);
            }), 1, smallSettings);
            rows.addChild(rollSlider, 1, sliderSettings);
            rows.addChild(scaleField, 1, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("scale"));
            rows.addChild(depthField, 1, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("depth"));
        }
        rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("save"), widgetWidth, button -> {
            ConfigFile.config().binding.putTarget(generateBindingTarget());
            ConfigFile.save();
            initWidgets(category, page);
        }));
        rows.addChild(priorityField = NumberField.ofInt(font, widgetWidth - 2, widgetHeight - 2, 0, priorityField), 1, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("priority"));
        rows.addChild(nameField = createTextField(widgetWidth * 2 + 4, nameField), 2, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("targetName"));
        nameField.setMaxLength(20);
        grid.arrangeElements();
        FrameLayout.alignInRectangle(grid, x, y + 2, x + (xSize - ySize) / 2 - 4, y + ySize, 0, 0);
        grid.visitWidgets(this::addRenderableWidget);
    }

    private void initRightWidgets(final int category, final int page) {
        disabledIdField = createTextField(widgetWidth * 2 + 4, disabledIdField);
        disabledIdField.setMaxLength(1024);
        GridLayout grid = new GridLayout();
        grid.defaultCellSetting().padding(4, 2, 0, 0);
        LayoutSettings smallSettings = grid.newCellSettings().padding(5, 3, 1, 1);
        GridLayout.RowHelper rows = grid.createRowHelper(4);
        rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("configs"), widgetWidth, button -> initWidgets(category & 0b01, 0)), 2).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("configs"));
        rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("disable"), widgetWidth, button -> initWidgets(0b11, 0)), 2).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("disable"));
        final int widgetsPerPage, size;
        if ((category & 0b10) == 0) {
            widgetsPerPage = 8;
            List<BindingTarget> fixedTargetList = ConfigFile.config().getFixedTargetList().stream().filter(target -> target.name.equals(RealCameraCore.currentTarget().name)).toList();
            List<BindingTarget> targetList = ConfigFile.config().getTargetList();
            final int fixedTargetCount = fixedTargetList.size();
            size = fixedTargetCount + targetList.size();
            for (int i = page * widgetsPerPage; i < Math.min((page + 1) * widgetsPerPage, size); i++) {
                BindingTarget target = i < fixedTargetCount ? fixedTargetList.get(i) : targetList.get(i - fixedTargetCount);
                String name = target.name;
                rows.addChild(createButton(LocUtil.literal(name), widgetWidth * 2 - 18, button -> loadBindingTarget(target)), 3).setTooltip(Tooltip.create(LocUtil.literal(name)));
                if (i < fixedTargetCount) continue;
                rows.addChild(new TexturedButton(48, 0, button -> {
                    targetList.remove(target);
                    ConfigFile.save();
                    initWidgets(category, page * widgetsPerPage > size - 2 && size > 1 ? page - 1 : page);
                }), 1, smallSettings);
            }
        } else {
            widgetsPerPage = 5;
            size = disabledIds.size();
            rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("copy"), widgetWidth, button -> idsInClipBoard = List.copyOf(disabledIds)), 2).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("copy"));
            rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("paste"), widgetWidth, button -> {
                idsInClipBoard.stream().filter(textureId -> !disabledIds.contains(textureId)).forEach(disabledIds::add);
                initWidgets(category, 0);
            }), 2);
            rows.addChild(disabledIdField, 4, smallSettings).setTooltip(LocUtil.MODEL_VIEW_TOOLTIP("disabledIdField"));
            rows.addChild(createButton(LocUtil.MODEL_VIEW_WIDGET("clear"), widgetWidth, button -> {
                disabledIds.clear();
                initWidgets(category, 0);
            }), 2);
            rows.addChild(showDisabled, 1, grid.newCellSettings().padding(7, 3, 1, 1));
            rows.addChild(new TexturedButton(64, 0, button -> {
                String disabledId = disabledIdField.getValue();
                if (disabledId.isBlank() || disabledIds.contains(disabledId)) return;
                disabledIds.add(disabledId);
                initWidgets(category, page);
            }), 1, smallSettings);
            for (int i = page * widgetsPerPage; i < Math.min((page + 1) * widgetsPerPage, size); i++) {
                String textureId = disabledIds.get(i);
                rows.addChild(createButton(LocUtil.literal(textureId), widgetWidth * 2 - 18, button -> disabledIdField.setValue(textureId)), 3).setTooltip(Tooltip.create(LocUtil.literal(textureId)));
                rows.addChild(new TexturedButton(48, 0, button -> {
                    disabledIds.remove(textureId);
                    initWidgets(category, page * widgetsPerPage > size - 2 && size > 1 ? page - 1 : page);
                }), 1, smallSettings);
            }
        }
        grid.arrangeElements();
        FrameLayout.alignInRectangle(grid, x + (xSize + ySize) / 2 + 4, y + 2, x + xSize, y + ySize, 0, 0);
        grid.visitWidgets(this::addRenderableWidget);
        final int pages = (size - 1) / widgetsPerPage + 1;
        addRenderableWidget(new TexturedButton(x + (xSize + ySize) / 2 + 8, y + ySize - 20, 16, 16, 16, 0, button -> initWidgets(category, (page - 1 + pages) % pages)));
        addRenderableWidget(new StringWidget(x + (xSize + ySize) / 2 + 30, y + ySize - 20, widgetWidth * 2 - 40, widgetHeight, LocUtil.literal((page + 1) + " / " + pages), font));
        addRenderableWidget(new TexturedButton(x + xSize - 21, y + ySize - 20, 16, 16, 32, 0, button -> initWidgets(category, (page + 1) % pages)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        renderEntityInViewArea(graphics, x + (xSize - ySize) / 2, y, x + (xSize + ySize) / 2, y + ySize, mouseX, mouseY, minecraft.player);
        
        // Handle continuous exclusion in brush mode
        if (brushMode && exclusionMode && mouseInViewArea(mouseX, mouseY)) {
            // Directly check if left mouse button is currently pressed
            boolean leftPressed = GLFW.glfwGetMouseButton(
                minecraft.getWindow().getWindow(), 
                GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            
            if (leftPressed) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastDeletionTime >= DELETION_DELAY) {
                    // Convert absolute mouse coordinates to relative coordinates within the view area
                    int relativeX = mouseX - (x + (xSize - ySize) / 2);
                    int relativeY = mouseY - y;
                    if (analyser != null) {
                        performBrushDeletion(relativeX, relativeY);
                        lastDeletionTime = currentTime;
                    }
                }
            }
        }
        
        // Render brush mode indicator
        if (brushMode) {
            renderBrushIndicator(graphics, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderBackground(graphics, mouseX, mouseY, delta);
        graphics.fill(x, y, x + (xSize - ySize) / 2 - 4, y + ySize, 0xFF444444);
        graphics.fill(x + (xSize - ySize) / 2, y, x + (xSize + ySize) / 2, y + ySize, 0xFF222222);
        graphics.fill(x + (xSize + ySize) / 2 + 4, y, x + xSize, y + ySize, 0xFF444444);
    }

    protected void renderEntityInViewArea(GuiGraphics graphics, int x1, int y1, int x2, int y2, int mouseX, int mouseY, LivingEntity entity) {
        float centerX = (float) (x1 + x2) / 2.0f;
        float centerY = (float) (y1 + y2) / 2.0f;
        graphics.enableScissor(x1, y1, x2, y2);
        Quaternionf quaternionf = new Quaternionf().rotateX((float) Math.PI / 6 + xRot).rotateY((float) Math.PI / 6 + yRot).rotateZ((float) Math.PI);
        float entityBodyYaw = entity.yBodyRot;
        float entityYaw = entity.getYRot();
        float entityPitch = entity.getXRot();
        float entityPrevHeadYaw = entity.yHeadRotO;
        float entityHeadYaw = entity.yHeadRot;
        entity.yBodyRot = 180.0f;
        entity.setYRot(180.0f + (float) entityYawSlider.getValue());
        entity.setXRot((float) entityPitchSlider.getValue());
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();
        Vector3f vector3f = new Vector3f((float) entityX, (float) entityY + entity.getBbHeight() / 2.0f, -2.0f);
        renderEntityWithAnalyser(graphics, centerX, centerY, mouseX, mouseY, vector3f, quaternionf, entity);
        entity.yBodyRot = entityBodyYaw;
        entity.setYRot(entityYaw);
        entity.setXRot(entityPitch);
        entity.yHeadRotO = entityPrevHeadYaw;
        entity.yHeadRot = entityHeadYaw;
        graphics.disableScissor();
    }

    protected void renderEntityWithAnalyser(GuiGraphics graphics, float x, float y, int mouseX, int mouseY, Vector3f offset, Quaternionf quaternionf, LivingEntity entity) {
        analyser.setup(generateBindingTarget());
        analyser.setSelectedExclusions(selectedExclusions);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(entitySize, entitySize, -entitySize);
        graphics.pose().translate(offset.x(), offset.y(), offset.z());
        graphics.pose().mulPose(quaternionf);
        analyser.updateModel(Minecraft.getInstance(), entity, 1.0f, graphics.pose());
        analyser.analyse(entitySize, mouseX, mouseY, layers, showDisabled.getValue() == 1, disabledIdField.getValue());
        
        // Render model with exclusions (hide excluded parts in exclusion mode)
        if (exclusionMode && (category & 0b100) != 0) {
            analyser.renderWithExclusions(graphics, selectedExclusions);
        } else {
            analyser.records().forEach(record -> VertexData.renderVertices(record.vertices(), graphics.bufferSource().getBuffer(record.renderType())));
        }
        graphics.flush();
        focusedUV = analyser.getFocusedUV();
        focusedTextureId = analyser.focusedTextureId();
        if (exclusionMode && (category & 0b100) != 0) {
            analyser.drawExcludedRegions(graphics, entitySize);
        } else if ((category & 0b1) == 0) {
            analyser.drawSelected(graphics, entitySize);
        } else {
            analyser.previewEffect(graphics, entitySize, (category & 0b10) == 2);
        }
        graphics.pose().popPose();
    }

    protected BindingTarget generateBindingTarget() {
        // 使用当前target的name和textureId，如果为空则使用界面输入的值
        BindingTarget currentTarget = RealCameraCore.currentTarget();
        String name = nameField.getValue();
        String textureId = textureIdField.getValue();
        
        // 如果当前有配置且name/textureId没有改变，则保持原有的
        if (!currentTarget.isEmpty() && currentTarget.name.equals(name) && currentTarget.textureId.equals(textureId)) {
            name = currentTarget.name;
            textureId = currentTarget.textureId;
        }
        
        return BindingTarget.create(name, textureId).setPriority(priorityField.getNumber())
                .setForwardU(forwardUField.getNumber()).setForwardV(forwardVField.getNumber())
                .setUpwardU(upwardUField.getNumber()).setUpwardV(upwardVField.getNumber())
                .setPosU(posUField.getNumber()).setPosV(posVField.getNumber())
                .setDisablingDepth(depthField.getNumber())
                .setBindX(bindXButton.getValue() == 0).setBindY(bindYButton.getValue() == 0).setBindZ(bindZButton.getValue() == 0).setBindRotation(bindRotButton.getValue() == 0)
                .setScale(scaleField.getNumber()).setOffsetX(offsetXSlider.getValue()).setOffsetY(offsetYSlider.getValue()).setOffsetZ(offsetZSlider.getValue())
                .setPitch((float) pitchSlider.getValue()).setYaw((float) yawSlider.getValue()).setRoll((float) rollSlider.getValue())
                .setDisabledTextureIds(List.copyOf(disabledIds))
                .setExcludedRegions(new ArrayList<>(selectedExclusions)); // 添加excludedRegions
    }

    protected void loadBindingTarget(BindingTarget target) {
        if (target.isEmpty()) return;
        nameField.setValue(target.name);
        textureIdField.setValue(target.textureId);
        priorityField.setNumber(target.getPriority());
        forwardUField.setNumber(target.getForwardU());
        forwardVField.setNumber(target.getForwardV());
        upwardUField.setNumber(target.getUpwardU());
        upwardVField.setNumber(target.getUpwardV());
        posUField.setNumber(target.getPosU());
        posVField.setNumber(target.getPosV());
        depthField.setNumber(target.getDisablingDepth());
        scaleField.setNumber((float) target.getScale());
        bindXButton.setValue(target.isBindX() ? 0 : 1);
        offsetXSlider.setValue(target.getOffsetX());
        bindYButton.setValue(target.isBindY() ? 0 : 1);
        offsetYSlider.setValue(target.getOffsetY());
        bindZButton.setValue(target.isBindZ() ? 0 : 1);
        offsetZSlider.setValue(target.getOffsetZ());
        bindRotButton.setValue(target.isBindRotation() ? 0 : 1);
        pitchSlider.setValue(target.getPitch());
        yawSlider.setValue(target.getYaw());
        rollSlider.setValue(target.getRoll());
        disabledIds.clear();
        disabledIds.addAll(target.getDisabledTextureIds());
        selectedExclusions.clear();
        selectedExclusions.addAll(target.getExcludedRegions());
        
        // Debug: Log what was loaded
        System.out.println("[RealCamera] Loaded target: " + target.name + 
                         " with " + target.getExcludedRegions().size() + " excluded regions");
    }

    private Button createButton(Component message, int width, Button.OnPress onPress) {
        return Button.builder(message, onPress).size(width, widgetHeight).build();
    }

    private CycleButton<Integer> createCyclingButton(Map<Integer, Component> messages, int width, Component optionText) {
        return new CycleButton.Builder<Integer>(messages::get).withValues(messages.keySet()).create(0, 0, width, widgetHeight, optionText);
    }

    private DoubleSlider createSlider(String key, int width, double min, double max) {
        return new DoubleSlider(width, widgetHeight, 0, min, max, d -> LocUtil.MODEL_VIEW_WIDGET(key, MathUtil.round(d, 2)));
    }

    private NumberField<Float> createFloatField(int width, float defaultValue, @Nullable NumberField<Float> copyFrom) {
        return NumberField.ofFloat(font, width - 2, widgetHeight - 2, defaultValue, copyFrom).setMax(1.0f).setMin(0f);
    }

    private EditBox createTextField(int width, @Nullable EditBox copyFrom) {
        return new EditBox(font, 0, 0, width - 2, widgetHeight - 2, copyFrom, Component.empty());
    }

    protected boolean mouseInViewArea(double mouseX, double mouseY) {
        return mouseX >= x + (double) (xSize - ySize) / 2 && mouseX <= x + (double) (xSize + ySize) / 2 && mouseY >= y && mouseY <= y + ySize;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseInViewArea(mouseX, mouseY) && focusedUV != null && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (exclusionMode && (category & 0b100) != 0) {
                if (brushMode) {
                    // In brush mode, perform immediate exclusion on click
                    // Convert absolute mouse coordinates to relative coordinates within the view area
                    int relativeX = (int)mouseX - (x + (xSize - ySize) / 2);
                    int relativeY = (int)mouseY - y;
                    performBrushDeletion(relativeX, relativeY);
                    return true;
                } else {
                    // Normal mode: toggle exclusion
                    ExcludedRegion newRegion = analyser.generateExclusionSignature();
                    if (newRegion != null) {
                        // Check if this region is already selected, if so remove it
                        boolean removed = selectedExclusions.removeIf(r -> 
                            r.getTextureId().equals(newRegion.getTextureId()) &&
                            r.getCenter().equals(newRegion.getCenter()));
                        
                        if (!removed) {
                            // Add the new exclusion
                            selectedExclusions.add(newRegion);
                        }
                        initWidgets(category, page); // Refresh to update count
                    }
                    return true;
                }
            } else if (InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_ALT)) {
                if (category == 0) {
                    if (selectingButton.getValue() == 0) {
                        forwardUField.setNumber(focusedUV.x);
                        forwardVField.setNumber(focusedUV.y);
                    } else if (selectingButton.getValue() == 1) {
                        upwardUField.setNumber(focusedUV.x);
                        upwardVField.setNumber(focusedUV.y);
                    } else {
                        posUField.setNumber(focusedUV.x);
                        posVField.setNumber(focusedUV.y);
                    }
                    textureIdField.setValue(focusedTextureId);
                    return true;
                } else if (category == 0b11) {
                    disabledIdField.setValue(focusedTextureId);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (mouseInViewArea(mouseX, mouseY)) {
            // Prevent rotation in brush mode to allow continuous deletion
            if (brushMode && exclusionMode && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                // In brush mode, dragging continues deletion instead of rotating
                return true;
            }
            
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_ALT)) {
                xRot += (float) (Math.PI * deltaY / ySize);
                yRot -= (float) (Math.PI * deltaX / ySize);
                return true;
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                entityX += deltaX / entitySize;
                entityY += deltaY / entitySize;
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseInViewArea(mouseX, mouseY)) {
            if (InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_ALT)) {
                layers = Math.max(0, layers + (int) verticalAmount);
            } else {
                entitySize = Mth.clamp(entitySize + (int) verticalAmount * entitySize / 16, 16, 1024);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void performBrushDeletion(int mouseX, int mouseY) {
        if (analyser == null) return;
        
        // Calculate absolute coordinates for the model view area
        // These coordinates match the ones used in renderEntityInViewArea
        int viewX = x + (xSize - ySize) / 2;
        int viewY = y;
        int absoluteX = viewX + mouseX;
        int absoluteY = viewY + mouseY;
        
        // Use the new collection method that works with current frame data
        // This ensures we're using the same vertex coordinates as the rendered model
        // No re-analysis needed - we use the data from the last updateModel call
        List<ExcludedRegion> regionsToExclude = analyser.collectExclusionsInBrushArea(
            absoluteX, absoluteY, brushRadius);
        
        if (regionsToExclude.isEmpty()) {
            return; // No faces to exclude in the brush area
        }
        
        // Set batch updating flag to prevent UI refreshes
        if (!batchUpdating) {
            batchUpdating = true;
            batchAddedCount = 0;
        }
        
        // Brush mode behavior: only exclude, never restore
        // This allows users to quickly paint exclusions without accidentally removing them
        for (ExcludedRegion region : regionsToExclude) {
            if (!isAlreadyExcluded(region)) {
                selectedExclusions.add(region);
                batchAddedCount++;
            }
        }
        
        // Note: We don't call initWidgets here to avoid UI refresh lag
        // The count will be updated when user releases the mouse or exits brush mode
    }
    
    private boolean isAlreadyExcluded(ExcludedRegion region) {
        return selectedExclusions.stream().anyMatch(existing -> 
            matchExcludedRegions(existing, region)
        );
    }
    
    private boolean matchExcludedRegions(ExcludedRegion r1, ExcludedRegion r2) {
        // First check basic properties
        if (!r1.getTextureId().equals(r2.getTextureId())) return false;
        if (Math.abs(r1.getCenter().x - r2.getCenter().x) >= 0.001f) return false;
        if (Math.abs(r1.getCenter().y - r2.getCenter().y) >= 0.001f) return false;
        
        // Check vertex count - must match exactly
        if (r1.getVertexCount() > 0 && r2.getVertexCount() > 0) {
            if (r1.getVertexCount() != r2.getVertexCount()) return false;
        }
        
        // Check UV area with tight tolerance
        if (r1.getUVArea() > 0 && r2.getUVArea() > 0) {
            if (Math.abs(r1.getUVArea() - r2.getUVArea()) > 0.01f) return false;
        }
        
        // Check neighborhood hash if available
        if (r1.getNeighborhoodHash() > 0 && r2.getNeighborhoodHash() > 0) {
            if (Math.abs(r1.getNeighborhoodHash() - r2.getNeighborhoodHash()) > 20) return false;
        }
        
        return true;
    }
    
    private void renderBrushIndicator(GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw brush mode text with size info
        graphics.drawString(font, LocUtil.MODEL_VIEW_WIDGET("brushModeTitle").getString(), x + 10, y + 10, 0x00FF00);
        graphics.drawString(font, LocUtil.MODEL_VIEW_WIDGET("brushSize", brushRadius).getString(), x + 10, y + 20, 0x00FF00);
        
        // Show current batch count if actively painting
        if (batchUpdating && batchAddedCount > 0) {
            String batchText = "Added: " + batchAddedCount + " faces";
            graphics.drawString(font, batchText, x + 10, y + 30, 0xFFFF00);
        }
        
        // Draw brush circle outline
        if (mouseInViewArea(mouseX, mouseY)) {
            // Calculate potential exclusions for visual feedback
            if (analyser != null && !batchUpdating) {
                int viewX = x + (xSize - ySize) / 2;
                int viewY = y;
                int absoluteX = viewX + (mouseX - (x + (xSize - ySize) / 2));
                int absoluteY = viewY + (mouseY - y);
                
                List<ExcludedRegion> potentialExclusions = analyser.collectExclusionsInBrushArea(
                    absoluteX, absoluteY, brushRadius);
                
                int newExclusions = 0;
                for (ExcludedRegion region : potentialExclusions) {
                    if (!isAlreadyExcluded(region)) {
                        newExclusions++;
                    }
                }
                
                // Show number of faces that will be excluded
                if (newExclusions > 0) {
                    String text = "+" + newExclusions + " faces";
                    graphics.drawString(font, text, mouseX + brushRadius + 5, mouseY - 10, 0x00FF00);
                } else {
                    // Show if all faces in area are already excluded
                    if (!potentialExclusions.isEmpty()) {
                        String text = "Already excluded";
                        graphics.drawString(font, text, mouseX + brushRadius + 5, mouseY - 10, 0xFF8800);
                    }
                }
            }
            
            // Draw the actual brush area as a circle
            drawCircleOutline(graphics, mouseX, mouseY, brushRadius, 0x80FFFFFF);
            
            // Draw animated effect when mouse is pressed
            boolean leftPressed = GLFW.glfwGetMouseButton(
                minecraft.getWindow().getWindow(), 
                GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            
            if (leftPressed) {
                // Fill the brush area with semi-transparent red when pressing
                drawFilledCircle(graphics, mouseX, mouseY, brushRadius, 0x40FF0000);
            }
        }
    }
    
    private void drawCircleOutline(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        // Draw circle using line segments
        int segments = Math.max(16, radius * 2); // More segments for larger circles
        double angleStep = 2 * Math.PI / segments;
        
        for (int i = 0; i < segments; i++) {
            double angle1 = i * angleStep;
            double angle2 = (i + 1) * angleStep;
            
            int x1 = centerX + (int)(Math.cos(angle1) * radius);
            int y1 = centerY + (int)(Math.sin(angle1) * radius);
            int x2 = centerX + (int)(Math.cos(angle2) * radius);
            int y2 = centerY + (int)(Math.sin(angle2) * radius);
            
            // Draw a line segment
            graphics.fill(Math.min(x1, x2), Math.min(y1, y2), 
                         Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, color);
        }
    }
    
    private void drawFilledCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        // Fill circle using a square approximation with distance check
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                if (dx * dx + dy * dy <= radius * radius) {
                    graphics.fill(centerX + dx, centerY + dy, 
                                centerX + dx + 1, centerY + dy + 1, color);
                }
            }
        }
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Handle brush size adjustment in brush mode
        if (brushMode) {
            if (keyCode == GLFW.GLFW_KEY_EQUAL || keyCode == GLFW.GLFW_KEY_KP_ADD) { // + key
                brushRadius = Math.min(brushRadius + 2, 50); // Max radius 50
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) { // - key
                brushRadius = Math.max(brushRadius - 2, 5); // Min radius 5
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // When mouse is released after brush painting, update the UI to show the new count
        if (batchUpdating && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            batchUpdating = false;
            if (batchAddedCount > 0) {
                // Only refresh UI if we actually added some exclusions
                initWidgets(category, page);
            }
            batchAddedCount = 0;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean isPauseScreen() {
        return pauseButton.getValue() == 1;
    }
}
