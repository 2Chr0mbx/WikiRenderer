package com.pigicial.wikirenderer.mixin;

import com.pigicial.wikirenderer.property.TickingPropertyBundle;
import com.pigicial.wikirenderer.screen.RenderScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureManager.class)
public class TextureManagerMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void stopTick(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof RenderScreen screen) {
            // there needs to be a cleaner way to do this
            if (screen.renderable.getProperties() instanceof TickingPropertyBundle ticking) {
                if (!ticking.getTickProperty().get()) {
                    ci.cancel();
                }
            }
        }
    }

}
