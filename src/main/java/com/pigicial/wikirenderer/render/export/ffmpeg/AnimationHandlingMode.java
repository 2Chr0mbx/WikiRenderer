package com.pigicial.wikirenderer.render.export.ffmpeg;

public enum AnimationHandlingMode {
    DISK_INSTANT_SAVE,
    MEMORY_CACHE,
    LIVE_FFMPEG;

    public boolean isStoredInMemory() {
        return this == MEMORY_CACHE;
    }
}
