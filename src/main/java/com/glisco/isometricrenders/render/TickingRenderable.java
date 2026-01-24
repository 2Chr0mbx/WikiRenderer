package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.property.TickingPropertyBundle;

public interface TickingRenderable<P extends TickingPropertyBundle> extends Renderable<P> {

    void tick(boolean tick);

    @Override
    P getProperties();

}
