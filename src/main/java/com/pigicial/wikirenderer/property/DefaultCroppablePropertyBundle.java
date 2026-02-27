package com.pigicial.wikirenderer.property;

import com.pigicial.wikirenderer.render.export.ImageRescaleMode;

public class DefaultCroppablePropertyBundle extends DefaultPropertyBundle implements CroppablePropertyBundle {

    protected Property<Boolean> crop = Property.of(this.shouldCropByDefault());
    protected Property<Boolean> ffmpegCrop = Property.of(this.shouldCropByDefault());
    protected Property<ImageRescaleMode> rescaleMode = Property.of(ImageRescaleMode.LONGER_SIDE);

    public DefaultCroppablePropertyBundle() {
        super();
    }

    @Override
    public Property<Boolean> getCropProperty() {
        return crop;
    }

    @Override
    public Property<Boolean> getFFmpegCropProperty() {
        return ffmpegCrop;
    }

    protected boolean shouldCropByDefault() {
        return true;
    }

    @Override
    public Property<ImageRescaleMode> getRescaleMode() {
        return rescaleMode;
    }
}
