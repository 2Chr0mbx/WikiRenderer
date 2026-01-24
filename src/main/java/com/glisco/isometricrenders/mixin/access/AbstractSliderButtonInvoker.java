package com.glisco.isometricrenders.mixin.access;

import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractSliderButton.class)
public interface AbstractSliderButtonInvoker {
    @Invoker("setValue")
    void isometric$setValue(double value);
}
