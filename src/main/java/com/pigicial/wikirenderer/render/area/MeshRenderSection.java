package com.pigicial.wikirenderer.render.area;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.pigicial.wikirenderer.util.AnimationTimingUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MeshRenderSection implements AutoCloseable {
    public static final int RENDER_SECTION_SIZE = 32; // vanilla is 16, but it seems 32 is perfectly doable without hitting vertex limits
    private final int sectionX;
    private final int sectionY;
    private final int sectionZ;
    private double distanceFromCenter;

    private final Map<ChunkSectionLayer, SectionBuffers> buffers = new EnumMap<>(ChunkSectionLayer.class);
    private Map<ChunkSectionLayer, MeshData> builtMeshes = new HashMap<>();

    protected Map<BlockPos, BlockEntity> blockEntities = new ConcurrentHashMap<>();
    protected List<Integer> animationCompletionTimings = new LinkedList<>();
    private volatile boolean dirty;
    protected volatile boolean isBuilding = false;
    protected volatile boolean forceUpdate = false;
    protected volatile boolean buildAttempted = false;

    public MeshRenderSection(int sectionX, int sectionY, int sectionZ) {
        this.sectionX = sectionX;
        this.sectionY = sectionY;
        this.sectionZ = sectionZ;
    }

    public void setDistanceFromCenter(double distanceFromCenter) {
        this.distanceFromCenter = distanceFromCenter;
    }

    public double getDistanceFromCenter() {
        return distanceFromCenter;
    }

    public BlockPos getFrom() {
        return new BlockPos(sectionToBlockCoordinate(sectionX), sectionToBlockCoordinate(sectionY), sectionToBlockCoordinate(sectionZ));
    }

    public BlockPos getTo() {
        return getFrom().offset(RENDER_SECTION_SIZE - 1, RENDER_SECTION_SIZE - 1, RENDER_SECTION_SIZE - 1);
    }

    public Map<ChunkSectionLayer, SectionBuffers> getBuffers() {
        return buffers;
    }

    public void buildAndSubmit(WorldBlockMesh mesh) {
        if (isBuilding) return;
        isBuilding = true;

        Minecraft client = Minecraft.getInstance();

        BlockRenderDispatcher blockRenderDispatcher = client.getBlockRenderer();

        List<Integer> animationCompletionTimings = new LinkedList<>();
        Map<BlockPos, BlockEntity> blockEntities = new ConcurrentHashMap<>();

        PoseStack poseStack = new PoseStack();
        HashMap<ChunkSectionLayer, BufferBuilder> startedLayers = new HashMap<>();
        SectionBufferBuilderPack builders = new SectionBufferBuilderPack();

        ModelBlockRenderer.enableCaching();
        List<Iterable<BlockPos>> blockPositionsForBuilding = mesh.bounds.buildBlockPositionsForSubMesh(getFrom(), getTo());
        for (Iterable<BlockPos> positions : blockPositionsForBuilding) {
            if (mesh.buildingCancelled) {
                builders.discardAll();
                setNotDirty();
                return;
            }

            for (BlockPos pos : positions) {

                BlockState blockState = mesh.world.getBlockState(pos);
                if (blockState.isAir()) continue;
                if (blockState.is(Blocks.LIGHT)) continue; // axiom fix

                if (mesh.world.getBlockEntity(pos) != null) {
                    blockEntities.put(new BlockPos(pos), mesh.world.getBlockEntity(pos));
                }

                FluidState fluidState = mesh.world.getFluidState(pos);
                if (!fluidState.isEmpty()) {
                    ChunkSectionLayer fluidLayer = ItemBlockRenderTypes.getRenderLayer(fluidState);

                    // Fluid renderer emits vertices in 0-15 local space.
                    // We need to bake the vanilla section origin into the buffer
                    // the same way solid blocks bake full world pos.
                    int vanillaSectionOriginX = pos.getX() & ~15;
                    int vanillaSectionOriginY = pos.getY() & ~15;
                    int vanillaSectionOriginZ = pos.getZ() & ~15;

                    poseStack.pushPose();
                    poseStack.translate(vanillaSectionOriginX, vanillaSectionOriginY, vanillaSectionOriginZ);
                    blockRenderDispatcher.renderLiquid(
                            pos,
                            mesh.world,
                            new FluidVertexConsumer(
                                    mesh.getOrBeginLayer(startedLayers, builders, fluidLayer),
                                    poseStack.last().pose(),
                                    poseStack.last().normal()
                            ),
                            blockState, fluidState
                    );
                    poseStack.popPose();
                }

                if (blockState.getRenderShape() == RenderShape.MODEL) {
                    BlockStateModel model = blockRenderDispatcher.getBlockModel(blockState);
                    long randomSeed = blockState.getSeed(pos);
                    AnimationTimingUtil.scanTicksToFullyAnimateBlock(model, animationCompletionTimings, randomSeed);

                    poseStack.pushPose();
                    poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
                    blockRenderDispatcher.getModelRenderer().render(mesh.world, model, blockState, pos, poseStack, blockLayer -> mesh.getOrBeginLayer(startedLayers, builders, blockLayer), true, randomSeed, OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();
                }
            }
        }

        if (mesh.buildingCancelled) {
            builders.discardAll();
            setNotDirty();
            return;
        }

        // based on SectionRenderDispatcher.RenderSection.RebuildTask#doTask (which calls compile)
        Map<ChunkSectionLayer, MeshData> newlyBuiltMeshes = new HashMap<>();
        startedLayers.forEach((layer, bufferBuilder) -> {
            MeshData builtMesh = bufferBuilder.build();
            if (builtMesh != null) {
                newlyBuiltMeshes.put(layer, builtMesh);
            }
        });

        if (mesh.buildingCancelled) {
            newlyBuiltMeshes.values().forEach(MeshData::close);
            setNotDirty();
            return;
        }

        client.execute((() -> {
            if (mesh.buildingCancelled) {
                newlyBuiltMeshes.values().forEach(MeshData::close);
                setNotDirty();
                return;
            }

            if (newlyBuiltMeshes.isEmpty()) {
                close();
                setNotDirty();
                this.blockEntities = blockEntities;
                return;
            }

            for (Map.Entry<ChunkSectionLayer, MeshData> entry : newlyBuiltMeshes.entrySet()) {
                ChunkSectionLayer layer = entry.getKey();
                MeshData meshData = entry.getValue();

                if (layer == ChunkSectionLayer.TRANSLUCENT && mesh.orthographicTransparencySorting != null) {
                    meshData.sortQuads(builders.buffer(layer), mesh.orthographicTransparencySorting);
                }

                ByteBuffer indexBuffer = meshData.indexBuffer();

                GpuBuffer vBuf = RenderSystem.getDevice().createBuffer(() -> "WorldMesh Sub-VBuf", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, meshData.vertexBuffer());
                GpuBuffer iBuf = indexBuffer != null ? RenderSystem.getDevice().createBuffer(() -> "WorldMesh Sub-IBuf", GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST, indexBuffer) : null;
                this.buffers.put(layer, new SectionBuffers(vBuf, iBuf, meshData.drawState().indexCount(), meshData.drawState().indexType()));
            }

            this.builtMeshes = newlyBuiltMeshes;
            this.animationCompletionTimings = animationCompletionTimings;
            this.blockEntities = blockEntities;
            setNotDirty();
        }));

        ModelBlockRenderer.clearCache();
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setNotDirty() {
        dirty = false;
        isBuilding = false;
        forceUpdate = false;
        buildAttempted = true;
    }

    public void setDirty(boolean force) {
        dirty = true;
        forceUpdate = force;
    }

    public boolean isForceUpdate() {
        return forceUpdate;
    }

    @Override
    public void close() {
        this.buffers.forEach((layer, buffers) -> buffers.close());
        this.buffers.clear();
        this.builtMeshes.forEach((layer, meshData) -> meshData.close());
        this.builtMeshes.clear();

        if (blockEntities != null) {
            blockEntities.clear();
        }
        if (animationCompletionTimings != null) {
            animationCompletionTimings.clear();
        }
    }

    public void markBuildNotAttempted() {
        buildAttempted = false;
    }

    public boolean hasBuildBeenAttempted() {
        return buildAttempted;
    }

    public void reSortTransparencyAndSubmit(WorldBlockMesh mesh) {
        MeshData translucentMeshData = this.builtMeshes.get(ChunkSectionLayer.TRANSLUCENT);
        if (translucentMeshData == null) return;

        SectionBuffers currentBuffer = this.buffers.get(ChunkSectionLayer.TRANSLUCENT);
        if (currentBuffer == null) return;

        MeshData.SortState transparencyState = translucentMeshData.sortQuads(mesh.resortBufferPack.buffer(ChunkSectionLayer.TRANSLUCENT), mesh.orthographicTransparencySorting);
        if (transparencyState == null) return;

        ByteBufferBuilder.Result indexBuffer = transparencyState.buildSortedIndexBuffer(mesh.resortBufferPack.buffer(ChunkSectionLayer.TRANSLUCENT), mesh.orthographicTransparencySorting);
        if (indexBuffer == null) return;
        ByteBuffer sortedTranslucencyIndexBuffer = indexBuffer.byteBuffer();

        Minecraft.getInstance().execute(() -> {
            if (currentBuffer.getIndexBuffer() == null) {
                currentBuffer.setIndexBuffer(RenderSystem.getDevice().createBuffer(() -> "WorldMesh Sub-IBuf", GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST, sortedTranslucencyIndexBuffer));
            } else {
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(currentBuffer.getIndexBuffer().slice(), sortedTranslucencyIndexBuffer);
            }
        });

    }

    public static long getSectionIndex(int sectionX, int sectionY, int sectionZ) {
        return SectionPos.asLong(sectionX, sectionY, sectionZ);
    }

    public static int getSection(int block) {
        return block >> 5;
    }

    public static int sectionToBlockCoordinate(int sectionCoordinate) {
        return sectionCoordinate << 5;
    }
}
