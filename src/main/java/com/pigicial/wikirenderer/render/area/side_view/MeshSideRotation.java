package com.pigicial.wikirenderer.render.area.side_view;

public enum MeshSideRotation {
    NORTH(0),
    EAST(90),
    SOUTH(180),
    WEST(270);

    private final int rotationDegrees;

    MeshSideRotation(int rotationDegrees) {
        this.rotationDegrees = rotationDegrees;
    }

    public MeshSideRotation nextRotation() {
        MeshSideRotation[] rotations = MeshSideRotation.values();
        return rotations[(this.ordinal() + 1) % rotations.length];
    }

    public int getRotationDegrees() {
        return rotationDegrees;
    }
}
