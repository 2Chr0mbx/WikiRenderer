package com.pigicial.wikirenderer.render.entity.options;

import com.pigicial.wikirenderer.render.entity.options.types.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.level.block.SkullBlock;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class EntityTypeSpecificOverrides<S extends EntityRenderState> {

    private static final Map<Class<? extends EntityRenderState>, Consumer<EntityTypeSpecificOverrides<?>>> REGISTERED_OVERRIDES = new HashMap<>();

    // this is pretty ugly to be honest, 26.1 will enable doing this via reflection
    static {
        registerOverrides(EntityRenderState.class, overrides -> {
            overrides.registerFloatOverride("ageInTicks", state -> state.ageInTicks, (state, value) -> state.ageInTicks = value);
            overrides.registerFloatOverride("eyeHeight", state -> state.eyeHeight, (state, value) -> state.eyeHeight = value);
            overrides.registerDoubleOverride("distanceToCameraSq", state -> state.distanceToCameraSq, (state, value) -> state.distanceToCameraSq = value);
            overrides.registerFloatOverride("eyeHeight", state -> state.eyeHeight, (state, value) -> state.eyeHeight = value);
            overrides.registerFloatOverride("eyeHeight", state -> state.eyeHeight, (state, value) -> state.eyeHeight = value);
        });

        registerOverrides(AllayRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isDancing", state -> state.isDancing, (state, value) -> state.isDancing = value);
            overrides.registerBooleanOverride("isSpinning", state -> state.isSpinning, (state, value) -> state.isSpinning = value);
            overrides.registerFloatOverride("spinningProgress", state -> state.spinningProgress, (state, value) -> state.spinningProgress = value);
            overrides.registerFloatOverride("holdingAnimationProgress", state -> state.holdingAnimationProgress, (state, value) -> state.holdingAnimationProgress = value);
        });

        registerOverrides(ArmadilloRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isHidingInShell", state -> state.isHidingInShell, (state, value) -> state.isHidingInShell = value);
        });

        registerOverrides(ArmedEntityRenderState.class, overrides -> {
            overrides.registerEnumOverride("mainArm", HumanoidArm.class, state -> state.mainArm, (state, value) -> state.mainArm = value);
            overrides.registerEnumOverride("rightArmPose", HumanoidModel.ArmPose.class, state -> state.rightArmPose, (state, value) -> state.rightArmPose = value);
            overrides.registerItemStackOverride("rightHandItemStack", state -> state.rightHandItemStack, (state, value) -> {
                state.rightHandItemStack = value;
                state.rightHandItemState.clear();
                Minecraft.getInstance().getItemModelResolver().updateForLiving(state.rightHandItemState, value, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, Minecraft.getInstance().player);
            });
            overrides.registerEnumOverride("leftArmPose", HumanoidModel.ArmPose.class, state -> state.leftArmPose, (state, value) -> state.leftArmPose = value);
            overrides.registerItemStackOverride("leftHandItemStack", state -> state.leftHandItemStack, (state, value) -> {
                state.leftHandItemStack = value;
                state.leftHandItemState.clear();
                Minecraft.getInstance().getItemModelResolver().updateForLiving(state.leftHandItemState, value, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, Minecraft.getInstance().player);
            });
            overrides.registerEnumOverride("swingAnimationType", SwingAnimationType.class, state -> state.swingAnimationType, (state, value) -> state.swingAnimationType = value);
            overrides.registerFloatOverride("attackTime", state -> state.attackTime, (state, value) -> state.attackTime = value);
        });

        registerOverrides(AvatarRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isSpectator", (state) -> state.isSpectator, (state, value) -> state.isSpectator = value);
            overrides.registerBooleanOverride("showCape", (state) -> state.showCape, (state, value) -> state.showCape = value);
            overrides.registerFloatOverride("capeFlap", (state) -> state.capeFlap, (state, value) -> state.capeFlap = value);
            overrides.registerFloatOverride("capeLean", (state) -> state.capeLean, (state, value) -> state.capeLean = value);
            overrides.registerFloatOverride("capeLean2", (state) -> state.capeLean2, (state, value) -> state.capeLean2 = value);
            overrides.registerIntOverride("arrowCount", (state) -> state.arrowCount, (state, value) -> state.arrowCount = value);
            overrides.registerIntOverride("stingerCount", (state) -> state.stingerCount, (state, value) -> state.stingerCount = value);
            overrides.registerFloatOverride("fallFlyingTimeInTicks", (state) -> state.fallFlyingTimeInTicks, (state, value) -> state.fallFlyingTimeInTicks = value);
            overrides.registerFloatOverride("flyingYRot", (state) -> state.flyingYRot, (state, value) -> {
                state.flyingYRot = value;
                state.shouldApplyFlyingYRot = true; // this is effectively replaced already
            });
            overrides.registerEnumOverride("parrotOnLeftShoulder", Parrot.Variant.class, (state) -> state.parrotOnLeftShoulder, (state, value) -> state.parrotOnLeftShoulder = value);
            overrides.registerEnumOverride("parrotOnRightShoulder", Parrot.Variant.class, (state) -> state.parrotOnRightShoulder, (state, value) -> state.parrotOnRightShoulder = value);
            overrides.registerBooleanOverride("showExtraEars", (state) -> state.showExtraEars, (state, value) -> state.showExtraEars = value);
        });

        registerOverrides(HumanoidRenderState.class, overrides -> {
            overrides.registerFloatOverride("speedValue", state -> state.speedValue, (state, value) -> state.speedValue = value);
            overrides.registerFloatOverride("maxCrossbowChargeDuration", state -> state.maxCrossbowChargeDuration, (state, value) -> state.maxCrossbowChargeDuration = value);
            overrides.registerFloatOverride("ticksUsingItem", state -> state.ticksUsingItem, (state, value) -> state.ticksUsingItem = value);
            overrides.registerBooleanOverride("isCrouching", state -> state.isCrouching, (state, value) -> state.isCrouching = value);
            overrides.registerBooleanOverride("isFallFlying", state -> state.isFallFlying, (state, value) -> state.isFallFlying = value);
            overrides.registerBooleanOverride("isVisuallySwimming", state -> state.isVisuallySwimming, (state, value) -> state.isVisuallySwimming = value);
            overrides.registerFloatOverride("swimAmount", state -> state.swimAmount, (state, value) -> state.swimAmount = value);
            overrides.registerBooleanOverride("isPassenger", state -> state.isPassenger, (state, value) -> state.isPassenger = value);
            overrides.registerBooleanOverride("isUsingItem", state -> state.isUsingItem, (state, value) -> state.isUsingItem = value);
            overrides.registerFloatOverride("elytraRotX", state -> state.elytraRotX, (state, value) -> state.elytraRotX = value);
            overrides.registerFloatOverride("elytraRotY", state -> state.elytraRotY, (state, value) -> state.elytraRotY = value);
            overrides.registerFloatOverride("elytraRotZ", state -> state.elytraRotZ, (state, value) -> state.elytraRotZ = value);
        });

        registerOverrides(HumanoidRenderState.class, overrides -> {
            overrides.registerFloatOverride("swimAmount", state -> state.swimAmount, (state, value) -> state.swimAmount = value);
            overrides.registerFloatOverride("speedValue", state -> state.speedValue, (state, value) -> state.speedValue = value);
            overrides.registerFloatOverride("maxCrossbowChargeDuration", state -> state.maxCrossbowChargeDuration, (state, value) -> state.maxCrossbowChargeDuration = value);
            overrides.registerFloatOverride("ticksUsingItem", state -> state.ticksUsingItem, (state, value) -> state.ticksUsingItem = value);
            overrides.registerEnumOverride("attackArm", HumanoidArm.class, state -> state.attackArm, (state, value) -> state.attackArm = value);
            overrides.registerEnumOverride("useItemHand", InteractionHand.class, state -> state.useItemHand, (state, value) -> state.useItemHand = value);
            overrides.registerBooleanOverride("isCrouching", state -> state.isCrouching, (state, value) -> state.isCrouching = value);
            overrides.registerBooleanOverride("isFallFlying", state -> state.isFallFlying, (state, value) -> state.isFallFlying = value);
            overrides.registerBooleanOverride("isVisuallySwimming", state -> state.isVisuallySwimming, (state, value) -> state.isVisuallySwimming = value);
            overrides.registerBooleanOverride("isPassenger", state -> state.isPassenger, (state, value) -> state.isPassenger = value);
            overrides.registerBooleanOverride("isUsingItem", state -> state.isUsingItem, (state, value) -> state.isUsingItem = value);
            overrides.registerFloatOverride("elytraRotX", state -> state.elytraRotX, (state, value) -> state.elytraRotX = value);
            overrides.registerFloatOverride("elytraRotY", state -> state.elytraRotY, (state, value) -> state.elytraRotY = value);
            overrides.registerFloatOverride("elytraRotZ", state -> state.elytraRotZ, (state, value) -> state.elytraRotZ = value);
            overrides.registerItemStackOverride("headEquipment", state -> state.headEquipment, (state, value) -> state.headEquipment = value);
            overrides.registerItemStackOverride("chestEquipment", state -> state.chestEquipment, (state, value) -> state.chestEquipment = value);
            overrides.registerItemStackOverride("legsEquipment", state -> state.legsEquipment, (state, value) -> state.legsEquipment = value);
            overrides.registerItemStackOverride("feetEquipment", state -> state.feetEquipment, (state, value) -> state.feetEquipment = value);
        });

        registerOverrides(LivingEntityRenderState.class, overrides -> {
            overrides.registerFloatOverride("bodyRot", state -> state.bodyRot, (state, value) -> state.bodyRot = value);
            overrides.registerFloatOverride("yRot", state -> state.yRot, (state, value) -> state.yRot = value);
            overrides.registerFloatOverride("xRot", state -> state.xRot, (state, value) -> state.xRot = value);
            overrides.registerFloatOverride("deathTime", state -> state.deathTime, (state, value) -> state.deathTime = value);
            overrides.registerFloatOverride("walkAnimationPos", state -> state.walkAnimationPos, (state, value) -> state.walkAnimationPos = value);
            overrides.registerFloatOverride("walkAnimationSpeed", state -> state.walkAnimationSpeed, (state, value) -> state.walkAnimationSpeed = value);
            overrides.registerFloatOverride("scale", state -> state.scale, (state, value) -> state.scale = value);
            overrides.registerFloatOverride("ageScale", state -> state.ageScale, (state, value) -> state.ageScale = value);
            overrides.registerFloatOverride("ticksSinceKineticHitFeedback", state -> state.ticksSinceKineticHitFeedback, (state, value) -> state.ticksSinceKineticHitFeedback = value);
            overrides.registerBooleanOverride("isUpsideDown", state -> state.isUpsideDown, (state, value) -> state.isUpsideDown = value);
            overrides.registerBooleanOverride("isFullyFrozen", state -> state.isFullyFrozen, (state, value) -> state.isFullyFrozen = value);
            overrides.registerBooleanOverride("isBaby", state -> state.isBaby, (state, value) -> state.isBaby = value);
            overrides.registerBooleanOverride("isInWater", state -> state.isInWater, (state, value) -> state.isInWater = value);
            overrides.registerBooleanOverride("isAutoSpinAttack", state -> state.isAutoSpinAttack, (state, value) -> state.isAutoSpinAttack = value);
            overrides.registerBooleanOverride("hasRedOverlay", state -> state.hasRedOverlay, (state, value) -> state.hasRedOverlay = value);
            overrides.registerBooleanOverride("isInvisibleToPlayer", state -> state.isInvisibleToPlayer, (state, value) -> state.isInvisibleToPlayer = value);

            overrides.registerEnumOverride("bedOrientation", Direction.class, state -> state.bedOrientation, (state, value) -> state.bedOrientation = value);
            overrides.registerEnumOverride("pose", Pose.class, state -> state.pose, (state, value) -> state.pose = value);
            overrides.registerEnumOverride("wornHeadType", SkullBlock.Types.class, state -> (SkullBlock.Types) state.wornHeadType, (state, value) -> state.wornHeadType = value);
        });

        registerOverrides(TropicalFishRenderState.class, overrides -> {
            overrides.registerEnumOverride("pattern", TropicalFish.Pattern.class, state -> state.pattern, (state, value) -> state.pattern = value);
            overrides.registerIntOverride("baseColor", state -> state.baseColor, (state, value) -> state.baseColor = value);
            overrides.registerIntOverride("patternColor", state -> state.patternColor, (state, value) -> state.patternColor = value);
        });
    }

    @SuppressWarnings("unchecked")
    public static <T extends EntityRenderState> void registerOverrides(Class<T> clazz, Consumer<EntityTypeSpecificOverrides<T>> overrides) {
        REGISTERED_OVERRIDES.put(clazz, (Consumer<EntityTypeSpecificOverrides<?>>) (Consumer<?>) overrides);
    }

    @Nullable
    public static EntityTypeSpecificOverrides<?> getOverrides(EntityRenderState renderState) {
        EntityTypeSpecificOverrides<?> overrides = new EntityTypeSpecificOverrides<>(renderState.getClass());
        boolean overridesApplied = false;

        for (Class<?> clazz = renderState.getClass(); EntityRenderState.class.isAssignableFrom(clazz); clazz = clazz.getSuperclass()) {
            Consumer<EntityTypeSpecificOverrides<?>> overrideRegistry = REGISTERED_OVERRIDES.get(clazz);
            if (overrideRegistry != null) {
                overridesApplied = true;
                overrideRegistry.accept(overrides);
            }
        }

        return overridesApplied ? overrides : null;
    }

    private final BooleanOverride<S> invisible;
    private final Class<S> stateClass;

    public EntityTypeSpecificOverrides(Class<S> stateClass) {
        this.stateClass = stateClass;
        this.invisible = new BooleanOverride<>("dontRender");
        this.overrides.put("dontRender", invisible);
    }

    private final Map<String, OptionalOverride<S, ?>> overrides = new LinkedHashMap<>();

    public void applyOverridesIfPossible(EntityRenderState renderState) {
        if (stateClass.isInstance(renderState)) {
            applyOverrides(stateClass.cast(renderState));
        }
    }

    public void applyOverrides(S state) {
        overrides.values().forEach(override -> override.apply(state));
    }

    public Collection<OptionalOverride<S, ?>> getOverrides() {
        return overrides.values();
    }

    public boolean isInvisible() {
        return invisible.getValue();
    }

    protected void registerFloatOverride(String key, Function<S, Float> getter, BiConsumer<S, Float> setter) {
        overrides.put(key, new FloatOverride<>(key, getter, setter));
    }

    protected void registerIntOverride(String key, Function<S, Integer> getter, BiConsumer<S, Integer> setter) {
        overrides.put(key, new IntegerOverride<>(key, getter, setter));
    }

    protected void registerBooleanOverride(String key, Function<S, Boolean> getter, BiConsumer<S, Boolean> setter) {
        overrides.put(key, new BooleanOverride<>(key, getter, setter));
    }

    protected void registerDoubleOverride(String key, Function<S, Double> getter, BiConsumer<S, Double> setter) {
        overrides.put(key, new DoubleOverride<>(key, getter, setter));
    }

    protected <E extends Enum<E>> void registerEnumOverride(String key, Class<E> enumClass, Function<S, E> getter, BiConsumer<S, E> setter) {
        overrides.put(key, new EnumOverride<>(key, enumClass, getter, setter));
    }

    protected void registerItemStackOverride(String key, Function<S, ItemStack> getter, BiConsumer<S, ItemStack> setter) {
        overrides.put(key, new ItemStackOverride<>(key, getter, setter));
    }

}
