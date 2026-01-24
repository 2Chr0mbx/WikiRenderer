package com.glisco.isometricrenders.mixin;

import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ReloadableTexture.class)
public abstract class ReloadableTextureMixin {

    @Shadow public abstract Identifier resourceId();

    @Shadow
    @Final
    private Identifier resourceId;

    @ModifyArg(
            method = "doLoad",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/GpuDevice;createTexture(Ljava/util/function/Supplier;ILcom/mojang/blaze3d/textures/TextureFormat;IIII)Lcom/mojang/blaze3d/textures/GpuTexture;"
            ),
            index = 1 // The 'usage' argument
    )
    private int injectCopySourceFlag(int usage) {
        if (this.resourceId().getPath().contains("skins/")) {
            return usage | GpuTexture.USAGE_COPY_SRC; // Turns 5 into 7
        }
        return usage;
    }
}