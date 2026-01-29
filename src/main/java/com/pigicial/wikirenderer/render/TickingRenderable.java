package com.pigicial.wikirenderer.render;

import com.pigicial.wikirenderer.property.TickingPropertyBundle;

public interface TickingRenderable<P extends TickingPropertyBundle> extends Renderable<P> {

    void tick(boolean tick);

    @Override
    P getProperties();

}
