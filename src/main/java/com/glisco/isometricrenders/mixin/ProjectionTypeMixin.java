package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.IsometricRenders;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProjectionType.class)
public class ProjectionTypeMixin {

    @Inject(method = "vertexSorting", at = @At("HEAD"), cancellable = true)
    public void vertexSorting(CallbackInfoReturnable<VertexSorting> cir) {
        ProjectionType self = (ProjectionType) (Object) this;
        if (self == ProjectionType.ORTHOGRAPHIC && IsometricRenders.orthographicSorting != null) {
            cir.setReturnValue(IsometricRenders.orthographicSorting);
        }
    }
}
