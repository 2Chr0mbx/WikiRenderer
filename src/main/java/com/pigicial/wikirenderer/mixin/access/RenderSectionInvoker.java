package com.pigicial.wikirenderer.mixin.access;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.nio.ByteBuffer;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public interface RenderSectionInvoker {
    @Invoker("addSectionBuffersToUberBuffer")
    boolean wikirenderer$addSectionBuffersToUberBuffer(final ChunkSectionLayer layer, final CompiledSectionMesh key, @Nullable final ByteBuffer vertexBuffer, @Nullable final ByteBuffer indexBuffer);
}
