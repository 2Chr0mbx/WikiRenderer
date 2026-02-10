package com.pigicial.wikirenderer.render.area.side_view;

import net.minecraft.core.Vec3i;

public enum ExpansionSide {
    TOP(0, -1),
    BOTTOM(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    private final int defaultDx;
    private final int defaultDz;

    ExpansionSide(int dx, int dz) {
        this.defaultDx = dx;
        this.defaultDz = dz;
    }

    public Vec3i getDirection(MeshSideRotation rotation) {
        return switch (rotation) {
            case NORTH -> new Vec3i(defaultDx, 0, defaultDz);
            case SOUTH -> new Vec3i(-defaultDx, 0, -defaultDz);
            case EAST -> new Vec3i(-defaultDz, 0, defaultDx);
            case WEST -> new Vec3i(defaultDz, 0, -defaultDx);
        };
    }
}
