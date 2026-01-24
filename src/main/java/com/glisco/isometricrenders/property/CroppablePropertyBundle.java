package com.glisco.isometricrenders.property;

import com.glisco.isometricrenders.util.ImageRescaleMode;

public interface CroppablePropertyBundle extends PropertyBundle {

    Property<Boolean> getCropProperty();

    Property<ImageRescaleMode> getRescaleMode();

}
