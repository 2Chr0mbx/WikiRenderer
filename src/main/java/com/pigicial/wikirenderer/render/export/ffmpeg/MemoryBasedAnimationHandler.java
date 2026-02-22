package com.pigicial.wikirenderer.render.export.ffmpeg;

import com.mojang.blaze3d.platform.NativeImage;
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

public class MemoryBasedAnimationHandler extends AnimationHandler {
    private final List<CompletableFuture<NativeImage>> frameExportFutures = new ArrayList<>();

    public MemoryBasedAnimationHandler(RenderScreen screen, Renderable<?> renderable, int framesToRender) {
        super(screen, renderable, framesToRender);
    }

    public void renderAndSaveFrame(float effectiveTickDelta) {
        if (this.closed || this.remainingAnimationFrames <= 0) return;

        if (GlobalProperties.SYNC_TEXTURE_ANIMATIONS_TO_ANIMATION.get()) {
            Minecraft.getInstance().getTextureManager().tick();
        }

        GpuTexture texture = RenderableDispatcher.drawIntoTexture(this.screen, this.renderable, effectiveTickDelta, screen.getTimeSinceCreationMs(), renderable.getExportResolution());

        WikiRenderer.skipWorldRender = true;

        CompletableFuture<NativeImage> future = RenderableDispatcher.copyTextureIntoImage(texture)
                .whenComplete((image, t) -> texture.close())
                .thenApply(image -> {
                    this.collectedCropData.add(ImageCropper.getCropData(image));
                    return image;
                });

        this.frameExportFutures.add(future);

        if (--this.remainingAnimationFrames == 0) {
            Minecraft.getInstance().getFramerateLimitTracker().setFramerateLimit(Minecraft.getInstance().options.framerateLimit().get());
            CompletableFuture.allOf(frameExportFutures.toArray(CompletableFuture[]::new))
                    .whenComplete((v, t) -> {
                        List<CompletableFuture<File>> fileFutures = new ArrayList<>();

                        Boolean overwriteValue = GlobalProperties.OVERWRITE_LATEST.get();
                        GlobalProperties.OVERWRITE_LATEST.set(false);

                        // make files all at the end
                        for (int i = 0, frameExportFuturesSize = frameExportFutures.size(); i < frameExportFuturesSize; i++) {
                            NativeImage image = frameExportFutures.get(i).join();
                            fileFutures.add(FileIO.saveImage(image, ExportPathSpec.forced(this.framesFolderName, "seq_" + i))
                                    .whenComplete((f, t_) -> image.close()));
                        }

                        this.mergeFilesIntoFinalResult(fileFutures, overwriteValue);
                    });
        }
    }
}
