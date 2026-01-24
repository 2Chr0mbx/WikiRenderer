package com.glisco.isometricrenders.render.entity;

import com.glisco.isometricrenders.property.DefaultCroppablePropertyBundle;
import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.property.IntProperty;
import com.glisco.isometricrenders.property.Property;
import com.glisco.isometricrenders.render.Renderable;
import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.util.Translate;
import com.mojang.math.Axis;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4fStack;

public class EntityPropertyBundle extends DefaultCroppablePropertyBundle {

    public static final EntityPropertyBundle INSTANCE = new EntityPropertyBundle();

    public final Property<Boolean> spriteRendering = Property.of(false);
    private final Property<Boolean> spriteCropping = Property.of(true);
    private int spriteExportResolution = 300;

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
    protected int getDefaultExportResolution() {
        return 512;
    }

    @Override
    public Property<Boolean> getCropProperty() {
        return this.spriteRendering.get() ? this.spriteCropping : super.getCropProperty();
    }

    @Override
    public int getExportResolution(Renderable<?> renderable) {
        return this.spriteRendering.get() ? this.spriteExportResolution : super.getExportResolution(renderable);
    }

    @Override
    public void setExportResolution(int exportResolution) {
        if (this.spriteRendering.get()) {
            this.spriteExportResolution = exportResolution;
        } else {
            super.setExportResolution(exportResolution);
        }
    }

    @Override
    public void buildGuiControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        IsometricUI.sectionHeader(container, "transform_options", false);
        IsometricUI.booleanControl(container, spriteRendering, "sprite_rendering");

        this.spriteRendering.listen(((booleanProperty, value) -> {
            screen.guiRebuildScheduled = true;
            this.yaw.set(0);
            this.pitch.set(0);
            this.rotation.set(180);
            this.slant.set(0D);
        }), false);

        IsometricUI.intControl(container, scale, "scale", 10);
        IsometricUI.intControl(container, rotation, "rotation", 45);
        if (!spriteRendering.get()) {
            IsometricUI.doubleControl(container, slant, "slant", 30);
            IsometricUI.intControl(container, lightAngle, "light_angle", 15);
            IsometricUI.intControl(container, rotationSpeed, "rotation_speed", 5);
        }

        IsometricUI.sectionHeader(container, "presets", true);
        try (IsometricUI.RowBuilder builder = IsometricUI.row(container)) {
            builder.row.child(Components.button(Translate.gui("dimetric"), (ButtonComponent button) -> {
                this.rotation.setToDefault();
                this.slant.set(30D);
            }).horizontalSizing(Sizing.content(5)).margins(Insets.right(5)));

            builder.row.child(Components.button(Translate.gui("isometric"), (ButtonComponent button) -> {
                this.rotation.setToDefault();
                this.slant.set(35.264);
            }));
        }

        container.child(Components.button(Translate.gui("reset_offset_and_scale"), (ButtonComponent button) -> {
                    this.xOffset.setToDefault();
                    this.yOffset.setToDefault();
                    this.scale.setToDefault();
                })
                .horizontalSizing(Sizing.content(5))
                .margins(Insets.top(5)));

        IsometricUI.sectionHeader(container, "entity_data", true);

        IsometricUI.intControl(container, yaw, "entity_data.yaw", 15);
        IsometricUI.intControl(container, pitch, "entity_data.pitch", 5);
        IsometricUI.intControl(container, entityRotation, "entity_data.rotation", 5);
        if (renderable instanceof EntityRenderable entityRenderable) {
            if (entityRenderable.entity instanceof Player) {
                IsometricUI.booleanControl(container, useSteveSkin, "entity_data.steve");
                IsometricUI.booleanControl(container, forceSmallArms, "entity_data.small_arms");
            }
            if (entityRenderable.entity instanceof LivingEntity) {
                IsometricUI.booleanControl(container, hideHeldItems, "entity_data.hide_held_items");
                IsometricUI.booleanControl(container, hideArmor, "entity_data.hide_armor");
                IsometricUI.booleanControl(container, hideEnchantments, "entity_data.hide_enchantments");
                IsometricUI.booleanControl(container, invisible, "entity_data.invisible");
            }
        }
    }

    @Override
    public void applyToViewMatrix(Renderable<?> renderable, Matrix4fStack modelViewStack) {
        final float scale = this.scale.get() / 100f;
        modelViewStack.scale(scale, scale, scale);

        modelViewStack.translate(this.xOffset.get() / 26000f, this.yOffset.get() / -26000f, 0);

        modelViewStack.rotate(Axis.XP.rotationDegrees(this.slant.get().floatValue()));
        modelViewStack.rotate(Axis.YP.rotationDegrees(this.rotation.get()));

        this.updateAndApplyRotationOffset(modelViewStack);
    }

    @Override
    protected void updateAndApplyRotationOffset(Matrix4fStack modelViewStack) {
        if (!this.spriteRendering.get()) {
            super.updateAndApplyRotationOffset(modelViewStack);
        }
    }
}
