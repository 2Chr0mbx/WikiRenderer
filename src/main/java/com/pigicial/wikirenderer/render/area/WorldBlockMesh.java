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
import com.pigicial.wikirenderer.ShaderCheck;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.render.OrthographicSort;
import com.pigicial.wikirenderer.render.area.bounds.MeshBounds;
import com.pigicial.wikirenderer.render.area.side_view.WalkabilityFilter;
import com.pigicial.wikirenderer.util.AnimationTimingUtil;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.impl.client.indigo.renderer.IndigoRenderer;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.WorldMesherRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
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

    public final MeshWorldOverrides world;
    public final MeshBounds bounds;
    private AreaRenderable renderable;

    private final List<Integer> animationCompletionTimings = new LinkedList<>();

    public final List<MeshSection> builtSubMeshes = new ArrayList<>();
    private MeshState state = MeshState.NEW;
    private CompletableFuture<Void> buildFuture = null;
    private float buildProgress = 0;
    private boolean buildCancelRequested = false;
    private final HashMap<BlockPos, BlockEntity> blockEntities = new HashMap<>();

    private OrthographicSort orthographicTransparencySorting = null;
    private float lastUsedRotation;
    private double lastUsedSlant;

    public WorldBlockMesh(
            BlockAndTintGetter world,
            MeshBounds bounds
    ) {
        this.bounds = bounds;
        this.world = new MeshWorldOverrides(world, bounds);
        this.lastUsedRotation = Float.MAX_VALUE;
        this.lastUsedSlant = Double.MAX_VALUE;

        this.scheduleRebuild(true);
    }

    public void setRenderable(AreaRenderable renderable) {
        this.renderable = renderable;
    }

    public void drawBlocks(PoseStack matrices) {
        if (!this.getMeshState().canRender) {
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
            boolean isProbablyLargeSpinningObjectShrunkenDown = renderable.getProperties().scale.get() <= 15 && renderable.getProperties().rotationSpeed.get() > 0;
            if (!isProbablyLargeSpinningObjectShrunkenDown) {
                // anything smaller than 15 you probably wont see transparency issues (i.e. rendering the skyblock hub)
                this.lastUsedRotation = currentRotation;
                this.lastUsedSlant = currentSlant;

                for (MeshSection meshSection : builtSubMeshes) {
                    meshSection.reSortTransparencyData(this.orthographicTransparencySorting);
                }
            }
        }

        List<ChunkSectionsToRender> preparedSections = new ArrayList<>();
        for (MeshSection meshSection : builtSubMeshes) {
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

    public void drawBlockEntities(PoseStack standardStack, SubmitNodeStorage nodeStorage, CameraRenderState cameraRenderState, float tickDelta) {
        BlockEntityRenderDispatcher blockEntityDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        this.blockEntities.forEach((blockPos, entity) -> {
            standardStack.pushPose();
            standardStack.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());

            BlockEntityRenderState state = blockEntityDispatcher.tryExtractRenderState(entity, tickDelta, null);
            if (state != null) {
                blockEntityDispatcher.submit(state, standardStack, nodeStorage, cameraRenderState);
            }

            standardStack.popPose();
        });
        renderable.drawSubmittedRenderFeatures();
    }

    public MeshState getMeshState() {
        return this.state;
    }

    public float getBuildProgress() {
        return this.buildProgress;
    }

    public boolean canRebuild() {
        return this.buildFuture == null;
    }

    public synchronized void scheduleRebuild(boolean async) {
        if (this.buildFuture != null) return;

        this.buildProgress = 0;
        this.state = this.state != MeshState.NEW
                ? MeshState.REBUILDING
                : MeshState.BUILDING;

        this.orthographicTransparencySorting = WikiRenderer.orthographicSorting;
        if (ShaderCheck.isUsingShaders() || !async) {
            this.buildFuture = CompletableFuture.completedFuture(null);
            this.buildMesh();
        } else {
            this.buildFuture = CompletableFuture.runAsync(this::buildMesh);
        }
    }

    public synchronized void stopBuilding() {
        this.buildCancelRequested = true;
        this.state = MeshState.CANCELLED;
    }

    private synchronized void buildMesh() {
        Minecraft.getInstance().executeBlocking((() -> {
            this.blockEntities.clear();
            this.builtSubMeshes.forEach(MeshSection::close);
            this.builtSubMeshes.clear();
        }));

        Minecraft client = Minecraft.getInstance();

        HashMap<BlockPos, BlockEntity> blockEntities = new HashMap<>();

        // large islands like the crimson isle hit a verticies limit, therefore we split into smaller (but still fairly large) meshes
        record SubMesh(double distance, List<Iterable<BlockPos>> positions) { }

        List<SubMesh> unsortedSubMeshes = new ArrayList<>();
        int scanningAreas = 0;
        int regionSize = this.bounds.getSizeForSubMesh();
        BlockPos minCorner = this.bounds.getMinCorner();
        BlockPos maxCorner = this.bounds.getMaxCorner();
        int middleX = maxCorner.getX() - (maxCorner.getX() - minCorner.getX()) / 2;
        int middleZ = maxCorner.getZ() - (maxCorner.getZ() - minCorner.getZ()) / 2;

        int currentScanIndex = 0;
        for (int x = minCorner.getX(); x <= maxCorner.getX(); x += regionSize) {
            for (int z = minCorner.getZ(); z <= maxCorner.getZ(); z += regionSize) {
                BlockPos subFrom = new BlockPos(x, minCorner.getY(), z);
                BlockPos subTo = new BlockPos(
                        Math.min(x + regionSize - 1, maxCorner.getX()),
                        maxCorner.getY(),
                        Math.min(z + regionSize - 1, maxCorner.getZ())
                );

                int subMiddleX = subTo.getX() - (subTo.getX() - subFrom.getX()) / 2;
                int subMiddleZ = subTo.getZ() - (subTo.getZ() - subFrom.getZ()) / 2;
                double distance = Math.pow(middleX - subMiddleX, 2) + Math.pow(middleZ - subMiddleZ, 2);

                List<Iterable<BlockPos>> positions = bounds.buildBlockPositionsForSubMesh(subFrom, subTo);
                scanningAreas += positions.size();
                unsortedSubMeshes.add(new SubMesh(distance, positions));
            }
        }

        List<SubMesh> subMeshes = unsortedSubMeshes.stream().sorted(Comparator.comparing(mesh -> mesh.distance)).toList();

        if (buildCancelRequested) {
            this.buildFuture = null;
            this.buildCancelRequested = false;
            return;
        }

        WalkabilityFilter walkabilityFilter = null;
        AreaPropertyBundle properties = AreaPropertyBundle.INSTANCE;
        if (properties.perPixel90DegreeRendering.get()) {
            if (properties.useWalkabilityFilter.get()) {
                walkabilityFilter = new WalkabilityFilter(this, renderable);
                walkabilityFilter.cacheData();
            }
        }
        this.world.setWalkabilityFilter(walkabilityFilter);

        AtomicInteger subMeshesUploaded = new AtomicInteger();
        AtomicInteger subMeshesToBeUploaded = new AtomicInteger();
        AtomicBoolean cancelled = new AtomicBoolean(false);

        this.animationCompletionTimings.clear();

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
                    if (state.is(Blocks.LIGHT)) continue; // axiom fix

                    BlockPos renderPos = pos.subtract(bounds.getMinCorner());
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
                    long randomSeed = state.getSeed(pos);
                    AnimationTimingUtil.scanTicksToFullyAnimateBlock(model, animationCompletionTimings, randomSeed);

                    if (renderContext != null) {
                        renderContext.tessellateBlock(state, pos, model, poseStack);
                    } else {
                        boolean cull = true; // for later searching
                        blockRenderDispatcher.getModelRenderer().render(this.world, model, state, pos, poseStack, blockLayer -> this.getOrCreateBuilder(bufferBuilderPack, builderStorage, blockLayer), cull, randomSeed, OverlayTexture.NO_OVERLAY);
                    }

                    poseStack.popPose();
                }
            }

            if (cancelled.get()) {
                bufferBuilderPack.close();
                return;
            }

            subMeshesToBeUploaded.incrementAndGet();
            Minecraft.getInstance().execute((() -> {
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
                this.builtSubMeshes.add(meshSection);

                synchronized (lock) {
                    subMeshesUploaded.getAndIncrement();
                    if (subMeshesUploaded.get() == subMeshes.size()) {
                        this.buildFuture = null;
                        this.state = MeshState.READY;

                    }
                }
            }));

        }

        this.world.setWalkabilityFilter(null);
        this.blockEntities.putAll(blockEntities);
    }

    private VertexConsumer getOrCreateBuilder(SectionBufferBuilderPack bufferBuilderPack, Map<ChunkSectionLayer, BufferBuilder> builderStorage, ChunkSectionLayer layer) {
        return builderStorage.computeIfAbsent(layer, renderLayer ->
                new BufferBuilder(bufferBuilderPack.buffer(layer), VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK));
    }

    public Optional<List<Integer>> getAnimationCompletionTimings() {
        return animationCompletionTimings.isEmpty() ? Optional.empty() : Optional.of(animationCompletionTimings);
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
