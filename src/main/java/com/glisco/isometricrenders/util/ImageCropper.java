package com.glisco.isometricrenders.util;

import com.mojang.blaze3d.platform.NativeImage;

public class ImageCropper {
    public static NativeImage cropTransparent(NativeImage source) {
        int width = source.getWidth();
        int height = source.getHeight();

        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                byte alphaByte = source.getLuminanceOrAlpha(x, y);
                int alpha = alphaByte & 0xFF;

                if (alpha > 0) {
                    minX = Math.min(x, minX);
                    maxX = Math.max(x, maxX);
                    minY = Math.min(y, minY);
                    maxY = Math.max(y, maxY);
                }
            }
        }

        if (maxX == -1) return source;

        int croppedWidth = maxX - minX + 1;
        int croppedHeight = maxY - minY + 1;
        NativeImage cropped = new NativeImage(source.format(), croppedWidth, croppedHeight, false);
        source.copyRect(cropped, minX, minY, 0, 0, croppedWidth, croppedHeight, false, false);
        return cropped;
    }

}
