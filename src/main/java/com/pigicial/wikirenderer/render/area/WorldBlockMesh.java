package com.pigicial.wikirenderer.render.area;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.render.OrthographicSort;
import com.pigicial.wikirenderer.render.area.chunk.MiniChunk;
import com.pigicial.wikirenderer.render.area.side_view.WalkabilityFilter;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.impl.client.indigo.renderer.IndigoRenderer;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.WorldMesherRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// todo: not a fan of how entities are handled in AreaRenderable and blocks are here, maybe they should be merged
public class WorldBlockMesh {

    public static boolean overrideCutoutRenderPipeline = false;
    public static GpuSampler terrainSampler = null;

    public final BlockAndTintGetter world;
    public final BlockPos from;
    private final BlockPos to;
    @Nullable
    private final Set<MiniChunk> chunksToGrabBlocksFrom;
    private AreaRenderable renderable;

    private final AABB dimensions;
    private final boolean cull;

    private MeshState state = MeshState.NEW;
    private float buildProgress = 0;
    private @Nullable CompletableFuture<Void> buildFuture = null;
    private boolean buildCancelRequested = false;

    // Vertex storage
    public final List<MeshSection> subMeshes = new ArrayList<>();
    private final HashMap<BlockPos, BlockEntity> blockEntities = new HashMap<>();

    private OrthographicSort orthographicTransparencySorting = null;
    private float lastUsedRotation;
    private double lastUsedSlant;

    private WorldBlockMesh(
            BlockAndTintGetter world,
            BlockPos from,
            BlockPos to,
            @Nullable Set<MiniChunk> chunks
    ) {
        this.from = from;
        this.to = to;
        this.chunksToGrabBlocksFrom = chunks;

        this.world = new MeshWorldOverrides(world, from, to);

        this.cull = true;
        this.dimensions = AABB.encapsulatingFullBlocks(this.from, this.to);

        this.lastUsedRotation = Float.MAX_VALUE;
        this.lastUsedSlant = Double.MAX_VALUE;

        this.scheduleRebuild();
    }

    public void setRenderable(AreaRenderable renderable) {
        this.renderable = renderable;
    }

    /**
     * Renders this world mesh into the current framebuffer, translated using the given matrix
     *
     * @param matrices The translation matrices. This is applied to the entire mesh
     */
    public void drawBlocks(PoseStack matrices) {
        if (!this.canRender()) {
            throw new IllegalStateException("World mesh not prepared!");
        }

        if (terrainSampler == null) {
            Options options = Minecraft.getInstance().options;
            int maxAnisotropy = options.textureFiltering().get() == TextureFilteringMethod.ANISOTROPIC ? options.maxAnisotropyValue() : 1;
            terrainSampler = RenderSystem.getDevice().createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.NEAREST, maxAnisotropy, OptionalDouble.empty());
        }

        if (WikiRenderer.orthographicSorting != null) {
            this.orthographicTransparencySorting = WikiRenderer.orthographicSorting;
        }

        float currentRotation = AreaPropertyBundle.INSTANCE.getUsedRotation();
        double currentSlant = AreaPropertyBundle.INSTANCE.getUsedSlant();
        if ((this.lastUsedRotation != currentRotation || this.lastUsedSlant != currentSlant) && this.orthographicTransparencySorting != null) {
            boolean isProbablyLargeSpinningObjectShrunkenDown = renderable.getProperties().scale.get() <= 8 && renderable.getProperties().rotationSpeed.get() > 0;
            if (!isProbablyLargeSpinningObjectShrunkenDown) {
                // anything smaller than 10 you probably wont see transparency issues (i.e. rendering the skyblock hub)
                this.lastUsedRotation = currentRotation;
                this.lastUsedSlant = currentSlant;

                for (MeshSection meshSection : subMeshes) {
                    meshSection.reSortTransparencyData(this.orthographicTransparencySorting);
                }
            }
        }

        List<ChunkSectionsToRender> preparedSections = new ArrayList<>();
        for (MeshSection meshSection : subMeshes) {
            preparedSections.add(renderBlockLayers(meshSection.getBuffers(), matrices.last().pose()));
        }

        for (ChunkSectionLayerGroup sectionLayer : new ChunkSectionLayerGroup[]{ChunkSectionLayerGroup.OPAQUE, ChunkSectionLayerGroup.TRANSLUCENT, ChunkSectionLayerGroup.TRIPWIRE}) {
            overrideCutoutRenderPipeline = sectionLayer == ChunkSectionLayerGroup.OPAQUE;
            for (ChunkSectionsToRender sections : preparedSections) {
                sections.renderGroup(sectionLayer, terrainSampler);
            }
        }
    }

    private ChunkSectionsToRender renderBlockLayers(Map<ChunkSectionLayer, SectionBuffers> bufferStorage, Matrix4fc posMatrix) {
        EnumMap<ChunkSectionLayer, List<RenderPass.Draw<GpuBufferSlice[]>>> enumMap = new EnumMap<>(ChunkSectionLayer.class);
        int maxIndicesRequired = 0;

        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            enumMap.put(layer, new ArrayList<>());
        }

        List<DynamicUniforms.ChunkSectionInfo> list = new ArrayList<>();
        GpuTextureView gpuTextureView = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        int width = gpuTextureView.getWidth(0);
        int height = gpuTextureView.getHeight(0);

        int infoIndex = -1;

        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            SectionBuffers buffers = bufferStorage.get(layer);
            if (buffers != null) {
                if (infoIndex == -1) {
                    infoIndex = 0;
                    list.add(new DynamicUniforms.ChunkSectionInfo(new Matrix4f(posMatrix), 0, 0, 0, 1.0F, width, height));
                }

                GpuBuffer gpuBuffer = null;
                VertexFormat.IndexType indexType = null;
                if (buffers.getIndexBuffer() == null) {
                    if (buffers.getIndexCount() > maxIndicesRequired) {
                        maxIndicesRequired = buffers.getIndexCount();
                    }
                } else {
                    gpuBuffer = buffers.getIndexBuffer();
                    indexType = buffers.getIndexType();
                }

                int sectionIndex = infoIndex;
                enumMap.put(layer, List.of(new RenderPass.Draw<>(0, buffers.getVertexBuffer(), gpuBuffer, indexType, 0, buffers.getIndexCount(),
                        (transforms, uniformUploader) -> uniformUploader.upload("ChunkSection", transforms[sectionIndex]))));
            } else {
                enumMap.put(layer, List.of());
            }
        }

        GpuBufferSlice[] gpuBufferSlices = RenderSystem.getDynamicUniforms()
                .writeChunkSections(list.toArray(new DynamicUniforms.ChunkSectionInfo[0]));
        return new ChunkSectionsToRender(gpuTextureView, enumMap, maxIndicesRequired, gpuBufferSlices);
    }

    /**
     * Checks whether this mesh is ready for rendering
     */
    public boolean canRender() {
        return this.state.canRender;
    }

    /**
     * Returns the current state of this mesh, used to indicate building progress and rendering availability
     *
     * @return The current {@code MeshState} constant
     */
    public MeshState state() {
        return this.state;
    }

    /**
     * How much of this mesh is built
     *
     * @return The build progress of this mesh
     */
    public float buildProgress() {
        return this.buildProgress;
    }

    public HashMap<BlockPos, BlockEntity> getBlockEntities() {
        return blockEntities;
    }

    /**
     * @return The origin position of this mesh's area
     */
    public BlockPos startPos() {
        return this.from;
    }

    /**
     * @return The end position of this mesh's area
     */
    public BlockPos endPos() {
        return this.to;
    }

    /**
     * @return The dimensions of this mesh's entire area
     */
    public AABB dimensions() {
        return dimensions;
    }

    public boolean canRebuild() {
        return this.buildFuture == null;
    }

    /**
     * Schedule a rebuild of this mesh on
     * an async executor
     */
    public synchronized void scheduleRebuild() {
        if (this.buildFuture != null) return;

        this.buildProgress = 0;
        this.state = this.state != MeshState.NEW
                ? MeshState.REBUILDING
                : MeshState.BUILDING;

        // todo: if i get around to properly adding iris shaders support, make sure mesh building isn't async when a shaderpack is active (and then look into shadow rendering)
        this.orthographicTransparencySorting = WikiRenderer.orthographicSorting;
        this.buildFuture = CompletableFuture.runAsync(this::buildMeshAsync);
    }

    public synchronized void stopBuilding() {
        this.buildCancelRequested = true;
        this.state = MeshState.CANCELLED;
    }

    private void buildMeshAsync() {
        Minecraft.getInstance().executeBlocking((() -> {
            this.blockEntities.clear();
            this.subMeshes.forEach(MeshSection::close);
            this.subMeshes.clear();
        }));

        Minecraft client = Minecraft.getInstance();

        HashMap<BlockPos, BlockEntity> blockEntities = new HashMap<>();

        // large islands like the crimson isle hit a verticies limit, therefore we split into smaller (but still fairly large) meshes
        record SubMesh(List<Iterable<BlockPos>> positions) {
        }

        List<SubMesh> subMeshes = new ArrayList<>();
        int regionSize = 64;
        if (this.chunksToGrabBlocksFrom != null) {
            MiniChunk firstChunk = chunksToGrabBlocksFrom.stream().findAny().orElseThrow();
            int chunkSize = (firstChunk.endX - firstChunk.startX) + 1;
            // the region size needs to be an interval of the mini chunk size, otherwise certain mini chunks can be missing
            while (chunkSize < 64) {
                chunkSize *= 2;
            }
            regionSize = chunkSize;
        }

        int scanningAreas = 0;
        int currentScanIndex = 0;
        for (int x = from.getX(); x <= to.getX(); x += regionSize) {
            for (int z = from.getZ(); z <= to.getZ(); z += regionSize) {
                BlockPos subFrom = new BlockPos(x, from.getY(), z);
                BlockPos subTo = new BlockPos(
                        Math.min(x + regionSize - 1, to.getX()),
                        to.getY(),
                        Math.min(z + regionSize - 1, to.getZ())
                );

                if (this.chunksToGrabBlocksFrom == null) {
                    subMeshes.add(new SubMesh(List.of(BlockPos.betweenClosed(subFrom, subTo))));
                    scanningAreas++;
                } else {
                    // combine the mini chunks (between 4x4-16x16, based on the user command input) into one bigger 64x64 section
                    List<Iterable<BlockPos>> miniChunkBlocksForThisMesh = new ArrayList<>();
                    for (MiniChunk chunk : this.chunksToGrabBlocksFrom) {
                        if (chunk.isWithin(subFrom.getX(), subFrom.getZ(), subTo.getX(), subTo.getZ())) {
                            miniChunkBlocksForThisMesh.add(BlockPos.betweenClosed(chunk.startX, from.getY(), chunk.startZ, chunk.endX, to.getY(), chunk.endZ));
                            scanningAreas++;
                        }
                    }

                    subMeshes.add(new SubMesh(miniChunkBlocksForThisMesh));
                }
            }
        }

        if (buildCancelRequested) {
            this.buildFuture = null;
            this.buildCancelRequested = false;
            return;
        }

        WalkabilityFilter walkabilityFilter = null;
        AreaPropertyBundle properties = AreaPropertyBundle.INSTANCE;
        if (properties.perPixel90DegreeRendering.get()) {
            if (properties.useWalkabilityFilter.get()) {
                walkabilityFilter = new WalkabilityFilter(this, properties.walkableBlocksThreshold.get(), renderable.minFloorYLevelForOverhead.get(),  renderable.maxFloorYLevelForOverhead.get(), properties.requireCeilingForCaveMode.get());
                walkabilityFilter.cacheData();
            }
        }

        AtomicInteger subMeshesUploaded = new AtomicInteger();
        AtomicInteger subMeshesToBeUploaded = new AtomicInteger();
        AtomicBoolean cancelled = new AtomicBoolean(false);

        Object lock = new Object();
        for (SubMesh data : subMeshes) {

            SectionBufferBuilderPack bufferBuilderPack = new SectionBufferBuilderPack();
            BlockRenderDispatcher blockRenderDispatcher = client.getBlockRenderer();
            PoseStack poseStack = new PoseStack();
            HashMap<ChunkSectionLayer, BufferBuilder> builderStorage = new HashMap<>();

            WorldMesherRenderContext renderContext = Renderer.get() instanceof IndigoRenderer
                    ? new WorldMesherRenderContext(this.world, layer -> this.getOrCreateBuilder(bufferBuilderPack, builderStorage, layer))
                    : null;

            for (Iterable<BlockPos> positions : data.positions) {
                if (cancelled.get()) {
                    bufferBuilderPack.close();
                    return;
                }

                currentScanIndex++;
                this.buildProgress = (float) currentScanIndex / (float) scanningAreas;
                for (BlockPos pos : positions) {

                    BlockState state = world.getBlockState(pos);
                    if (state.isAir()) continue;

                    if (walkabilityFilter != null && !walkabilityFilter.shouldRenderBlock(pos)) {
                        continue;
                    }

                    BlockPos renderPos = pos.subtract(from);
                    if (world.getBlockEntity(pos) != null) {
                        blockEntities.put(renderPos, world.getBlockEntity(pos));
                    }

                    if (!world.getFluidState(pos).isEmpty()) {
                        FluidState fluidState = world.getFluidState(pos);
                        ChunkSectionLayer fluidLayer = ItemBlockRenderTypes.getRenderLayer(fluidState);

                        poseStack.pushPose();
                        poseStack.translate(-(pos.getX() & 15), -(pos.getY() & 15), -(pos.getZ() & 15));
                        poseStack.translate(renderPos.getX(), renderPos.getY(), renderPos.getZ());

                        blockRenderDispatcher.renderLiquid(pos, world, new FluidVertexConsumer(this.getOrCreateBuilder(bufferBuilderPack, builderStorage, fluidLayer), poseStack.last().pose(), poseStack.last().normal()), state, fluidState);

                        poseStack.popPose();
                    }

                    poseStack.pushPose();
                    poseStack.translate(renderPos.getX(), renderPos.getY(), renderPos.getZ());

                    BlockStateModel model = blockRenderDispatcher.getBlockModel(state);
                    if (renderContext != null) {
                        renderContext.tessellateBlock(state, pos, model, poseStack);
                    } else {
                        blockRenderDispatcher.getModelRenderer().render(this.world, model, state, pos, poseStack, blockLayer -> this.getOrCreateBuilder(bufferBuilderPack, builderStorage, blockLayer), cull, state.getSeed(pos), OverlayTexture.NO_OVERLAY);
                    }

                    poseStack.popPose();
                }
            }

            if (cancelled.get()) {
                bufferBuilderPack.close();
                return;
            }

            subMeshesToBeUploaded.incrementAndGet();
            Minecraft.getInstance().executeBlocking((() -> {
                subMeshesToBeUploaded.decrementAndGet();

                if (cancelled.get() || buildCancelRequested) {
                    cancelled.set(true);
                    bufferBuilderPack.close();
                    if (buildCancelRequested && subMeshesToBeUploaded.get() == 0) {
                        this.buildFuture = null;
                        this.buildCancelRequested = false;
                    }
                    return;
                }

                Map<ChunkSectionLayer, MeshData> builtMeshes = new HashMap<>();

                builderStorage.forEach((layer, bufferBuilder) -> {
                    MeshData builtData = bufferBuilder.build();
                    if (builtData != null) {
                        builtMeshes.put(layer, builtData);
                    }
                });

                MeshSection meshSection = new MeshSection(bufferBuilderPack, builtMeshes, this.orthographicTransparencySorting);
                meshSection.upload();
                this.subMeshes.add(meshSection);

                synchronized (lock) {
                    subMeshesUploaded.getAndIncrement();
                    if (subMeshesUploaded.get() == subMeshes.size()) {
                        this.buildFuture = null;
                        this.state = MeshState.READY;
                    }
                }
            }));

        }

        this.blockEntities.putAll(blockEntities);
    }

    private VertexConsumer getOrCreateBuilder(SectionBufferBuilderPack bufferBuilderPack, Map<ChunkSectionLayer, BufferBuilder> builderStorage, ChunkSectionLayer layer) {
        return builderStorage.computeIfAbsent(layer, renderLayer ->
                new BufferBuilder(bufferBuilderPack.buffer(layer), VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK));
    }

    public static class Builder {

        private final BlockAndTintGetter world;

        private final BlockPos origin;
        private final BlockPos end;
        private Set<MiniChunk> chunks = null;

        public Builder(BlockAndTintGetter world, BlockPos origin, BlockPos end) {
            this.world = world;
            this.origin = origin;
            this.end = end;
        }

        public Builder(Level world, Set<MiniChunk> chunks, BlockPos origin, BlockPos end) {
            this(world, origin, end);
            this.chunks = chunks;
        }

        public WorldBlockMesh build() {
            BlockPos start = new BlockPos(Math.min(origin.getX(), end.getX()), Math.min(origin.getY(), end.getY()), Math.min(origin.getZ(), end.getZ()));
            BlockPos target = new BlockPos(Math.max(origin.getX(), end.getX()), Math.max(origin.getY(), end.getY()), Math.max(origin.getZ(), end.getZ()));

            return new WorldBlockMesh(world, start, target, chunks);
        }
    }

    public enum MeshState {
        NEW(false, true),
        CANCELLED(true, true),
        BUILDING(true, true),
        REBUILDING(true, true),
        READY(false, true),
        CORRUPT(false, false);

        public final boolean isBuildStage;
        public final boolean canRender;

        MeshState(boolean buildStage, boolean canRender) {
            this.isBuildStage = buildStage;
            this.canRender = canRender;
        }
    }
}
