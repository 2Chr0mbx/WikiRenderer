package com.pigicial.wikirenderer.render.area;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.context.CommandContext;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.mixin.access.ItemStackRenderStateAccessor;
import com.pigicial.wikirenderer.property.IntProperty;
import com.pigicial.wikirenderer.render.CameraOrientationUtil;
import com.pigicial.wikirenderer.render.DefaultRenderable;
import com.pigicial.wikirenderer.render.ParticleRestriction;
import com.pigicial.wikirenderer.render.area.bounds.ChunkScannedMeshBounds;
import com.pigicial.wikirenderer.render.area.bounds.MeshBounds;
import com.pigicial.wikirenderer.render.area.bounds.SingleCuboidMeshBounds;
import com.pigicial.wikirenderer.render.area.bounds.chunk.ChunkScanResult;
import com.pigicial.wikirenderer.render.area.bounds.chunk.HorizontalMiniChunk;
import com.pigicial.wikirenderer.render.area.bounds.chunk.MiniChunkScanner;
import com.pigicial.wikirenderer.render.entity.EntityCloner;
import com.pigicial.wikirenderer.render.entity.EntityRenderBoundsUtil;
import com.pigicial.wikirenderer.render.entity.EntityVertexBounds;
import com.pigicial.wikirenderer.render.export.ExportPathSpec;
import com.pigicial.wikirenderer.render.item.AnimationTimingsProvider;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.util.AnimationTimingUtil;
import com.pigicial.wikirenderer.util.Translate;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Rotations;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fStack;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class AreaRenderable extends DefaultRenderable<AreaPropertyBundle> implements AnimationTimingsProvider {

    private final Minecraft client = Minecraft.getInstance();

    public final IntProperty minFloorYLevelForOverhead;
    public final IntProperty maxFloorYLevelForOverhead;
    public final WorldBlockMesh mesh;

    protected List<Entity> entities = new ArrayList<>();
    private boolean entitiesFrozen;

    public AreaRenderable(WorldBlockMesh mesh) {
        this.mesh = mesh;

        AABB dimensions = mesh.bounds.buildBoundingBox();
        this.minFloorYLevelForOverhead = IntProperty.of((int) dimensions.minY, (int) dimensions.minY, (int) dimensions.maxY);
        this.maxFloorYLevelForOverhead = IntProperty.of((int) dimensions.maxY, (int) dimensions.minY, (int) dimensions.maxY);
        this.mesh.setRenderable(this);
    }

    public static AreaRenderable of(BlockPos start, BlockPos end) {
        MeshBounds bounds = new SingleCuboidMeshBounds(start, end);
        WorldBlockMesh mesh = new WorldBlockMesh(Minecraft.getInstance().level, bounds);
        return new AreaRenderable(mesh);
    }

    @Nullable
    public static AreaRenderable of(CommandContext<FabricClientCommandSource> commandContext, BlockPos origin, int chunkSize, int scanLimit) {
        ClientLevel level = Minecraft.getInstance().level;
        assert level != null;

        ChunkScanResult scanResult = MiniChunkScanner.getConnectedChunks(level, origin, chunkSize, scanLimit);
        if (scanResult == null) {
            Translate.commandError(commandContext, "no_valid_chunks");
            return null;
        }

        Set<HorizontalMiniChunk> chunks = scanResult.chunks();
        if (chunks.isEmpty()) {
            Translate.commandError(commandContext, "no_valid_chunks");
            return null;
        }

        MeshBounds bounds = new ChunkScannedMeshBounds(scanResult);
        WorldBlockMesh mesh = new WorldBlockMesh(Minecraft.getInstance().level, bounds);
        return new AreaRenderable(mesh);
    }

    @Override
    public boolean usesWorldLightMap() {
        return true;
    }

    @Override
    public void emitVerticesThenDraw(RenderScreen renderScreen, Matrix4fStack modelViewStack, PoseStack standardStack, float tickDelta, long timeSinceCreationMs) {
        if (!mesh.getMeshState().canRender) {
            if (mesh.getMeshState() == WorldBlockMesh.MeshState.CORRUPT) return;

            mesh.scheduleRebuild(true);
            return;
        }

        WikiRenderer.inAreaRenderDraw = true;

        GlobalSettingsUniform globalSettings = Minecraft.getInstance().gameRenderer.getGlobalSettingsUniform();
        globalSettings.update(
                client.getWindow().getGuiScaledWidth(),
                client.getWindow().getGuiScaledHeight(),
                1.0, 0, client.getDeltaTracker(), 0,
                new Camera(), // Passing a new/empty camera sets pos to 0,0,0
                false
        );

        AABB boundingBox = this.mesh.bounds.buildBoundingBox();
        double xSize = boundingBox.getXsize();
        double ySize = boundingBox.getYsize();
        double zSize = boundingBox.getZsize();

        AreaPropertyBundle properties = getProperties();
        SubmitNodeStorage nodeStorage = client.gameRenderer.getSubmitNodeStorage();
        CameraRenderState cameraRenderState = CameraOrientationUtil.createRenderState(this);

        standardStack.setIdentity();
        standardStack.translate(-xSize / 2f, -ySize / 2f, -zSize / 2f);

        // this could be better but whatever
        Runnable preTranslucencyTask = () -> {
            this.mesh.drawBlockEntities(standardStack, nodeStorage, cameraRenderState, tickDelta);
            if (!properties.hideEntities.get()) {
                this.drawEntities(cameraRenderState, tickDelta, standardStack, nodeStorage);
            }

            if (client.player != null) {
                Vec3 diff = Vec3.atLowerCornerOf(mesh.bounds.getMinCorner()).subtract(client.player.trackingPosition());
                standardStack.translate(-diff.x, -diff.y + 1.65, -diff.z);
                this.drawParticles(standardStack.last().pose(), tickDelta);
            }
        };

        if (!properties.hideMesh.get()) {
            PoseStack meshStack = new PoseStack();
            meshStack.mulPose(modelViewStack);
            meshStack.translate(-xSize / 2f, -ySize / 2f, -zSize / 2f);

            this.mesh.drawBlocks(meshStack, preTranslucencyTask);
        } else {
            preTranslucencyTask.run(); // run otherwise above
        }

        WikiRenderer.inAreaRenderDraw = false;
    }

    private void refreshEntities() {
        if (this.getProperties().freezeEntities.get()) {
            if (!this.entitiesFrozen) {
                this.entitiesFrozen = true;

                this.entities = this.entities
                        .stream()
                        .map(originalEntity -> {
                            Entity clonedEntity = EntityCloner.copy(originalEntity);
                            if (clonedEntity == null) return null;

                            clonedEntity.restoreFrom(originalEntity);
                            if (originalEntity instanceof LivingEntity livingOriginal && clonedEntity instanceof LivingEntity livingClone) {
                                livingClone.yHeadRot = livingOriginal.yHeadRot;
                                livingClone.yHeadRotO = livingOriginal.yHeadRotO;
                                livingClone.yBodyRot = livingOriginal.yBodyRot;
                                livingClone.yBodyRotO = livingOriginal.yBodyRotO;
                                // this doesn't copy for some reason
                            }
                            clonedEntity.baseTick();
                            return clonedEntity;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
            return;
        }

        // not frozen selected by here
        if (getProperties().autoRefreshVisibleEntities.get() || this.entitiesFrozen) {
            ClientLevel level = Minecraft.getInstance().level;
            assert level != null;
            BlockPos start = mesh.bounds.getMinCorner();
            BlockPos end = mesh.bounds.getMaxCorner();
            AABB areaBoundingBox = AABB.encapsulatingFullBlocks(start, end);

            this.entities = level.getEntities((Entity) null, AABB.encapsulatingFullBlocks(start.offset(-8, -8, -8), end.offset(8, 8, 8)), entity -> {
                EntityRenderState state = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(entity, 0);
                this.updateEntityState(entity, state);

                EntityVertexBounds entityVertexBounds = EntityRenderBoundsUtil.getPositionOffsetBasedBounds(entity, state, CameraOrientationUtil.createRenderState(this));
                AABB entityBounds = entityVertexBounds == null ? null : entityVertexBounds.getBounds();

                if (entityBounds != null && entityBounds.intersects(areaBoundingBox)) {
                    AABB intersection = entityBounds.intersect(areaBoundingBox);
                    double entityVolume = entityBounds.getXsize() * entityBounds.getYsize() * entityBounds.getZsize();
                    if (entityVolume == 0) return true; // fixes what seems to be single-vertex entities (like some item displays) not appearing from NaN math

                    double intersectionVolume = intersection.getXsize() * intersection.getYsize() * intersection.getZsize();
                    double intersectionPercentage = (intersectionVolume / entityVolume) * 100D;
                    return intersectionPercentage >= getProperties().entityBoundsIntersectionRequirement.get();
                }

                return false;
            });
        }

        this.entities.removeIf(Entity::isRemoved);
        this.entitiesFrozen = false;
    }

    private void drawEntities(CameraRenderState cameraRenderState, float delta, PoseStack standardStack, SubmitNodeStorage nodeStorage) {
        float tickDelta = entitiesFrozen ? 0 : delta;
        AreaPropertyBundle properties = this.getProperties();
        EntityRenderDispatcher entityDispatcher = client.getEntityRenderDispatcher();

        this.refreshEntities();
        this.entities.forEach(entity -> {
            if (entity instanceof Player && properties.hidePlayers.get()) return;
            if (entity instanceof ArmorStand && properties.hideArmorStands.get()) return;
            if (entity instanceof LivingEntity && properties.hideLivingEntities.get()) return;

            BlockPos meshStartPos = mesh.bounds.getMinCorner();
            Vec3 entityPosition = entity.getPosition(tickDelta);
            Vec3 offsetFromMesh = entityPosition.subtract(meshStartPos.getX(), meshStartPos.getY(), meshStartPos.getZ());

            EntityRenderState state = entityDispatcher.extractEntity(entity, tickDelta);
            this.updateEntityState(entity, state);

            entityDispatcher.submit(state, cameraRenderState, offsetFromMesh.x, offsetFromMesh.y, offsetFromMesh.z, standardStack, nodeStorage);
        });
        super.drawSubmittedRenderFeatures();
    }

    private void updateEntityState(Entity entity, EntityRenderState state) {
        AreaPropertyBundle properties = this.getProperties();
        if (properties.useFullBrightGamma.get() || properties.emulateDaylight.get()) {
            state.lightCoords = LightTexture.FULL_BRIGHT;
        } else {
            state.lightCoords = client.getEntityRenderDispatcher().getPackedLightCoords(entity, 0);
        }
        state.outlineColor = 0; // remove glow (doesn't render properly)

        if (this.entitiesFrozen && (state instanceof AvatarRenderState avatarRenderState)) {
            // fix weird cape behavior with frozen models - there might be a better way to do this but ehh this is fine for now
            avatarRenderState.capeFlap = 0;
            avatarRenderState.capeLean = 0;
            avatarRenderState.capeLean2 = 0;
            avatarRenderState.ageInTicks = 1; // 1 allows for an armor offset to fix z-fighting
        }

        if (properties.hideText.get()) {
            state.nameTag = null;
            state.nameTagAttachment = null;
        }

        if (properties.overrideEntityRotations.get()) {
            if (state instanceof LivingEntityRenderState livingEntityRenderState) {
                livingEntityRenderState.bodyRot = (properties.entityRotationOverride.get() + 180); // 180 makes it face the camera by default in the standard 135-degree rotation
                livingEntityRenderState.xRot = properties.entityPitchOverride.get();
                livingEntityRenderState.yRot = properties.entityYawOverride.get();
            }
            if (state instanceof ArmorStandRenderState armorStandRenderState) {
                armorStandRenderState.headPose = new Rotations(properties.entityPitchOverride.get(), properties.entityYawOverride.get(), 0);
            }
        }

        // todo: de-dupe
        if (state instanceof AvatarRenderState avatarRenderState) {
            if (properties.useSteveSkinForEntities.get()) {
                avatarRenderState.skin = DefaultPlayerSkin.getDefaultSkin();
            }
            if (properties.forceSmallArmsForEntities.get()) {
                avatarRenderState.skin = avatarRenderState.skin.with(PlayerSkin.Patch.create(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(PlayerModelType.SLIM))
                );
            }

            if (properties.freezePlayerArms.get()) {
                avatarRenderState.ageInTicks = 1; // 1 allows for an armor offset to fix z-fighting
            }
        }

        if (state instanceof ArmedEntityRenderState armedEntityRenderState) {
            if (properties.hideHeldItemsForEntities.get()) {
                armedEntityRenderState.leftHandItemStack = ItemStack.EMPTY;
                armedEntityRenderState.rightHandItemStack = ItemStack.EMPTY;
                armedEntityRenderState.leftHandItemState.clear();
                armedEntityRenderState.rightHandItemState.clear();
                armedEntityRenderState.leftArmPose = HumanoidModel.ArmPose.EMPTY;
                armedEntityRenderState.rightArmPose = HumanoidModel.ArmPose.EMPTY;
            } else if (properties.hideEnchantmentsForEntities.get()) {
                for (ItemStackRenderState.LayerRenderState layer : ((ItemStackRenderStateAccessor) armedEntityRenderState.leftHandItemState).wikirenderer$getLayers()) {
                    layer.setFoilType(ItemStackRenderState.FoilType.NONE);
                }
                for (ItemStackRenderState.LayerRenderState layer : ((ItemStackRenderStateAccessor) armedEntityRenderState.rightHandItemState).wikirenderer$getLayers()) {
                    layer.setFoilType(ItemStackRenderState.FoilType.NONE);
                }
            }
        }

        if (state instanceof HumanoidRenderState humanoidRenderState) {
            if (properties.hideArmorForEntities.get()) {
                humanoidRenderState.headItem.clear();
                humanoidRenderState.wornHeadType = null;
                humanoidRenderState.headEquipment = ItemStack.EMPTY;
                humanoidRenderState.chestEquipment = ItemStack.EMPTY;
                humanoidRenderState.legsEquipment = ItemStack.EMPTY;
                humanoidRenderState.feetEquipment = ItemStack.EMPTY;
            } else if (properties.hideEnchantmentsForEntities.get()) {
                humanoidRenderState.headEquipment.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
                humanoidRenderState.chestEquipment.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
                humanoidRenderState.legsEquipment.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
                humanoidRenderState.feetEquipment.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
            }
        }

        if (properties.toggleInvisibilityForEntities.get()) {
            state.isInvisible = true;
            if (state instanceof LivingEntityRenderState livingEntityRenderState) {
                livingEntityRenderState.isInvisibleToPlayer = true;
            }
        }

        if (!state.shadowPieces.isEmpty()) {
            if (properties.hideMesh.get()) {
                state.shadowPieces.clear();
            } else {
                // increase shadow height by a tiny amount to fix z-fighting, +0.001 is enough
                List<EntityRenderState.ShadowPiece> newPieces = state.shadowPieces
                        .stream()
                        .map(piece -> new EntityRenderState.ShadowPiece(piece.relativeX(), piece.relativeY() + 0.001f, piece.relativeZ(), piece.shapeBelow(), piece.alpha()))
                        .toList();
                state.shadowPieces.clear();
                state.shadowPieces.addAll(newPieces);
            }
        }
    }

    @Override
    public AreaPropertyBundle getProperties() {
        return AreaPropertyBundle.INSTANCE;
    }

    @Override
    public ParticleRestriction<?> getParticleRestriction() {
        AABB dimensions = this.mesh.bounds.buildBoundingBox();
        return ParticleRestriction.inArea(dimensions);
    }

    @Override
    public ExportPathSpec getExportPath() {
        return ExportPathSpec.of("area_renders", "area_render");
    }

    @Override
    public void dispose() {
        super.dispose();
        mesh.builtSubMeshes.forEach(MeshSection::close);
        mesh.builtSubMeshes.clear();
    }

    @Override
    public List<List<Integer>> getTicksToFullyAnimate() {
        List<List<Integer>> animationTimings = new ArrayList<>();
        if (!mesh.getMeshState().isBuildStage && mesh.getAnimationCompletionTimings().isPresent()) {
            animationTimings.add(mesh.getAnimationCompletionTimings().get());
        }

        AreaPropertyBundle properties = getProperties();
        if (!properties.hideEntities.get()) {
            List<Integer> entityAnimationTimings = new LinkedList<>();
            for (Entity entity : entities) {
                if (entity instanceof Player && properties.hidePlayers.get()) continue;
                if (entity instanceof ArmorStand && properties.hideArmorStands.get()) continue;
                if (entity instanceof LivingEntity && properties.hideLivingEntities.get()) continue;
                AnimationTimingUtil.scanTicksToFullyAnimateEntityItems(entity, entityAnimationTimings);
            }
            if (!entityAnimationTimings.isEmpty()) {
                animationTimings.add(entityAnimationTimings);
            }
        }
        return animationTimings;
    }
}
