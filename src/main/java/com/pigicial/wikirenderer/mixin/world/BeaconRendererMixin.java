package com.pigicial.wikirenderer.mixin.world;

import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.render.area.AreaPropertyBundle;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BeaconRenderer.class)
public class BeaconRendererMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    public void shouldRender(CallbackInfoReturnable<Boolean> cir) {
        if (WikiRenderer.inRenderableDraw && AreaPropertyBundle.INSTANCE.hideBeaconBeams.get()) {
            cir.setReturnValue(false);
        }
    }

}
