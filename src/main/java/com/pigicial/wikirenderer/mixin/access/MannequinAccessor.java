package com.pigicial.wikirenderer.mixin.access;

import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mannequin.class)
public interface MannequinAccessor {

    @Invoker("getProfile")
    ResolvableProfile wikirenderer$getProfile();
}
