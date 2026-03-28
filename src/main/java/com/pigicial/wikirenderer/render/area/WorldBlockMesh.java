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
import com.pigicial.wikirenderer.mixin.access.RenderSectionInvoker;
import com.pigicial.wikirenderer.render.OrthographicSort;
import com.pigicial.wikirenderer.render.area.bounds.MeshBounds;
import com.pigicial.wikirenderer.render.area.side_view.WalkabilityFilter;
import com.pigicial.wikirenderer.util.AnimationTimingUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.*;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// todo: not a fan of how entities are handled in AreaRenderable and blocks are here, maybe they should be merged
public class WorldBlockMesh {

    public static boolean overrideTerrainTransparencyRenderPipelines = false;
    public static GpuSampler terrainSampler = null;

    public final MeshWorldOverrides world;
    public final MeshBounds bounds;
    private AreaRenderable renderable;

    private final List<Integer> animationCompletionTimings = new LinkedList<>();

    private final SectionRenderDispatcher sectionRenderDispatcher;
    public final List<SectionRenderDispatcher.RenderSection> builtSubMeshes = new ArrayList<>();
    private MeshState state = MeshState.NEW;
    private CompletableFuture<Void> buildFuture = null;
    private float buildProgress = 0;
    private boolean buildCancelRequested = false;
    private final Map<BlockPos, BlockEntity> blockEntities = new ConcurrentHashMap<>();

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

        Minecraft client = Minecraft.getInstance();
        SectionCompiler sectionCompiler = getSectionCompiler(client);
        this.sectionRenderDispatcher = new SectionRenderDispatcher(client.level, client.levelRenderer, Util.backgroundExecutor(), client.renderBuffers(), sectionCompiler);
    }

    private SectionCompiler getSectionCompiler(Minecraft client) {
        Options options = client.options;
        boolean ambientOcclusion = options.ambientOcclusion().get();
        boolean cutoutLeaves = options.cutoutLeaves().get();
        ModelManager modelManager = client.getModelManager();
        return new SectionCompiler(
                ambientOcclusion,
                cutoutLeaves,
                modelManager.getBlockStateModelSet(),
                modelManager.getFluidStateModelSet(),
                client.getBlockColors(),
                client.getBlockEntityRenderDispatcher()
        );
    }

    public void setRenderable(AreaRenderable renderable) {
        this.renderable = renderable;
    }

    public void drawBlocks(PoseStack matrices, Runnable preTranslucencyTask) {
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
                reSortMeshSections();
            }
        }

        ChunkSectionsToRender sections = prepareBlockLayers(matrices.last().pose());

        for (ChunkSectionLayerGroup sectionLayer : new ChunkSectionLayerGroup[]{ChunkSectionLayerGroup.OPAQUE, ChunkSectionLayerGroup.TRANSLUCENT}) {
            if (sectionLayer == ChunkSectionLayerGroup.TRANSLUCENT) {
                preTranslucencyTask.run();
            }
            overrideTerrainTransparencyRenderPipelines = sectionLayer == ChunkSectionLayerGroup.OPAQUE;
            sections.renderGroup(sectionLayer, terrainSampler);
        }
    }

    // Based on LevelRenderer#prepareChunkRenders
    private ChunkSectionsToRender prepareBlockLayers(Matrix4fc posMatrix) {
        EnumMap<ChunkSectionLayer, Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>>> drawGroups = new EnumMap<>(ChunkSectionLayer.class);
        int largestIndexCount = 0;

        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            drawGroups.put(layer, new Int2ObjectOpenHashMap<>());
        }

        List<DynamicUniforms.ChunkSectionInfo> sectionInfos = new ArrayList<>();
        GpuTextureView gpuTextureView = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        int width = gpuTextureView.getWidth(0);
        int height = gpuTextureView.getHeight(0);

        if (sectionRenderDispatcher != null) {
            sectionRenderDispatcher.lock();
            try {
                try (Zone ignored = Profiler.get().zone("Upload WikiRenderer Mesh Global Buffers")) {
                    sectionRenderDispatcher.uploadGlobalGeomBuffersToGPU();
                }

                for (SectionRenderDispatcher.RenderSection section : builtSubMeshes) {
                    SectionMesh sectionMesh = section.getSectionMesh();
                    int uboIndex = -1;

                    for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                        SectionMesh.SectionDraw draw = sectionMesh.getSectionDraw(layer);
                        SectionRenderDispatcher.RenderSectionBufferSlice slice = sectionRenderDispatcher.getRenderSectionSlice(sectionMesh, layer);
                        if (slice != null && draw != null && (!draw.hasCustomIndexBuffer() || slice.indexBuffer() != null)) {
                            if (uboIndex == -1) {
                                uboIndex = sectionInfos.size();
                                sectionInfos.add(new DynamicUniforms.ChunkSectionInfo(new Matrix4f(posMatrix), 0, 0, 0, 1.0F, width, height));
                            }

                            int combinedHash = 173;
                            VertexFormat vertexFormat = layer.pipeline().getVertexFormat();
                            GpuBuffer vertexBuffer = slice.vertexBuffer();
                            if (layer != ChunkSectionLayer.TRANSLUCENT) {
                                combinedHash = 31 * combinedHash + vertexBuffer.hashCode();
                            }

                            int firstIndex = 0;
                            GpuBuffer indexBuffer;
                            VertexFormat.IndexType indexType;
                            if (!draw.hasCustomIndexBuffer()) {
                                if (draw.indexCount() > largestIndexCount) {
                                    largestIndexCount = draw.indexCount();
                                }

                                indexBuffer = null;
                                indexType = null;
                            } else {
                                indexBuffer = slice.indexBuffer();
                                indexType = draw.indexType();
                                if (layer != ChunkSectionLayer.TRANSLUCENT) {
                                    combinedHash = 31 * combinedHash + indexBuffer.hashCode();
                                    combinedHash = 31 * combinedHash + indexType.hashCode();
                                }

                                firstIndex = (int) (slice.indexBufferOffset() / indexType.bytes);
                            }

                            int sectionIndex = uboIndex;
                            int baseVertex = (int) (slice.vertexBufferOffset() / vertexFormat.getVertexSize());
                            List<RenderPass.Draw<GpuBufferSlice[]>> draws = drawGroups.get(layer)
                                    .computeIfAbsent(combinedHash, (_ -> new ArrayList<>()));

                            draws.add(new RenderPass.Draw<>(
                                    0,
                                    vertexBuffer,
                                    indexBuffer,
                                    indexType,
                                    firstIndex,
                                    draw.indexCount(),
                                    baseVertex,
                                    (transforms, uniformUploader) -> uniformUploader.upload("ChunkSection", transforms[sectionIndex])
                            ));
                        }
                    }
                }
            } finally {
                sectionRenderDispatcher.unlock();
            }
        }

        GpuBufferSlice[] gpuBufferSlices = RenderSystem.getDynamicUniforms()
                .writeChunkSections(sectionInfos.toArray(new DynamicUniforms.ChunkSectionInfo[0]));
        return new ChunkSectionsToRender(gpuTextureView, drawGroups, largestIndexCount, gpuBufferSlices);
    }

    public void drawBlockEntities(PoseStack standardStack, SubmitNodeStorage nodeStorage, CameraRenderState cameraRenderState, float tickDelta) {
        BlockEntityRenderDispatcher blockEntityDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        this.blockEntities.forEach((blockPos, entity) -> {
            BlockEntityRenderState state = blockEntityDispatcher.tryExtractRenderState(entity, tickDelta, null);
            if (state instanceof BeaconRenderState && AreaPropertyBundle.INSTANCE.hideBeaconBeams.get()) {
                return;
            }

            standardStack.pushPose();
            standardStack.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());

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

        this.blockEntities.clear();
        this.builtSubMeshes.forEach(SectionRenderDispatcher.RenderSection::reset);
        this.builtSubMeshes.clear();

        this.orthographicTransparencySorting = WikiRenderer.orthographicSorting;
        if (ShaderCheck.isUsingShaders() || !async) {
            this.buildFuture = CompletableFuture.completedFuture(null);
            this.compileMesh();
        } else {
            this.buildFuture = CompletableFuture.runAsync(this::compileMesh);
        }
    }

    public synchronized void stopBuilding() {
        this.buildCancelRequested = true;
        this.state = MeshState.CANCELLED;
    }

    // Based on SectionCompiler#compile and then SectionRenderDispatcher.RenderSection.RebuildTask#doTask (which calls compile)
    private void compileMesh() {
        if (buildCancelRequested) {
            synchronized (this) {
                this.buildFuture = null;
                this.buildCancelRequested = false;
            }
            return;
        }

        Minecraft client = Minecraft.getInstance();

        HashMap<BlockPos, BlockEntity> blockEntities = new HashMap<>();

        // large islands like the crimson isle hit a verticies limit, therefore we split into smaller (but still fairly large) meshes
        record SubMesh(double distance, List<Iterable<BlockPos>> positions) {
        }

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

        int index = -1;
        AtomicInteger subMeshesUploaded = new AtomicInteger();
        AtomicInteger subMeshesToBeUploaded = new AtomicInteger();
        AtomicBoolean cancelled = new AtomicBoolean(false);

        this.animationCompletionTimings.clear();

        boolean cutoutLeaves = Minecraft.getInstance().options.cutoutLeaves().get();
        BlockStateModelSet blockModelSet = client.getModelManager().getBlockStateModelSet();
        FluidRenderer fluidRenderer = new FluidRenderer(client.getModelManager().getFluidStateModelSet());
        ModelBlockRenderer blockRenderer = new ModelBlockRenderer(client.options.ambientOcclusion().get(), true, client.getBlockColors());

        BlockModelLighter.enableCaching();

        Object lock = new Object();
        for (SubMesh data : subMeshes) {
            index++;

            SectionBufferBuilderPack builders = new SectionBufferBuilderPack();

            PoseStack poseStack = new PoseStack();
            HashMap<ChunkSectionLayer, BufferBuilder> startedLayers = new HashMap<>();

            BlockQuadOutput quadOutput = (x, y, z, quad, instance) -> {
                BufferBuilder builder = this.getOrBeginLayer(startedLayers, builders, quad.materialInfo().layer());
                builder.putBlockBakedQuad(x, y, z, quad, instance);
            };
            BlockQuadOutput opaqueQuadOutput = (x, y, z, quad, instance) -> {
                BufferBuilder builder = this.getOrBeginLayer(startedLayers, builders, ChunkSectionLayer.SOLID);
                builder.putBlockBakedQuad(x, y, z, quad, instance);
            };

            for (Iterable<BlockPos> positions : data.positions) {
                if (cancelled.get()) {
                    builders.close();
                    return;
                }

                currentScanIndex++;
                this.buildProgress = (float) currentScanIndex / (float) scanningAreas;
                for (BlockPos pos : positions) {

                    BlockState blockState = world.getBlockState(pos);
                    if (blockState.isAir()) continue;
                    if (blockState.is(Blocks.LIGHT)) continue; // axiom fix

                    BlockPos renderPos = pos.subtract(minCorner);
                    if (world.getBlockEntity(pos) != null) {
                        blockEntities.put(renderPos, world.getBlockEntity(pos));
                    }

                    FluidState fluidState = world.getFluidState(pos);
                    if (!fluidState.isEmpty()) {

                        poseStack.pushPose();
                        poseStack.translate(-(pos.getX() & 15), -(pos.getY() & 15), -(pos.getZ() & 15));
                        poseStack.translate(renderPos.getX(), renderPos.getY(), renderPos.getZ());
                        //FluidVertexConsumer is used because insert renderPos into tesselarate will break what it thinks the water looks like (height/adjacent blocks and whatnot)
                        FluidRenderer.Output fluidOutput = l -> new FluidVertexConsumer(this.getOrBeginLayer(startedLayers, builders, l), poseStack.last().pose(), poseStack.last().normal());
                        fluidRenderer.tesselate(world, pos, fluidOutput, blockState, fluidState);
                        poseStack.popPose();
                    }

                    if (blockState.getRenderShape() == RenderShape.MODEL) {
                        BlockStateModel model = blockModelSet.get(blockState);
                        long randomSeed = blockState.getSeed(pos);
                        AnimationTimingUtil.scanTicksToFullyAnimateBlock(model, animationCompletionTimings, randomSeed);

                        BlockQuadOutput output = ModelBlockRenderer.forceOpaque(cutoutLeaves, blockState) ? opaqueQuadOutput : quadOutput;
                        blockRenderer.tesselateBlock(output, renderPos.getX(), renderPos.getY(), renderPos.getZ(), world, pos, blockState, model, randomSeed);
                    }
                }
            }

            if (cancelled.get()) {
                builders.close();
                return;
            }

            // based on SectionRenderDispatcher.RenderSection.RebuildTask#doTask (which calls compile)
            SectionCompiler.Results results = new SectionCompiler.Results();
            startedLayers.forEach((layer, bufferBuilder) -> {
                MeshData mesh = bufferBuilder.build();
                if (mesh != null) {
                    if (layer == ChunkSectionLayer.TRANSLUCENT && orthographicTransparencySorting != null) {
                        results.transparencyState = mesh.sortQuads(builders.buffer(layer), orthographicTransparencySorting);
                    }
                    results.renderedLayers.put(layer, mesh);
                }
            });

            subMeshesToBeUploaded.incrementAndGet();
            final int chunkIndex = index;
            Minecraft.getInstance().execute((() -> {
                subMeshesToBeUploaded.decrementAndGet();

                if (cancelled.get() || buildCancelRequested) {
                    cancelled.set(true);
                    builders.close();
                    if (buildCancelRequested && subMeshesToBeUploaded.get() == 0) {
                        this.buildFuture = null;
                        this.buildCancelRequested = false;
                    }
                    return;
                }

                CompiledSectionMesh compiledSectionMesh = new CompiledSectionMesh(null, results);
                SectionRenderDispatcher.RenderSection section = sectionRenderDispatcher.new RenderSection(chunkIndex, 0);
                this.builtSubMeshes.add(section);

                for (Map.Entry<ChunkSectionLayer, MeshData> entry : results.renderedLayers.entrySet()) {
                    MeshData meshData = entry.getValue();
                    boolean success = false;
                    while (!success) {
                        success = ((RenderSectionInvoker) section).wikirenderer$addSectionBuffersToUberBuffer(entry.getKey(), compiledSectionMesh, meshData.vertexBuffer(), meshData.indexBuffer());
                        if (!success && !RenderSystem.isOnRenderThread()) {
                            Thread.onSpinWait();
                        }
                    }

                    meshData.close();
                }

                synchronized (lock) {
                    subMeshesUploaded.getAndIncrement();
                    if (subMeshesUploaded.get() == subMeshes.size()) {
                        this.buildFuture = null;
                        this.state = MeshState.READY;
                    }
                }
            }));
        }

        BlockModelLighter.clearCache();
        this.world.setWalkabilityFilter(null);
        this.blockEntities.putAll(blockEntities);
    }

    // Based on SectionRenderDispatcher.RenderSection.ResortTransparencyTask#doTask
    private synchronized void reSortMeshSections() {
        SectionBufferBuilderPack sectionBufferBuilderPack = new SectionBufferBuilderPack();
        for (SectionRenderDispatcher.RenderSection section : builtSubMeshes) {
            if (!(section.getSectionMesh() instanceof CompiledSectionMesh compiledSectionMesh)) {
                continue;
            }

            MeshData.SortState state = compiledSectionMesh.getTransparencyState();
            if (state != null && !compiledSectionMesh.isEmpty(ChunkSectionLayer.TRANSLUCENT)) {

                ByteBufferBuilder.Result indexBuffer = state.buildSortedIndexBuffer(sectionBufferBuilderPack.buffer(ChunkSectionLayer.TRANSLUCENT), orthographicTransparencySorting);
                if (indexBuffer == null) {
                    continue;
                }

                boolean success = false;
                while (!success) {
                    success = ((RenderSectionInvoker) section).wikirenderer$addSectionBuffersToUberBuffer(ChunkSectionLayer.TRANSLUCENT, compiledSectionMesh, null, indexBuffer.byteBuffer());
                    if (!success && !RenderSystem.isOnRenderThread()) {
                        Thread.onSpinWait();
                    }
                }

                indexBuffer.close();
            }
        }
    }

    private BufferBuilder getOrBeginLayer(Map<ChunkSectionLayer, BufferBuilder> startedLayers, SectionBufferBuilderPack buffers, ChunkSectionLayer layer) {
        return startedLayers.computeIfAbsent(layer, _ -> new BufferBuilder(buffers.buffer(layer), VertexFormat.Mode.QUADS, layer.vertexFormat()));
    }

    public Optional<List<Integer>> getAnimationCompletionTimings() {
        return animationCompletionTimings.isEmpty() ? Optional.empty() : Optional.of(animationCompletionTimings);
    }

    public enum MeshState {
        NEW(false, false),
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
