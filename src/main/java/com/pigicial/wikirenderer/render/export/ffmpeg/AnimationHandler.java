package com.pigicial.wikirenderer.render.export.ffmpeg;

import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.render.export.ExportPathSpec;
import com.pigicial.wikirenderer.render.export.FileIO;
import com.pigicial.wikirenderer.render.export.ImageCropper;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.util.Translate;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class AnimationHandler implements AutoCloseable {
    protected final List<ImageCropper.CropData> collectedCropData = Collections.synchronizedList(new ArrayList<>());

    protected final RenderScreen screen;
    protected final Renderable<?> renderable;

    protected final String framesFolderName;
    protected final Path framesFolder;

    private final int animationFrames;
    protected int remainingAnimationFrames;
    protected boolean closed = false;
    private boolean finished = false;

    private String currentFFmpegFrame = null;
    private String currentFFmpegFps = null;

    protected AnimationHandler(RenderScreen screen, Renderable<?> renderable, int framesToRender) {
        this.screen = screen;
        this.renderable = renderable;
        this.framesFolderName = "sequence_frames/" + UUID.randomUUID();
        this.framesFolder = ExportPathSpec.exportRoot().resolve(this.framesFolderName + "/");
        this.animationFrames = framesToRender;
        this.remainingAnimationFrames = framesToRender;
    }

    public abstract void renderAndSaveFrame(float effectiveTickDelta);

    protected final void mergeFilesIntoFinalResult(List<CompletableFuture<File>> fileFutures, boolean overwriteValue) {
        this.finished = true;
        ExportPathSpec defaultExportPath = this.renderable.getExportPath();
        ExportPathSpec exportPath = defaultExportPath.differentFileName(renderable.getCustomFileName());

        CompletableFuture.allOf(fileFutures.toArray(CompletableFuture[]::new))
                .whenComplete((v_, throwable) -> {
                    GlobalProperties globalProperties = GlobalProperties.get();
                    globalProperties.overwriteLatest.set(overwriteValue);

                    boolean keepingFiles = globalProperties.saveIndividualFrames.get();
                    if (throwable != null || closed) {
                        FileIO.deleteSequenceFilesFromPath(this.framesFolder);
                        return;
                    }

                    this.screen.exportAnimationButton.setMessage(Translate.gui("converting"));
                    Minecraft.getInstance().execute(() -> screen.notify(Translate.gui("converting_image_sequence")));

                    FFmpegDispatcher.exportAnimation(
                            exportPath,
                            this.framesFolder,
                            globalProperties.animationFormat,
                            this,
                            ImageCropper.getFFmpegCropSize(renderable, collectedCropData)
                    ).whenComplete((animationFile, animationThrowable) -> this.finishAndCleanup(animationFile, keepingFiles ? this.framesFolder : null));
                });
    }

    protected void finishAndCleanup(File animationFile, @Nullable Path framesFolderToLinkTo) {
        this.screen.exportAnimationButton.active = true;
        this.screen.exportAnimationButton.setMessage(Translate.gui("export_animation"));
        if (this.screen.refreshCustomFFmpegPathButton != null) {
            this.screen.refreshCustomFFmpegPathButton.active = true;
            this.screen.refreshCustomFFmpegPathButton.setMessage(Translate.gui("check_ffmpeg_path"));
        }

        this.screen.currentAnimationExportData = null;
        this.closed = true;
        this.collectedCropData.clear();
        WikiRenderer.currentAnimationHandler = null;

        Minecraft.getInstance().execute(() -> screen.notify(
                () -> Util.getPlatform().openFile(animationFile),
                Translate.gui("animation_saved"),
                Component.literal(ExportPathSpec.exportRoot().relativize(animationFile.toPath()).toString())
        ));

        if (framesFolderToLinkTo != null) {
            Minecraft.getInstance().execute(() -> screen.notify(
                    () -> Util.getPlatform().openFile(framesFolderToLinkTo.toFile()),
                    Translate.gui("animation_frames_saved"),
                    Component.literal(ExportPathSpec.exportRoot().relativize(framesFolderToLinkTo).toString())
            ));
        }
    }

    public boolean isFinished() {
        return this.finished || this.closed;
    }

    @Override
    public void close() {
        this.closed = true;
        this.collectedCropData.clear();
        if (remainingAnimationFrames > 0) {
            FileIO.deleteSequenceFilesFromPath(this.framesFolder);
        }
    }

    public int getAnimationFrames() {
        return animationFrames;
    }

    public int getRemainingFrames() {
        return this.remainingAnimationFrames;
    }

    public void setFFmpegData(String frame, String fps) {
        this.currentFFmpegFrame = frame;
        this.currentFFmpegFps = fps;
    }

    public String getCurrentFFmpegFrame() {
        return currentFFmpegFrame;
    }

    public String getCurrentFFmpegFps() {
        return currentFFmpegFps;
    }
}
