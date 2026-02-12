package com.pigicial.wikirenderer.render.export.ffmpeg;

import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.render.export.ffmpeg.live.LiveRenderFFmpegAnimationHandler;
import com.pigicial.wikirenderer.screen.RenderScreen;

import static com.pigicial.wikirenderer.property.GlobalProperties.EXPORT_FRAMES;

public enum AnimationHandlingMode {
    DISK_INSTANT_SAVE,
    MEMORY_CACHE,
    LIVE_FFMPEG;

    public boolean isStoredInMemory() {
        return this == MEMORY_CACHE;
    }

    public AnimationHandler createAnimationHandler(RenderScreen screen, Renderable<?> renderable) {
        return switch (this) {
            case DISK_INSTANT_SAVE -> new InstantDiskSaveAnimationHandler(screen, renderable, EXPORT_FRAMES.get());
            case MEMORY_CACHE -> new MemoryBasedAnimationHandler(screen, renderable, EXPORT_FRAMES.get());
            case LIVE_FFMPEG -> new LiveRenderFFmpegAnimationHandler(screen, renderable, EXPORT_FRAMES.get());
        };
    }
}
