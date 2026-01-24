package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.render.area.AreaPropertyBundle;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BeaconRenderer.class)
public class BeaconRendererMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    public void shouldRender(CallbackInfoReturnable<Boolean> cir) {
        if (IsometricRenders.inRenderableDraw && AreaPropertyBundle.INSTANCE.hideBeaconBeams.get()) {
            cir.setReturnValue(false);
        }
    }

}
