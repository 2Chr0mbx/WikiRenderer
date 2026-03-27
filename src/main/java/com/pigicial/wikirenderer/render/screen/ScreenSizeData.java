package com.pigicial.wikirenderer.render.screen;

public record ScreenSizeData(
        int previewWidth,
        int previewHeight,
        int guiScale
) {
    public int[] getWidthAndHeightForHigherScale(int exportScale) {
        double scaleFactor = (double) exportScale / guiScale;
        int width  = (int) (previewWidth  * scaleFactor);
        int height = (int) (previewHeight * scaleFactor);
        return new int[]{width, height};
    }
}
