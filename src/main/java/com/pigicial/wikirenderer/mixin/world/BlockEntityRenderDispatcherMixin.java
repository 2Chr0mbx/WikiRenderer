package com.pigicial.wikirenderer.mixin.world;

import com.pigicial.wikirenderer.WikiRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {

    @Redirect(
            method = "tryExtractRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;shouldRender(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/phys/Vec3;)Z")
    )
    public <E extends BlockEntity, S extends BlockEntityRenderState> boolean overrideDistanceCheck(BlockEntityRenderer<E, S> instance, E blockEntity, Vec3 cameraPos) {
        if (WikiRenderer.inRenderableDraw) {
            return true;
        } else {
            return instance.shouldRender(blockEntity, cameraPos);
        }
    }
}
