package com.pigicial.wikirenderer.render.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.mixin.access.ClientMannequinAccessor;
import com.pigicial.wikirenderer.mixin.access.ElytraAnimationStateAccessor;
import com.pigicial.wikirenderer.mixin.access.ItemStackRenderStateAccessor;
import com.pigicial.wikirenderer.mixin.access.MannequinAccessor;
import com.pigicial.wikirenderer.render.CameraOrientationUtil;
import com.pigicial.wikirenderer.render.DefaultRenderable;
import com.pigicial.wikirenderer.render.ParticleRestriction;
import com.pigicial.wikirenderer.render.batch.DynamicBatchLabelProvider;
import com.pigicial.wikirenderer.render.entity.player.ProfileFetchMode;
import com.pigicial.wikirenderer.render.entity.player.RenderablePlayerEntity;
import com.pigicial.wikirenderer.render.export.ExportPathSpec;
import com.pigicial.wikirenderer.render.item.AnimationTimingsProvider;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.textures.PlayerTextureUtils;
import com.pigicial.wikirenderer.textures.TextureData;
import com.pigicial.wikirenderer.textures.TextureDataProvider;
import com.pigicial.wikirenderer.util.AnimationTimingUtil;
import com.pigicial.wikirenderer.util.EntityNBTValidityFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.Rotations;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EntityRenderable extends DefaultRenderable<EntityPropertyBundle> implements TextureDataProvider, DynamicBatchLabelProvider, AnimationTimingsProvider {

    private final Minecraft client = Minecraft.getInstance();

    @Nullable
    protected final Entity liveNonTickableEntity;
    protected final Entity clonedTickableEntity;

    private final Map<String, TextureData> textureData = new LinkedHashMap<>();
    protected boolean requireTextureReCache = true;
    protected AtomicBoolean textureCancelMarker = null;
    protected Vec3 cachedCenterOffset = null;
    protected Float cachedScaleMultiplier = null;

    public EntityRenderable(@Nullable Entity liveNonTickableEntity, Entity clonedTickableEntity) {
        this.liveNonTickableEntity = liveNonTickableEntity;
        this.clonedTickableEntity = clonedTickableEntity;
    }

    @Nullable
    public static EntityRenderable of(EntityType<?> type, @Nullable CompoundTag nbt) {
        return of(type, nbt, EntityNBTValidityFilter.NO_FILTER);
    }

    @Nullable
    public static EntityRenderable of(EntityType<?> type, @Nullable CompoundTag nbt, @Nullable EntityNBTValidityFilter nbtFilterRequirement) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return null;

        if (nbt == null) nbt = new CompoundTag();
        if (nbtFilterRequirement == null) nbtFilterRequirement = EntityNBTValidityFilter.NO_FILTER;

        nbt.putString("id", EntityType.getKey(type).toString());

        Entity entity = EntityType.loadEntityRecursive(nbt, minecraft.level, EntitySpawnReason.LOAD, EntityProcessor.NOP);
        if (entity == null) return null;

        CompoundTag savedNbt = saveNBT(entity);
        nbt.remove("id");
        if (!nbtFilterRequirement.passesFilter(nbt, savedNbt)) return null;

        entity.absSnapTo(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ());
        return new EntityRenderable(null, entity);
    }

    private static CompoundTag saveNBT(Entity entity) {
        ProblemReporter.ScopedCollector logging = new ProblemReporter.ScopedCollector(entity.problemPath(), WikiRenderer.LOGGER);
        TagValueOutput view = TagValueOutput.createWithContext(logging, entity.registryAccess());
        entity.saveWithoutId(view);

        CompoundTag savedNbt = view.buildResult();
        logging.close();

        return savedNbt;
    }

    public static EntityRenderable copyAsRenderable(Entity source) {
        return new EntityRenderable(source, copyEntityAndPassengers(source));
    }

    @Nullable
    public static Entity copy(Entity source) {
        if (source instanceof Player player) {
            return copyPlayer(player);
        }

        CompoundTag nbt = saveNBT(source);
        nbt.putString("id", EntityType.getKey(source.getType()).toString());

        Entity clonedEntity = EntityType.loadEntityRecursive(nbt, source.level(), EntitySpawnReason.LOAD, EntityProcessor.NOP);
        if (clonedEntity == null) return null;

        List<SynchedEntityData.DataValue<?>> nonDefaultValues = source.getEntityData().getNonDefaultValues();
        if (nonDefaultValues != null) {
            clonedEntity.getEntityData().assignValues(nonDefaultValues);
        }

        if (clonedEntity instanceof LivingEntity living) {
            living.hurtTime = 0;
            living.deathTime = 0;
        }
        clonedEntity.tick();


        clonedEntity.setXRot(source.getXRot());
        clonedEntity.setYRot(source.getYRot());
        if (clonedEntity instanceof LivingEntity livingClone && source instanceof LivingEntity livingSource) {
            livingClone.yHeadRot = livingSource.yHeadRot;
            livingClone.yHeadRotO = livingSource.yHeadRotO;
            livingClone.yBodyRot = livingSource.yBodyRot;
            livingClone.yBodyRotO = livingSource.yBodyRotO;
        }

        return clonedEntity;
    }

    public static Entity copyEntityAndPassengers(Entity source) {
        source = source.getRootVehicle();

        List<Entity> entityStack = new ArrayList<>();
        applyToEntityAndPassengers(source, entity -> {
            Entity entityClone = copy(entity);
            if (entityClone != null) {
                if (entityClone.getVehicle() != null) {
                    entityClone.getVehicle().remove(Entity.RemovalReason.DISCARDED);
                }
                entityClone.getPassengers().forEach(passenger -> passenger.remove(Entity.RemovalReason.DISCARDED));

                entityStack.add(entityClone);
            }
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

    public static RenderablePlayerEntity copyPlayer(Player originalPlayer) {
        GameProfile originalProfile = originalPlayer.getGameProfile();
        GameProfile fakeProfile = new GameProfile(originalProfile.id(), originalProfile.name(), new PropertyMap(originalProfile.properties()));

        RenderablePlayerEntity playerClone = new RenderablePlayerEntity(fakeProfile, ProfileFetchMode.UUID);

        ProblemReporter.ScopedCollector problemReporter = new ProblemReporter.ScopedCollector(originalPlayer.problemPath(), WikiRenderer.LOGGER);
        TagValueOutput view = TagValueOutput.createWithContext(problemReporter, originalPlayer.registryAccess());
        originalPlayer.saveWithoutId(view);

        CompoundTag nbt = view.buildResult();
        problemReporter.close();
        try (ProblemReporter.ScopedCollector loggingRead = new ProblemReporter.ScopedCollector(playerClone.problemPath(), WikiRenderer.LOGGER)) {
            playerClone.load(TagValueInput.create(loggingRead, playerClone.registryAccess(), nbt));
        }

        List<SynchedEntityData.DataValue<?>> nonDefaultValues = originalPlayer.getEntityData().getNonDefaultValues();
        if (nonDefaultValues != null) {
            playerClone.getEntityData().assignValues(nonDefaultValues);
        }

        playerClone.hurtTime = 0;
        playerClone.deathTime = 0;
        playerClone.tick();

        playerClone.setXRot(originalPlayer.getXRot());
        playerClone.setYRot(originalPlayer.getYRot());

        playerClone.yHeadRot = originalPlayer.yHeadRot;
        playerClone.yHeadRotO = originalPlayer.yHeadRotO;
        playerClone.yBodyRot = originalPlayer.yBodyRot;
        playerClone.yBodyRotO = originalPlayer.yBodyRotO;

        ElytraAnimationStateAccessor elytraData = (ElytraAnimationStateAccessor) playerClone.elytraAnimationState;
        elytraData.isometric$setRotX((float) (Math.PI / 12));
        elytraData.isometric$setRotZ((float) (-Math.PI / 12));
        playerClone.elytraAnimationState.tick();

        return playerClone;
    }

    public boolean isUsingLiveEntity() {
        return liveNonTickableEntity != null && this.getProperties().useLiveEntity.get();
    }

    public Entity getUsedEntity() {
        if (this.isUsingLiveEntity()) {
            return this.liveNonTickableEntity;
        } else {
            return this.clonedTickableEntity;
        }
    }

    @Override
    public void emitVerticesThenDraw(RenderScreen renderScreen, Matrix4fStack matrix4fStack, PoseStack matrices, float delta, long timeSinceCreationMs) {
        EntityPropertyBundle properties = this.getProperties();

        boolean usingLiveEntity = isUsingLiveEntity();
        Entity usedEntity = this.getUsedEntity();
        float tickDelta = usingLiveEntity ? delta : 0;

        EntityRenderDispatcher renderDispatcher = client.getEntityRenderDispatcher();
        SubmitNodeStorage nodeStorage = client.gameRenderer.getSubmitNodeStorage();

        matrices.pushPose();
        applyToEntityAndPassengers(usedEntity, entity -> {
            Vec3 offset = Vec3.ZERO;
            Vec3 entityPosition = entity.position();
            if (entity.isPassenger()) {
                offset = entityPosition.subtract(usedEntity.position());
            }

            if (entity instanceof ClientMannequin mannequin) {
                this.updateMannequinSkin(mannequin);
            }

            EntityRenderState state = renderDispatcher.extractEntity(entity, properties.tickEntityAnimations.get() ? tickDelta : 0);
            this.updateRenderState(state, properties, timeSinceCreationMs, usingLiveEntity);

            List<Runnable> partVisibilityCallbacks = new ArrayList<>();
            if (properties.spriteRendering.get()) {
                EntityRenderer<?, ?> renderer = renderDispatcher.getRenderer(state);
                if (renderer instanceof LivingEntityRenderer<?, ?, ?> livingEntityRenderer) {
                    EntitySpriteModelVisibilityUtil.hideNonHeadParts(livingEntityRenderer, partVisibilityCallbacks);
                }

                WikiRenderer.inSpriteEntityDraw = true;
            }
            WikiRenderer.inEntityDraw = true;

            if (cachedCenterOffset == null || cachedScaleMultiplier == null) {
                AABB regularBounds = entity.getBoundingBox();
                AABB renderedBounds = EntityRenderBoundsUtil.getBounds(state, this, 0, 0, 0);
                if (renderedBounds == null) {
                    cachedCenterOffset = new Vec3(0, 0, 0);
                    cachedScaleMultiplier = 1F;
                } else {
                    double xDifference = renderedBounds.getCenter().x - (regularBounds.getCenter().x - entityPosition.x);
                    double zDifference = renderedBounds.getCenter().z - (regularBounds.getCenter().z - entityPosition.z);
                    cachedCenterOffset = new Vec3(-xDifference, -renderedBounds.minY - renderedBounds.getYsize() / 2, -zDifference);
                    // centers to the screen

                    cachedScaleMultiplier = (float) (1f / Math.max(renderedBounds.getXsize(), Math.max(renderedBounds.getYsize(), renderedBounds.getZsize())));
                }
            }

            matrices.pushPose();
            matrices.scale(cachedScaleMultiplier, cachedScaleMultiplier, cachedScaleMultiplier);
            matrices.translate(cachedCenterOffset); // this fits it into the default frame

            renderDispatcher.submit(state, CameraOrientationUtil.createRenderState(this), offset.x(), offset.y(), offset.z(), matrices, nodeStorage);
            client.gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();

            matrices.popPose();
            WikiRenderer.inSpriteEntityDraw = false;
            WikiRenderer.inEntityDraw = false;
            partVisibilityCallbacks.forEach(Runnable::run);
        });

        // if these aren't undone them the bottom of certain armor boots look weird (for some reason, and despite popPose(), idk)

        matrices.popPose();
    }

    private void updateRenderState(EntityRenderState state, EntityPropertyBundle properties, long timeSinceCreationMs, boolean usingLiveEntity) {
        //state.outlineColor = 0; // remove glow
        state.shadowPieces.clear(); // remove shadows
        state.lightCoords = LightTexture.FULL_BRIGHT;
        state.nameTag = null;
        state.nameTagAttachment = null;

        if (properties.tickEntityAnimations.get()) {
            if (!usingLiveEntity) {
                state.ageInTicks = timeSinceCreationMs / 50f;
            } // otherwise just use the live entity state for more accuracy of what's being seen in the actual game
        } else {
            state.ageInTicks = 1;
        }

        if (state instanceof LivingEntityRenderState livingState) {
            if (properties.overrideHeadRotations.get()) {
                livingState.yRot = properties.yaw.get();
                livingState.xRot = properties.pitch.get();

                if (state instanceof ArmorStandRenderState armorStandRenderState) {
                    armorStandRenderState.headPose = new Rotations(properties.pitch.get(), properties.yaw.get(), 0);
                }
            }

            // +180 is to rotate the camera at 135 degrees
            if (properties.overrideBodyRotations.get()) {
                livingState.bodyRot = properties.entityRotation.get() + 180;
            } else {
                livingState.bodyRot += 180; // rotate camera at 135 degrees
            }
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
                for (ItemStackRenderState.LayerRenderState layer : ((ItemStackRenderStateAccessor) armedEntityRenderState.leftHandItemState).wikirenderer$getLayers()) {
                    layer.setFoilType(ItemStackRenderState.FoilType.NONE);
                }
                for (ItemStackRenderState.LayerRenderState layer : ((ItemStackRenderStateAccessor) armedEntityRenderState.rightHandItemState).wikirenderer$getLayers()) {
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
    public void setupLighting() {
        if (this.getProperties().spriteRendering.get()) {
            float rotation = (float) Math.toRadians(getProperties().getUsedRotation());

            // forces the face to be bright
            Vector3f faceLight = new Vector3f(0, 0, -1).rotateY((float) (Math.PI - rotation)).normalize();
            this.setupLighting(faceLight, faceLight);
        } else {
            super.setupLighting();
        }
    }

    private static void applyToEntityAndPassengers(Entity entity, Consumer<Entity> action) {
        action.accept(entity);
        if (entity.getPassengers().isEmpty()) {
            return;
        }

        for (Entity e : entity.getPassengers()) {
            applyToEntityAndPassengers(e, action);
        }
    }

    protected boolean hasEntityType(Class<? extends Entity> entityTypeClass) {
        AtomicBoolean found = new AtomicBoolean(false);

        applyToEntityAndPassengers(getUsedEntity(), e -> {
            if (entityTypeClass.isAssignableFrom(e.getClass())) {
                found.set(true);
            }
        });

        return found.get();
    }

    protected <T extends Entity> boolean hasEntityProperty(Class<T> entityTypeClass, Predicate<T> predicate) {
        AtomicBoolean found = new AtomicBoolean(false);

        applyToEntityAndPassengers(getUsedEntity(), e -> {
            if (entityTypeClass.isAssignableFrom(e.getClass()) && predicate.test((T) e)) {
                found.set(true);
            }
        });

        return found.get();
    }

    // alternative to the tick method, which has a bunch of other stuff done i dont want
    private void updateMannequinSkin(ClientMannequin clientMannequin) {
        ClientMannequinAccessor mannequin = (ClientMannequinAccessor) clientMannequin;

        CompletableFuture<Optional<PlayerSkin>> skinLookup = mannequin.getSkinLookup();
        if (skinLookup != null && skinLookup.isDone()) {
            try {
                skinLookup.get().ifPresent(mannequin::setSkin);
                mannequin.setSkinLookup(null);
            } catch (Exception ignored) {

            }
        }
    }

    @Override
    public void cacheTextureData(Runnable rebuildCallback) {
        if (!this.requireTextureReCache) return;
        this.requireTextureReCache = false;
        this.textureData.clear();

        if (this.textureCancelMarker != null) {
            this.textureCancelMarker.set(true);
        }

        AtomicBoolean cancelMarker = new AtomicBoolean(false);
        this.textureCancelMarker = cancelMarker;

        // i hate how there are like 5 billion ways skins are handled but whatever, there's probably a better way to do this but that's a later project
        applyToEntityAndPassengers(getUsedEntity(), usedEntity -> {
            switch (usedEntity) {
                case RenderablePlayerEntity player -> player.getSkinGrabber().whenComplete((data, throwable) -> {
                    if (throwable != null || cancelMarker.get()) return;
                    textureData.put("player", data);
                    rebuildCallback.run();
                });
                case AbstractClientPlayer player -> {
                    ClientPacketListener connection = Minecraft.getInstance().getConnection();
                    if (connection == null) return;

                    PlayerInfo playerInfo = connection.getPlayerInfo(player.getUUID());
                    GameProfile profile = playerInfo != null ? playerInfo.getProfile() : player.getGameProfile();

                    TextureData playerTexture = PlayerTextureUtils.getTextureDataFromGameProfile(profile);
                    if (playerTexture != null) {
                        textureData.put("player", playerTexture);
                    }
                }
                case Player player -> {
                    TextureData playerTexture = PlayerTextureUtils.getTextureDataFromGameProfile(player.getGameProfile());
                    if (playerTexture != null) {
                        textureData.put("player", playerTexture);
                    }
                }
                case ClientMannequin mannequin -> {
                    CompletableFuture<Optional<PlayerSkin>> skinLookup = ((ClientMannequinAccessor) mannequin).getSkinLookup();
                    if (skinLookup != null) {
                        skinLookup.whenComplete((skin, throwable) -> {
                            if (throwable != null || skin.isEmpty() || cancelMarker.get()) return;

                            ResolvableProfile profile = ((MannequinAccessor) mannequin).wikirenderer$getProfile();
                            PlayerSkinRenderCache.RenderInfo renderInfo = Minecraft.getInstance().playerSkinRenderCache().getOrDefault(profile);
                            TextureData playerTexture = PlayerTextureUtils.getTextureDataFromGameProfile(renderInfo.gameProfile());
                            if (playerTexture != null) {
                                textureData.put("player", playerTexture);
                                rebuildCallback.run();
                            }
                        });
                    }
                }
                case Mannequin mannequin -> {
                    ResolvableProfile profile = ((MannequinAccessor) mannequin).wikirenderer$getProfile();
                    PlayerSkinRenderCache.RenderInfo renderInfo = Minecraft.getInstance().playerSkinRenderCache().getOrDefault(profile);
                    TextureData playerTexture = PlayerTextureUtils.getTextureDataFromGameProfile(renderInfo.gameProfile());
                    if (playerTexture != null) {
                        textureData.put("player", playerTexture);
                    }
                }
                case Display.ItemDisplay itemDisplay -> {
                    Display.ItemDisplay.ItemRenderState itemRenderState = itemDisplay.itemRenderState();
                    if (itemRenderState != null) {
                        ItemStack item = itemRenderState.itemStack();
                        TextureData itemTextureData = PlayerTextureUtils.getTextureDataFromPlayerHead(item);
                        if (itemTextureData != null) {
                            textureData.put("item", itemTextureData);
                        }
                    }
                }
                case ItemEntity itemEntity -> {
                    ItemStack item = itemEntity.getItem();
                    TextureData itemTextureData = PlayerTextureUtils.getTextureDataFromPlayerHead(item);
                    if (itemTextureData != null) {
                        textureData.put("item", itemTextureData);
                    }
                }
                default -> {
                }
            }

            if (usedEntity instanceof LivingEntity livingEntity) {
                for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                    ItemStack item = livingEntity.getItemBySlot(equipmentSlot);
                    TextureData itemTextureData = PlayerTextureUtils.getTextureDataFromPlayerHead(item);
                    if (itemTextureData != null) {
                        textureData.put(equipmentSlot.getName(), itemTextureData);
                    }
                }
            }
        });
    }

    @Override
    public @NotNull Map<String, TextureData> getTextureData(Runnable rebuildCallback) {
        if (this.requireTextureReCache) {
            this.cacheTextureData(rebuildCallback);
        }
        return this.textureData;
    }

    @Override
    public List<List<Integer>> getTicksToFullyAnimate() {
        List<Integer> animationTimings = new LinkedList<>();
        applyToEntityAndPassengers(getUsedEntity(), entity -> AnimationTimingUtil.scanTicksToFullyAnimateEntityItems(entity, animationTimings));
        return List.of(animationTimings);
    }

    @Override
    public String buildFileName(String preset) {
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(this.getUsedEntity().getType()).getPath();
        String name = this.getUsedEntity().getDisplayName().getString();
        return preset.replace("%entity_type%", type).replace("%name%", name);
    }

    @Override
    public Collection<String> buildPresetExamples() {
        return List.of("label_example.entity_type", "label_example.entity_name");
    }
}
