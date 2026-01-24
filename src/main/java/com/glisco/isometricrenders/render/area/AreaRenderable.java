package com.glisco.isometricrenders.render.area;

import com.glisco.isometricrenders.mixin.access.ItemStackRenderStateAccessor;
import com.glisco.isometricrenders.render.DefaultRenderable;
import com.glisco.isometricrenders.render.entity.EntityRenderable;
import com.glisco.isometricrenders.util.ExportPathSpec;
import com.glisco.isometricrenders.util.ParticleRestriction;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AreaRenderable extends DefaultRenderable<AreaPropertyBundle> {

    private final Minecraft client = Minecraft.getInstance();

    public final WorldBlockMesh mesh;
    public final int ySize;
    public final int xSize;
    public final int zSize;

    protected List<Entity> entities = new ArrayList<>();
    private boolean entitiesFrozen;

    public AreaRenderable(WorldBlockMesh mesh) {
        this.mesh = mesh;

        AABB dimensions = mesh.dimensions();
        this.xSize = (int) dimensions.getXsize();
        this.ySize = (int) dimensions.getYsize();
        this.zSize = (int) dimensions.getZsize();
    }

    public static AreaRenderable of(BlockPos origin, BlockPos end) {
        return new AreaRenderable(new WorldBlockMesh.Builder(Minecraft.getInstance().level, origin, end).build());
    }

    @Nullable
    public static AreaRenderable of(BlockPos origin, int chunkSize) {
        ClientLevel level = Minecraft.getInstance().level;
        assert level != null;

        Set<MiniChunk> chunks = MiniChunkScanner.getConnectedChunks(level, origin, chunkSize);
        if (chunks.isEmpty()) {
            return null;
        }

        int minX = chunks.stream().mapToInt(c -> c.startX).min().getAsInt();
        int maxX = chunks.stream().mapToInt(c -> c.endX).max().getAsInt();
        int minZ = chunks.stream().mapToInt(c -> c.startZ).min().getAsInt();
        int maxZ = chunks.stream().mapToInt(c -> c.endZ).max().getAsInt();
        BlockPos firstPos = new BlockPos(minX, 0, minZ);
        BlockPos secondPos = new BlockPos(maxX, level.getMaxY(), maxZ);

        return new AreaRenderable(new WorldBlockMesh.Builder(level, chunks, firstPos, secondPos).build());
    }

    @Override
    public void emitVerticesThenDraw(Matrix4fStack modelViewStack, PoseStack standardStack, MultiBufferSource vertexConsumers, float tickDelta) {
        if (!mesh.canRender()) {
            if (mesh.state() == WorldBlockMesh.MeshState.CORRUPT) return;

            mesh.scheduleRebuild();
            return;
        }

        GlobalSettingsUniform globalSettings = Minecraft.getInstance().gameRenderer.getGlobalSettingsUniform();
        globalSettings.update(
                client.getWindow().getGuiScaledWidth(),
                client.getWindow().getGuiScaledHeight(),
                1.0, 0, client.getDeltaTracker(), 0,
                new Camera(), // Passing a new/empty camera sets pos to 0,0,0
                false
        );

        AreaPropertyBundle properties = getProperties();
        if (!properties.hideMesh.get()) {
            PoseStack meshStack = new PoseStack();
            meshStack.mulPose(modelViewStack);
            meshStack.translate(-xSize / 2f, -ySize / 2f, -zSize / 2f);
            this.mesh.drawBlocks(meshStack);
        }

        standardStack.setIdentity();
        standardStack.translate(-xSize / 2f, -ySize / 2f, -zSize / 2f);

        SubmitNodeStorage nodeStorage = client.gameRenderer.getSubmitNodeStorage();

        CameraRenderState cameraRenderState = new CameraRenderState();
        // this makes certain things face the camera, like text, fishing bobbers, etc, see what uses the orientation field
        cameraRenderState.orientation.rotationYXZ(
                (float) Math.PI - (float) Math.toRadians(this.getProperties().getUsedRotation()),
                (float) Math.PI + (float) Math.toRadians(this.getProperties().getUsedSlant()),
                (float) Math.PI);

        this.drawBlockEntities(standardStack, nodeStorage, cameraRenderState, tickDelta);
        this.drawEntities(cameraRenderState, tickDelta, standardStack, nodeStorage);

        Vec3 diff = Vec3.atLowerCornerOf(mesh.startPos()).subtract(client.player.trackingPosition());
        standardStack.translate(-diff.x, -diff.y + 1.65, -diff.z);
        this.drawParticles(standardStack.last().pose(), tickDelta);

        super.drawSubmittedRenderFeatures();
    }

    private void drawBlockEntities(PoseStack standardStack, SubmitNodeStorage nodeStorage, CameraRenderState cameraRenderState, float tickDelta) {
        BlockEntityRenderDispatcher blockEntityDispatcher = client.getBlockEntityRenderDispatcher();
        mesh.getBlockEntities().forEach((blockPos, entity) -> {
            standardStack.pushPose();
            standardStack.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());

            BlockEntityRenderState state = blockEntityDispatcher.tryExtractRenderState(entity, tickDelta, null);
            if (state != null) {
                blockEntityDispatcher.submit(state, standardStack, nodeStorage, cameraRenderState);
            }

            standardStack.popPose();
        });
        super.drawSubmittedRenderFeatures();
    }

    private void refreshEntities() {
        if (this.getProperties().freezeEntities.get()) {
            if (!this.entitiesFrozen) {
                this.entitiesFrozen = true;

                this.entities = this.entities
                        .stream()
                        .map(originalEntity -> {
                            Entity clonedEntity = EntityRenderable.copy(originalEntity);
                            clonedEntity.restoreFrom(originalEntity);
                            clonedEntity.copyPosition(originalEntity);
                            clonedEntity.tick();
                            return clonedEntity;
                        })
                        .collect(Collectors.toList());
            }
            return;
        }

        // not frozen selected by here
        if (getProperties().autoRefreshVisibleEntities.get() || this.entitiesFrozen) {
            ClientLevel level = Minecraft.getInstance().level;
            assert level != null;
            BlockPos start = mesh.startPos();
            BlockPos end = mesh.endPos();
            this.entities = level.getEntities((Entity) null, AABB.encapsulatingFullBlocks(start.offset(-5, -5, -5), end.offset(5, 5, 5)), e -> {
                AABB entityBoundingBox = e.getBoundingBox();

                // kinda jank, might redo this
                double halfX = entityBoundingBox.getXsize() / 2D;
                double halfY = entityBoundingBox.getYsize() / 2D;
                double halfZ = entityBoundingBox.getZsize() / 2D;
                AABB expandedBlockBoundingBox = new AABB(start.getX() - halfX, start.getY() - halfY, start.getZ() - halfZ, end.getX() + halfX, end.getY() + halfY, end.getZ() + halfZ);

                return entityBoundingBox.intersects(expandedBlockBoundingBox);
            });
        }

        this.entities.removeIf(Entity::isRemoved);
        this.entitiesFrozen = false;
    }

    private void drawEntities(CameraRenderState cameraRenderState, float tickDelta, PoseStack standardStack, SubmitNodeStorage nodeStorage) {
        AreaPropertyBundle properties = this.getProperties();
        EntityRenderDispatcher entityDispatcher = client.getEntityRenderDispatcher();
        float effectiveDelta = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        this.refreshEntities();
        if (!properties.hideEntities.get()) {
            this.entities.forEach(entity -> {
                if (entity instanceof Player && properties.hidePlayers.get()) return;
                if (entity instanceof ArmorStand && properties.hideArmorStands.get()) return;

                Vec3 offsetFromMesh = entity.getPosition(effectiveDelta).subtract(mesh.startPos().getX(), mesh.startPos().getY(), mesh.startPos().getZ());

                EntityRenderState state = entityDispatcher.extractEntity(entity, tickDelta);
                state.lightCoords = client.getEntityRenderDispatcher().getPackedLightCoords(entity, 0); // entry.light();
                state.outlineColor = 0; // remove glow

                if (this.entitiesFrozen && (state instanceof AvatarRenderState avatarRenderState)) {
                    // fix weird cape behavior with frozen models - there might be a better way to do this but ehh this is fine for now
                    avatarRenderState.capeFlap = 0;
                    avatarRenderState.capeLean = 0;
                    avatarRenderState.capeLean2 = 0;
                }

                if (properties.hideText.get()) {
                    state.nameTag = null;
                    state.nameTagAttachment = null;
                }

                if (properties.overrideRotations.get()) {
                    if (state instanceof LivingEntityRenderState livingEntityRenderState) {
                        livingEntityRenderState.bodyRot = (properties.entityRotation.get() + 180); // 180 makes it face the camera by default in the isometric preset (i think)
                        livingEntityRenderState.xRot = properties.pitch.get();
                        livingEntityRenderState.yRot = properties.yaw.get();
                    }
                }

                // todo: de-dupe
                if (state instanceof AvatarRenderState avatarRenderState) {
                    avatarRenderState.capeFlap = 0;
                    avatarRenderState.capeLean = 0;
                    avatarRenderState.capeLean2 = 0;

                    if (properties.useSteveSkin.get()) {
                        avatarRenderState.skin = DefaultPlayerSkin.getDefaultSkin();
                    }
                    if (properties.forceSmallArms.get()) {
                        avatarRenderState.skin = avatarRenderState.skin.with(PlayerSkin.Patch.create(
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(PlayerModelType.SLIM))
                        );
                    }
                }

                if (state instanceof ArmedEntityRenderState armedEntityRenderState) {
                    if (properties.hideHeldItems.get()) {
                        armedEntityRenderState.leftHandItemStack = ItemStack.EMPTY;
                        armedEntityRenderState.rightHandItemStack = ItemStack.EMPTY;
                        armedEntityRenderState.leftHandItemState.clear();
                        armedEntityRenderState.rightHandItemState.clear();
                        armedEntityRenderState.leftArmPose = HumanoidModel.ArmPose.EMPTY;
                        armedEntityRenderState.rightArmPose = HumanoidModel.ArmPose.EMPTY;
                    } else if (properties.hideEnchantments.get()) {
                        for (ItemStackRenderState.LayerRenderState layer : ((ItemStackRenderStateAccessor) armedEntityRenderState.leftHandItemState).isometric$getLayers()) {
                            layer.setFoilType(ItemStackRenderState.FoilType.NONE);
                        }
                        for (ItemStackRenderState.LayerRenderState layer : ((ItemStackRenderStateAccessor) armedEntityRenderState.rightHandItemState).isometric$getLayers()) {
                            layer.setFoilType(ItemStackRenderState.FoilType.NONE);
                        }
                    }
                }

                if (state instanceof HumanoidRenderState humanoidRenderState) {
                    if (properties.hideArmor.get()) {
                        humanoidRenderState.headItem.clear();
                        humanoidRenderState.wornHeadType = null;
                        humanoidRenderState.headEquipment = ItemStack.EMPTY;
                        humanoidRenderState.chestEquipment = ItemStack.EMPTY;
                        humanoidRenderState.legsEquipment = ItemStack.EMPTY;
                        humanoidRenderState.feetEquipment = ItemStack.EMPTY;
                    } else if (properties.hideEnchantments.get()) {
                        humanoidRenderState.headEquipment.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
                        humanoidRenderState.chestEquipment.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
                        humanoidRenderState.legsEquipment.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
                        humanoidRenderState.feetEquipment.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
                    }
                }

                if (properties.invisible.get()) {
                    state.isInvisible = true;
                    if (state instanceof LivingEntityRenderState livingEntityRenderState) {
                        livingEntityRenderState.isInvisibleToPlayer = true;
                    }
                }

                if (!state.shadowPieces.isEmpty()) {
                    // increase shadow height by a tiny amount to fix z-fighting, +0.001 is enough
                    List<EntityRenderState.ShadowPiece> newPieces = state.shadowPieces
                            .stream()
                            .map(piece -> new EntityRenderState.ShadowPiece(piece.relativeX(), piece.relativeY() + 0.001f, piece.relativeZ(), piece.shapeBelow(), piece.alpha()))
                            .toList();
                    state.shadowPieces.clear();
                    state.shadowPieces.addAll(newPieces);
                }
                entityDispatcher.submit(state, cameraRenderState, offsetFromMesh.x, offsetFromMesh.y, offsetFromMesh.z, standardStack, nodeStorage);
            });
        }
        super.drawSubmittedRenderFeatures();
    }

    @Override
    public AreaPropertyBundle getProperties() {
        return AreaPropertyBundle.INSTANCE;
    }

    @Override
    public ParticleRestriction<?> getParticleRestriction() {
        AABB dimensions = this.mesh.dimensions();
        return ParticleRestriction.inArea(new AABB(
                dimensions.minX,
                dimensions.minY,
                dimensions.minZ,
                dimensions.maxX,
                dimensions.maxY,
                dimensions.maxZ
        ));
    }

    @Override
    public ExportPathSpec getExportPath() {
        return ExportPathSpec.of("area_renders", "area_render");
    }

    @Override
    public void dispose() {
        super.dispose();
        mesh.subMeshes.forEach(MeshSection::close);
        mesh.subMeshes.clear();
    }
}
