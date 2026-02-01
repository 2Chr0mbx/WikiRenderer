package com.pigicial.wikirenderer.render.export.ffmpeg;

public enum AnimationHandlingMode {
    DISK_INSTANT_SAVE,
    MEMORY_CACHE;

    public boolean isStoredInMemory() {
        return this == MEMORY_CACHE;
    }
}
