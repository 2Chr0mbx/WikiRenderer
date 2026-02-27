package com.pigicial.wikirenderer.render.export.ffmpeg;

import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.render.export.ffmpeg.live.LiveRenderFFmpegAnimationHandler;
import com.pigicial.wikirenderer.screen.RenderScreen;

public enum AnimationHandlingMode {
    DISK_INSTANT_SAVE,
    MEMORY_CACHE,
    LIVE_FFMPEG;

    public boolean isStoredInMemory() {
        return this == MEMORY_CACHE;
    }

    public AnimationHandler createAnimationHandler(RenderScreen screen, Renderable<?> renderable) {
        int frameRate = GlobalProperties.get().exportFrames.get();
        return switch (this) {
            case DISK_INSTANT_SAVE -> new InstantDiskSaveAnimationHandler(screen, renderable, frameRate);
            case MEMORY_CACHE -> new MemoryBasedAnimationHandler(screen, renderable, frameRate);
            case LIVE_FFMPEG -> new LiveRenderFFmpegAnimationHandler(screen, renderable, frameRate);
        };
    }
}
