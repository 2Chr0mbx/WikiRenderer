package com.pigicial.wikirenderer.mixin.access;

import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Mixin(ClientMannequin.class)
public interface ClientMannequinAccessor {

    @Accessor("skinLookup")
    CompletableFuture<Optional<PlayerSkin>> getSkinLookup();

    @Accessor("skinLookup")
    void setSkinLookup(CompletableFuture<Optional<PlayerSkin>> skinLookup);

    @Invoker("setSkin")
    void setSkin(PlayerSkin skin);

}
