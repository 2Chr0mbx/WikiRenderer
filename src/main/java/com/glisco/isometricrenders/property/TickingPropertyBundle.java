package com.glisco.isometricrenders.property;

import com.glisco.isometricrenders.render.Renderable;
import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.screen.RenderScreen;
import io.wispforest.owo.ui.container.FlowLayout;

public interface TickingPropertyBundle extends PropertyBundle {

    Property<Boolean> getTickProperty();

    String getOptionTranslationKey();

    @Override
    default void buildRenderOptionGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        PropertyBundle.super.buildRenderOptionGUIControls(renderable, screen, container);
        IsometricUI.booleanControl(container, this.getTickProperty(), this.getOptionTranslationKey());
    }
}
