package com.pigicial.wikirenderer.screen;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.components.IOStateComponent;
import com.pigicial.wikirenderer.components.NonResettingScrollContainer;
import com.pigicial.wikirenderer.components.NotificationComponent;
import com.pigicial.wikirenderer.mixin.access.ParticleEngineAccessor;
import com.pigicial.wikirenderer.property.*;
import com.pigicial.wikirenderer.render.DefaultRenderable;
import com.pigicial.wikirenderer.render.ParticleRestriction;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.render.TickingRenderable;
import com.pigicial.wikirenderer.render.area.AreaRenderable;
import com.pigicial.wikirenderer.render.area.side_view.MinimapCalibratorData;
import com.pigicial.wikirenderer.render.entity.EntityRenderable;
import com.pigicial.wikirenderer.render.export.ExportPathSpec;
import com.pigicial.wikirenderer.render.export.FileIO;
import com.pigicial.wikirenderer.render.export.RenderableDispatcher;
import com.pigicial.wikirenderer.render.export.ffmpeg.*;
import com.pigicial.wikirenderer.render.export.ffmpeg.live.LiveRenderFFmpegAnimationHandler;
import com.pigicial.wikirenderer.textures.TextureDataProvider;
import com.pigicial.wikirenderer.util.Translate;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Matrix4fStack;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.pigicial.wikirenderer.property.GlobalProperties.*;

public class RenderScreen extends BaseOwoScreen<FlowLayout> {

    private static final Int2ObjectMap<Consumer<DefaultPropertyBundle>> KEYBOARD_CONTROLS = new Int2ObjectOpenHashMap<>();

    static {
        KEYBOARD_CONTROLS.put(GLFW.GLFW_KEY_W, properties -> properties.yOffset.modify(-1000));
        KEYBOARD_CONTROLS.put(GLFW.GLFW_KEY_S, properties -> properties.yOffset.modify(1000));
        KEYBOARD_CONTROLS.put(GLFW.GLFW_KEY_D, properties -> properties.xOffset.modify(1000));
        KEYBOARD_CONTROLS.put(GLFW.GLFW_KEY_A, properties -> properties.xOffset.modify(-1000));

        KEYBOARD_CONTROLS.put(GLFW.GLFW_KEY_UP, properties -> properties.modifySlant(-5D));
        KEYBOARD_CONTROLS.put(GLFW.GLFW_KEY_DOWN, properties -> properties.modifySlant(5D));
        KEYBOARD_CONTROLS.put(GLFW.GLFW_KEY_LEFT, properties -> properties.modifyRotation(-10));
        KEYBOARD_CONTROLS.put(GLFW.GLFW_KEY_RIGHT, properties -> properties.modifyRotation(10));

        KEYBOARD_CONTROLS.put(GLFW.GLFW_KEY_RIGHT_BRACKET, properties -> properties.scale.modify(10));
        KEYBOARD_CONTROLS.put(GLFW.GLFW_KEY_SLASH, properties -> properties.scale.modify(-10));
    }

    public final MemoryGuard memoryGuard = new MemoryGuard(0.75f);

    private final FlowLayout notificationArea = UIContainers.verticalFlow(Sizing.content(), Sizing.content());
    private final IOStateComponent ioStateComponent = new IOStateComponent();

    private final FlowLayout leftAnchor = UIContainers.verticalFlow(Sizing.content(), Sizing.content());
    private final FlowLayout rightAnchor = UIContainers.verticalFlow(Sizing.content(), Sizing.content());

    private final FlowLayout leftColumn = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content()).gap(-4);
    private final FlowLayout rightColumn = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content()).gap(-4);

    public final Renderable<?> renderable;

    private boolean drawOnlyBackground = false;
    public boolean captureScheduled = false;
    public boolean guiRebuildScheduled = false;

    private int viewportBeginX;
    private int viewportEndX;
    private boolean hasBothColumns = false;

    public ButtonComponent exportButton = null;
    private Consumer<File> exportCallback = null;

    public Button exportAnimationButton;
    @Nullable
    public AnimationHandler currentAnimationExportData = null;

    public EditBox fileNameField = null;
    private double[] scrollOffsetData = null;

    public RenderScreen(Renderable<?> renderable) {
        this.renderable = renderable;
        this.memoryGuard.update();
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::horizontalFlow);
    }

    @Override
    protected void init() {
        this.viewportBeginX = (int) ((this.width - this.height) * 0.5);
        this.viewportEndX = (int) (this.width - (this.width - this.height) * 0.5) + 1;

        if (!guiRebuildScheduled) {
            this.saveScrollOffsetDataIfPossible();
        }

        this.leftAnchor.clearChildren();
        this.rightAnchor.clearChildren();

        if (this.viewportBeginX < 200) {
            this.viewportEndX -= this.viewportBeginX;
            this.viewportBeginX = 0;
            this.hasBothColumns = false;

            this.leftAnchor.horizontalSizing(Sizing.fixed(0)).verticalSizing(Sizing.fixed(this.height));
            this.rightAnchor.positioning(Positioning.absolute(viewportEndX + 5, 0)).horizontalSizing(Sizing.fixed(this.width - this.viewportEndX - 5)).verticalSizing(Sizing.fixed(this.height));

            this.rightAnchor.child(new NonResettingScrollContainer(ScrollContainer.ScrollDirection.VERTICAL, Sizing.fill(100), Sizing.fill(100), (FlowLayout) UIContainers.verticalFlow(Sizing.content(), Sizing.content(10))
                    .child(leftColumn)
                    .child(UIComponents.box(Sizing.fill(85), Sizing.fixed(1)).color(Color.ofDye(DyeColor.GRAY)).fill(true).margins(Insets.top(15)))
                    .child(rightColumn)
                    .horizontalAlignment(HorizontalAlignment.CENTER))

            );
        } else {
            this.hasBothColumns = true;

            this.leftAnchor.horizontalSizing(Sizing.fixed(viewportBeginX)).verticalSizing(Sizing.fixed(this.height));
            this.rightAnchor.positioning(Positioning.absolute(viewportEndX, 0)).horizontalSizing(Sizing.fixed(viewportBeginX)).verticalSizing(Sizing.fixed(this.height));

            this.leftAnchor.child(new NonResettingScrollContainer(ScrollContainer.ScrollDirection.VERTICAL, Sizing.fill(100), Sizing.fill(100), UIContainers.verticalFlow(Sizing.content(), Sizing.content(10)).child(this.leftColumn)));
            this.rightAnchor.child(new NonResettingScrollContainer(ScrollContainer.ScrollDirection.VERTICAL, Sizing.fill(100), Sizing.fill(100), UIContainers.verticalFlow(Sizing.content(), Sizing.content(10)).child(this.rightColumn)));
        }

        this.notificationArea.positioning(Positioning.absolute(this.viewportBeginX + 5, 5)).sizing(Sizing.fixed(this.height - 10));

        super.init();
        this.applyScrollData();
    }

    private void saveScrollOffsetDataIfPossible() {
        boolean hasScrollData = false;
        double leftScrollStep = 0;
        if (!this.leftAnchor.children().isEmpty()) {
            NonResettingScrollContainer container = (NonResettingScrollContainer) this.leftAnchor.children().getFirst();
            if (!container.children().isEmpty()) {
                hasScrollData = true;
                leftScrollStep = ((NonResettingScrollContainer) this.leftAnchor.children().getFirst()).getScrollOffset();
            }
        }

        double rightScrollStep = 0;
        if (!this.rightAnchor.children().isEmpty()) {
            NonResettingScrollContainer container = (NonResettingScrollContainer) this.rightAnchor.children().getFirst();
            if (!container.children().isEmpty()) {
                hasScrollData = true;
                rightScrollStep = ((NonResettingScrollContainer) this.rightAnchor.children().getFirst()).getScrollOffset();
            }
        }

        if (hasScrollData) {
            this.scrollOffsetData = new double[]{leftScrollStep, rightScrollStep};
        }
    }

    public void applyScrollData() {
        if (this.scrollOffsetData != null) {
            double leftScrollStep = this.scrollOffsetData[0];
            double rightScrollStep = this.scrollOffsetData[1];
            if (!this.leftAnchor.children().isEmpty() && leftScrollStep != 0) {
                ((NonResettingScrollContainer) this.leftAnchor.children().getFirst()).setScrollPosition(leftScrollStep);
            }
            if (!this.rightAnchor.children().isEmpty() && rightScrollStep != 0) {
                ((NonResettingScrollContainer) this.rightAnchor.children().getFirst()).setScrollPosition(rightScrollStep);
            }
            this.scrollOffsetData = null;
        }
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        this.minecraft.options.setCameraType(CameraType.FIRST_PERSON);

        // todo maybe we dont need this line?
        ((ParticleEngineAccessor) Minecraft.getInstance().particleEngine).wikirenderer$getParticles().clear();
        WikiRenderer.particleRestriction = this.renderable.getParticleRestriction();

        this.leftColumn.margins(Insets.top(20));
        this.rightColumn.margins(Insets.top(20));

        rootComponent.child(this.leftAnchor.padding(Insets.left(10)).positioning(Positioning.absolute(0, 0)));
        rootComponent.child(this.rightAnchor.padding(Insets.left(10)));

        rootComponent.child(this.notificationArea.child(this.ioStateComponent.positioning(Positioning.relative(0, 100)))
                .horizontalAlignment(HorizontalAlignment.RIGHT)
                .verticalAlignment(VerticalAlignment.BOTTOM)
                .padding(Insets.of(5))
        );

        this.renderable.getProperties().buildMainGUIControls(this.renderable, this, this.leftColumn);

        if (renderable instanceof TextureDataProvider textureProvider) {
            textureProvider.buildTextureGrabSection(this, leftColumn);
        }

        WikiRendererUI.sectionHeader(rightColumn, "render_options", false);
        this.buildBackgroundColorGUIControls();
        this.renderable.getProperties().buildRenderOptionGUIControls(this.renderable, this, this.rightColumn);

        WikiRendererUI.sectionHeader(rightColumn, "export_options", true);
        this.renderable.getProperties().buildExportOptionGUIControls(this.renderable, this, this.rightColumn);
        this.renderable.getProperties().buildExportResolutionGUIControls(this.renderable, this, this.rightColumn);
        this.renderable.getProperties().buildFileNameGUIControls(this.renderable, this, this.rightColumn);

        WikiRendererUI.sectionHeader(rightColumn, "animation_options", true);
        this.buildFFmpegSection();
    }

    private void buildBackgroundColorGUIControls() {
        EditBox colorField = WikiRendererUI.labelledTextField(rightColumn, "#000000", "background_color", Sizing.fixed(50));
        colorField.setFilter(s -> s.matches("^#([A-Fa-f\\d]{0,6})$"));
        colorField.setValue("#" + String.format("%02X", backgroundColor >> 16) + String.format("%02X", backgroundColor >> 8 & 0xFF) + String.format("%02X", backgroundColor & 0xFF));
        colorField.moveCursorToStart(false);
        colorField.setResponder(s -> {
            if (s.substring(1).length() < 6) return;
            backgroundColor = Integer.parseInt(s.substring(1), 16);
        });
    }

    private void buildFFmpegSection() {
        if (!FFmpegDispatcher.wasFFmpegDetected()) {
            WikiRendererUI.sectionHeader(rightColumn, "detecting_ffmpeg", false);
            FFmpegDispatcher.detectFFmpeg().whenComplete((aBoolean, throwable) -> this.guiRebuildScheduled = true);
            return;
        }

        if (!FFmpegDispatcher.ffmpegAvailable()) {
            WikiRendererUI.sectionHeader(rightColumn, "no_ffmpeg_1", true);
            WikiRendererUI.sectionHeader(rightColumn, "no_ffmpeg_2", false);
            WikiRendererUI.sectionHeader(rightColumn, "no_ffmpeg_3", false)
                    .cursorStyle(CursorStyle.HAND)
                    .mouseDown().subscribe((click, doubled) -> {
                        this.minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
                            if (confirmed) {
                                Util.getPlatform().openUri("https://ffmpeg.org/download.html");
                            }

                            this.minecraft.setScreen(this);
                        }, "https://ffmpeg.org/download.html", true));
                        return true;
                    });
            return;
        }

        if (renderable.getProperties() instanceof CroppablePropertyBundle croppablePropertyBundle) {
            Property<Boolean> animatedCropProperty = croppablePropertyBundle.getFFmpegCropProperty();
            WikiRendererUI.booleanControl(rightColumn, animatedCropProperty, "crop");
        }

        WikiRendererUI.booleanControl(rightColumn, SPEED_UP_ENCHANTMENT_GLINTS, "speed_up_enchantment_glints");
        SPEED_UP_ENCHANTMENT_GLINTS.futureListen((p, v) -> guiRebuildScheduled = true);

        if (SPEED_UP_ENCHANTMENT_GLINTS.get()) {
            rightColumn.child(UIComponents.button(Translate.gui("enchantment_glint_preset"), button -> {
                int seconds = 120000 / 8000;
                int framerate = 20;
                EXPORT_FRAMERATE.set(framerate);
                EXPORT_FRAMES.set(seconds * framerate);
            }).margins(Insets.vertical(5)));
        }

        WikiRendererUI.booleanControl(rightColumn, SYNC_ENCHANTMENT_GLINTS_TO_EXPORT, "sync_and_speed_up_glint_rendering");

        WikiRendererUI.labelledTextField(rightColumn, EXPORT_FRAMES, "animation_frames", Sizing.fixed(30));
        WikiRendererUI.labelledTextField(rightColumn, EXPORT_FRAMERATE, "animation_framerate", Sizing.fixed(30));

        try (WikiRendererUI.RowBuilder builder = WikiRendererUI.row(rightColumn)) {
            this.exportAnimationButton = UIComponents.button(Translate.gui("export_animation"), button -> this.queueAnimationExport());
            builder.row.child(this.exportAnimationButton.margins(Insets.right(5)));

            builder.row.child(UIComponents.button(Translate.gui("format." + animationFormat.extension), button -> {
                animationFormat = animationFormat.next();
                button.setMessage(Translate.gui("format." + animationFormat.extension));
            }).horizontalSizing(Sizing.fixed(35)));
        }

        WikiRendererUI.dynamicLabel(rightColumn, () -> {
            if (this.currentAnimationExportData == null) {
                return Component.empty();
            } else if (this.currentAnimationExportData.getRemainingFrames() > 0) {
                return Translate.gui("export_remaining_frames", this.currentAnimationExportData.getRemainingFrames());
            } else if (this.currentAnimationExportData.getCurrentFFmpegFrame() != null) {
                String frame = this.currentAnimationExportData.getCurrentFFmpegFrame();
                int totalFrames = currentAnimationExportData.getAnimationFrames();
                String exportFps = currentAnimationExportData.getCurrentFFmpegFps();
                return Translate.gui("ffmpeg_data", frame, totalFrames, exportFps);
            } else {
                if (this.currentAnimationExportData instanceof LiveRenderFFmpegAnimationHandler) {
                    return Translate.gui("setting_up_second_ffmpeg_pass");
                } else {
                    return Translate.gui("setting_up_ffmpeg");
                }
            }
        });

        WikiRendererUI.dynamicLabel(rightColumn, () -> switch (animationHandlingMode) {
            case DISK_INSTANT_SAVE -> Translate.gui("animation_mode_selected_instant_file_save");
            case MEMORY_CACHE -> Translate.gui("animation_mode_selected_save_in_memory");
            case LIVE_FFMPEG -> Translate.gui("animation_mode_selected_live_ffmpeg");
        }).margins(Insets.of(10, 0, 5, 0));

        rightColumn.child(UIComponents.dropdown(Sizing.content())
                .button(Translate.gui("animation_mode_name_instant_file_save"), b -> animationHandlingMode = AnimationHandlingMode.DISK_INSTANT_SAVE)
                .text(Translate.gui("animation_mode_description_instant_file_save_1"))
                .text(Translate.gui("animation_mode_description_instant_file_save_2"))
                .button(Translate.gui("animation_mode_name_save_in_memory"), b -> animationHandlingMode = AnimationHandlingMode.MEMORY_CACHE)
                .text(Translate.gui("animation_mode_description_save_in_memory_1"))
                .text(Translate.gui("animation_mode_description_save_in_memory_2"))
                .button(Translate.gui("animation_mode_name_live_ffmpeg"), b -> animationHandlingMode = AnimationHandlingMode.LIVE_FFMPEG)
                .text(Translate.gui("animation_mode_description_live_ffmpeg_1"))
                .text(Translate.gui("animation_mode_description_live_ffmpeg_2"))
                .closeWhenNotHovered(false)
                .padding(Insets.of(5))
                .surface(Surface.blur(10, 10))
        );
    }

    public void queueAnimationExport() {
        int framesStoreInMemory = animationHandlingMode.isStoredInMemory() ? EXPORT_FRAMES.get() : 1;
        if (this.memoryGuard.canFitInRam(memoryGuard.estimateMemoryMBUsage(renderable, framesStoreInMemory)) || this.minecraft.hasControlDown()) {
            this.currentAnimationExportData = animationHandlingMode.createAnimationHandler(this, renderable);
            WikiRenderer.currentAnimationHandler = this.currentAnimationExportData;

            if (!SYNC_ENCHANTMENT_GLINTS_TO_EXPORT.get()) {
                this.minecraft.getFramerateLimitTracker().setFramerateLimit(EXPORT_FRAMERATE.get());
            }
            WikiRenderer.skipWorldRender = true;

            this.exportAnimationButton.active = false;
            this.exportAnimationButton.setMessage(Translate.gui("exporting"));
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (this.guiRebuildScheduled) {
            this.saveScrollOffsetDataIfPossible();
            this.uiAdapter = null;
            this.rightColumn.clearChildren();
            this.leftColumn.clearChildren();
            this.rebuildWidgets();

            this.guiRebuildScheduled = false;
        }

        Window window = minecraft.getWindow();
        boolean tick = renderable.getProperties() instanceof TickingPropertyBundle ticking && ticking.getTickProperty().get();
        float effectiveTickDelta = tick ? minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false) : 0;

        // basically just for batch rendering
        this.renderable.onScreenHandle(this);

        Consumer<Matrix4fStack> positionTransformer = this.hasBothColumns ? null : matrixStack -> matrixStack.translate(1 - window.getWidth() / (float) window.getHeight(), 0, 0);
        RenderTarget renderedOutput = RenderableDispatcher.drawIntoDuplicateFramebuffer(this, this.renderable, effectiveTickDelta, positionTransformer);

        if (this.drawOnlyBackground) {
            context.fill(0, 0, this.width, this.height, backgroundColor | 255 << 24);
        } else {
            this.renderTransparentBackground(context);
        }

        context.guiRenderState.submitGuiElement(new BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                TextureSetup.singleTexture(Objects.requireNonNull(renderedOutput.getColorTextureView()), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)),
                new Matrix3x2f(context.pose()),
                0,
                0,
                window.getGuiScaledWidth(),
                window.getGuiScaledHeight(),
                0,
                1,
                1,
                0,
                -1,
                null
        ));

        if (!this.drawOnlyBackground && this.uiAdapter != null) {
            drawFramingHint(context);
            drawGuiBackground(context);

            if (this.exportAnimationButton != null) {
                int framesStoreInMemory = animationHandlingMode.isStoredInMemory() ? EXPORT_FRAMES.get() : 1;
                int memoryMB = memoryGuard.estimateMemoryMBUsage(renderable, framesStoreInMemory);
                List<ClientTooltipComponent> tooltip = this.memoryGuard.getStatusTooltip(memoryMB)
                        .stream()
                        .map(text -> ClientTooltipComponent.create(text.getVisualOrderText()))
                        .toList();
                this.exportAnimationButton.tooltip(tooltip);
            }

            super.render(context, mouseX, mouseY, delta);

            if (FileIO.taskCount() > 0) {
                if (!this.ioStateComponent.hasParent()) {
                    this.notificationArea.child(this.ioStateComponent);
                }
            } else if (this.ioStateComponent.hasParent()) {
                this.notificationArea.removeChild(this.ioStateComponent);
            }
        }

        if (this.captureScheduled) {
            this.exportImage(true);
            this.captureScheduled = false;
        }

        this.renderOrExportAnimationIfNecessary(effectiveTickDelta);
    }

    public void exportImage(boolean popupText) {
        ExportPathSpec defaultExportPath = this.renderable.getExportPath();
        String customFileName = renderable.getCustomFileName();
        ExportPathSpec exportPath = customFileName == null || customFileName.isBlank() ? defaultExportPath : defaultExportPath.differentFileName(customFileName);

        AtomicReference<MinimapCalibratorData> data = new AtomicReference<>();
        Consumer<MinimapCalibratorData> dataConsumer = null;
        if (renderable instanceof AreaRenderable areaRenderable
            && areaRenderable.getProperties().perPixel90DegreeRendering.get()
            && areaRenderable.getProperties().exportSideViewMinimapData.get()
            && areaRenderable.getProperties().areMinimapSettingsExportable()) {
            dataConsumer = data::set;
        }

        RenderableDispatcher.drawIntoImage(this, this.renderable, 0, renderable.getExportResolution(), renderable.shouldCrop(), dataConsumer)
                .thenCompose(img -> FileIO.saveImage(img, exportPath).whenComplete((f, t) -> img.close()))
                .whenComplete((imageFile, throwable) -> {
                    if (this.exportCallback != null) {
                        this.exportCallback.accept(imageFile);
                    }

                    if (popupText) {
                        this.minecraft.execute(() -> this.notify(
                                () -> Util.getPlatform().openFile(imageFile),
                                Translate.gui("exported_as"),
                                Component.literal(ExportPathSpec.exportRoot().relativize(imageFile.toPath()).toString())
                        ));
                    }

                    if (data.get() != null) {
                        String fileText = data.get().toFileText(imageFile.getName());
                        ExportPathSpec minimapExportPath = customFileName == null || customFileName.isBlank()
                                ? defaultExportPath.differentFileName("area_render_minimap_data")
                                : defaultExportPath.differentFileName(customFileName + "_area_render_minimap_data");

                        FileIO.saveText(fileText, minimapExportPath).whenComplete((textFile, textThrowable) -> {
                            if (popupText) {
                                this.minecraft.execute(() -> this.notify(
                                        () -> Util.getPlatform().openFile(textFile),
                                        Translate.gui("exported_minimap_data_as"),
                                        Component.literal(ExportPathSpec.exportRoot().relativize(textFile.toPath()).toString())
                                ));
                            }
                        });
                    }
                });
    }

    private void renderOrExportAnimationIfNecessary(float effectiveTickDelta) {
        if (this.currentAnimationExportData != null) {
            if (SYNC_ENCHANTMENT_GLINTS_TO_EXPORT.get()) {
                // overrides tabbing out lowering the fps cap
                FramerateLimitTracker framerateLimitTracker = Minecraft.getInstance().getFramerateLimitTracker();
                framerateLimitTracker.setFramerateLimit(Minecraft.getInstance().options.framerateLimit().get());
                framerateLimitTracker.onInputReceived();
            }
            this.currentAnimationExportData.renderAndSaveFrame(effectiveTickDelta);
        }
    }

    @Override
    public void tick() {
        if (this.minecraft.level == null) return;

        if (this.minecraft.level.getGameTime() % 40 == 0) {
            this.memoryGuard.update();
        }

        if (this.renderable instanceof TickingRenderable<?> ticking) {
            boolean tick = ticking.getProperties().getTickProperty().get();
            if (tick) WikiRenderer.inRenderableTick = true;
            ticking.tick(tick);
            if (tick) WikiRenderer.inRenderableTick = false;
        }
    }

    public void notify(@NotNull Runnable onClick, Component... messages) {
        this.notificationArea.child(0, new NotificationComponent(onClick, messages));
    }

    public void notify(Component... messages) {
        this.notificationArea.child(0, new NotificationComponent(null, messages));
    }

    private boolean isInViewport(double mouseX) {
        return mouseX > viewportBeginX && mouseX < viewportEndX;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if (!(this.renderable.getProperties() instanceof DefaultPropertyBundle properties))
            return super.mouseDragged(click, offsetX, offsetY);

        if (this.isInViewport(click.x())) {
            int button = click.button();
            if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                double xScaling = (100d / properties.scale.get()) * (this.minecraft.getWindow().getScreenWidth() / (float) this.minecraft.getWindow().getGuiScaledWidth());
                double yScaling = (100d / properties.scale.get()) * (this.minecraft.getWindow().getScreenHeight() / (float) this.minecraft.getWindow().getGuiScaledHeight());

                properties.xOffset.modify((int) (50 * offsetX * xScaling));
                properties.yOffset.modify((int) (50 * offsetY * yScaling));
                return true;
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                properties.modifyRotation((int) (offsetX * 2));
                return true;
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                properties.modifySlant(offsetY * 2);
                return true;
            }
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubled) {
        if (!(this.renderable.getProperties() instanceof DefaultPropertyBundle properties))
            return super.mouseClicked(click, doubled);

        if (this.isInViewport(click.x()) && click.hasControlDown()) {
            int button = click.button();
            if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                properties.xOffset.setToDefault();
                properties.yOffset.setToDefault();
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                properties.rotation.setToDefault();
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                properties.slant.setToDefault();
            }
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!(this.renderable.getProperties() instanceof DefaultPropertyBundle properties)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        if (this.isInViewport(mouseX)) {
            properties.scale.modify((int) (verticalAmount * Math.max(1, properties.scale.get() * 0.075)));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (super.keyPressed(input)) return true;

        int keyCode = input.key();

        if (keyCode == GLFW.GLFW_KEY_F12) {
            this.captureScheduled = true;
        } else if (keyCode == GLFW.GLFW_KEY_F10) {
            this.drawOnlyBackground = !this.drawOnlyBackground;
        } else if (KEYBOARD_CONTROLS.containsKey(keyCode) && this.renderable instanceof DefaultRenderable) {
            if (fileNameField != null && fileNameField.isFocused()) {
                return true;
            }
            KEYBOARD_CONTROLS.get(keyCode).accept((DefaultPropertyBundle) this.renderable.getProperties());
        }

        return true;
    }

    public void scheduleCapture() {
        this.captureScheduled = true;
    }

    public void setExportCallback(Consumer<File> exportCallback) {
        this.exportCallback = exportCallback;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        WikiRenderer.particleRestriction = ParticleRestriction.always();
        this.minecraft.getFramerateLimitTracker().setFramerateLimit(this.minecraft.options.framerateLimit().get());

        if (ScreenSchedulerAndSaver.getScheduledScreen() == null) {
            ScreenSchedulerAndSaver.setSavedScreen(this);
        } else {
            // discrd is called for any saved screens in schedule, but ignore that if this is the saved screen and is being reopened
            this.renderable.dispose();
        }

        if (this.currentAnimationExportData != null) {
            this.currentAnimationExportData.close();
            this.currentAnimationExportData = null;
            WikiRenderer.currentAnimationHandler = null;
        }
        if (this.exportAnimationButton != null) {
            this.exportAnimationButton.active = true;
            this.exportAnimationButton.setMessage(Translate.gui("export_animation"));
        }
    }

    private void drawFramingHint(GuiGraphics context) {
        context.fill(viewportBeginX, 0, viewportEndX, 0, 0x90000000);
        context.fill(viewportBeginX, height, viewportEndX, height, 0x90000000);
        context.fill(viewportBeginX, 0, viewportBeginX, height, 0x90000000);
        context.fill(viewportEndX, 0, viewportEndX, height, 0x90000000);
    }

    private void drawGuiBackground(GuiGraphics context) {
        context.fill(0, 0, viewportBeginX, height, 0x90000000);
        context.fill(viewportEndX, 0, width, height, 0x90000000);
    }
}
