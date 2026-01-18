package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.mixin.access.ItemStackRenderStateAccessor;
import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.property.GlobalProperties;
import com.glisco.isometricrenders.property.IntProperty;
import com.glisco.isometricrenders.property.Property;
import com.glisco.isometricrenders.render.area.DynamicRenderInfo;
import com.glisco.isometricrenders.render.area.MiniChunk;
import com.glisco.isometricrenders.render.area.MiniChunkScanner;
import com.glisco.isometricrenders.render.area.WorldMesh;
import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.util.ExportPathSpec;
import com.glisco.isometricrenders.util.ParticleRestriction;
import com.glisco.isometricrenders.util.Translate;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.ChatFormatting;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fStack;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public class AreaRenderable extends DefaultRenderable<AreaRenderable.AreaPropertyBundle> {

    private final Minecraft client = Minecraft.getInstance();

    private final WorldMesh mesh;
    private final int ySize;
    private final int xSize;
    private final int zSize;

    public AreaRenderable(WorldMesh mesh) {
        this.mesh = mesh;

        AABB dimensions = mesh.dimensions();
        this.xSize = (int) dimensions.getXsize();
        this.ySize = (int) dimensions.getYsize();
        this.zSize = (int) dimensions.getZsize();
    }

    public static AreaRenderable of(BlockPos origin, BlockPos end) {
        final WorldMesh.Builder builder = new WorldMesh.Builder(Minecraft.getInstance().level, origin, end);
        if (AreaPropertyBundle.INSTANCE.freezeEntities.get()) {
            builder.freezeEntities();
        }
        return new AreaRenderable(builder.build());
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

        final WorldMesh.Builder builder = new WorldMesh.Builder(level, chunks, firstPos, secondPos);
        if (AreaPropertyBundle.INSTANCE.freezeEntities.get()) {
            builder.freezeEntities();
        }

        return new AreaRenderable(builder.build());
    }

    @Override
    public void emitVerticesThenDraw(Matrix4fStack modelViewStack, PoseStack standardStack, MultiBufferSource vertexConsumers, float tickDelta) {
        if (!mesh.canRender()) {
            if (mesh.state() == WorldMesh.MeshState.CORRUPT) return;

            mesh.scheduleRebuild();
            return;
        }

        GlobalSettingsUniform globalSettings = Minecraft.getInstance().gameRenderer.getGlobalSettingsUniform();
        globalSettings.update(
                client.getWindow().getGuiScaledWidth(),
                client.getWindow().getGuiScaledHeight(),
                1.0, 0, client.getDeltaTracker(), 0,
                new net.minecraft.client.Camera(), // Passing a new/empty camera sets pos to 0,0,0
                false
        );

        PoseStack meshStack = new PoseStack();
        meshStack.mulPose(modelViewStack);
        meshStack.translate(-xSize / 2f, -ySize / 2f, -zSize / 2f);
        this.mesh.draw(meshStack);

        standardStack.setIdentity();
        standardStack.translate(-xSize / 2f, -ySize / 2f, -zSize / 2f);

        SubmitNodeStorage nodeStorage = client.gameRenderer.getSubmitNodeStorage();

        AreaPropertyBundle properties = properties();

        CameraRenderState cameraRenderState = new CameraRenderState();
        // this makes certain things face the camera, like text, fishing bobbers, etc, see what uses the orientation field
        cameraRenderState.orientation.rotationYXZ(
                (float) Math.PI - (float) Math.toRadians(properties.rotation.get() + properties.rotationOffset),
                (float) Math.PI + (float) Math.toRadians(properties.slant.get()),
                (float) Math.PI);

        BlockEntityRenderDispatcher blockEntityDispatcher = client.getBlockEntityRenderDispatcher();
        mesh.renderInfo().blockEntities().forEach((blockPos, entity) -> {
            standardStack.pushPose();
            standardStack.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());

            BlockEntityRenderState state = blockEntityDispatcher.tryExtractRenderState(entity, tickDelta, null);
            if (state != null) {
                blockEntityDispatcher.submit(state, standardStack, nodeStorage, cameraRenderState);
            }

            standardStack.popPose();
        });
        super.drawSubmittedRenderFeatures();

        float effectiveDelta = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        Multimap<Vec3, DynamicRenderInfo.EntityEntry> entities = mesh.renderInfo().entities();
        EntityRenderDispatcher entityDispatcher = client.getEntityRenderDispatcher();

        if (!properties.hideEntities.get()) {
            entities.forEach((entityPos, entry) -> {
                if (!mesh.entitiesFrozen()) {
                    entityPos = entry.entity().getPosition(effectiveDelta).subtract(mesh.startPos().getX(), mesh.startPos().getY(), mesh.startPos().getZ());
                }
                EntityRenderState state = entityDispatcher.extractEntity(entry.entity(), tickDelta);
                state.lightCoords = entry.light();
                state.outlineColor = 0; // remove glow

                if (mesh.entitiesFrozen() && (state instanceof AvatarRenderState avatarRenderState)) {
                    // fix weird cape behavior with frozen models - there might be a better way to do this but ehh this is fine for now
                    avatarRenderState.capeFlap = 0;
                    avatarRenderState.capeLean = 0;
                    avatarRenderState.capeLean2 = 0;
                    // todo remove this?
                }

                if (properties.hideText.get()) {
                    state.nameTag = null;
                    state.nameTagAttachment = null;
                }

                if (properties.overrideRotations.get()) {
                    if (state instanceof LivingEntityRenderState livingEntityRenderState) {
                        livingEntityRenderState.bodyRot = (properties.entityRotation.get() + 180);
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

                // +0.01 fixes z-fighting
                entityDispatcher.submit(state, cameraRenderState, entityPos.x, entityPos.y + 0.01, entityPos.z, standardStack, nodeStorage);
            });
        }
        super.drawSubmittedRenderFeatures();

        Vec3 diff = Vec3.atLowerCornerOf(mesh.startPos()).subtract(client.player.trackingPosition());
        standardStack.translate(-diff.x, -diff.y + 1.65, -diff.z);
        this.drawParticles(standardStack.last().pose(), tickDelta);

        super.drawSubmittedRenderFeatures();
    }


    @Override
    public AreaPropertyBundle properties() {
        return AreaPropertyBundle.INSTANCE;
    }

    @Override
    public ParticleRestriction<?> particleRestriction() {
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
    public ExportPathSpec exportPath() {
        return ExportPathSpec.of("area_renders", "area_render");
    }

    public static class AreaPropertyBundle extends DefaultPropertyBundle {

        private static final AreaPropertyBundle INSTANCE = new AreaPropertyBundle();

        public final Property<Boolean> hideEntities = Property.of(false);
        public final Property<Boolean> freezeEntities = Property.of(false);
        public final Property<Boolean> hideText = Property.of(false);

        public final Property<Boolean> perPixel90DegreeRendering = Property.of(false);
        public final IntProperty alternativeRotation = IntProperty.of(0, 0, 360).withRollover();
        public final IntProperty alternativeSlant = IntProperty.of(90, -90, 90);

        public final Property<Boolean> overrideRotations = Property.of(false);
        public final IntProperty yaw = IntProperty.of(0, -180, 180).withRollover();
        public final IntProperty pitch = IntProperty.of(0, -90, 90).withRollover();
        public final IntProperty entityRotation = IntProperty.of(0, -90, 90).withRollover();

        public final Property<Boolean> useSteveSkin = Property.of(false);
        public final Property<Boolean> hideHeldItems = Property.of(false);
        public final Property<Boolean> hideArmor = Property.of(false);
        public final Property<Boolean> hideEnchantments = Property.of(false);
        public final Property<Boolean> invisible = Property.of(false); // idk what this is for but its a requested option
        public final Property<Boolean> forceSmallArms = Property.of(false);


        @Override
        public void buildGuiControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
            IsometricUI.sectionHeader(container, "transform_options", false);

            IsometricUI.booleanControl(container, this.perPixel90DegreeRendering, "per_pixel_90_degree_rendering");
            this.perPixel90DegreeRendering.listen((booleanProperty, value) -> {
                if (value) {
                    this.alternativeRotation.set(0);
                    this.alternativeSlant.set(90);
                }
                screen.guiRebuildScheduled = true;
            }, false);

            if (!this.perPixel90DegreeRendering.get()) {
                try (IsometricUI.RowBuilder builder = IsometricUI.row(container)) {
                    builder.row.child(Components.button(Translate.gui("dimetric"), (ButtonComponent button) -> {
                        this.rotation.setToDefault();
                        this.slant.set(30);
                    }).horizontalSizing(Sizing.fixed(60)).margins(Insets.right(5)));

                    builder.row.child(Components.button(Translate.gui("isometric"), (ButtonComponent button) -> {
                        this.rotation.setToDefault();
                        this.slant.set(36);
                    }).horizontalSizing(Sizing.fixed(60)));
                }
                IsometricUI.intControl(container, scale, "scale", 10);
                IsometricUI.intControl(container, rotation, "rotation", 45);
                IsometricUI.intControl(container, slant, "slant", 30);
                IsometricUI.intControl(container, lightAngle, "light_angle", 15);
                IsometricUI.intControl(container, rotationSpeed, "rotation_speed", 5);
            } else {
                try (IsometricUI.RowBuilder builder = IsometricUI.row(container)) {
                    builder.row.child(Components.button(Translate.gui("cycle_rotation"), (ButtonComponent button) -> {
                        this.alternativeRotation.set((this.alternativeRotation.get() + 90) % 360);
                    }).horizontalSizing(Sizing.fixed(110)).margins(Insets.right(5)));

                    builder.row.child(Components.button(Translate.gui("cycle_slant"), (ButtonComponent button) -> {
                        switch (this.alternativeSlant.get()) {
                            case -90 -> this.alternativeSlant.set(0);
                            case 0 -> this.alternativeSlant.set(90);
                            case 90 -> this.alternativeSlant.set(-90);
                        }
                    }).horizontalSizing(Sizing.fixed(110)).margins(Insets.right(5)));
                }
                container.child(Components.button(Translate.gui("reset_rotation_and_slant"), (ButtonComponent button) -> {
                    this.alternativeRotation.set(0);
                    this.alternativeSlant.set(90);
                }).horizontalSizing(Sizing.fixed(130)).margins(Insets.right(5)));
            }

            container.child(Components.button(Translate.gui("reset_offset_and_scale"), (ButtonComponent button) -> {
                        this.xOffset.setToDefault();
                        this.yOffset.setToDefault();
                        this.scale.setToDefault();
                    })
                    .horizontalSizing(Sizing.fixed(140))
                    .margins(Insets.top(5)));

            WorldMesh mesh = ((AreaRenderable) renderable).mesh;
            container.child(Components.button(Translate.gui("rebuild_mesh"), (ButtonComponent button) -> mesh.scheduleRebuild())
                    .horizontalSizing(Sizing.fixed(80))
                    .margins(Insets.top(5)));
            IsometricUI.dynamicLabel(container, () -> {
                MutableComponent meshStatusText = Translate.gui("mesh_status");
                if (!mesh.state().isBuildStage) {
                    meshStatusText.append(Translate.gui("mesh_ready").withStyle(ChatFormatting.GREEN));
                } else {
                    meshStatusText.append(Translate.gui(
                            switch (mesh.state()) {
                                case BUILDING -> "mesh_building";
                                case CORRUPT -> "mesh_corrupt";
                                default -> "mesh_rebuilding";
                            },
                            (int) (mesh.buildProgress() * 100)
                    ).withStyle(ChatFormatting.RED));
                }
                return meshStatusText;
            });

            IsometricUI.sectionHeader(container, "mesh_entity_overrides", true);

            IsometricUI.booleanControl(container, this.hideEntities, "hide_entities");
            // todo: probably not needed since emitVerticies checks for hidden entities
            this.hideEntities.listen((booleanProperty, hidden) -> mesh.setHideEntities(hidden));

            IsometricUI.booleanControl(container, this.freezeEntities, "freeze_entities");
            this.freezeEntities.listen((booleanProperty, frozen) -> mesh.setFreezeEntities(frozen));
            IsometricUI.booleanControl(container, this.hideText, "hide_text");

            IsometricUI.booleanControl(container, this.overrideRotations, "mesh_entity_data.override_rotations");
            IsometricUI.intControl(container, yaw, "entity_data.yaw", 15);
            IsometricUI.intControl(container, pitch, "entity_data.pitch", 5);
            IsometricUI.intControl(container, entityRotation, "entity_data.rotation", 5);
            IsometricUI.booleanControl(container, useSteveSkin, "entity_data.steve");
            IsometricUI.booleanControl(container, forceSmallArms, "entity_data.small_arms");
            IsometricUI.booleanControl(container, hideHeldItems, "entity_data.hide_held_items");
            IsometricUI.booleanControl(container, hideArmor, "entity_data.hide_armor");
            IsometricUI.booleanControl(container, hideEnchantments, "entity_data.hide_enchantments");
            IsometricUI.booleanControl(container, invisible, "entity_data.invisible");

        }

        @Override
        public void applyToViewMatrix(Renderable<?> r, Matrix4fStack modelViewStack) {
            AreaRenderable renderable = (AreaRenderable) r;

            if (renderable.properties().perPixel90DegreeRendering.get()) {
                WorldMesh mesh = renderable.mesh;
                BlockPos cornerOne = mesh.startPos();
                BlockPos cornerTwo = mesh.endPos();

                int totalBlocksX = cornerTwo.getX() - cornerOne.getX() + 1;
                int totalBlocksY = cornerTwo.getY() - cornerOne.getY() + 1;
                int totalBlocksZ = cornerTwo.getZ() - cornerOne.getZ() + 1;

                int highest = Math.max(totalBlocksY, Math.max(totalBlocksX, totalBlocksZ));

                // force pixel count per blocks without blurriness
                double pixelsPerBlock = GlobalProperties.sideViewPixelsPerBlockResolution;
                double bufferSize = highest * pixelsPerBlock;
                GlobalProperties.exportResolution = (int) bufferSize;
                double orthoWidth = 2.0; // Because your ortho is -1 to 1

                float pixelPerfectScale = (float) (pixelsPerBlock / (bufferSize / orthoWidth));

                modelViewStack.scale(pixelPerfectScale, pixelPerfectScale, pixelPerfectScale);
                modelViewStack.rotate(Axis.XP.rotationDegrees(this.alternativeSlant.get()));
                modelViewStack.rotate(Axis.YP.rotationDegrees(this.alternativeRotation.get()));
            } else {
                final float scale = this.scale.get() / 1000f;
                modelViewStack.scale(scale, scale, scale);

                // offsets arent needed for side rendering because they're already perfectly aligned
                modelViewStack.translate(this.xOffset.get() / 2600f, this.yOffset.get() / -2600f, 0);

                modelViewStack.rotate(Axis.XP.rotationDegrees(this.slant.get()));
                modelViewStack.rotate(Axis.YP.rotationDegrees(this.rotation.get()));
            }

            this.updateAndApplyRotationOffset(modelViewStack);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        mesh.subMeshes.forEach(map -> {
            map.forEach((layer, buffers) -> buffers.close());
            map.clear();
        });
        mesh.subMeshes.clear();
    }
}
