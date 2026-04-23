package com.pigicial.wikirenderer.mixin.world;

import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.util.compatibility.EntityCullingCheck;
import dev.tr7zw.entityculling.EntityCullingModBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = ClientLevel.class, priority = 500)
public class EntityCullingBypassMixin {

    @Unique
    private Boolean savedTickCulling;

    // fixes the entity culling mod causing out-of-view entities to not tick, which breaks how entities can look in area renders
    @Inject(method = "tickNonPassenger", at = @At("HEAD"))
    public void wikirenderer$forceTickDuringGui(Entity entity, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof RenderScreen && EntityCullingCheck.isUsingEntityCulling()) {
            savedTickCulling = EntityCullingModBase.instance.config.tickCulling;
            EntityCullingModBase.instance.config.tickCulling = false;
        }
    }

    @Inject(method = "tickNonPassenger", at = @At("RETURN"))
    public void wikirenderer$restoreCullingDataAfterTick(Entity entity, CallbackInfo ci) {
        if (savedTickCulling != null && EntityCullingCheck.isUsingEntityCulling()) {
            EntityCullingModBase.instance.config.tickCulling = savedTickCulling;
            savedTickCulling = null;
        }
    }
}
