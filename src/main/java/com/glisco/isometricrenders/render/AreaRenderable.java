package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.property.GlobalProperties;
import com.glisco.isometricrenders.property.IntProperty;
import com.glisco.isometricrenders.property.Property;
import com.glisco.isometricrenders.render.area.MiniChunk;
import com.glisco.isometricrenders.render.area.MiniChunkScanner;
import com.glisco.isometricrenders.render.area.WorldMesh;
import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.util.ExportPathSpec;
import com.glisco.isometricrenders.util.ParticleRestriction;
import com.glisco.isometricrenders.util.Translate;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public class AreaRenderable extends DefaultRenderable<AreaRenderable.AreaPropertyBundle> {

    private final Minecraft client = Minecraft.getInstance();

    private final WorldMesh mesh;
    private final int ySize;
    private final int xSize;
    private final int zSize;

    public AreaRenderable(WorldMesh mesh) {
        this.mesh = mesh;

        final var dimensions = mesh.dimensions();
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
    public void emitVertices(PoseStack matrices, MultiBufferSource vertexConsumers, float tickDelta) {
        if (!mesh.canRender()) {
            if (mesh.state() == WorldMesh.MeshState.CORRUPT) return;

            mesh.scheduleRebuild();
            return;
        }

        matrices.setIdentity();
        matrices.translate(-xSize / 2f, -ySize / 2f, -zSize / 2f);

        final var commandQueue = client.gameRenderer.getSubmitNodeStorage();
        final var cameraRenderState = new CameraRenderState();
        final var blockEntities = mesh.renderInfo().blockEntities();
        final var blockEntityDispatcher = client.getBlockEntityRenderDispatcher();
        blockEntities.forEach((blockPos, entity) -> {
            matrices.pushPose();
            matrices.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            var state = blockEntityDispatcher.tryExtractRenderState(entity, tickDelta, null);
            if (state != null) blockEntityDispatcher.submit(state, matrices, commandQueue, cameraRenderState);
            matrices.popPose();
        });

        // todo: this is called here but also in the other draw call, but emitVerticies is called before and then this draw is called, which also calls super, so draws is called twice?
        super.draw(RenderSystem.getModelViewMatrix());
        // todo 2: this impacts entity positions, somehow

        final var effectiveDelta = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        final var entities = mesh.renderInfo().entities();
        final var entityDispatcher = client.getEntityRenderDispatcher();

        if (!properties().hideEntities.get()) {
            entities.forEach((entityPos, entry) -> {
                if (!mesh.entitiesFrozen()) {
                    entityPos = entry.entity().getPosition(effectiveDelta).subtract(mesh.startPos().getX(), mesh.startPos().getY(), mesh.startPos().getZ());
                }
                var state = entityDispatcher.extractEntity(entry.entity(), tickDelta);
                state.lightCoords = entry.light();

                if (mesh.entitiesFrozen() && (state instanceof AvatarRenderState avatarRenderState)) {
                    // fix weird cape behavior with frozen models - there might be a better way to do this but ehh this is fine for now
                    avatarRenderState.capeFlap = 0;
                    avatarRenderState.capeLean = 0;
                    avatarRenderState.capeLean2 = 0;
                }

                // fix nametag rotations to look at the camera (i think this looks better)
                CameraRenderState newState = new CameraRenderState();
                if (state.nameTagAttachment != null) {
                    newState.orientation.rotationX((float) -Math.toRadians(this.properties().slant.get()));
                    newState.orientation.rotationY((float) -Math.toRadians(this.properties().rotation.get() + this.properties().rotationOffset));
                    if (this.properties().rotationSpeed.get() != 0) {
                        // newState.orientation.rotationY((float) -Math.toRadians(this.properties().rotationOffset));
                    }
                }

                // +0.01 fixes z-fighting
                entityDispatcher.submit(state, newState, entityPos.x, entityPos.y + 0.01, entityPos.z, matrices, commandQueue);
                super.draw(RenderSystem.getModelViewMatrix());
            });
        }


        var diff = Vec3.atLowerCornerOf(mesh.startPos()).subtract(client.player.trackingPosition());
        matrices.translate(-diff.x, -diff.y + 1.65, -diff.z);

        this.renderParticles(matrices.last().pose(), tickDelta);
    }

    @Override
    public void draw(Matrix4f modelViewMatrix) {
        if (!mesh.canRender()) return;

        // 1. Save the "Real" Globals if necessary, or just override them
        // You need to call the method we found in your GlobalSettingsUniform class
        var globalSettings = Minecraft.getInstance().gameRenderer.getGlobalSettingsUniform();

        // We "Zero Out" the camera so the shader math:
        // (Position + ChunkPos - CameraPos) becomes (Position + 0 - 0)
        globalSettings.update(
                client.getWindow().getGuiScaledWidth(),
                client.getWindow().getGuiScaledHeight(),
                1.0, 0, client.getDeltaTracker(), 0,
                new net.minecraft.client.Camera(), // Passing a new/empty camera sets pos to 0,0,0
                false
        );

        final var meshStack = new PoseStack();
        meshStack.mulPose(modelViewMatrix);
        meshStack.translate(-xSize / 2f, -ySize / 2f, -zSize / 2f);
        this.mesh.render(meshStack);
    }

    @Override
    public AreaPropertyBundle properties() {
        return AreaPropertyBundle.INSTANCE;
    }

    @Override
    public ParticleRestriction<?> particleRestriction() {
        final var dimensions = this.mesh.dimensions();
        return ParticleRestriction.inArea(new AABB(
                dimensions.minX,
                dimensions.minY,
                dimensions.minZ,
                dimensions.maxX + 1,
                dimensions.maxY + 1,
                dimensions.maxZ + 1
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
        public final Property<Boolean> perPixel90DegreeRendering = Property.of(false);

        public final IntProperty alternativeRotation = IntProperty.of(0, 0, 360).withRollover();
        public final IntProperty alternativeSlant = IntProperty.of(90, -90, 90);

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
            IsometricUI.sectionHeader(container, "mesh_controls", true);

            IsometricUI.dynamicLabel(container, () -> {
                var meshStatus = Translate.gui("mesh_status");
                if (!mesh.state().isBuildStage) {
                    meshStatus.append(Translate.gui("mesh_ready").withStyle(ChatFormatting.GREEN));
                } else {
                    meshStatus.append(Translate.gui(
                            switch (mesh.state()) {
                                case BUILDING -> "mesh_building";
                                case CORRUPT -> "mesh_corrupt";
                                default -> "mesh_rebuilding";
                            },
                            (int) (mesh.buildProgress() * 100)
                    ).withStyle(ChatFormatting.RED));
                }
                return meshStatus;
            });

            IsometricUI.booleanControl(container, this.hideEntities, "hide_entities");
            // todo: probably not needed since emitVerticies checks for hidden entities
            this.hideEntities.listen((booleanProperty, hidden) -> mesh.setHideEntities(hidden));

            IsometricUI.booleanControl(container, this.freezeEntities, "freeze_entities");
            this.freezeEntities.listen((booleanProperty, frozen) -> mesh.setFreezeEntities(frozen));

            container.child(Components.button(Translate.gui("rebuild_mesh"), (ButtonComponent button) -> mesh.scheduleRebuild())
                    .horizontalSizing(Sizing.fixed(80))
                    .margins(Insets.top(5)));
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
}
