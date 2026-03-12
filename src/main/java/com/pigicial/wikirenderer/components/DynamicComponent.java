package com.pigicial.wikirenderer.components;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;

import java.util.function.Supplier;

public class DynamicComponent extends FlowLayout {
    protected DynamicComponent(Supplier<UIComponent> componentSupplier) {
        super(Sizing.content(), Sizing.content(), Algorithm.VERTICAL);
    }
}
