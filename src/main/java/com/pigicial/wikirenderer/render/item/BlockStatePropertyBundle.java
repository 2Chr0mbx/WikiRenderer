package com.pigicial.wikirenderer.render.item;

import com.pigicial.wikirenderer.property.DefaultCroppablePropertyBundle;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.screen.WikiRendererUI;
import io.wispforest.owo.ui.container.FlowLayout;

public class BlockStatePropertyBundle extends DefaultCroppablePropertyBundle {

    @Override
    public void buildRenderOptionGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        WikiRendererUI.booleanControl(container, GlobalProperties.TICK_PARTICLES, "particles");
    }
}
