package com.pigicial.wikirenderer.mixin.access;

import net.minecraft.client.gui.components.Checkbox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Checkbox.class)
public interface CheckBoxAccessor {

    @Accessor("selected")
    void wikirenderer$setSelected(boolean selected);
}
