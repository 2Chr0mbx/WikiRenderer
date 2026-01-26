package com.glisco.isometricrenders.render.entity;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.mixin.access.ItemStackRenderStateAccessor;
import com.glisco.isometricrenders.mixin.access.ModelPartAccessor;
import com.glisco.isometricrenders.render.DefaultRenderable;
import com.glisco.isometricrenders.render.TickingRenderable;
import com.glisco.isometricrenders.textures.SkinGrabber;
import com.glisco.isometricrenders.textures.TextureDataProvider;
import com.glisco.isometricrenders.util.CameraOrientationUtil;
import com.glisco.isometricrenders.util.ExportPathSpec;
import com.glisco.isometricrenders.util.ParticleRestriction;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.wispforest.owo.ui.component.EntityComponent;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;

import java.util.*;
import java.util.function.Consumer;

public class EntityRenderable extends DefaultRenderable<EntityPropertyBundle> implements TickingRenderable<EntityPropertyBundle>, TextureDataProvider {

    private final Minecraft client = Minecraft.getInstance();
    private final Entity liveNonTickableEntity;
    public final Entity clonedTickableEntity;

    public EntityRenderable(Entity liveNonTickableEntity, Entity clonedTickableEntity) {
        this.liveNonTickableEntity = liveNonTickableEntity;
        this.clonedTickableEntity = clonedTickableEntity;
    }

    public static EntityRenderable of(EntityType<?> type, @Nullable CompoundTag nbt) {
        Minecraft minecraft = Minecraft.getInstance();

        if (nbt == null) {
            nbt = new CompoundTag();
        }

        nbt.putString("id", EntityType.getKey(type).toString());

        Entity entity = EntityType.loadEntityRecursive(nbt, minecraft.level, EntitySpawnReason.LOAD, EntityProcessor.NOP);
        entity.absSnapTo(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ());

        return new EntityRenderable(entity, entity);
    }

    public static EntityRenderable copyAsRenderable(Entity source) {
        return new EntityRenderable(source, copyEntityAndPassengers(source));
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

    private boolean isUsingLiveEntity() {
        return this.getProperties().useLiveEntity.get();
    }

    protected Entity getUsedEntity() {
        if (this.isUsingLiveEntity()) {
            return this.liveNonTickableEntity;
        } else {
            return this.clonedTickableEntity;
        }
    }

    @Override
    public void emitVerticesThenDraw(Matrix4fStack matrix4fStack, PoseStack matrices, MultiBufferSource vertexConsumers, float tickDelta) {
        matrices.pushPose();

        boolean usingLiveEntity = isUsingLiveEntity();
        Entity usedEntity = this.getUsedEntity();

        double verticalOffset = -usedEntity.getBbHeight() * (this.getProperties().spriteRendering.get() ? 1 : 0.5);
        matrices.translate(0, verticalOffset, 0); // this fits it into the default frame
        matrices.mulPose(Axis.YP.rotationDegrees(180)); // face towards camera by default

        EntityRenderDispatcher renderDispatcher = client.getEntityRenderDispatcher();
        SubmitNodeStorage nodeStorage = client.gameRenderer.getSubmitNodeStorage();

        if (!usingLiveEntity) {
            applyToEntityAndPassengers(usedEntity, Entity::rideTick);
        }

        EntityPropertyBundle properties = this.getProperties();
        applyToEntityAndPassengers(usedEntity, entity -> {
            Vec3 offset = Vec3.ZERO;
            if (entity.isPassenger()) {
                offset = entity.position().subtract(usedEntity.position());
            }

            EntityRenderState state = renderDispatcher.extractEntity(entity, tickDelta);

            state.outlineColor = 0; // remove glow
            state.shadowPieces.clear(); // remove shadows
            state.lightCoords = LightTexture.FULL_BRIGHT;

            if (state instanceof LivingEntityRenderState livingState) {
                livingState.yRot = properties.yaw.get();
                livingState.xRot = properties.pitch.get();
                livingState.bodyRot = properties.entityRotation.get();
            }

            // fix weird cape behavior with frozen models - there might be a better way to do this but ehh this is fine for now
            if (state instanceof AvatarRenderState avatarRenderState) {
                if (!usingLiveEntity) {
                    avatarRenderState.capeFlap = 0;
                    avatarRenderState.capeLean = 0;
                    avatarRenderState.capeLean2 = 0;
                }

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

                if (!properties.getTickProperty().get()) {
                    avatarRenderState.ageInTicks = 1; // 1 allows for an armor offset to fix z-fighting
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

            renderDispatcher.submit(state, CameraOrientationUtil.createRenderState(this), offset.x(), offset.y(), offset.z(), matrices, nodeStorage);
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
    public EntityPropertyBundle getProperties() {
        return EntityPropertyBundle.INSTANCE;
    }

    @Override
    public ParticleRestriction<?> getParticleRestriction() {
        return ParticleRestriction.duringTick();
    }

    @Override
    public ExportPathSpec getExportPath() {
        return ExportPathSpec.ofIdentified(
                BuiltInRegistries.ENTITY_TYPE.getKey(this.getUsedEntity().getType()),
                "entity"
        );
    }

    @Override
    public void tick(boolean tick) {
        if (isUsingLiveEntity()) {
            return;
        }

        applyToEntityAndPassengers(this.getUsedEntity(), entity -> {
            if (!tick) {
                entity.tickCount = 0;
                return;
            }
            client.level.tickNonPassenger(entity);
        });
    }

    private static void applyToEntityAndPassengers(Entity entity, Consumer<Entity> action) {
        action.accept(entity);
        if (entity.getPassengers().isEmpty()) return;
        for (Entity e : entity.getPassengers()) applyToEntityAndPassengers(e, action);
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

    @Override
    public @NotNull Map<String, MinecraftTexturesPayload> getTextureData() {
        Map<String, MinecraftTexturesPayload> textureData = new LinkedHashMap<>();

        Entity usedEntity = getUsedEntity();

        if (usedEntity instanceof Player player) {
            MinecraftTexturesPayload playerTexture = SkinGrabber.getGameProfileTextureData(player.getGameProfile());
            if (playerTexture != null) {
                textureData.put("player", playerTexture);
            }
        }

        if (usedEntity instanceof LivingEntity livingEntity) {
            for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                ItemStack item = livingEntity.getItemBySlot(equipmentSlot);
                MinecraftTexturesPayload itemTextureData = SkinGrabber.getPlayerHeadTextureData(item);
                if (itemTextureData != null) {
                    textureData.put(equipmentSlot.getName(), itemTextureData);
                }
            }
        }

        return textureData;
    }
}
