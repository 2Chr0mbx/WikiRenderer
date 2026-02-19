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
import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class InstantDiskSaveAnimationHandler extends AnimationHandler {

    private final List<CompletableFuture<File>> frameFileExportFutures = new ArrayList<>();
    private int currentFrameIndex = 0;

    public InstantDiskSaveAnimationHandler(RenderScreen screen, Renderable<?> renderable, int framesToRender) {
        super(screen, renderable, framesToRender);
        this.remainingAnimationFrames = framesToRender;
    }

    public void renderAndSaveFrame(float effectiveTickDelta) {
        if (this.closed || this.remainingAnimationFrames <= 0) return;
        if (!screen.memoryGuard.canFitInRam(screen.memoryGuard.estimateMemoryMBUsage(renderable, FileIO.taskCount()))) {
            return;
        }
        // delay until ram is available

        if (GlobalProperties.SYNC_TEXTURE_ANIMATIONS_TO_ANIMATION.get()) {
            Minecraft.getInstance().getTextureManager().tick();
        }

        GpuTexture texture = RenderableDispatcher.drawIntoTexture(this.screen, this.renderable, effectiveTickDelta, renderable.getExportResolution());

        WikiRenderer.skipWorldRender = true;

        Boolean overwriteValue = GlobalProperties.OVERWRITE_LATEST.get();
        GlobalProperties.OVERWRITE_LATEST.set(false);

        int frameIndex = this.currentFrameIndex;
        this.currentFrameIndex++;

        // makes new file each frame
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
            this.mergeFilesIntoFinalResult(this.frameFileExportFutures, overwriteValue);
        }
    }
}
