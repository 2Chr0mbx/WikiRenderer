package com.pigicial.wikirenderer.render.export.ffmpeg;

import net.minecraft.ChatFormatting;

public enum CustomPathState {
    NOT_CHECKED(ChatFormatting.GRAY),
    CHECKING(ChatFormatting.YELLOW),
    FOUND(ChatFormatting.GREEN),
    NOT_FOUND(ChatFormatting.RED);

    private final ChatFormatting color;

    CustomPathState(ChatFormatting color) {
        this.color = color;
    }

    public ChatFormatting getColor() {
        return color;
    }
}
