package com.glisco.isometricrenders.screen;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.mixin.access.ParticleEngineAccessor;
import com.glisco.isometricrenders.property.CroppablePropertyBundle;
import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.property.Property;
import com.glisco.isometricrenders.property.TickingPropertyBundle;
import com.glisco.isometricrenders.render.DefaultRenderable;
import com.glisco.isometricrenders.render.Renderable;
import com.glisco.isometricrenders.render.RenderableDispatcher;
import com.glisco.isometricrenders.render.TickingRenderable;
import com.glisco.isometricrenders.render.area.AreaRenderable;
import com.glisco.isometricrenders.render.area.side_view.MinimapCalibratorData;
import com.glisco.isometricrenders.textures.TextureDataProvider;
import com.glisco.isometricrenders.util.*;
import com.glisco.isometricrenders.widget.IOStateComponent;
import com.glisco.isometricrenders.widget.NotificationComponent;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.textures.GpuTexture;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.glisco.isometricrenders.property.GlobalProperties.*;

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

    private final MemoryGuard memoryGuard = new MemoryGuard(0.75f);

    private final FlowLayout notificationArea = Containers.verticalFlow(Sizing.content(), Sizing.content());
    private final IOStateComponent ioStateComponent = new IOStateComponent();

    private final FlowLayout leftAnchor = Containers.verticalFlow(Sizing.content(), Sizing.content());
    private final FlowLayout rightAnchor = Containers.verticalFlow(Sizing.content(), Sizing.content());

    private final FlowLayout leftColumn = Containers.verticalFlow(Sizing.fill(100), Sizing.content()).gap(-4);
    private final FlowLayout rightColumn = Containers.verticalFlow(Sizing.fill(100), Sizing.content()).gap(-4);

    public final Renderable<?> renderable;

    private boolean drawOnlyBackground = false;
    public boolean captureScheduled = false;
    public boolean guiRebuildScheduled = false;

    private int viewportBeginX;
    private int viewportEndX;
    private boolean hasBothColumns = false;

    public ButtonComponent exportButton = null;
    private Consumer<File> exportCallback = (file) -> {
    };
    private Button exportAnimationButton;
    private final List<GpuTexture> renderedFrames = new ArrayList<>();
    private int remainingAnimationFrames;

    private String customFileName = "";
    private EditBox fileNameField = null;

    public RenderScreen(Renderable<?> renderable) {
        this.renderable = renderable;
        this.memoryGuard.update();
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::horizontalFlow);
    }

    @Override
    protected void init() {
        this.viewportBeginX = (int) ((this.width - this.height) * 0.5);
        this.viewportEndX = (int) (this.width - (this.width - this.height) * 0.5) + 1;

        this.leftAnchor.clearChildren();
        this.rightAnchor.clearChildren();

        if (this.viewportBeginX < 200) {
            this.viewportEndX -= this.viewportBeginX;
            this.viewportBeginX = 0;
            this.hasBothColumns = false;

            this.leftAnchor.horizontalSizing(Sizing.fixed(0)).verticalSizing(Sizing.fixed(this.height));
            this.rightAnchor.positioning(Positioning.absolute(viewportEndX, 0)).horizontalSizing(Sizing.fixed(this.width - this.viewportEndX)).verticalSizing(Sizing.fixed(this.height));

            this.rightAnchor.child(Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), Containers.verticalFlow(Sizing.content(), Sizing.content(10))
                    .child(leftColumn)
                    .child(Components.box(Sizing.fill(85), Sizing.fixed(1)).color(Color.ofDye(DyeColor.GRAY)).fill(true).margins(Insets.top(15)))
                    .child(rightColumn)
                    .horizontalAlignment(HorizontalAlignment.CENTER))

            );
        } else {
            this.hasBothColumns = true;

            this.leftAnchor.horizontalSizing(Sizing.fixed(viewportBeginX)).verticalSizing(Sizing.fixed(this.height));
            this.rightAnchor.positioning(Positioning.absolute(viewportEndX, 0)).horizontalSizing(Sizing.fixed(viewportBeginX)).verticalSizing(Sizing.fixed(this.height));

            this.leftAnchor.child(Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), Containers.verticalFlow(Sizing.content(), Sizing.content(10)).child(this.leftColumn)));
            this.rightAnchor.child(Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), Containers.verticalFlow(Sizing.content(), Sizing.content(10)).child(this.rightColumn)));
        }

        this.notificationArea.positioning(Positioning.absolute(this.viewportBeginX + 5, 5)).sizing(Sizing.fixed(this.height - 10));

        super.init();
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        this.minecraft.options.setCameraType(CameraType.FIRST_PERSON);

        // todo maybe we dont need this line?
        ((ParticleEngineAccessor) Minecraft.getInstance().particleEngine).isometric$getParticles().clear();
        IsometricRenders.particleRestriction = this.renderable.getParticleRestriction();

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

        IsometricUI.sectionHeader(this.rightColumn, "render_options", false);
        this.renderable.getProperties().buildRenderOptionGUIControls(this.renderable, this, this.rightColumn);
        IsometricUI.sectionHeader(rightColumn, "export_options", true);
        this.renderable.getProperties().buildExportOptionGUIControls(this.renderable, this, this.rightColumn);
        this.renderable.getProperties().buildExportResolutionGUIControls(this.renderable, this, this.rightColumn);

        this.fileNameField = IsometricUI.labelledTextField(this.rightColumn, this.customFileName, "file_name", Sizing.fixed(120));
        this.fileNameField.setFilter(s -> s.matches("^[^<>:\"/\\\\|?*\\x00-\\x1F]*$")); // file name regex
        this.fileNameField.setResponder(s -> this.customFileName = s);

        IsometricUI.sectionHeader(rightColumn, "animation_options", true);
        this.buildFfmpegSection();

        if (renderable instanceof TextureDataProvider textureProvider) {
            textureProvider.buildTextureGrabSection(this, rightColumn);
        }
    }

    private void buildFfmpegSection() {
        if (FFmpegDispatcher.wasFFmpegDetected()) {
            if (FFmpegDispatcher.ffmpegAvailable()) {

                if (renderable.getProperties() instanceof CroppablePropertyBundle croppablePropertyBundle) {
                    Property<Boolean> animatedCropProperty = croppablePropertyBundle.getFfmpegCropProperty();
                    IsometricUI.booleanControl(rightColumn, animatedCropProperty, "crop");
                }

                IsometricUI.booleanControl(rightColumn, speedUpEnchantmentGlints, "speed_up_enchantment_glints");
                speedUpEnchantmentGlints.listen((p, v) -> guiRebuildScheduled = true, false);

                if (speedUpEnchantmentGlints.get()) {
                    rightColumn.child(Components.button(Translate.gui("enchantment_glint_preset"), button -> {
                        int seconds = 120000 / 8000;
                        int framerate = 20;
                        exportFramerate.set(framerate);
                        exportFrames.set(seconds * framerate);
                    }).margins(Insets.vertical(5)));
                }

                IsometricUI.labelledTextField(rightColumn, exportFrames, "animation_frames", Sizing.fixed(30));
                IsometricUI.labelledTextField(rightColumn, exportFramerate, "animation_framerate", Sizing.fixed(30));

                try (IsometricUI.RowBuilder builder = IsometricUI.row(rightColumn)) {
                    this.exportAnimationButton = Components.button(Translate.gui("export_animation"), button -> {
                        if (this.memoryGuard.canFit(this.estimateMemoryUsage(exportFrames.get())) || this.minecraft.hasControlDown()) {
                            this.remainingAnimationFrames = exportFrames.get();

                            this.minecraft.getFramerateLimitTracker().setFramerateLimit(exportFramerate.get());
                            IsometricRenders.skipNextWorldRender();

                            button.active = false;
                            button.setMessage(Translate.gui("exporting"));
                        }
                    });
                    builder.row.child(this.exportAnimationButton.margins(Insets.right(5)));

                    builder.row.child(Components.button(Translate.gui("format." + animationFormat.extension), button -> {
                        animationFormat = animationFormat.next();
                        button.setMessage(Translate.gui("format." + animationFormat.extension));
                    }).horizontalSizing(Sizing.fixed(35)));
                }

                IsometricUI.dynamicLabel(rightColumn, () ->
                        this.remainingAnimationFrames == 0
                                ? Component.empty()
                                : Translate.gui("export_remaining_frames", this.remainingAnimationFrames));
            } else {
                IsometricUI.sectionHeader(rightColumn, "no_ffmpeg_1", true);
                IsometricUI.sectionHeader(rightColumn, "no_ffmpeg_2", false);
                IsometricUI.sectionHeader(rightColumn, "no_ffmpeg_3", false)
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
            }
        } else {
            IsometricUI.sectionHeader(rightColumn, "detecting_ffmpeg", false);
            FFmpegDispatcher.detectFFmpeg().whenComplete((aBoolean, throwable) -> this.guiRebuildScheduled = true);
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (this.guiRebuildScheduled) {
            this.guiRebuildScheduled = false;

            this.uiAdapter = null;
            this.rightColumn.clearChildren();
            this.leftColumn.clearChildren();

            this.rebuildWidgets();
        }

        Window window = minecraft.getWindow();
        boolean tick = renderable.getProperties() instanceof TickingPropertyBundle ticking && ticking.getTickProperty().get();
        float effectiveTickDelta = tick ? minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false) : 0;
        RenderableDispatcher.drawIntoActiveFramebuffer(
                this.renderable,
                window.getWidth() / (float) window.getHeight(),
                effectiveTickDelta,
                this.hasBothColumns
                        ? matrixStack -> {
                }
                        : matrixStack -> matrixStack.translate(1 - window.getWidth() / (float) window.getHeight(), 0, 0)
        );

        if (this.drawOnlyBackground) {
            context.fill(0, 0, this.width, this.height, backgroundColor | 255 << 24);
        } else {
            this.renderTransparentBackground(context);
        }

        IsometricRenders.forceGuiDepthTesting = false;
        if (!this.drawOnlyBackground && this.uiAdapter != null) {
            drawFramingHint(context);
            drawGuiBackground(context);

            super.render(context, mouseX, mouseY, delta);

            if (this.exportAnimationButton != null) {
                this.exportAnimationButton.tooltip(this.memoryGuard.getStatusTooltip(this.estimateMemoryUsage(exportFrames.get())).stream().map(text -> ClientTooltipComponent.create(text.getVisualOrderText())).toList());
            }

            if (FileIO.taskCount() > 0) {
                if (!this.ioStateComponent.hasParent()) {
                    this.notificationArea.child(this.ioStateComponent);
                }
            } else if (this.ioStateComponent.hasParent()) {
                this.notificationArea.removeChild(this.ioStateComponent);
            }
        }

        if (this.captureScheduled) {
            ExportPathSpec defaultExportPath = this.renderable.getExportPath();
            ExportPathSpec exportPath = this.customFileName.isBlank() ? defaultExportPath : defaultExportPath.differentFileName(this.customFileName);

            AtomicReference<MinimapCalibratorData> data = new AtomicReference<>();
            Consumer<MinimapCalibratorData> dataConsumer = null;
            if (renderable instanceof AreaRenderable areaRenderable
                && areaRenderable.getProperties().perPixel90DegreeRendering.get()
                && areaRenderable.getProperties().exportSideViewMinimapData.get()
                && areaRenderable.getProperties().areMinimapSettingsExportable()) {
                dataConsumer = data::set;
            }

            RenderableDispatcher.drawIntoImage(this.renderable, 0, renderable.getExportResolution(), renderable.shouldCrop(), dataConsumer)
                    .thenCompose(img -> FileIO.saveImage(img, exportPath).whenComplete((f, t) -> img.close()))
                    .whenComplete((imageFile, throwable) -> {
                        exportCallback.accept(imageFile);
                        this.minecraft.execute(() -> this.notify(
                                () -> Util.getPlatform().openFile(imageFile),
                                Translate.gui("exported_as"),
                                Component.literal(ExportPathSpec.exportRoot().relativize(imageFile.toPath()).toString())
                        ));

                        if (data.get() != null) {
                            String fileText = data.get().toFileText(imageFile.getName());
                            ExportPathSpec minimapExportPath = this.customFileName.isBlank()
                                    ? defaultExportPath.differentFileName("area_render_minimap_data")
                                    : defaultExportPath.differentFileName(this.customFileName + "_area_render_minimap_data");

                            FileIO.saveText(fileText, minimapExportPath).whenComplete((textFile, textThrowable) -> {
                                this.minecraft.execute(() -> this.notify(
                                        () -> Util.getPlatform().openFile(textFile),
                                        Translate.gui("exported_minimap_data_as"),
                                        Component.literal(ExportPathSpec.exportRoot().relativize(textFile.toPath()).toString())
                                ));
                            });
                        }
                    });

            this.captureScheduled = false;
        }

        if (this.remainingAnimationFrames > 0) {
            this.renderedFrames.add(RenderableDispatcher.drawIntoTexture(this.renderable, effectiveTickDelta, renderable.getExportResolution()));

            IsometricRenders.skipNextWorldRender();

            if (--this.remainingAnimationFrames == 0) {
                this.minecraft.getFramerateLimitTracker().setFramerateLimit(this.minecraft.options.framerateLimit().get());

                Boolean overwriteValue = overwriteLatest.get();
                overwriteLatest.set(false);

                List<CompletableFuture<File>> exportFutures = new ArrayList<>();
                List<ImageCropper.CropData> collectedCropData = Collections.synchronizedList(new ArrayList<>());

                for (int i = 0; i < this.renderedFrames.size(); i++) {
                    int sequenceIndex = i;
                    GpuTexture frame = this.renderedFrames.get(i);
                    CompletableFuture<File> future = RenderableDispatcher.copyTextureIntoImage(frame)
                            .thenApply(image -> {
                                collectedCropData.add(ImageCropper.getCropData(image));
                                return image;
                            })
                            .thenCompose(img -> FileIO.saveImage(img, ExportPathSpec.forced("sequence", "seq_" + sequenceIndex))
                                    .whenComplete((f, t) -> img.close()));

                    exportFutures.add(future);
                    frame.close();
                }

                this.renderedFrames.clear();

                ExportPathSpec defaultExportPath = this.renderable.getExportPath();
                ExportPathSpec exportPath = this.customFileName.isBlank() ? defaultExportPath : defaultExportPath.differentFileName(this.customFileName);

                CompletableFuture.allOf(exportFutures.toArray(CompletableFuture[]::new))
                        .whenComplete((file, throwable) -> {
                            overwriteLatest.set(overwriteValue);
                            if (throwable != null) return;

                            this.exportAnimationButton.setMessage(Translate.gui("converting"));
                            this.minecraft.execute(() -> this.notify(Translate.gui("converting_image_sequence")));

                            FFmpegDispatcher.assemble(
                                    exportPath,
                                    ExportPathSpec.exportRoot().resolve("sequence/"),
                                    animationFormat,
                                    renderable.shouldCropForFfmpeg() ? ImageCropper.getFfmpegCropSize(renderable, collectedCropData) : ""
                            ).whenComplete((animationFile, animationThrowable) -> {
                                this.exportAnimationButton.active = true;
                                this.exportAnimationButton.setMessage(Translate.gui("export_animation"));

                                this.minecraft.execute(() -> this.notify(
                                        () -> Util.getPlatform().openFile(animationFile),
                                        Translate.gui("animation_saved"),
                                        Component.literal(ExportPathSpec.exportRoot().relativize(animationFile.toPath()).toString())
                                ));
                            });
                        });
            }
        }
        IsometricRenders.forceGuiDepthTesting = true;
    }

    @Override
    public void tick() {
        if (this.minecraft.level.getGameTime() % 40 == 0) {
            this.memoryGuard.update();
        }

        if (this.renderable instanceof TickingRenderable<?> ticking) {
            boolean tick = ticking.getProperties().getTickProperty().get();
            if (tick) IsometricRenders.beginRenderableTick();
            ticking.tick(tick);
            if (tick) IsometricRenders.endRenderableTick();
        }
    }

    private void notify(@NotNull Runnable onClick, Component... messages) {
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

    private int estimateMemoryUsage(int frames) {
        return (int) ((renderable.getExportResolution() * renderable.getExportResolution() * 4L * frames) / 1024L / 1024L);
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
        IsometricRenders.particleRestriction = ParticleRestriction.always();
        this.minecraft.getFramerateLimitTracker().setFramerateLimit(this.minecraft.options.framerateLimit().get());

        if (ScreenSchedulerAndSaver.getScheduledScreen() == null) {
            ScreenSchedulerAndSaver.setSavedScreen(this);
        } else {
            // discrd is called for any saved screens in schedule, but ignore that if this is the saved screen and is being reopened
            this.renderable.dispose();
        }

        this.renderedFrames.forEach(GpuTexture::close);
        this.renderedFrames.clear();
        this.remainingAnimationFrames = 0;
        if (this.exportAnimationButton != null) {
            this.exportAnimationButton.active = true;
            this.exportAnimationButton.setMessage(Translate.gui("export_animation"));
        }
    }

    private void drawFramingHint(GuiGraphics context) {
        context.fill(viewportBeginX + 5, 0, viewportEndX - 5, 5, 0x90000000);
        context.fill(viewportBeginX + 5, height - 5, viewportEndX - 5, height, 0x90000000);
        context.fill(viewportBeginX, 0, viewportBeginX + 5, height, 0x90000000);
        context.fill(viewportEndX - 5, 0, viewportEndX, height, 0x90000000);
    }

    private void drawGuiBackground(GuiGraphics context) {
        context.fill(0, 0, viewportBeginX, height, 0x90000000);
        context.fill(viewportEndX, 0, width, height, 0x90000000);
    }
}
