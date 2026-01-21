package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.property.GlobalProperties;
import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ImageCropper {
    public static NativeImage cropTransparent(NativeImage source) {
        CropData cropData = getCropData(source);
        if (cropData == null) return source;

        return cropTransparent(source, cropData);
    }

    public static NativeImage cropTransparent(NativeImage source, CropData cropData) {
        int croppedWidth = cropData.maxX - cropData.minX + 1;
        int croppedHeight = cropData.maxY - cropData.minY + 1;
        NativeImage cropped = new NativeImage(source.format(), croppedWidth, croppedHeight, false);
        source.copyRect(cropped, cropData.minX, cropData.minY, 0, 0, croppedWidth, croppedHeight, false, false);
        return cropped;
    }

    @Nullable
    public static CropData getCropData(NativeImage source) {
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

        System.out.println("minX = " + minX + ", maxX = " + maxX + ", minY = " + minY + ", maxY = " + maxY);

        if (maxX == -1) return null;
        return new CropData(minX, maxX, minY, maxY);
    }

    public static String getFfmpegCropSize(List<CropData> dataList) {
        int offsetFromLeft = dataList.stream().mapToInt(CropData::minX).min().orElseThrow();
        int offsetFromRight = GlobalProperties.exportResolution - dataList.stream().mapToInt(CropData::maxX).max().orElseThrow();
        // min y is a little confusing since to my brain it implies from the bottom, but its from the top instead
        int offsetFromTop = dataList.stream().mapToInt(CropData::minY).min().orElseThrow();
        int offsetFromBottom = GlobalProperties.exportResolution - dataList.stream().mapToInt(CropData::maxY).max().orElseThrow();

        int width = GlobalProperties.exportResolution - offsetFromRight - offsetFromLeft;
        int height = GlobalProperties.exportResolution - offsetFromBottom - offsetFromTop;

        if (width % 2 != 0) width++;
        if (height % 2 != 0) height++;

        // fFmpeg syntax: crop=w:h:x:y
        return "crop="+ width + ":" + height + ":" + offsetFromLeft + ":" + offsetFromTop;
    }

    public record CropData(int minX, int maxX, int minY, int maxY) {}

}
