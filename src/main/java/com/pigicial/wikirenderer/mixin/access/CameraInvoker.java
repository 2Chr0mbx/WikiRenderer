package com.pigicial.wikirenderer.mixin.access;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraInvoker {

    @Invoker("setRotation")
    void wikirenderer$setRotation(float yaw, float pitch);
}
