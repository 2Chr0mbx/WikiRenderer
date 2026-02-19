package com.pigicial.wikirenderer.render;

import com.pigicial.wikirenderer.property.PropertyBundle;

public interface TickingRenderable<P extends PropertyBundle> extends Renderable<P> {

    void tick();

    @Override
    P getProperties();

}
