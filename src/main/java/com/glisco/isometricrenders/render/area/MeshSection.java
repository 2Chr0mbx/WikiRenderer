package com.glisco.isometricrenders.render.area;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionBuffers;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;

/**
 * Based on {@link net.minecraft.client.renderer.chunk.CompiledSectionMesh}
 */
public class MeshSection implements AutoCloseable {

    private final Map<ChunkSectionLayer, SectionBuffers> buffers = new EnumMap<>(ChunkSectionLayer.class);
    private final Map<ChunkSectionLayer, MeshData> builtMeshes;
    private final SectionBufferBuilderPack bufferBuilderPack;
    private VertexSorting isometricSort;

    public MeshSection(SectionBufferBuilderPack bufferBuilderPack, Map<ChunkSectionLayer, MeshData> builtMeshes, VertexSorting isometricSort) {
        this.bufferBuilderPack = bufferBuilderPack;
        this.builtMeshes = builtMeshes;
        this.isometricSort = isometricSort;
    }

    // sorts the translucency layer to consider the isometric camera angle, this fixes issues that occur where
    // multiple translucent objects are above each other but rendered improperly (i.e. flowing water) and is done
    // in vanilla rendering
    private ByteBuffer getSortedTranslucencyIndexBuffer(VertexSorting isometricSorting) {
        MeshData translucentMeshData = this.builtMeshes.get(ChunkSectionLayer.TRANSLUCENT);
        if (translucentMeshData == null) {
            return null;
        }

        MeshData.SortState transparencyState = translucentMeshData.sortQuads(bufferBuilderPack.buffer(ChunkSectionLayer.TRANSLUCENT), isometricSorting);
        if (transparencyState != null) {
            ByteBufferBuilder.Result result = transparencyState.buildSortedIndexBuffer(bufferBuilderPack.buffer(ChunkSectionLayer.TRANSLUCENT), isometricSorting);
            if (result != null) {
                return result.byteBuffer();
            }
        }

        return null;
    }

    public void upload() {
        builtMeshes.forEach((layer, meshData) -> {
            ByteBuffer indexBuffer = meshData.indexBuffer();
            if (layer == ChunkSectionLayer.TRANSLUCENT && this.isometricSort != null) {
                ByteBuffer sortedTranslucencyIndexBuffer = getSortedTranslucencyIndexBuffer(isometricSort);
                if (sortedTranslucencyIndexBuffer != null) {
                    indexBuffer = sortedTranslucencyIndexBuffer;
                }
            }

            GpuBuffer vBuf = RenderSystem.getDevice().createBuffer(() -> "WorldMesh Sub-VBuf", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, meshData.vertexBuffer());
            GpuBuffer iBuf = indexBuffer != null ? RenderSystem.getDevice().createBuffer(() -> "WorldMesh Sub-IBuf", GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST, indexBuffer) : null;
            this.buffers.put(layer, new SectionBuffers(vBuf, iBuf, meshData.drawState().indexCount(), meshData.drawState().indexType()));
        });
    }


    public void reSortTransparencyData(VertexSorting isometricSort) {
        this.isometricSort = isometricSort;
        ByteBuffer sortedTranslucencyIndexBuffer = getSortedTranslucencyIndexBuffer(isometricSort);
        if (sortedTranslucencyIndexBuffer != null) {
            SectionBuffers currentBuffer = this.buffers.get(ChunkSectionLayer.TRANSLUCENT);
            if (currentBuffer == null) {
                return;
            }

            if (currentBuffer.getIndexBuffer() == null) {
                currentBuffer.setIndexBuffer(RenderSystem.getDevice().createBuffer(() -> "WorldMesh Sub-IBuf", GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST, sortedTranslucencyIndexBuffer));
            } else {
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(currentBuffer.getIndexBuffer().slice(), sortedTranslucencyIndexBuffer);
            }
        }
    }

    public Map<ChunkSectionLayer, SectionBuffers> getBuffers() {
        return buffers;
    }

    @Override
    public void close() {
        this.bufferBuilderPack.close();
        this.buffers.forEach((layer, buffers) -> buffers.close());
        this.buffers.clear();
        this.builtMeshes.forEach((layer, meshData) -> meshData.close());
        this.builtMeshes.clear();
    }
}
