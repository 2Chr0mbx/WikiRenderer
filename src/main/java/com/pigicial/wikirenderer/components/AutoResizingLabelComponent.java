package com.pigicial.wikirenderer.components;

import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.core.Size;
import net.minecraft.network.chat.Component;

public class AutoResizingLabelComponent extends LabelComponent {
    public AutoResizingLabelComponent(Component text) {
        super(text);
    }

    @Override
    public void inflate(Size space) {
        this.maxWidth(Math.max(space.width() - 5, 20));
        super.inflate(space);
    }
}
