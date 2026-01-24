package com.glisco.isometricrenders.property;

import com.glisco.isometricrenders.util.ImageRescaleMode;

public class DefaultCroppablePropertyBundle extends DefaultPropertyBundle implements CroppablePropertyBundle {

    private final Property<Boolean> crop = Property.of(this.shouldCropByDefault());
    private final Property<ImageRescaleMode> rescaleMode = Property.of(ImageRescaleMode.VERTICAL);

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

    @Override
    public Property<ImageRescaleMode> getRescaleMode() {
        return rescaleMode;
    }
}
