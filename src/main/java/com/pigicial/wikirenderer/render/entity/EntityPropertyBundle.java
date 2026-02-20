package com.pigicial.wikirenderer.render.entity;

import com.mojang.math.Axis;
import com.pigicial.wikirenderer.property.*;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.screen.WikiRendererUI;
import com.pigicial.wikirenderer.util.Translate;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fStack;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.text.DecimalFormat;

public class EntityPropertyBundle extends DefaultCroppablePropertyBundle implements TickingPropertyBundle {

    public static final EntityPropertyBundle INSTANCE = new EntityPropertyBundle();

    public final Property<Boolean> tick = Property.of(true);
    public final Property<Boolean> spriteRendering = Property.of(false);
    private final Property<Boolean> spriteCropping = Property.of(true);
    private int spriteExportResolution = 64;
    private final IntProperty spriteRotation = IntProperty.of(180, 0, 360).withRollover();
    private final IntProperty spriteSlant = IntProperty.of(0, -90, 90);

    public final Property<Boolean> useLiveEntity = Property.of(false);

    public final IntProperty yaw = IntProperty.of(0, -180, 180).withRollover();
    public final IntProperty pitch = IntProperty.of(0, -90, 90);
    public final IntProperty entityRotation = IntProperty.of(0, -180, 180).withRollover();
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
    public void setExportResolution(Renderable<?> renderable, int exportResolution) {
        if (this.spriteRendering.get()) {
            this.spriteExportResolution = exportResolution;
        } else {
            super.setExportResolution(renderable, exportResolution);
        }
    }

    @Override
    public float getUsedRotation() {
        return this.spriteRendering.get() ? this.spriteRotation.get() : super.getUsedRotation();
    }

    @Override
    public double getUsedSlant() {
        return this.spriteRendering.get() ? spriteSlant.get() : super.getUsedSlant();
    }

    @Override
    public void modifyRotation(int amount) {
        if (this.spriteRendering.get()) {
            this.spriteRotation.modify(amount);
        } else {
            super.modifyRotation(amount);
        }
    }

    @Override
    public void modifySlant(double amount) {
        if (this.spriteRendering.get()) {
            this.spriteSlant.modify(amount);
        } else {
            super.modifySlant(amount);
        }
    }

    @Override
    public boolean supportsAutomaticRotations() {
        return !this.spriteRendering.get();
    }

    @Override
    public void buildMainGUIControls(Renderable<?> r, RenderScreen screen, FlowLayout container) {
        EntityRenderable renderable = (EntityRenderable) r;

        WikiRendererUI.text(container, "transform_options", false);
        WikiRendererUI.booleanControl(container, this.spriteRendering, "sprite_rendering");

        this.spriteRendering.futureListen(((booleanProperty, value) -> {
            screen.guiRebuildScheduled = true;
            renderable.cachedVerticalOffset = null;
            this.yaw.set(0);
            this.pitch.set(0);
            this.spriteRotation.set(180);
            this.spriteSlant.set(0);
        }));

        WikiRendererUI.intControl(container, this.scale, "scale", 10);
        if (!this.spriteRendering.get()) {
            WikiRendererUI.intControl(container, this.rotation, "rotation", 45);
            WikiRendererUI.doubleControl(container, this.slant, "slant", 30);
            WikiRendererUI.intControl(container, this.rotationSpeed, "rotation_speed", 5);
        } else {
            WikiRendererUI.intControl(container, this.spriteRotation, "rotation", 45);
            WikiRendererUI.intControl(container, this.spriteSlant, "slant", 30);
        }
        WikiRendererUI.booleanControl(container, this.allowRotatingWithMouse, "allow_rotating_with_mouse");
        if (!this.spriteRendering.get()) {
            try (WikiRendererUI.RowBuilder builder = WikiRendererUI.autoNewLineRow(container)) {
                builder.row.child(UIComponents.button(Translate.gui("dimetric_recommended"), (ButtonComponent button) -> {
                    this.rotation.setToDefault();
                    this.slant.set(30D);
                }));
                builder.row.child(UIComponents.button(Translate.gui("isometric"), (ButtonComponent button) -> {
                    this.rotation.setToDefault();
                    this.slant.set(35.264);
                }));
            }
        }

        container.child(UIComponents.button(Translate.gui("reset_transformations"), (ButtonComponent button) -> {
            this.xOffset.setToDefault();
            this.yOffset.setToDefault();
            this.scale.setToDefault();
            this.rotation.setToDefault();
            this.slant.setToDefault();
            this.rotationSpeed.setToDefault();
            this.spriteRotation.setToDefault();
            this.spriteSlant.setToDefault();
        }).margins(Insets.of(5, 0, 0, 0)));

        WikiRendererUI.text(container, "entity_data", true);
        container.child(UIComponents.button(Translate.gui("copy_entity_coordinates"), b -> {
            Vec3 coords = renderable.getUsedEntity().position();

            DecimalFormat df = new DecimalFormat("0.#######");
            String text = df.format(coords.x) + " " + df.format(coords.y) + " " + df.format(coords.z);

            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), (clipboard, contents) -> {});
            screen.notify(Translate.gui("copied_entity_coordinates_to_clipboard"));
        }));

        WikiRendererUI.intControl(container, this.yaw, "entity_data.yaw", 15);
        WikiRendererUI.intControl(container, this.pitch, "entity_data.pitch", 5);
        WikiRendererUI.intControl(container, this.entityRotation, "entity_data.rotation", 5);
        if (renderable instanceof EntityRenderable entityRenderable) {
            Entity usedEntity = entityRenderable.getUsedEntity();
            if (usedEntity instanceof Player) {
                WikiRendererUI.booleanControl(container, this.useSteveSkin, "entity_data.steve");
                WikiRendererUI.booleanControl(container, this.forceSmallArms, "entity_data.small_arms");
            }
            if (usedEntity instanceof LivingEntity) {
                WikiRendererUI.booleanControl(container, this.hideHeldItems, "entity_data.hide_held_items");
                WikiRendererUI.booleanControl(container, this.hideArmor, "entity_data.hide_armor");
                WikiRendererUI.booleanControl(container, this.hideEnchantments, "entity_data.hide_enchantments");
                WikiRendererUI.booleanControl(container, this.invisible, "entity_data.invisible");
            }
        }
    }

    @Override
    public void buildRenderOptionGUIControls(Renderable<?> r, RenderScreen screen, FlowLayout container) {
        EntityRenderable renderable = (EntityRenderable) r;

        if (renderable.liveNonTickableEntity != null) {
            WikiRendererUI.booleanControl(container, this.useLiveEntity, "entity_data.use_live_entity");
            this.useLiveEntity.futureListen(((booleanProperty, value) -> {
                renderable.requireTextureReCache = true;
                if (renderable.textureCancelMarker != null) {
                    renderable.textureCancelMarker.set(true);
                }
                screen.guiRebuildScheduled = true;
                if (value) {
                    tick.set(true);
                }
                renderable.cachedVerticalOffset = null;
            }));
        }

        TickingPropertyBundle.super.buildRenderOptionGUIControls(r, screen, container);
    }

    @Override
    public void applyToViewMatrix(Renderable<?> renderable, Matrix4fStack modelViewStack) {
        float scale = this.scale.get() / 100f;
        modelViewStack.scale(scale, scale, scale);

        modelViewStack.translate(this.xOffset.get() / 26000f, this.yOffset.get() / -26000f, 0);

        if (this.spriteRendering.get()) {
            modelViewStack.rotate(Axis.XP.rotationDegrees(this.spriteSlant.get()));
            modelViewStack.rotate(Axis.YP.rotationDegrees(this.spriteRotation.get()));
        } else {
            modelViewStack.rotate(Axis.XP.rotationDegrees(this.slant.get().floatValue()));
            modelViewStack.rotate(Axis.YP.rotationDegrees(this.rotation.get() + this.updateAndGetSpinningRotationOffset()));
        }
    }

    @Override
    public Property<Boolean> getTickProperty() {
        return this.tick;
    }

    @Override
    public String getTickTranslationKey() {
        return "entity_animations";
    }
}
