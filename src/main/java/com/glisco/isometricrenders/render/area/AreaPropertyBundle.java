package com.glisco.isometricrenders.render.area;

import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.property.GlobalProperties;
import com.glisco.isometricrenders.property.IntProperty;
import com.glisco.isometricrenders.property.Property;
import com.glisco.isometricrenders.render.Renderable;
import com.glisco.isometricrenders.render.area.side_view.MeshSideRotation;
import com.glisco.isometricrenders.render.area.side_view.MeshSideSlant;
import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.util.Translate;
import com.mojang.math.Axis;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import org.joml.Matrix4fStack;

public class AreaPropertyBundle extends DefaultPropertyBundle {

    public static final AreaPropertyBundle INSTANCE = new AreaPropertyBundle();

    public final Property<Boolean> hideEntities = Property.of(false);
    public final Property<Boolean> freezeEntities = Property.of(false);
    public final Property<Boolean> autoRefreshVisibleEntities = Property.of(true);
    public final Property<Boolean> hideText = Property.of(false);

    public final Property<Boolean> perPixel90DegreeRendering = Property.of(false);
    public MeshSideRotation sideViewRotation = MeshSideRotation.NORTH;
    public MeshSideSlant sideViewSlant = MeshSideSlant.ABOVE;

    public final Property<Boolean> useWalkabilityFilter = Property.of(false);
    public final IntProperty walkableBlocksThreshold = IntProperty.of(2, 1, 20);
    public final Property<Boolean> requireCeilingForCaveMode = Property.of(false);

    public final Property<Boolean> hideMesh = Property.of(false);
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
    public void buildGuiControls(Renderable<?> r, RenderScreen screen, FlowLayout container) {
        AreaRenderable renderable = (AreaRenderable) r;
        IsometricUI.sectionHeader(container, "transform_options", false);

        IsometricUI.booleanControl(container, this.perPixel90DegreeRendering, "per_pixel_90_degree_rendering");
        this.perPixel90DegreeRendering.listen((booleanProperty, value) -> {
            if (value) {
                this.sideViewRotation = MeshSideRotation.NORTH;
                this.sideViewSlant = MeshSideSlant.ABOVE;
            }
            screen.guiRebuildScheduled = true;
        }, false);

        if (!this.perPixel90DegreeRendering.get()) {
            try (IsometricUI.RowBuilder builder = IsometricUI.row(container)) {
                builder.row.child(Components.button(Translate.gui("dimetric"), (ButtonComponent button) -> {
                    this.rotation.setToDefault();
                    this.slant.set(30D);
                }).horizontalSizing(Sizing.fixed(60)).margins(Insets.right(5)));

                builder.row.child(Components.button(Translate.gui("isometric"), (ButtonComponent button) -> {
                    this.rotation.setToDefault();
                    this.slant.set(35.264);

                }).horizontalSizing(Sizing.fixed(60)));
            }
            IsometricUI.intControl(container, scale, "scale", 10);
            IsometricUI.intControl(container, rotation, "rotation", 45);
            IsometricUI.doubleControl(container, slant, "slant", 30);
            IsometricUI.intControl(container, lightAngle, "light_angle", 15);
            IsometricUI.intControl(container, rotationSpeed, "rotation_speed", 5);

            container.child(Components.button(Translate.gui("reset_offset_and_scale"), (ButtonComponent button) -> {
                        this.xOffset.setToDefault();
                        this.yOffset.setToDefault();
                        this.scale.setToDefault();
                    })
                    .horizontalSizing(Sizing.fixed(120))
                    .margins(Insets.top(5)));
        } else {
            try (IsometricUI.RowBuilder builder = IsometricUI.row(container)) {
                builder.row.child(Components.button(Translate.gui("cycle_rotation"), (ButtonComponent button) -> {
                    this.sideViewRotation = this.sideViewRotation.nextRotation();
                    screen.guiRebuildScheduled = true;
                }).horizontalSizing(Sizing.fixed(110)).margins(Insets.right(5)));

                builder.row.child(Components.button(Translate.gui("cycle_slant"), (ButtonComponent button) -> {
                    this.sideViewSlant = this.sideViewSlant.nextSlant();
                    screen.guiRebuildScheduled = true;
                }).horizontalSizing(Sizing.fixed(110)).margins(Insets.right(5)));
            }
            container.child(Components.button(Translate.gui("reset_rotation_and_slant"), (ButtonComponent button) -> {
                this.sideViewRotation = MeshSideRotation.NORTH;
                this.sideViewSlant = MeshSideSlant.ABOVE;
                screen.guiRebuildScheduled = true;
            }).horizontalSizing(Sizing.fixed(110)).margins(Insets.right(5)));

            IsometricUI.booleanControl(container, this.useWalkabilityFilter, "walkability_filter");
            this.useWalkabilityFilter.listen((booleanProperty, value) -> screen.guiRebuildScheduled = true, false);
            if (this.useWalkabilityFilter.get()) {
                IsometricUI.intControl(container, walkableBlocksThreshold, "walkable_blocks_threshold", 1);
                IsometricUI.booleanControl(container, this.requireCeilingForCaveMode, "require_ceiling");
            }
        }

        WorldBlockMesh mesh = renderable.mesh;
        container.child(Components.button(Translate.gui("rebuild_mesh"), (ButtonComponent button) -> mesh.scheduleRebuild())
                .horizontalSizing(Sizing.fixed(80))
                .margins(Insets.top(5)));
        IsometricUI.dynamicLabel(container, () -> {
            MutableComponent meshStatusText = Translate.gui("mesh_status");
            if (!mesh.state().isBuildStage) {
                meshStatusText.append(Translate.gui("mesh_ready"));
            } else {
                meshStatusText.append(Translate.gui(
                        switch (mesh.state()) {
                            case BUILDING -> "mesh_building";
                            case CORRUPT -> "mesh_corrupt";
                            default -> "mesh_rebuilding";
                        },
                        (int) (mesh.buildProgress() * 100)
                ));
            }

            return meshStatusText;
        });

        IsometricUI.booleanControl(container, this.hideMesh, "hide_blocks");

        IsometricUI.sectionHeader(container, "mesh_entity_overrides", true);

        IsometricUI.booleanControl(container, this.hideEntities, "hide_entities");
        this.hideEntities.listen((booleanProperty, hidden) -> renderable.hideEntities = hidden);
        IsometricUI.booleanControl(container, this.freezeEntities, "freeze_entities");
        this.freezeEntities.listen((booleanProperty, frozen) -> renderable.freezeEntities = frozen);
        IsometricUI.booleanControl(container, this.autoRefreshVisibleEntities, "auto_refresh_visible_entities");

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
        AreaPropertyBundle properties = renderable.properties();

        if (properties.perPixel90DegreeRendering.get()) {
            WorldBlockMesh mesh = renderable.mesh;
            BlockPos cornerOne = mesh.startPos();
            BlockPos cornerTwo = mesh.endPos();

            Direction.Axis[] visibleAxes = switch (properties.sideViewSlant) {
                case BELOW, ABOVE -> new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z};
                case SIDE -> switch (properties.sideViewRotation) {
                    case NORTH, SOUTH -> new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Y};
                    case EAST, WEST -> new Direction.Axis[]{Direction.Axis.Z, Direction.Axis.Y};
                };
            };

            int totalBlocksA = cornerTwo.get(visibleAxes[0]) - cornerOne.get(visibleAxes[0]) + 1;
            int totalBlocksB = cornerTwo.get(visibleAxes[1]) - cornerOne.get(visibleAxes[1]) + 1;

            int highest = Math.max(totalBlocksA, totalBlocksB);

            // force pixel count per blocks without blurriness
            double pixelsPerBlock = GlobalProperties.sideViewPixelsPerBlockResolution;
            double bufferSize = highest * pixelsPerBlock;
            GlobalProperties.exportResolution = (int) bufferSize;
            double orthoWidth = 2.0; // bcause ortho is -1 to 1

            float pixelPerfectScale = (float) (pixelsPerBlock / (bufferSize / orthoWidth));

            modelViewStack.scale(pixelPerfectScale, pixelPerfectScale, pixelPerfectScale);
            modelViewStack.rotate(Axis.XP.rotationDegrees(this.sideViewSlant.getRotationDegrees()));
            modelViewStack.rotate(Axis.YP.rotationDegrees(this.sideViewRotation.getRotationDegrees()));

            if (pixelsPerBlock == 4 && GlobalProperties.halfPixelOffsetFor4x4.get()) {
                float halfPixelWorld = 0.5f / (float) pixelsPerBlock;
                modelViewStack.translate(halfPixelWorld, 0, halfPixelWorld);
            }
        } else {
            final float scale = this.scale.get() / 1000f;
            modelViewStack.scale(scale, scale, scale);

            // offsets arent needed for side rendering because they're already perfectly aligned
            modelViewStack.translate(this.xOffset.get() / 2600f, this.yOffset.get() / -2600f, 0);

            modelViewStack.rotate(Axis.XP.rotationDegrees(this.slant.get().floatValue()));
            modelViewStack.rotate(Axis.YP.rotationDegrees(this.rotation.get()));
        }

        this.updateAndApplyRotationOffset(modelViewStack);
    }

    @Override
    public float getUsedRotation() {
        if (this.perPixel90DegreeRendering.get()) {
            return this.sideViewRotation.getRotationDegrees();
        } else {
            return super.getUsedRotation();
        }
    }

    @Override
    public double getUsedSlant() {
        if (this.perPixel90DegreeRendering.get()) {
            return this.sideViewSlant.getRotationDegrees();
        } else {
            return super.getUsedSlant();
        }
    }
}
