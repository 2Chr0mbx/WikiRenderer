package com.glisco.isometricrenders.screen;

import net.minecraft.client.Minecraft;

public class ScreenSchedulerAndSaver {

    private static RenderScreen SCHEDULED_SCREEN = null;
    private static RenderScreen SAVED_SCREEN = null;

    public static void schedule(RenderScreen screen) {
        if (Minecraft.getInstance().screen == null) {
            SCHEDULED_SCREEN = screen;
            openScheduledScreen();
        } else {
            SCHEDULED_SCREEN = screen;
        }
        // logic for my brain:
        // - if opening render screen, if already in render screen, discard that render screen, unless its (somehow) reopening itself
        // -  if opening saved screen, make sure not to discard it, therefore only discard if opening a new render screen
    }

    public static void setSavedScreen(RenderScreen screen) {
        SAVED_SCREEN = screen;
    }

    public static RenderScreen getSavedScreen() {
        return SAVED_SCREEN;
    }

    public static void openImmediately(RenderScreen screen) {
        SCHEDULED_SCREEN = screen;
        openScheduledScreen();
    }

    public static boolean hasScheduled() {
        return SCHEDULED_SCREEN != null;
    }

    public static RenderScreen getScheduledScreen() {
        return SCHEDULED_SCREEN;
    }

    public static void openScheduledScreen() {
        if (SAVED_SCREEN != null && SAVED_SCREEN != SCHEDULED_SCREEN) {
            SAVED_SCREEN.removed();
            SAVED_SCREEN = null;
        }
        Minecraft.getInstance().setScreen(SCHEDULED_SCREEN);
        SCHEDULED_SCREEN = null;
    }

}
