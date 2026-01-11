package com.glisco.isometricrenders.mixin;

import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DefaultPlayerSkin.class)
public class DefaultPlayerSkinMixin {

    @Final
    @Shadow
    private static PlayerSkin[] DEFAULT_SKINS;

    /**
     * @author Pigicial
     * @reason slim steve bad, old steve good
     */
    @Overwrite
    public static PlayerSkin getDefaultSkin() {
        return DEFAULT_SKINS[15];
    }
}
