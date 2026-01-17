package com.glisco.isometricrenders.render.area;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.render.EntityRenderable;
import com.google.common.collect.HashMultimap;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.impl.client.indigo.renderer.IndigoRenderer;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.WorldMesherRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderPipelines;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

// todo: not a fan of how entities are handled in AreaRenderable and blocks are here, maybe they should be merged
public class WorldMesh {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldMesh.class);

    public static final RenderPipeline CUTOUT_WITH_NO_TRANSPARENCY_AVERAGING = RenderPipeline.builder(RenderPipelines.TERRAIN_SNIPPET)
            .withLocation("pipeline/iso_cutout_terrain")
            .withFragmentShader(Identifier.fromNamespaceAndPath(IsometricRenders.MOD_ID, "cutout_layer_no_transparency"))
            .withBlend(new BlendFunction(
                    SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
                    SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA
            ))
            .withShaderDefine("ALPHA_CUTOUT", 0.5f)
            .build();

    public static boolean overrideCutoutRenderPipeline = false;
    private static GpuSampler terrainSampler = null;

    // Render setup data
    private final BlockAndTintGetter world;
    private final BlockPos from;
    private final BlockPos to;
    @Nullable
    private final Set<MiniChunk> chunksToGrabBlocksFrom;

    private final AABB dimensions;

    private final boolean cull;

    private final TriFunction<Player, BlockPos, BlockPos, List<Entity>> entitySupplier;
    private DynamicRenderInfo renderInfo = DynamicRenderInfo.EMPTY;
    private boolean entitiesFrozen;
    private boolean freezeEntities;
    private boolean entitiesHidden;
    private boolean hideEntities;

    // Build process data
    private WorldMesh.MeshState state = WorldMesh.MeshState.NEW;

    private float buildProgress = 0;
    private @Nullable CompletableFuture<Void> buildFuture = null;

    // Vertex storage
    public final List<Map<ChunkSectionLayer, SectionBuffers>> subMeshes = new ArrayList<>();

    private WorldMesh(
            BlockAndTintGetter world,
            BlockPos from,
            BlockPos to,
            @Nullable Set<MiniChunk> chunks,
            boolean cull,
            boolean useGlobalNeighbors,
            boolean freezeEntities,
            TriFunction<Player,
                    BlockPos,
                    BlockPos,
                    List<Entity>> entitySupplier
    ) {
        this.from = from;
        this.to = to;
        this.chunksToGrabBlocksFrom = chunks;

        this.world = useGlobalNeighbors
                ? world
                : new MeshRenderView(world, from, to);

        this.cull = cull;
        this.freezeEntities = freezeEntities;
        this.dimensions = AABB.encapsulatingFullBlocks(this.from, this.to);
        this.entitySupplier = entitySupplier;

        this.scheduleRebuild();
    }

    /**
     * Renders this world mesh into the current framebuffer, translated using the given matrix
     *
     * @param matrices The translation matrices. This is applied to the entire mesh
     */
    public void draw(PoseStack matrices) {
        if (!this.canRender()) {
            throw new IllegalStateException("World mesh not prepared!");
        }

        if (terrainSampler == null) {
            Options options = Minecraft.getInstance().options;
            int maxAnisotropy = options.textureFiltering().get() == TextureFilteringMethod.ANISOTROPIC ? options.maxAnisotropyValue() : 1;
            terrainSampler = RenderSystem.getDevice().createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.NEAREST, maxAnisotropy, OptionalDouble.empty());
        }

        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.LEVEL);

        List<ChunkSectionsToRender> preparedSections = new ArrayList<>();
        for (Map<ChunkSectionLayer, SectionBuffers> storage : subMeshes) {
            preparedSections.add(renderBlockLayers(storage, matrices.last().pose()));
        }

        // opaque runs first
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
                    infoIndex = list.size();
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
    public WorldMesh.MeshState state() {
        return this.state;
    }

    /**
     * Renamed to {@link #state()}
     */
    @Deprecated(forRemoval = true)
    public WorldMesh.MeshState getState() {
        return this.state();
    }

    /**
     * How much of this mesh is built
     *
     * @return The build progress of this mesh
     */
    public float buildProgress() {
        return this.buildProgress;
    }

    /**
     * @return An object describing the entities and block
     * entities in the area this mesh is covering, with positions
     * relative to the mesh
     */
    public DynamicRenderInfo renderInfo() {
        return this.renderInfo;
    }

    /**
     * Renamed to {@link #renderInfo()}
     */
    @Deprecated(forRemoval = true)
    public DynamicRenderInfo getRenderInfo() {
        return this.renderInfo();
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

    public boolean entitiesFrozen() {
        return this.entitiesFrozen;
    }

    public void setFreezeEntities(boolean freezeEntities) {
        this.freezeEntities = freezeEntities;
    }

    public boolean entitiesHidden() {
        return this.entitiesHidden;
    }

    public void setHideEntities(boolean hideEntities) {
        this.hideEntities = hideEntities;
    }

    /**
     * @return The dimensions of this mesh's entire area
     */
    public AABB dimensions() {
        return dimensions;
    }

    /**
     * Reset this mesh to {@link WorldMesh.MeshState#NEW}, releasing
     * all vertex buffers in the process
     */
    public void reset() {
        this.subMeshes.forEach(map -> map.values().forEach(SectionBuffers::close));
        this.subMeshes.clear();

        this.state = WorldMesh.MeshState.NEW;
    }

    /**
     * Renamed to {@link #reset()}
     */
    @Deprecated(forRemoval = true)
    public void clear() {
        this.reset();
    }

    /**
     * Schedule a rebuild of this mesh on
     * the main worker executor
     */
    public synchronized void scheduleRebuild() {
        this.scheduleRebuild(Minecraft.getInstance());
    }

    /**
     * Schedule a rebuild of this mesh,
     * on the supplied executor
     *
     * @return A future completing when the build process is finished,
     * or {@code null} if this mesh is already building
     */
    public synchronized CompletableFuture<Void> scheduleRebuild(Executor executor) {
        if (this.buildFuture != null) return this.buildFuture;

        this.buildProgress = 0;
        this.state = this.state != WorldMesh.MeshState.NEW
                ? WorldMesh.MeshState.REBUILDING
                : WorldMesh.MeshState.BUILDING;

        this.buildFuture = CompletableFuture.runAsync(this::build, executor).whenComplete((unused, throwable) -> {
            this.buildFuture = null;

            if (throwable == null) {
                state = WorldMesh.MeshState.READY;
            } else {
                LOGGER.warn("World mesh building failed", throwable);
                state = WorldMesh.MeshState.CORRUPT;
            }
        });

        return this.buildFuture;
    }

    private void build() {
        RenderSystem.assertOnRenderThread(); // Everything in here must be run in the render thread, so enforce that.

        this.entitiesFrozen = this.freezeEntities;
        this.entitiesHidden = this.hideEntities;
        this.subMeshes.forEach(map -> {
            map.forEach((layer, buffers) -> buffers.close());
            map.clear();
        });
        this.subMeshes.clear();

        Minecraft client = Minecraft.getInstance();

        HashMap<BlockPos, BlockEntity> blockEntities = new HashMap<>();
        List<DynamicRenderInfo.EntityEntry> entitiesList = this.entitySupplier.apply(client.player, this.from, this.to)
                .stream()
                .map(entity -> {
                    if (this.freezeEntities) {
                        Entity originalEntity = entity;
                        if (entity instanceof Player) {
                            entity = EntityRenderable.copy(originalEntity);
                        }

                        entity.restoreFrom(originalEntity);
                        entity.copyPosition(originalEntity);
                        entity.tick();
                    }

                    return new DynamicRenderInfo.EntityEntry(
                            entity,
                            client.getEntityRenderDispatcher().getPackedLightCoords(entity, 0)
                    );
                })
                .toList();


        // large islands like the crimson isle hit a verticies limit, therefore we split into smaller (but still fairly large) meshes
        record SubMesh(List<Iterable<BlockPos>> positions) { }
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

        for (int x = from.getX(); x <= to.getX(); x += regionSize) {
            for (int z = from.getZ(); z <= to.getZ(); z += regionSize) {
                // Calculate bounds for this specific sub-mesh
                BlockPos subFrom = new BlockPos(x, from.getY(), z);
                BlockPos subTo = new BlockPos(
                        Math.min(x + regionSize - 1, to.getX()),
                        to.getY(),
                        Math.min(z + regionSize - 1, to.getZ())
                );

                if (this.chunksToGrabBlocksFrom == null) {
                    subMeshes.add(new SubMesh(List.of(BlockPos.betweenClosed(subFrom, subTo))));
                } else {
                    // combine the mini chunks (between 4x4-16x16, based on the user command input) into one bigger 64x64 section
                    List<Iterable<BlockPos>> miniChunkBlocksForThisMesh = new ArrayList<>();
                    for (MiniChunk chunk : this.chunksToGrabBlocksFrom) {
                        if (chunk.isWithin(subFrom.getX(), subFrom.getZ(), subTo.getX(), subTo.getZ())) {
                            miniChunkBlocksForThisMesh.add(BlockPos.betweenClosed(chunk.startX, from.getY(), chunk.startZ, chunk.endX, to.getY(), chunk.endZ));
                        }
                    }

                    subMeshes.add(new SubMesh(miniChunkBlocksForThisMesh));
                }
            }
        }

        int currentBlockIndex = 0;
        int blocksToBuild = (this.to.getX() - this.from.getX() + 1)
                            * (this.to.getY() - this.from.getY() + 1)
                            * (this.to.getZ() - this.from.getZ() + 1);

        for (SubMesh data : subMeshes) {

            SectionBufferBuilderPack bufferBuilderPack = new SectionBufferBuilderPack();
            BlockRenderDispatcher blockRenderDispatcher = client.getBlockRenderer();
            PoseStack poseStack = new PoseStack();
            HashMap<ChunkSectionLayer, BufferBuilder> builderStorage = new HashMap<>();

            WorldMesherRenderContext renderContext = Renderer.get() instanceof IndigoRenderer
                    ? new WorldMesherRenderContext(this.world, layer -> this.getOrCreateBuilder(bufferBuilderPack, builderStorage, layer))
                    : null;

            for (Iterable<BlockPos> positions : data.positions) {
                for (BlockPos pos : positions) {
                    currentBlockIndex++;
                    this.buildProgress = currentBlockIndex / (float) blocksToBuild;

                    BlockState state = world.getBlockState(pos);
                    if (state.isAir()) continue;

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

                        blockRenderDispatcher.renderLiquid(pos, world, new FluidVertexConsumer(this.getOrCreateBuilder(bufferBuilderPack, builderStorage, fluidLayer), poseStack.last().pose()), state, fluidState);

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

            Map<ChunkSectionLayer, SectionBuffers> regionBuffers = new HashMap<>();
            builderStorage.forEach((layer, bufferBuilder) -> {
                MeshData builtData = bufferBuilder.build();
                if (builtData == null) return;

                GpuBuffer vBuf = RenderSystem.getDevice().createBuffer(() -> "WorldMesh Sub-VBuf", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, builtData.vertexBuffer());
                GpuBuffer iBuf = builtData.indexBuffer() != null ? RenderSystem.getDevice().createBuffer(() -> "WorldMesh Sub-IBuf", GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST, builtData.indexBuffer()) : null;

                regionBuffers.put(layer, new SectionBuffers(vBuf, iBuf, builtData.drawState().indexCount(), builtData.drawState().indexType()));
            });
            this.subMeshes.add(regionBuffers);

            bufferBuilderPack.close();
        }

        HashMultimap<Vec3, DynamicRenderInfo.EntityEntry> entities = HashMultimap.create();
        for (DynamicRenderInfo.EntityEntry entityEntry : entitiesList) {
            entities.put(
                    entityEntry.entity().trackingPosition().subtract(this.from.getX(), this.from.getY(), this.from.getZ()),
                    entityEntry
            );
        }

        this.renderInfo = new DynamicRenderInfo(blockEntities, entities);
    }

    private VertexConsumer getOrCreateBuilder(SectionBufferBuilderPack allocatorStorage, Map<ChunkSectionLayer, BufferBuilder> builderStorage, ChunkSectionLayer layer) {
        return builderStorage.computeIfAbsent(layer, renderLayer ->
                new BufferBuilder(allocatorStorage.buffer(layer), VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK));
    }


    public static class Builder {

        private final BlockAndTintGetter world;
        private final TriFunction<Player, BlockPos, BlockPos, List<Entity>> entitySupplier;

        private final BlockPos origin;
        private final BlockPos end;
        private boolean cull = false;
        private boolean useGlobalNeighbors = false;
        private boolean freezeEntities = false;
        private Set<MiniChunk> chunks = null;

        public Builder(BlockAndTintGetter world, BlockPos origin, BlockPos end, TriFunction<Player, BlockPos, BlockPos, List<Entity>> entitySupplier) {
            this.world = world;
            this.origin = origin;
            this.end = end;
            this.entitySupplier = entitySupplier;
        }

        public Builder(Level world, BlockPos origin, BlockPos end) {
            this(world, origin, end, (except, min, max) -> world.getEntities((Entity) null, AABB.encapsulatingFullBlocks(min, max), obj -> true));
        }

        public Builder(Level world, Set<MiniChunk> chunks, BlockPos origin, BlockPos end) {
            this(world, origin, end);
            this.chunks = chunks;
        }

        public WorldMesh.Builder disableCulling() {
            this.cull = false;
            return this;
        }

        public WorldMesh.Builder useGlobalNeighbors() {
            this.useGlobalNeighbors = true;
            return this;
        }

        public WorldMesh.Builder freezeEntities() {
            this.freezeEntities = true;
            return this;
        }

        public WorldMesh build() {
            BlockPos start = new BlockPos(Math.min(origin.getX(), end.getX()), Math.min(origin.getY(), end.getY()), Math.min(origin.getZ(), end.getZ()));
            BlockPos target = new BlockPos(Math.max(origin.getX(), end.getX()), Math.max(origin.getY(), end.getY()), Math.max(origin.getZ(), end.getZ()));

            return new WorldMesh(world, start, target, chunks, cull, useGlobalNeighbors, freezeEntities, entitySupplier);
        }
    }

    public enum MeshState {
        NEW(false, false),
        BUILDING(true, false),
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
