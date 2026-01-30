package com.pigicial.wikirenderer.mixin.access;

import net.minecraft.world.entity.ElytraAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ElytraAnimationState.class)
public interface ElytraAnimationStateAccessor {

    @Accessor("rotX")
    void isometric$setRotX(float rotX);

    @Accessor("rotZ")
    void isometric$setRotZ(float rotZ);
}
