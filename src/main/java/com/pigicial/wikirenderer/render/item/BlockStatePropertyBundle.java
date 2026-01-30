package com.pigicial.wikirenderer.render.item;

import com.pigicial.wikirenderer.property.DefaultCroppablePropertyBundle;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.property.Property;
import com.pigicial.wikirenderer.property.TickingPropertyBundle;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.screen.WikiRendererUI;
import com.pigicial.wikirenderer.screen.RenderScreen;
import io.wispforest.owo.ui.container.FlowLayout;

public class BlockStatePropertyBundle extends DefaultCroppablePropertyBundle implements TickingPropertyBundle {

    private final Property<Boolean> tick = Property.of(true);

    @Override
    public Property<Boolean> getTickProperty() {
        return tick;
    }

    @Override
    public String getTickTranslationKey() {
        return "block_animations";
    }

    @Override
    public void buildRenderOptionGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        TickingPropertyBundle.super.buildRenderOptionGUIControls(renderable, screen, container);
        WikiRendererUI.booleanControl(container, GlobalProperties.TICK_PARTICLES, "particles");
    }
}
