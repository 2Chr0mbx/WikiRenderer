package com.glisco.isometricrenders.render.item;

import com.glisco.isometricrenders.property.DefaultCroppablePropertyBundle;
import com.glisco.isometricrenders.property.Property;
import com.glisco.isometricrenders.property.TickingPropertyBundle;

public class BlockStatePropertyBundle extends DefaultCroppablePropertyBundle implements TickingPropertyBundle {

    private final Property<Boolean> tick = Property.of(true);

    @Override
    public Property<Boolean> getTickProperty() {
        return tick;
    }

    @Override
    public String getOptionTranslationKey() {
        return "animated_blocks";
    }

    @Override
    protected int getDefaultScale() {
        return 125; // what the mc wiki uses it seems, and what helps to match block states with regular item (block) renders
    }
}
