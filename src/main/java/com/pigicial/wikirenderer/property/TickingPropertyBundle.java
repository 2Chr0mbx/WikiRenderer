package com.pigicial.wikirenderer.property;

import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.screen.WikiRendererUI;
import com.pigicial.wikirenderer.screen.RenderScreen;
import io.wispforest.owo.ui.container.FlowLayout;

public interface TickingPropertyBundle extends PropertyBundle {

    Property<Boolean> getTickProperty();

    @Override
    default void buildRenderOptionGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        PropertyBundle.super.buildRenderOptionGUIControls(renderable, screen, container);
        WikiRendererUI.booleanControl(container, this.getTickProperty(), "block_animations");
    }
}
