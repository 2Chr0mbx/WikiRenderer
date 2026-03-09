package com.pigicial.wikirenderer.util;

public record CornerData(int minX, int minY, int maxX, int maxY) {

    public boolean contains(int x, int y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    public int getDistanceToCenterSquared(int x, int y) {
        int middleX = minX + (maxX - minX) / 2;
        int middleY = minY + (maxY - minY) / 2;
        return (int) (Math.pow(Math.abs(x - middleX), 2) + Math.pow(Math.abs(y - middleY), 2));
    }
}
