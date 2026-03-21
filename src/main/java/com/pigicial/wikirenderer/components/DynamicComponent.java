package com.pigicial.wikirenderer.components;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;

import java.util.function.Supplier;

public class DynamicComponent extends FlowLayout {
    private final UIComponent component;
    private final Supplier<Boolean> displayCondition;
    private boolean exists = false;

    public DynamicComponent(UIComponent component, Supplier<Boolean> displayCondition) {
        super(Sizing.content(), Sizing.content(), Algorithm.HORIZONTAL);
        this.component = component;
        this.displayCondition = displayCondition;
        this.margins(component.margins().get());
        component.margins(Insets.of(0));
    }

    @Override
    public void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        boolean shouldShow = displayCondition.get();
        if (!exists && shouldShow) {
            exists = true;
            this.child(component);
        } else if (exists && !shouldShow) {
            exists = false;
            this.removeChild(component);
        }

        super.draw(graphics, mouseX, mouseY, partialTicks, delta);
    }
}
