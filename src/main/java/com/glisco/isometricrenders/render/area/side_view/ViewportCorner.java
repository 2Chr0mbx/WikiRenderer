package com.glisco.isometricrenders.render.area.side_view;

public enum ViewportCorner {
    TOP_LEFT(-1, 1),
    TOP_RIGHT(1, 1),
    BOTTOM_LEFT(-1, -1),
    BOTTOM_RIGHT(1, -1);

    private final int normalizedDeviceCoordsX;
    private final int normalizedDeviceCoordsY;

    ViewportCorner(int normalizedDeviceCoordsX, int normalizedDeviceCoordsY) {
        this.normalizedDeviceCoordsX = normalizedDeviceCoordsX;
        this.normalizedDeviceCoordsY = normalizedDeviceCoordsY;
    }

    public int getNDCX() {
        return normalizedDeviceCoordsX;
    }

    public int getNDCY() {
        return normalizedDeviceCoordsY;
    }
}
