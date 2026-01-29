package com.pigicial.wikirenderer.mixin.access;

import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LightTexture.class)
public interface LightTextureAccessor {

    @Accessor("updateLightTexture")
    void wikirenderer$setUpdateLightTexture(boolean updateLightTexture);

}
