package com.pigicial.wikirenderer.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;

public record DrawEntityDataCache(EntityRenderState renderState, Vec3 offset, PoseStack poseStack, boolean sprite) {
}
