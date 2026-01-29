package com.pigicial.wikirenderer.mixin.access;

import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldCoordinates.class)
public interface WorldCoordinatesAccessor {

    @Accessor("x")
    WorldCoordinate wikirenderer$getX();

    @Accessor("y")
    WorldCoordinate wikirenderer$getY();

    @Accessor("z")
    WorldCoordinate wikirenderer$getZ();

}
