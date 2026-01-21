package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.mixin.access.ItemStackRenderStateAccessor;
import com.glisco.isometricrenders.mixin.access.ModelPartAccessor;
import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.property.IntProperty;
import com.glisco.isometricrenders.property.Property;
import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.util.ExportPathSpec;
import com.glisco.isometricrenders.util.ParticleRestriction;
import com.glisco.isometricrenders.util.Translate;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.EntityComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class EntityRenderable extends DefaultRenderable<DefaultPropertyBundle> implements TickingRenderable<DefaultPropertyBundle> {

    private final Minecraft client = Minecraft.getInstance();
    private final Entity entity;

    public EntityRenderable(Entity entity) {
        this.entity = entity;
    }

    public static EntityRenderable of(EntityType<?> type, @Nullable CompoundTag nbt) {
        Minecraft minecraft = Minecraft.getInstance();

        if (nbt == null) {
            nbt = new CompoundTag();
        }

        nbt.putString("id", EntityType.getKey(type).toString());

        Entity entity = EntityType.loadEntityRecursive(nbt, minecraft.level, EntitySpawnReason.LOAD, EntityProcessor.NOP);
        entity.absSnapTo(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ());

        return new EntityRenderable(entity);
    }

    public static EntityRenderable copyAsRenderable(Entity source) {
        return new EntityRenderable(copyEntityAndPassengers(source));
    }

    public static Entity copy(Entity source) {
       if (source instanceof Player player) {
            return copyPlayer(player);
        }

        ProblemReporter.ScopedCollector logging = new ProblemReporter.ScopedCollector(source.problemPath(), IsometricRenders.LOGGER);
        TagValueOutput view = TagValueOutput.createWithContext(logging, source.registryAccess());
        source.saveWithoutId(view);

        CompoundTag nbt = view.buildResult();
        logging.close();
        nbt.putString("id", EntityType.getKey(source.getType()).toString());

        Entity entity = EntityType.loadEntityRecursive(nbt, Minecraft.getInstance().level, EntitySpawnReason.LOAD, EntityProcessor.NOP);
        entity.setPose(source.getPose());
        entity.getEntityData().assignValues(source.getEntityData().getNonDefaultValues());
        if (entity instanceof LivingEntity living) {
            living.hurtTime = 0;
            living.deathTime = 0;
        }

        return entity;
    }

    public static Entity copyEntityAndPassengers(Entity source) {
        source = source.getRootVehicle();

        List<Entity> entityStack = new ArrayList<>();
        applyToEntityAndPassengers(source, entity -> {
            Entity clonedEntity = copy(entity);
            if (clonedEntity.getVehicle() != null) {
                clonedEntity.getVehicle().remove(Entity.RemovalReason.DISCARDED);
            }
            clonedEntity.getPassengers().forEach(passenger -> {
                passenger.remove(Entity.RemovalReason.DISCARDED);
            });

            entityStack.add(clonedEntity);
        });

        Entity bottomEntity = entityStack.getFirst();

        Entity recentTopEntity = bottomEntity;
        for (int i = 1, entityStackSize = entityStack.size(); i < entityStackSize; i++) {
            Entity passenger = entityStack.get(i);
            passenger.startRiding(recentTopEntity);
            recentTopEntity = passenger;
        }

        applyToEntityAndPassengers(bottomEntity, Entity::tick);
        return bottomEntity;
    }

    public static EntityComponent.RenderablePlayerEntity copyPlayer(Player originalPlayer) {
        GameProfile originalProfile = originalPlayer.getGameProfile();
        GameProfile fakeProfile = new GameProfile(originalProfile.id(), originalProfile.name(), new PropertyMap(originalProfile.properties()));

        EntityComponent.RenderablePlayerEntity player = EntityComponent.createRenderablePlayer(fakeProfile);

        ProblemReporter.ScopedCollector problemReporter = new ProblemReporter.ScopedCollector(originalPlayer.problemPath(), IsometricRenders.LOGGER);
        TagValueOutput view = TagValueOutput.createWithContext(problemReporter, originalPlayer.registryAccess());
        originalPlayer.saveWithoutId(view);

        CompoundTag nbt = view.buildResult();
        problemReporter.close();
        try (ProblemReporter.ScopedCollector loggingRead = new ProblemReporter.ScopedCollector(player.problemPath(), IsometricRenders.LOGGER)) {
            player.load(TagValueInput.create(loggingRead, player.registryAccess(), nbt));
        }

        player.hurtTime = 0;
        player.deathTime = 0;
        player.getEntityData().assignValues(player.getEntityData().getNonDefaultValues());

        return player;
    }

    @Override
    public void emitVerticesThenDraw(Matrix4fStack matrix4fStack, PoseStack matrices, MultiBufferSource vertexConsumers, float tickDelta) {
        matrices.pushPose();

        double verticalOffset = -this.entity.getBbHeight() * (this.properties().spriteRendering.get() ? 1 : 0.5);
        matrices.translate(0, verticalOffset, 0); // this fits it into the default frame
        matrices.mulPose(Axis.YP.rotationDegrees(180)); // face towards camera by default

        EntityPropertyBundle properties = this.properties();
        this.entity.setYHeadRot(properties.yaw.get());
        if (entity instanceof LivingEntity living) {
            living.yHeadRotO = properties.yaw.get();
            living.yBodyRotO = properties.entityRotation.get();
            living.yBodyRot = properties.entityRotation.get();
        }
        this.entity.yRotO = properties.yaw.get();

        this.entity.setXRot(properties.pitch.get());
        this.entity.xRotO = properties.pitch.get();

        EntityRenderDispatcher renderDispatcher = client.getEntityRenderDispatcher();
        SubmitNodeStorage nodeStorage = client.gameRenderer.getSubmitNodeStorage();

        applyToEntityAndPassengers(this.entity, Entity::rideTick);

        applyToEntityAndPassengers(this.entity, entity -> {
            Vec3 offset = Vec3.ZERO;
            if (entity.isPassenger()) {
                offset = entity.position().subtract(this.entity.position());
            }

            EntityRenderState state = renderDispatcher.extractEntity(entity, tickDelta);

            state.outlineColor = 0; // remove glow
            state.shadowPieces.clear(); // remove shadows
            state.lightCoords = LightTexture.FULL_BRIGHT;

            // fix weird cape behavior with frozen models - there might be a better way to do this but ehh this is fine for now
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
                if (properties.hideArmor.get() || properties.spriteRendering.get()) {
                    if (properties.hideArmor.get()) {
                        humanoidRenderState.headItem.clear();
                        humanoidRenderState.wornHeadType = null;
                        humanoidRenderState.headEquipment = ItemStack.EMPTY;
                    }
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

            matrices.pushPose();

            List<Runnable> toggleCallbacks = new ArrayList<>();
            if (properties.spriteRendering.get()) {
                if (state instanceof AvatarRenderState avatarRenderState) {
                    avatarRenderState.isSpectator = true;
                }
                EntityRenderer<?, ?> renderer = renderDispatcher.getRenderer(entity);
                if (renderer instanceof LivingEntityRenderer<?, ?, ?> livingEntityRenderer) {
                    EntityModel<?> model = livingEntityRenderer.getModel();
                    ModelPart root = model.root();
                    this.hideNonHeadParts(toggleCallbacks, root);
                }
            }

            renderDispatcher.submit(state, new CameraRenderState(), offset.x(), offset.y(), offset.z(), matrices, nodeStorage);
            client.gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();

            matrices.popPose();
            toggleCallbacks.forEach(Runnable::run);
        });

        // if these aren't undone them the bottom of certain armor boots look weird (for some reason, and despite popPose(), idk)
        matrices.mulPose(Axis.YP.rotationDegrees(-180));
        matrices.translate(0, -verticalOffset, 0);
        matrices.popPose();

        this.drawParticles(matrices.last().pose(), tickDelta);
    }

    @Override
    public EntityPropertyBundle properties() {
        return EntityPropertyBundle.INSTANCE;
    }

    @Override
    public ParticleRestriction<?> particleRestriction() {
        return ParticleRestriction.duringTick();
    }

    @Override
    public ExportPathSpec exportPath() {
        return ExportPathSpec.ofIdentified(
                BuiltInRegistries.ENTITY_TYPE.getKey(this.entity.getType()),
                "entity"
        );
    }

    @Override
    public void tick() {
        applyToEntityAndPassengers(this.entity, entity -> {
            if (entity instanceof Player) return;
            client.level.tickNonPassenger(entity);
        });
    }

    private static void applyToEntityAndPassengers(Entity entity, Consumer<Entity> action) {
        action.accept(entity);
        if (entity.getPassengers().isEmpty()) return;
        for (Entity e : entity.getPassengers()) applyToEntityAndPassengers(e, action);
    }

    public static class EntityPropertyBundle extends DefaultPropertyBundle {

        public static final EntityPropertyBundle INSTANCE = new EntityPropertyBundle();

        public final Property<Boolean> spriteRendering = Property.of(false);

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
            IsometricUI.booleanControl(container, spriteRendering, "sprite_rendering");

            this.spriteRendering.listen(((booleanProperty, value) -> {
                screen.guiRebuildScheduled = true;
                this.yaw.set(0);
                this.pitch.set(0);
            }), false);

            IsometricUI.intControl(container, scale, "scale", 10);
            if (!spriteRendering.get()) {
                IsometricUI.intControl(container, rotation, "rotation", 45);
                IsometricUI.doubleControl(container, slant, "slant", 30);
                IsometricUI.intControl(container, lightAngle, "light_angle", 15);
                IsometricUI.intControl(container, rotationSpeed, "rotation_speed", 5);
            }

            IsometricUI.sectionHeader(container, "presets", true);
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

            container.child(Components.button(Translate.gui("reset_offset_and_scale"), (ButtonComponent button) -> {
                        this.xOffset.setToDefault();
                        this.yOffset.setToDefault();
                        this.scale.setToDefault();
                    })
                    .horizontalSizing(Sizing.fixed(140))
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

            if (!this.spriteRendering.get()) {
                modelViewStack.rotate(Axis.XP.rotationDegrees(this.slant.get().floatValue()));
                modelViewStack.rotate(Axis.YP.rotationDegrees(this.rotation.get()));
            } else {
                modelViewStack.rotate(Axis.YP.rotationDegrees(180));
            }

            this.updateAndApplyRotationOffset(modelViewStack);
        }

        @Override
        protected void updateAndApplyRotationOffset(Matrix4fStack modelViewStack) {
            if (!this.spriteRendering.get()) {
                super.updateAndApplyRotationOffset(modelViewStack);
            }
        }
    }

    private void hideNonHeadParts(List<Runnable> toggleCallbacks, ModelPart part) {
        Map<String, ModelPart> childParts = ((ModelPartAccessor) (Object) part).isometric$getChildren();
        childParts.forEach((identifier, modelPart) -> {
            if (!identifier.equals("head")) {
                boolean previouslySkippedDraw = modelPart.skipDraw;
                modelPart.skipDraw = true;
                toggleCallbacks.add(() -> modelPart.skipDraw = previouslySkippedDraw);
                hideNonHeadParts(toggleCallbacks, modelPart);
            }
        });
    }
}
