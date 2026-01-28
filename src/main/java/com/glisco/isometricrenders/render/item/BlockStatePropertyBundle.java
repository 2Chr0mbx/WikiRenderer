package com.glisco.isometricrenders.render.item;

import com.glisco.isometricrenders.property.DefaultCroppablePropertyBundle;
import com.glisco.isometricrenders.property.GlobalProperties;
import com.glisco.isometricrenders.property.Property;
import com.glisco.isometricrenders.property.TickingPropertyBundle;
import com.glisco.isometricrenders.render.Renderable;
import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.screen.RenderScreen;
import io.wispforest.owo.ui.container.FlowLayout;

public class BlockStatePropertyBundle extends DefaultCroppablePropertyBundle implements TickingPropertyBundle {

    private final Property<Boolean> tick = Property.of(true);

    @Override
    public Property<Boolean> getTickProperty() {
        return tick;
    }

    @Override
    public String getOptionTranslationKey() {
        return "block_animations";
    }

    @Override
    public void buildRenderOptionGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        TickingPropertyBundle.super.buildRenderOptionGUIControls(renderable, screen, container);
        IsometricUI.booleanControl(container, GlobalProperties.tickParticles, "particles");
    }
}
