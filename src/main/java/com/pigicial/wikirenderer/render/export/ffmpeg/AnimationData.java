package com.pigicial.wikirenderer.render.export.ffmpeg;

import com.mojang.blaze3d.textures.GpuTexture;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.render.export.ExportPathSpec;
import com.pigicial.wikirenderer.render.export.FileIO;
import com.pigicial.wikirenderer.render.export.ImageCropper;
import com.pigicial.wikirenderer.render.export.RenderableDispatcher;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.util.Translate;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AnimationData {
    private final List<CompletableFuture<File>> frameFileExportFutures = new ArrayList<>();
    private final List<ImageCropper.CropData> collectedCropData = Collections.synchronizedList(new ArrayList<>());

    private final RenderScreen screen;
    private final Renderable<?> renderable;

    private final String framesFolderName;
    private final Path framesFolder;

    private boolean closed = false;
    private int remainingAnimationFrames;
    private int currentFrameIndex = 0;

    public AnimationData(RenderScreen screen, Renderable<?> renderable, int framesToRender) {
        this.screen = screen;
        this.renderable = renderable;
        this.framesFolderName = "sequence-" + UUID.randomUUID();
        this.framesFolder = ExportPathSpec.exportRoot().resolve(this.framesFolderName + "/");
        this.remainingAnimationFrames = framesToRender;
    }

    public void renderFrameAndSaveToFile(float effectiveTickDelta) {
        if (this.closed || this.remainingAnimationFrames <= 0) return;

        GpuTexture texture = RenderableDispatcher.drawIntoTexture(this.renderable, effectiveTickDelta, renderable.getExportResolution());

        WikiRenderer.skipWorldRender = true;

        Boolean overwriteValue = GlobalProperties.OVERWRITE_LATEST.get();
        GlobalProperties.OVERWRITE_LATEST.set(false);

        int frameIndex = this.currentFrameIndex;
        this.currentFrameIndex++;

        CompletableFuture<File> future = RenderableDispatcher.copyTextureIntoImage(texture)
                .whenComplete((image, t) -> texture.close())
                .thenApply(image -> {
                    this.collectedCropData.add(ImageCropper.getCropData(image));
                    return image;
                })
                .thenCompose(img -> {
                    if (this.closed) {
                        img.close();
                        return CompletableFuture.completedFuture(null);
                    } else {
                        return FileIO.saveImage(img, ExportPathSpec.forced(this.framesFolderName, "seq_" + frameIndex))
                                .whenComplete((f, t) -> img.close());
                    }
                });

        this.frameFileExportFutures.add(future);

        if (--this.remainingAnimationFrames == 0) {
            Minecraft.getInstance().getFramerateLimitTracker().setFramerateLimit(Minecraft.getInstance().options.framerateLimit().get());

            ExportPathSpec defaultExportPath = this.renderable.getExportPath();
            ExportPathSpec exportPath = screen.customFileName.isBlank() ? defaultExportPath : defaultExportPath.differentFileName(screen.customFileName);

            CompletableFuture.allOf(frameFileExportFutures.toArray(CompletableFuture[]::new))
                    .whenComplete((file, throwable) -> {
                        GlobalProperties.OVERWRITE_LATEST.set(overwriteValue);
                        if (throwable != null || closed) {
                            FileIO.deleteSequenceFilesFromPath(this.framesFolder);
                            return;
                        }

                        this.screen.exportAnimationButton.setMessage(Translate.gui("converting"));
                        Minecraft.getInstance().execute(() -> screen.notify(Translate.gui("converting_image_sequence")));

                        FFmpegDispatcher.exportAnimation(
                                exportPath,
                                this.framesFolder,
                                GlobalProperties.animationFormat,
                                renderable.shouldCropForFfmpeg() ? ImageCropper.getFfmpegCropSize(renderable, collectedCropData) : ""
                        ).whenComplete((animationFile, animationThrowable) -> {
                            this.screen.exportAnimationButton.active = true;
                            this.screen.exportAnimationButton.setMessage(Translate.gui("export_animation"));
                            this.screen.currentAnimationExportData = null;
                            this.closed = true;

                            Minecraft.getInstance().execute(() -> screen.notify(
                                    () -> Util.getPlatform().openFile(animationFile),
                                    Translate.gui("animation_saved"),
                                    Component.literal(ExportPathSpec.exportRoot().relativize(animationFile.toPath()).toString())
                            ));
                        });
                    });
        }
    }

    public void close() {
        this.closed = true;
        if (remainingAnimationFrames > 0) {
            FileIO.deleteSequenceFilesFromPath(this.framesFolder);
        }
    }

    public int getRemainingFrames() {
        return this.remainingAnimationFrames;
    }
}
