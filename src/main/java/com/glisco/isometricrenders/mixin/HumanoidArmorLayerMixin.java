package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.IsometricRenders;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin {

    @Inject(
            method = "renderArmorPiece",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onlyAllowHeadPiece(CallbackInfo ci, @Local(argsOnly = true) EquipmentSlot equipmentSlot) {
        if (equipmentSlot != EquipmentSlot.HEAD && IsometricRenders.inSpriteEntityDraw) {
            ci.cancel();
        }
    }
}
