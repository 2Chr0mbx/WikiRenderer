package com.pigicial.wikirenderer.property;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import net.minecraft.network.chat.Component;

public class ResetPropertyButton extends ButtonComponent {
    private final Property<?> property;

    public ResetPropertyButton(Property<?> property) {
        super(Component.literal("✖"), b -> property.setToDefault());
        this.property = property;
    }

    @Override
    public void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        this.active = !property.isDefault();
        super.draw(graphics, mouseX, mouseY, partialTicks, delta);
    }
}
