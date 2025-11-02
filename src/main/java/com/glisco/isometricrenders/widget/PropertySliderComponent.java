package com.glisco.isometricrenders.widget;

import com.glisco.isometricrenders.mixin.SliderWidgetInvoker;
import com.glisco.isometricrenders.property.IntProperty;
import io.wispforest.owo.ui.component.SliderComponent;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class PropertySliderComponent extends SliderComponent {

    private final IntProperty setting;
    private final int scrollIncrement;

    public PropertySliderComponent(Sizing horizontalSizing, Text text, int scrollIncrement, IntProperty setting) {
        super(horizontalSizing);
        this.setting = setting;
        this.scrollIncrement = scrollIncrement;

        this.message(s -> text);

        this.onChanged().subscribe(this.setting::setFromProgress);
        setting.listen((intSetting, integer) -> ((SliderWidgetInvoker) this).isometric$setValue(setting.progress()));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            this.setting.setToDefault();
            return true;
        } else {
            return super.mouseClicked(click, doubled);
        }
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        this.setting.modify((int) Math.round(amount * this.scrollIncrement));
        return true;
    }
}
