package com.glisco.isometricrenders.widget;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class DynamicLabelComponent extends LabelComponent {

    private final Font textRenderer = Minecraft.getInstance().font;

    private final Supplier<Component> content;

    public DynamicLabelComponent(Supplier<Component> content) {
        super(content.get());
        super.color(Color.ofArgb(0xFFFFFF));
        super.shadow(true);
        this.content = content;
    }

    public DynamicLabelComponent shadow(boolean shadow) {
        super.shadow(shadow);
        return this;
    }

    @Override
    protected int determineHorizontalContentSize(Sizing sizing) {
        return 100;
    }

    @Override
    protected int determineVerticalContentSize(Sizing sizing) {
        return this.textRenderer.lineHeight;
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        if (this.content.get().getString().isEmpty()) {
            return;
        }
        this.text(this.content.get());
        super.draw(context, mouseX, mouseY, partialTicks, delta);
    }
}
