package com.glisco.isometricrenders.render.area.side_view;

public enum MeshSideSlant {
    BELOW(-90),
    SIDE(0),
    ABOVE(90);

    private final int rotationDegrees;

    MeshSideSlant(int rotationDegrees) {
        this.rotationDegrees = rotationDegrees;
    }

    public MeshSideSlant nextSlant() {
        MeshSideSlant[] slants = MeshSideSlant.values();
        return slants[(this.ordinal() + 1) % slants.length];
    }

    public int getRotationDegrees() {
        return rotationDegrees;
    }
}
