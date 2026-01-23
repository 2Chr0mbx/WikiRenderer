package com.glisco.isometricrenders.property;

public class DefaultCroppablePropertyBundle extends DefaultPropertyBundle implements CroppablePropertyBundle {

    private final Property<Boolean> crop = Property.of(this.shouldCropByDefault());

    public DefaultCroppablePropertyBundle() {
        super();
    }

    @Override
    public Property<Boolean> getCropProperty() {
        return crop;
    }

    protected boolean shouldCropByDefault() {
        return true;
    }
}
