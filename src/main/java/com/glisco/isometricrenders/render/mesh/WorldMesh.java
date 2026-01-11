package com.glisco.isometricrenders.render.mesh;

import com.glisco.isometricrenders.render.EntityRenderable;
import com.google.common.collect.HashMultimap;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.Lighting;
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
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class WorldMesh {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldMesh.class);
    private static GpuSampler terrainSampler;


    // Render setup data
    private final BlockAndTintGetter world;
    private final BlockPos from, to;
    private final AABB dimensions;

    private final boolean cull;

    // TODO: figure out a viable replacement for this
    private final Runnable renderStartAction;
    private final Runnable renderEndAction;

    private final TriFunction<Player, BlockPos, BlockPos, List<Entity>> entitySupplier;
    private DynamicRenderInfo renderInfo = DynamicRenderInfo.EMPTY;
    private boolean entitiesFrozen;
    private boolean freezeEntities;

    // Build process data
    private WorldMesh.MeshState state = WorldMesh.MeshState.NEW;

    private float buildProgress = 0;
    private @Nullable CompletableFuture<Void> buildFuture = null;

    // Vertex storage
    private final Map<ChunkSectionLayer, SectionBuffers> bufferStorage = new HashMap<>();

    private WorldMesh(BlockAndTintGetter world, BlockPos from, BlockPos to, boolean cull, boolean useGlobalNeighbors, boolean freezeEntities, Runnable renderStartAction, Runnable renderEndAction, TriFunction<Player, BlockPos, BlockPos, List<Entity>> entitySupplier) {
        this.from = from;
        this.to = to;

        this.world = useGlobalNeighbors
                ? world
                : new MeshRenderView(world, from, to);

        this.cull = cull;
        this.freezeEntities = freezeEntities;
        this.dimensions = AABB.encapsulatingFullBlocks(this.from, this.to);
        this.entitySupplier = entitySupplier;

        this.renderStartAction = renderStartAction;
        this.renderEndAction = renderEndAction;

        this.scheduleRebuild();
    }

    /**
     * Renders this world mesh into the current framebuffer, translated using the given matrix
     *
     * @param matrices The translation matrices. This is applied to the entire mesh
     */
    public void render(PoseStack matrices) {
        if (!this.canRender()) {
            throw new IllegalStateException("World mesh not prepared!");
        }

        ChunkSectionsToRender sections = renderBlockLayers(bufferStorage, matrices.last().pose());
        if (terrainSampler == null) {
            Options options = Minecraft.getInstance().options;
            int maxAnisotropy = options.textureFiltering().get() == TextureFilteringMethod.ANISOTROPIC ? options.maxAnisotropyValue() : 1;
            terrainSampler = RenderSystem.getDevice().createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, maxAnisotropy, OptionalDouble.empty());
        }

        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.LEVEL);
        sections.renderGroup(ChunkSectionLayerGroup.OPAQUE, terrainSampler);
        sections.renderGroup(ChunkSectionLayerGroup.TRANSLUCENT, terrainSampler);
        sections.renderGroup(ChunkSectionLayerGroup.TRIPWIRE, terrainSampler);
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

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

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
     * Renamed to {@link #buildProgress()}
     */
    @Deprecated(forRemoval = true)
    public float getBuildProgress() {
        return this.buildProgress();
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
        this.bufferStorage.forEach((renderLayer, buffers) -> buffers.close());
        this.bufferStorage.clear();

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
        var allocatorStorage = new SectionBufferBuilderPack();

        var client = Minecraft.getInstance();
        var blockRenderManager = client.getBlockRenderer();

        var matrices = new PoseStack();
        var builderStorage = new HashMap<ChunkSectionLayer, BufferBuilder>();

        WorldMesherRenderContext renderContext = null;
        try {
            //noinspection UnstableApiUsage
            renderContext = Renderer.get() instanceof IndigoRenderer
                    ? new WorldMesherRenderContext(this.world, layer -> this.getOrCreateBuilder(allocatorStorage, builderStorage, layer))
                    : null;
        } catch (Throwable throwable) {
            var fabricApiVersion = FabricLoader.getInstance().getModContainer("worldmesher").get().getMetadata().getCustomValue("worldmesher:fabric_api_build_version").getAsString();
            LOGGER.error(
                    "Could not create a context for rendering Fabric API models. This is most likely due to an incompatible Fabric API version - this build of WorldMesher was compiled against '{}', try that instead",
                    fabricApiVersion,
                    throwable
            );
        }

        this.entitiesFrozen = this.freezeEntities;
        var entitiesList = this.entitySupplier.apply(client.player, this.from, this.to.offset(1, 1, 1))
                .stream()
                .map(entity -> {
                    Minecraft.getInstance().player.displayClientMessage(Component.literal("entity2 = " + entity.getType().getDescriptionId()), false);
                    if (this.freezeEntities) {
                        var originalEntity = entity;
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
                }).toList();

        var blockEntities = new HashMap<BlockPos, BlockEntity>();

        int currentBlockIndex = 0;
        int blocksToBuild = (this.to.getX() - this.from.getX() + 1)
                            * (this.to.getY() - this.from.getY() + 1)
                            * (this.to.getZ() - this.from.getZ() + 1);

        for (var pos : BlockPos.betweenClosed(this.from, this.to)) {
            currentBlockIndex++;
            this.buildProgress = currentBlockIndex / (float) blocksToBuild;

            var state = world.getBlockState(pos);
            if (state.isAir()) continue;

            var renderPos = pos.subtract(from);
            if (world.getBlockEntity(pos) != null) {
                blockEntities.put(renderPos, world.getBlockEntity(pos));
            }

            if (!world.getFluidState(pos).isEmpty()) {
                var fluidState = world.getFluidState(pos);
                var fluidLayer = ItemBlockRenderTypes.getRenderLayer(fluidState);

                matrices.pushPose();
                matrices.translate(-(pos.getX() & 15), -(pos.getY() & 15), -(pos.getZ() & 15));
                matrices.translate(renderPos.getX(), renderPos.getY(), renderPos.getZ());

                blockRenderManager.renderLiquid(pos, world, new FluidVertexConsumer(this.getOrCreateBuilder(allocatorStorage, builderStorage, fluidLayer), matrices.last().pose()), state, fluidState);

                matrices.popPose();
            }

            matrices.pushPose();
            matrices.translate(renderPos.getX(), renderPos.getY(), renderPos.getZ());

            final var model = blockRenderManager.getBlockModel(state);
            if (renderContext != null) {
                renderContext.tessellateBlock(state, pos, model, matrices);
            } else {
                blockRenderManager.getModelRenderer().render(this.world, model, state, pos, matrices, blockLayer -> this.getOrCreateBuilder(allocatorStorage, builderStorage, blockLayer), cull, state.getSeed(pos), OverlayTexture.NO_OVERLAY);
            }

            matrices.popPose();
        }

        this.bufferStorage.forEach((renderLayer, buffers) -> buffers.close());
        this.bufferStorage.clear();

        builderStorage.forEach((renderLayer, bufferBuilder) -> {
            var built = bufferBuilder.build();
            if (built == null) return;

            if (renderLayer.sortOnUpload()) {
                var camera = client.gameRenderer.getMainCamera();
                built.sortQuads(allocatorStorage.buffer(renderLayer), VertexSorting.byDistance((float) camera.position().x - (float) from.getX(), (float) camera.position().y - (float) from.getY(), (float) camera.position().z - (float) from.getZ()));
            }

            GpuBuffer vertexBuffer = RenderSystem.getDevice()
                    .createBuffer(
                            () -> "WorldMesher vertex buffer",
                            GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                            built.vertexBuffer()
                    );

            ByteBuffer indices = built.indexBuffer();
            GpuBuffer indexBuffer = indices != null
                    ? RenderSystem.getDevice()
                    .createBuffer(
                            () -> "WorldMesher index buffer",
                            GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST,
                            indices
                    )
                    : null;

            var discardedBuffer = this.bufferStorage.put(renderLayer, new SectionBuffers(vertexBuffer, indexBuffer, built.drawState().indexCount(), built.drawState().indexType()));
            if (discardedBuffer != null) {
                discardedBuffer.close();
            }
        });

        var entities = HashMultimap.<Vec3, DynamicRenderInfo.EntityEntry>create();
        for (var entityEntry : entitiesList) {
            entities.put(
                    entityEntry.entity().trackingPosition().subtract(this.from.getX(), this.from.getY(), this.from.getZ()),
                    entityEntry
            );
        }

        allocatorStorage.close();
        this.renderInfo = new DynamicRenderInfo(
                blockEntities, entities
        );
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
        private boolean cull = true;
        private boolean useGlobalNeighbors = false;
        private boolean freezeEntities = false;

        private Runnable startAction = () -> {
        };
        private Runnable endAction = () -> {
        };

        @Deprecated(forRemoval = true)
        public Builder(BlockAndTintGetter world, BlockPos origin, BlockPos end, Function<Player, List<Entity>> entitySupplier) {
            this.world = world;
            this.origin = origin;
            this.end = end;
            this.entitySupplier = (player, $, $$) -> entitySupplier.apply(player);
        }

        public Builder(BlockAndTintGetter world, BlockPos origin, BlockPos end, TriFunction<Player, BlockPos, BlockPos, List<Entity>> entitySupplier) {
            this.world = world;
            this.origin = origin;
            this.end = end;
            this.entitySupplier = entitySupplier;
        }

        public Builder(Level world, BlockPos origin, BlockPos end) {
            this(world, origin, end, (except, min, max) -> world.getEntities((Entity) null, AABB.encapsulatingFullBlocks(min, max), Objects::nonNull));
        }

        public Builder(BlockAndTintGetter world, BlockPos origin, BlockPos end) {
            this(world, origin, end, (except, min, max) -> List.of());
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

        public WorldMesh.Builder renderActions(Runnable startAction, Runnable endAction) {
            this.startAction = startAction;
            this.endAction = endAction;
            return this;
        }

        public WorldMesh build() {
            BlockPos start = new BlockPos(Math.min(origin.getX(), end.getX()), Math.min(origin.getY(), end.getY()), Math.min(origin.getZ(), end.getZ()));
            BlockPos target = new BlockPos(Math.max(origin.getX(), end.getX()), Math.max(origin.getY(), end.getY()), Math.max(origin.getZ(), end.getZ()));

            return new WorldMesh(world, start, target, cull, useGlobalNeighbors, freezeEntities, startAction, endAction, entitySupplier);
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
