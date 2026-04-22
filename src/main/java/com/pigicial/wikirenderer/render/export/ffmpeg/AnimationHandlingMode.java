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

    public boolean savesFramesToFiles() {
        return this == DISK_INSTANT_SAVE || this == MEMORY_CACHE;
    }

    public AnimationHandler createAnimationHandler(RenderScreen screen, Renderable<?> renderable) {
        int framesToRender = GlobalProperties.get().exportFrames.get();
        return switch (this) {
            case DISK_INSTANT_SAVE -> new InstantDiskSaveAnimationHandler(screen, renderable, framesToRender);
            case MEMORY_CACHE -> new MemoryBasedAnimationHandler(screen, renderable, framesToRender);
            case LIVE_FFMPEG -> new LiveRenderFFmpegAnimationHandler(screen, renderable, framesToRender);
        };
    }
}
