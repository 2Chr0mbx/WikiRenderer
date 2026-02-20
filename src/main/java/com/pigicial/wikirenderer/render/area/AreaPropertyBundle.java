package com.pigicial.wikirenderer.render.area;

import com.mojang.math.Axis;
import com.pigicial.wikirenderer.property.DefaultCroppablePropertyBundle;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.property.IntProperty;
import com.pigicial.wikirenderer.property.Property;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.render.area.bounds.ExpandableMeshBounds;
import com.pigicial.wikirenderer.render.area.side_view.ExpansionSide;
import com.pigicial.wikirenderer.render.area.side_view.MeshSideRotation;
import com.pigicial.wikirenderer.render.area.side_view.MeshSideSlant;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.screen.WikiRendererUI;
import com.pigicial.wikirenderer.util.Translate;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import org.joml.Matrix4fStack;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

import static com.pigicial.wikirenderer.property.GlobalProperties.UNSAFE;

public class AreaPropertyBundle extends DefaultCroppablePropertyBundle {

    public static final AreaPropertyBundle INSTANCE = new AreaPropertyBundle();

    public final Property<Boolean> emulateDaylight = Property.of(true);
    public final Property<Boolean> useFullBrightGamma = Property.of(false);
    public final Property<Boolean> useNightVision = Property.of(false);
    public final Property<Boolean> hideBeaconBeams = Property.of(false);

    public final IntProperty entityBoundsIntersectionRequirement = IntProperty.of(20, 0, 100);
    public final Property<Boolean> hideEntities = Property.of(false);
    public final Property<Boolean> hidePlayers = Property.of(false);
    public final Property<Boolean> hideArmorStands = Property.of(false);
    public final Property<Boolean> hideLivingEntities = Property.of(false);
    public final Property<Boolean> hideText = Property.of(false);
    public final Property<Boolean> freezeEntities = Property.of(false);
    public final Property<Boolean> freezePlayerArms = Property.of(false);
    public final Property<Boolean> autoRefreshVisibleEntities = Property.of(true);

    public final Property<Boolean> perPixel90DegreeRendering = Property.of(false);
    public MeshSideRotation sideViewRotation = MeshSideRotation.NORTH;
    public MeshSideSlant sideViewSlant = MeshSideSlant.ABOVE;

    public final Property<Boolean> exportSideViewMinimapData = Property.of(true);
    public final Property<Boolean> halfPixelOffsetFor4x4 = Property.of(true); // helps fix certain things like fence lines not rendering
    private int pixelsPerBlockResolution = 16;
    private int faceRenderingActualResolution = this.getDefaultExportResolution();

    public final Property<Boolean> useWalkabilityFilter = Property.of(false);
    public final IntProperty dontSearchForHigherFloorsThreshold = IntProperty.of(255, 0, 255);
    public final IntProperty walkableBlocksThreshold = IntProperty.of(2, 1, 20);
    public final Property<Boolean> requireCeilingForCaveMode = Property.of(false);
    public final Property<Boolean> includeWallsForCaveMode = Property.of(false);
    public final Property<Boolean> showMeshExpansionControls = Property.of(false);

    public final Property<Boolean> hideMesh = Property.of(false);
    public final Property<Boolean> hideFluids = Property.of(false);
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

    public boolean areMinimapSettingsExportable() {
        return sideViewRotation == MeshSideRotation.NORTH && sideViewSlant == MeshSideSlant.ABOVE;
    }

    @Override
    protected double getDefaultSlant() {
        return 35.264;
    }

    @Override
    protected boolean allowRotatingWithMouseByDefault() {
        return true;
    }

    @Override
    public void setExportResolution(Renderable<?> renderable, int exportResolution) {
        if (perPixel90DegreeRendering.get()) {
            this.faceRenderingActualResolution = exportResolution;
        } else {
            super.setExportResolution(renderable, exportResolution);
        }
    }

    @Override
    public int getExportResolution(Renderable<?> renderable) {
        if (this.perPixel90DegreeRendering.get()) {
            return this.faceRenderingActualResolution;
        } else {
            return super.getExportResolution(renderable);
        }
    }

    public int getPixelsPerBlockResolution() {
        return pixelsPerBlockResolution;
    }

    public void setPixelsPerBlockResolution(int pixelsPerBlockResolution) {
        this.pixelsPerBlockResolution = pixelsPerBlockResolution;
    }

    @Override
    public float getUsedRotation() {
        return this.perPixel90DegreeRendering.get() ? this.sideViewRotation.getRotationDegrees() : super.getUsedRotation();
    }

    @Override
    public double getUsedSlant() {
        return this.perPixel90DegreeRendering.get() ? this.sideViewSlant.getRotationDegrees() : super.getUsedSlant();
    }

    @Override
    public void modifyRotation(int amount) {
        if (this.perPixel90DegreeRendering.get()) return;
        super.modifyRotation(amount);
    }

    @Override
    public void modifySlant(double amount) {
        if (this.perPixel90DegreeRendering.get()) return;
        super.modifySlant(amount);
    }

    @Override
    public boolean supportsAutomaticRotations() {
        return !this.perPixel90DegreeRendering.get();
    }

    @Override
    public void buildMainGUIControls(Renderable<?> r, RenderScreen screen, FlowLayout container) {
        AreaRenderable renderable = (AreaRenderable) r;
        WikiRendererUI.text(container, "transform_options", false);

        WikiRendererUI.booleanControl(container, this.perPixel90DegreeRendering, "per_pixel_90_degree_rendering");
        this.perPixel90DegreeRendering.futureListen((booleanProperty, value) -> {
            if (value) {
                this.sideViewRotation = MeshSideRotation.NORTH;
                this.sideViewSlant = MeshSideSlant.ABOVE;
            }
            screen.guiRebuildScheduled = true;
        });

        if (!this.perPixel90DegreeRendering.get()) {
            try (WikiRendererUI.RowBuilder builder = WikiRendererUI.autoNewLineRow(container)) {
                builder.row.child(UIComponents.button(Translate.gui("dimetric"), (ButtonComponent button) -> {
                    this.rotation.setToDefault();
                    this.slant.set(30D);
                }).margins(Insets.right(5)));

                builder.row.child(UIComponents.button(Translate.gui("isometric_recommended"), (ButtonComponent button) -> {
                    this.rotation.setToDefault();
                    this.slant.set(35.264);

                }));
            }
            WikiRendererUI.intControl(container, scale, "scale", 10);
            WikiRendererUI.intControl(container, rotation, "rotation", 45);
            WikiRendererUI.doubleControl(container, slant, "slant", 30);
            WikiRendererUI.intControl(container, rotationSpeed, "rotation_speed", 5);
            WikiRendererUI.booleanControl(container, allowRotatingWithMouse, "allow_rotating_with_mouse");
            container.child(this.buildResetButton());
        } else {
            try (WikiRendererUI.RowBuilder builder = WikiRendererUI.autoNewLineRow(container)) {
                builder.row.child(UIComponents.button(Translate.gui("cycle_rotation"), (ButtonComponent button) -> {
                    this.sideViewRotation = this.sideViewRotation.nextRotation();
                    screen.guiRebuildScheduled = true;
                }));

                builder.row.child(UIComponents.button(Translate.gui("cycle_slant"), (ButtonComponent button) -> {
                    this.sideViewSlant = this.sideViewSlant.nextSlant();
                    screen.guiRebuildScheduled = true;
                }));
            }
            container.child(UIComponents.button(Translate.gui("reset_rotation_and_slant"), (ButtonComponent button) -> {
                this.sideViewRotation = MeshSideRotation.NORTH;
                this.sideViewSlant = MeshSideSlant.ABOVE;
                screen.guiRebuildScheduled = true;
            }).margins(Insets.right(5)));

            WikiRendererUI.booleanControl(container, this.useWalkabilityFilter, "walkability_filter");
            this.useWalkabilityFilter.futureListen((booleanProperty, value) -> screen.guiRebuildScheduled = true);
            if (this.useWalkabilityFilter.get()) {
                WikiRendererUI.intControl(container, this.walkableBlocksThreshold, "walkable_blocks_threshold", 1);
                WikiRendererUI.intControl(container, this.dontSearchForHigherFloorsThreshold, "dont_search_for_higher_floors_threshold", 1);
                WikiRendererUI.intControl(container, renderable.minFloorYLevelForOverhead, "min_floor_y_level", 1);
                WikiRendererUI.intControl(container, renderable.maxFloorYLevelForOverhead, "max_floor_y_level", 1);
                WikiRendererUI.booleanControl(container, this.includeWallsForCaveMode, "show_walls");
                WikiRendererUI.booleanControl(container, this.requireCeilingForCaveMode, "require_ceiling");
            }

            if (renderable.mesh.bounds instanceof ExpandableMeshBounds expandableMeshBounds && sideViewSlant == MeshSideSlant.ABOVE) {
                WikiRendererUI.booleanControl(container, this.showMeshExpansionControls, "show_expansion_buttons");
                this.showMeshExpansionControls.futureListen((booleanProperty, value) -> screen.guiRebuildScheduled = true);

                if (showMeshExpansionControls.get()) {
                    for (ExpansionSide expansionSide : ExpansionSide.values()) {
                        try (WikiRendererUI.RowBuilder rowBuilder = WikiRendererUI.row(container)) {
                            rowBuilder.row.child(UIComponents.button(Translate.gui("minus_five"), button -> {
                                expandableMeshBounds.move(expansionSide, sideViewRotation, -5);
                                renderable.mesh.scheduleRebuild(true);
                            }));
                            rowBuilder.row.child(UIComponents.button(Translate.gui("minus_one"), button -> {
                                expandableMeshBounds.move(expansionSide, sideViewRotation, -1);
                                renderable.mesh.scheduleRebuild(true);
                            }));
                            rowBuilder.row.child(UIComponents.button(Translate.gui("plus_one"), button -> {
                                expandableMeshBounds.move(expansionSide, sideViewRotation, 1);
                                renderable.mesh.scheduleRebuild(true);
                            }));
                            rowBuilder.row.child(UIComponents.button(Translate.gui("plus_five"), button -> {
                                expandableMeshBounds.move(expansionSide, sideViewRotation, 5);
                                renderable.mesh.scheduleRebuild(true);
                            }));


                            rowBuilder.row.child(UIComponents.label(Translate.gui(expansionSide.name().toLowerCase())).margins(Insets.of(0, 0, 10, 10)));
                        }
                    }
                }
            }
        }

        WorldBlockMesh mesh = renderable.mesh;

        try (WikiRendererUI.RowBuilder builder = WikiRendererUI.autoNewLineRow(container)) {
            ButtonComponent buildMeshButton = (ButtonComponent) UIComponents.button(Translate.gui("rebuild_mesh"), (ButtonComponent button) -> mesh.scheduleRebuild(true)).margins(Insets.top(5));
            builder.row.child(buildMeshButton);

            ButtonComponent stopBuildingButton = (ButtonComponent) UIComponents.button(Translate.gui("stop_building"), (ButtonComponent button) -> mesh.stopBuilding()).margins(Insets.of(5, 0, 5, 0));
            stopBuildingButton.active = false;
            builder.row.child(stopBuildingButton);

            WikiRendererUI.dynamicLabel(builder.row, () -> {
                MutableComponent meshStatusText;
                if (!mesh.getMeshState().isBuildStage) {
                    meshStatusText = Translate.gui("mesh_ready").withStyle(ChatFormatting.GREEN);
                } else {
                    meshStatusText = Translate.gui(
                            switch (mesh.getMeshState()) {
                                case BUILDING -> "mesh_building";
                                case CANCELLED -> "mesh_cancelled";
                                case CORRUPT -> "mesh_corrupt";
                                default -> "mesh_rebuilding";
                            },
                            (int) (mesh.getBuildProgress() * 100)
                    ).withStyle(ChatFormatting.RED);
                }

                buildMeshButton.active = mesh.canRebuild();
                stopBuildingButton.active = mesh.getMeshState() == WorldBlockMesh.MeshState.BUILDING || mesh.getMeshState() == WorldBlockMesh.MeshState.REBUILDING;

                return meshStatusText;
            }).margins(Insets.of(10, 0, 10, 0));
        }

        container.child(UIComponents.button(Translate.gui("copy_render_command"), button -> {
            screen.notify(Translate.gui("copied_coordinates_command_to_clipboard"));

            BlockPos minCorner = mesh.bounds.getMinCorner();
            BlockPos maxCorner = mesh.bounds.getMaxCorner();
            String command = "/wikirender area pos " + minCorner.getX() + " " + minCorner.getY() + " " + minCorner.getZ() + " " + maxCorner.getX() + " " + maxCorner.getY() + " " + maxCorner.getZ();

            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(command), (clipboard, contents) -> {});
        }));

        WikiRendererUI.text(container, "block_visibility", true);
        WikiRendererUI.booleanControl(container, this.hideMesh, "hide_blocks");
        this.hideMesh.futureListen((booleanProperty, hidden) -> screen.guiRebuildScheduled = true);
        if (!this.hideMesh.get()) {
            WikiRendererUI.booleanControl(container, this.hideFluids, "hide_fluids");
        }
        WikiRendererUI.booleanControl(container, this.hideBeaconBeams, "hide_beacon_beams");

        WikiRendererUI.text(container, "entity_visibility_overrides", 10);
        WikiRendererUI.booleanControl(container, this.hideEntities, "hide_entities");
        this.hideEntities.futureListen((booleanProperty, hidden) -> screen.guiRebuildScheduled = true);
        if (!this.hideEntities.get()) {
            WikiRendererUI.booleanControl(container, this.hidePlayers, "hide_players");
            WikiRendererUI.booleanControl(container, this.hideArmorStands, "hide_armor_stands");
            WikiRendererUI.booleanControl(container, this.hideLivingEntities, "hide_living_entities");
            WikiRendererUI.intPercentageControl(container, this.entityBoundsIntersectionRequirement, "entity_collision_threshold_requirement", 1);

            WikiRendererUI.booleanControl(container, this.freezeEntities, "freeze_entities");
            this.freezeEntities.futureListen((booleanProperty, hidden) -> screen.guiRebuildScheduled = true);
            if (!this.freezeEntities.get()) {
                WikiRendererUI.booleanControl(container, this.freezePlayerArms, "freeze_player_arms");
            }

            WikiRendererUI.booleanControl(container, this.autoRefreshVisibleEntities, "auto_refresh_visible_entities");

            WikiRendererUI.booleanControl(container, this.hideText, "hide_text");
            this.hideText.futureListen((booleanProperty, hidden) -> screen.guiRebuildScheduled = true);
            if (!this.hideText.get()) {
                WikiRendererUI.booleanControl(container, GlobalProperties.HIDE_NAMETAGS, "hide_player_nametags");
            }
        }

        WikiRendererUI.text(container, "entity_overrides", 10);
        WikiRendererUI.booleanControl(container, this.overrideRotations, "mesh_entity_data.override_rotations");
        WikiRendererUI.intControl(container, yaw, "entity_data.yaw", 15);
        WikiRendererUI.intControl(container, pitch, "entity_data.pitch", 5);
        WikiRendererUI.intControl(container, entityRotation, "entity_data.rotation", 5);
        WikiRendererUI.booleanControl(container, useSteveSkin, "entity_data.steve");
        WikiRendererUI.booleanControl(container, forceSmallArms, "entity_data.small_arms");
        WikiRendererUI.booleanControl(container, hideHeldItems, "entity_data.hide_held_items");
        WikiRendererUI.booleanControl(container, hideArmor, "entity_data.hide_armor");
        WikiRendererUI.booleanControl(container, hideEnchantments, "entity_data.hide_enchantments");
        WikiRendererUI.booleanControl(container, invisible, "entity_data.invisible");
    }

    @Override
    public void buildRenderOptionGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        WikiRendererUI.booleanControl(container, this.emulateDaylight, "render_as_daytime");
        WikiRendererUI.booleanControl(container, this.useFullBrightGamma, "full_bright");
        WikiRendererUI.booleanControl(container, this.useNightVision, "night_vision");
        WikiRendererUI.booleanControl(container, GlobalProperties.TICK_PARTICLES, "particles");
    }

    @Override
    public void buildExportResolutionGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        if (!this.perPixel90DegreeRendering.get()) {
            super.buildExportResolutionGUIControls(renderable, screen, container);
        } else {
            AreaRenderable areaRenderable = (AreaRenderable) renderable;

            BlockPos cornerOne = areaRenderable.mesh.bounds.getMinCorner();
            BlockPos cornerTwo = areaRenderable.mesh.bounds.getMaxCorner();

            int totalBlocksX = cornerTwo.getX() - cornerOne.getX() + 1;
            int totalBlocksY = cornerTwo.getY() - cornerOne.getY() + 1;
            int totalBlocksZ = cornerTwo.getZ() - cornerOne.getZ() + 1;
            int highest = Math.max(totalBlocksY, Math.max(totalBlocksX, totalBlocksZ));

            EditBox resolutionField = WikiRendererUI.labelledTextField(container, String.valueOf(this.getPixelsPerBlockResolution()), "block_resolution", Sizing.fixed(50));
            resolutionField.setFilter(s -> s.matches("\\d{0,5}"));
            resolutionField.setResponder(s -> {
                if (s.isBlank()) return;
                int pixelsPerBlock = Integer.parseInt(s);

                double bufferSize = highest * pixelsPerBlock;

                if ((pixelsPerBlock < 1 || pixelsPerBlock > 256 || bufferSize > 16384) && !UNSAFE.get()) {
                    screen.exportButton.active = false;
                } else {
                    if ((this.getPixelsPerBlockResolution() != 4 && pixelsPerBlock == 4) || (pixelsPerBlock != 4 && this.getPixelsPerBlockResolution() == 4)) {
                        screen.guiRebuildScheduled = true;
                    }
                    this.setPixelsPerBlockResolution(pixelsPerBlock);
                    screen.exportButton.active = true;
                }
            });

            boolean allowMinimapExporting = this.areMinimapSettingsExportable();
            if (allowMinimapExporting) {
                WikiRendererUI.booleanControl(container, this.exportSideViewMinimapData, "export_minimap_data");
            } else {
                WikiRendererUI.text(container, "minimap_disabled_notice_1", false);
                WikiRendererUI.text(container, "minimap_disabled_notice_2", false);
            }

            if (this.getPixelsPerBlockResolution() == 4) {
                WikiRendererUI.booleanControl(container, this.halfPixelOffsetFor4x4, "half_pixel_offset_for_4x4");
                WikiRendererUI.text(container, "half_pixel_offset_for_4x4_note_1", true);
                WikiRendererUI.text(container, "half_pixel_offset_for_4x4_note_2", false);
                WikiRendererUI.text(container, "half_pixel_offset_for_4x4_note_3", false);
            }
        }
    }

    @Override
    public boolean allowForRescaling() {
        return !perPixel90DegreeRendering.get();
    }

    @Override
    public void applyToViewMatrix(Renderable<?> r, Matrix4fStack modelViewStack) {
        AreaRenderable renderable = (AreaRenderable) r;
        AreaPropertyBundle properties = renderable.getProperties();

        if (properties.perPixel90DegreeRendering.get()) {
            WorldBlockMesh mesh = renderable.mesh;
            BlockPos cornerOne = mesh.bounds.getMinCorner();
            BlockPos cornerTwo = mesh.bounds.getMaxCorner();

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
            double pixelsPerBlock = this.getPixelsPerBlockResolution();
            double bufferSize = highest * pixelsPerBlock;
            this.setExportResolution(renderable, (int) bufferSize);
            double orthoWidth = 2.0; // bcause ortho is -1 to 1

            float pixelPerfectScale = (float) (pixelsPerBlock / (bufferSize / orthoWidth));

            modelViewStack.scale(pixelPerfectScale, pixelPerfectScale, pixelPerfectScale);
            modelViewStack.rotate(Axis.XP.rotationDegrees(this.sideViewSlant.getRotationDegrees()));
            modelViewStack.rotate(Axis.YP.rotationDegrees(this.sideViewRotation.getRotationDegrees()));

            if (pixelsPerBlock == 4 && this.halfPixelOffsetFor4x4.get()) {
                float halfPixelWorld = 0.5f / (float) pixelsPerBlock;
                modelViewStack.translate(halfPixelWorld, 0, halfPixelWorld);
            }
        } else {
            float scale = this.scale.get() / 1000f;
            modelViewStack.scale(scale, scale, scale);

            // offsets arent needed for side rendering because they're already perfectly aligned
            modelViewStack.translate(this.xOffset.get() / 2600f, this.yOffset.get() / -2600f, 0);

            modelViewStack.rotate(Axis.XP.rotationDegrees(this.slant.get().floatValue()));
            modelViewStack.rotate(Axis.YP.rotationDegrees(this.rotation.get() + this.updateAndGetSpinningRotationOffset()));
        }
    }
}
