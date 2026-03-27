package com.pigicial.wikirenderer.components;

import com.pigicial.wikirenderer.mixin.access.AbstractSliderButtonInvoker;
import com.pigicial.wikirenderer.property.NumberProperty;
import com.pigicial.wikirenderer.screen.RenderScreen;
import io.wispforest.owo.ui.component.SliderComponent;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class PropertySliderComponent extends SliderComponent {

    private final NumberProperty<? extends Number> setting;
    private boolean ignoringChange = false;

    public PropertySliderComponent(RenderScreen screen, Sizing horizontalSizing, Component text, NumberProperty<? extends Number> setting) {
        super(horizontalSizing);
        this.setting = setting;

        this.message(s -> text);

        this.onChanged().subscribe(value -> {
            if (!ignoringChange) {
                this.ignoringChange = true;
                this.setting.setFromProgress(value);
                this.ignoringChange = false;
            }
        });
        setting.instantListen(screen, (intSetting, integer) -> ((AbstractSliderButtonInvoker) this).wikirenderer$setValue(setting.progress()));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            this.setting.setToDefault();
            return true;
        } else {
            return super.mouseClicked(click, doubled);
        }
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        // its hard to safely scroll (safely meaning without adjusting settings) on big ui scales because of this
        return false;
    }
}
