package com.pigicial.wikirenderer.render.entity.options;

import com.pigicial.wikirenderer.mixin.access.LivingEntityRendererAccessor;
import com.pigicial.wikirenderer.render.entity.options.types.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.animal.fish.SalmonModel;
import net.minecraft.client.model.animal.fish.TropicalFishLargeModel;
import net.minecraft.client.model.animal.fish.TropicalFishSmallModel;
import net.minecraft.client.model.animal.frog.TadpoleModel;
import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.SalmonRenderer;
import net.minecraft.client.renderer.entity.TropicalFishRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.core.Direction;
import net.minecraft.core.Rotations;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.piglin.PiglinArmPose;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class EntityTypeSpecificOverrides<S extends EntityRenderState> {

    private static final Map<Class<? extends EntityRenderState>, Consumer<EntityTypeSpecificOverrides<?>>> REGISTERED_OVERRIDES = new HashMap<>();

    // this is possibly the worst piece of code in this entire mod, but at least 26.1 will enable doing most this via reflection
    static {
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

        registerOverrides(ArmorStandRenderState.class, overrides -> {
            overrides.registerFloatOverride("yRot", state -> state.yRot, (state, value) -> state.yRot = value);
            overrides.registerFloatOverride("wiggle", state -> state.wiggle, (state, value) -> state.wiggle = value);
            overrides.registerBooleanOverride("isMarker", state -> state.isMarker, (state, value) -> state.isMarker = value);
            overrides.registerBooleanOverride("isSmall", state -> state.isSmall, (state, value) -> state.isSmall = value);
            overrides.registerBooleanOverride("showArms", state -> state.showArms, (state, value) -> state.showArms = value);
            overrides.registerBooleanOverride("showBasePlate", state -> state.showBasePlate, (state, value) -> state.showBasePlate = value);
            // rotations
            overrides.registerRotationsOverrides("headPose", state -> state.headPose, (state, value) -> state.headPose = value);
            overrides.registerRotationsOverrides("bodyPose", state -> state.bodyPose, (state, value) -> state.bodyPose = value);
            overrides.registerRotationsOverrides("leftArmPose", state -> state.leftArmPose, (state, value) -> state.leftArmPose = value);
            overrides.registerRotationsOverrides("rightArmPose", state -> state.rightArmPose, (state, value) -> state.rightArmPose = value);
            overrides.registerRotationsOverrides("leftLegPose", state -> state.leftLegPose, (state, value) -> state.leftLegPose = value);
            overrides.registerRotationsOverrides("rightLegPose", state -> state.rightLegPose, (state, value) -> state.rightLegPose = value);
       });

        registerOverrides(ArrowRenderState.class, overrides -> {
            overrides.registerFloatOverride("xRot", state -> state.xRot, (state, value) -> state.xRot = value);
            overrides.registerFloatOverride("yRot", state -> state.yRot, (state, value) -> state.yRot = value);
            overrides.registerFloatOverride("shake", state -> state.shake, (state, value) -> state.shake = value);
        });

        registerOverrides(ArrowRenderState.class, overrides -> {
            overrides.registerFloatOverride("xRot", state -> state.xRot, (state, value) -> state.xRot = value);
            overrides.registerFloatOverride("yRot", state -> state.yRot, (state, value) -> state.yRot = value);
            overrides.registerFloatOverride("shake", state -> state.shake, (state, value) -> state.shake = value);
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

        registerOverrides(AxolotlRenderState.class, overrides -> {
            overrides.registerEnumOverride("variant", Axolotl.Variant.class, state -> state.variant, (state, value) -> state.variant = value);
            overrides.registerFloatOverride("playingDeadFactor", state -> state.playingDeadFactor, (state, value) -> state.playingDeadFactor = value);
            overrides.registerFloatOverride("movingFactor", state -> state.movingFactor, (state, value) -> state.movingFactor = value);
            overrides.registerFloatOverride("inWaterFactor", state -> state.inWaterFactor, (state, value) -> state.inWaterFactor = value);
            overrides.registerFloatOverride("onGroundFactor", state -> state.onGroundFactor, (state, value) -> state.onGroundFactor = value);
        });

        registerOverrides(BatRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isResting", state -> state.isResting, (state, value) -> state.isResting = value);
            overrides.registerAnimationStateOverride("flyAnimationState", state -> state.flyAnimationState);
            overrides.registerAnimationStateOverride("restAnimationState", state -> state.restAnimationState);
        });

        registerOverrides(BeeRenderState.class, overrides -> {
            overrides.registerFloatOverride("rollAmount", state -> state.rollAmount, (state, value) -> state.rollAmount = value);
            overrides.registerBooleanOverride("hasStinger", (state) -> state.hasStinger, (state, value) -> state.hasStinger = value);
            overrides.registerBooleanOverride("isOnGround", (state) -> state.isOnGround, (state, value) -> state.isOnGround = value);
            overrides.registerBooleanOverride("isAngry", (state) -> state.isAngry, (state, value) -> state.isAngry = value);
            overrides.registerBooleanOverride("hasNectar", (state) -> state.hasNectar, (state, value) -> state.hasNectar = value);
        });

        // block display entity

        registerOverrides(BoatRenderState.class, overrides -> {
            overrides.registerFloatOverride("yRot", state -> state.yRot, (state, value) -> state.yRot = value);
            overrides.registerIntOverride("hurtDir", (state) -> state.hurtDir, (state, value) -> state.hurtDir = value);
            overrides.registerFloatOverride("hurtTime", (state) -> state.hurtTime, (state, value) -> state.hurtTime = value);
            overrides.registerFloatOverride("damageTime", (state) -> state.damageTime, (state, value) -> state.damageTime = value);
            overrides.registerFloatOverride("bubbleAngle", (state) -> state.bubbleAngle, (state, value) -> state.bubbleAngle = value);
            overrides.registerBooleanOverride("boolean", (state) -> state.isUnderWater, (state, value) -> state.isUnderWater = value);
            overrides.registerFloatOverride("rowingTimeLeft", (state) -> state.rowingTimeLeft, (state, value) -> state.rowingTimeLeft = value);
            overrides.registerFloatOverride("rowingTimeRight", (state) -> state.rowingTimeRight, (state, value) -> state.rowingTimeRight = value);
        });

        registerOverrides(BoggedRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isSheared", state -> state.isSheared, (state, value) -> state.isSheared = value);
        });

        registerOverrides(BreezeRenderState.class, overrides -> {
            overrides.registerAnimationStateOverride("idle", state -> state.idle);
            overrides.registerAnimationStateOverride("shoot", state -> state.shoot);
            overrides.registerAnimationStateOverride("slide", state -> state.slide);
            overrides.registerAnimationStateOverride("slideBack", state -> state.slideBack);
            overrides.registerAnimationStateOverride("inhale", state -> state.inhale);
            overrides.registerAnimationStateOverride("longJump", state -> state.longJump);
        });

        registerOverrides(CamelRenderState.class, overrides -> {
            overrides.registerItemStackOverride("saddle", state -> state.saddle, (state, value) -> state.saddle = value);
            overrides.registerBooleanOverride("isRidden", state -> state.isRidden, (state, value) -> state.isRidden = value);
            overrides.registerFloatOverride("jumpCooldown", state -> state.jumpCooldown, (state, value) -> state.jumpCooldown = value);
            overrides.registerAnimationStateOverride("sitAnimationState", state -> state.sitAnimationState);
            overrides.registerAnimationStateOverride("sitPoseAnimationState", state -> state.sitPoseAnimationState);
            overrides.registerAnimationStateOverride("sitUpAnimationState", state -> state.sitUpAnimationState);
            overrides.registerAnimationStateOverride("idleAnimationState", state -> state.idleAnimationState);
            overrides.registerAnimationStateOverride("dashAnimationState", state -> state.dashAnimationState);
        });

        registerOverrides(CatRenderState.class, overrides -> {
            // identifier
            overrides.registerBooleanOverride("isLyingOnTopOfSleepingPlayer", state -> state.isLyingOnTopOfSleepingPlayer, (state, value) -> state.isLyingOnTopOfSleepingPlayer = value);
            overrides.registerEnumOverride("collarColor", DyeColor.class, state -> state.collarColor, (state, value) -> state.collarColor = value);
        });

        registerOverrides(ChickenRenderState.class, overrides -> {
            overrides.registerFloatOverride("flap", state -> state.flap, (state, value) -> state.flap = value);
            overrides.registerFloatOverride("flapSpeed", state -> state.flapSpeed, (state, value) -> state.flapSpeed = value);
            // variant
        });

        registerOverrides(CopperGolemRenderState.class, overrides -> {
            overrides.registerEnumOverride("weathering", WeatheringCopper.WeatherState.class, state -> state.weathering, (state, value) -> state.weathering = value);
            overrides.registerEnumOverride("copperGolemState", CopperGolemState.class, state -> state.copperGolemState, (state, value) -> state.copperGolemState = value);
            overrides.registerAnimationStateOverride("idleAnimationState", state -> state.idleAnimationState);
            overrides.registerAnimationStateOverride("interactionGetItem", state -> state.interactionGetItem);
            overrides.registerAnimationStateOverride("interactionGetNoItem", state -> state.interactionGetNoItem);
            overrides.registerAnimationStateOverride("interactionDropItem", state -> state.interactionDropItem);
            overrides.registerAnimationStateOverride("interactionDropNoItem", state -> state.interactionDropNoItem);
            // block state
        });

        registerOverrides(CowRenderState.class, overrides -> {
            // variant
        });

        registerOverrides(CreakingRenderState.class, overrides -> {
            overrides.registerAnimationStateOverride("invulnerabilityAnimationState", state -> state.invulnerabilityAnimationState);
            overrides.registerAnimationStateOverride("attackAnimationState", state -> state.attackAnimationState);
            overrides.registerAnimationStateOverride("deathAnimationState", state -> state.deathAnimationState);
            overrides.registerBooleanOverride("eyesGlowing", state -> state.eyesGlowing, (state, value) -> state.eyesGlowing = value);
            overrides.registerBooleanOverride("canMove", state -> state.canMove, (state, value) -> state.canMove = value);
        });

        registerOverrides(CreeperRenderState.class, overrides -> {
            overrides.registerFloatOverride("swelling", state -> state.swelling, (state, value) -> state.swelling = value);
            overrides.registerBooleanOverride("isPowered", state -> state.isPowered, (state, value) -> state.isPowered = value);
        });

        // display entity

        registerOverrides(DolphinRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isMoving", state -> state.isMoving, (state, value) -> state.isMoving = value);
        });

        registerOverrides(DonkeyRenderState.class, overrides -> {
            overrides.registerBooleanOverride("hasChest", state -> state.hasChest, (state, value) -> state.hasChest = value);
        });

        registerOverrides(EndCrystalRenderState.class, overrides -> {
            overrides.registerBooleanOverride("showsBottom", state -> state.showsBottom, (state, value) -> state.showsBottom = value);
            // vec3 beam offset
        });

        registerOverrides(EnderDragonRenderState.class, overrides -> {
            overrides.registerFloatOverride("flapTime", state -> state.flapTime, (state, value) -> state.flapTime = value);
            overrides.registerFloatOverride("deathTime", state -> state.deathTime, (state, value) -> state.deathTime = value);
            overrides.registerBooleanOverride("hasRedOverlay", state -> state.hasRedOverlay, (state, value) -> state.hasRedOverlay = value);
            // vec3 beam offset
            overrides.registerBooleanOverride("isLandingOrTakingOff", state -> state.isLandingOrTakingOff, (state, value) -> state.isLandingOrTakingOff = value);
            overrides.registerBooleanOverride("isSitting", state -> state.isSitting, (state, value) -> state.isSitting = value);
            overrides.registerDoubleOverride("distanceToEgg", state -> state.distanceToEgg, (state, value) -> state.distanceToEgg = value);
            overrides.registerFloatOverride("partialTicks", state -> state.partialTicks, (state, value) -> state.partialTicks = value);
            // flight history
        });

        registerOverrides(EndermanRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isCreepy", state -> state.isCreepy, (state, value) -> state.isCreepy = value);
            // carried block
        });

        registerOverrides(EntityRenderState.class, overrides -> {
            overrides.registerFloatOverride("ageInTicks", state -> state.ageInTicks, (state, value) -> state.ageInTicks = value);

            if (overrides.renderer instanceof LivingEntityRenderer) {
                overrides.registerFloatOverride("eyeHeight", state -> state.eyeHeight, (state, value) -> state.eyeHeight = value);
            }
        });

        registerOverrides(EquineRenderState.class, overrides -> {
            overrides.registerBooleanOverride("hasSaddle", state -> state.saddle != null, (state, value) -> {
                if (value) {
                    state.saddle = new ItemStack(Items.SADDLE);
                } else {
                    state.saddle = ItemStack.EMPTY;
                }
            });
            overrides.registerItemStackOverride("bodyArmorItem", state -> state.bodyArmorItem, (state, value) -> state.bodyArmorItem = value);
            overrides.registerBooleanOverride("isRidden", state -> state.isRidden, (state, value) -> state.isRidden = value);
            overrides.registerBooleanOverride("animateTail", state -> state.animateTail, (state, value) -> state.animateTail = value);
            overrides.registerFloatOverride("eatAnimation", state -> state.eatAnimation, (state, value) -> state.eatAnimation = value);
            overrides.registerFloatOverride("standAnimation", state -> state.standAnimation, (state, value) -> state.standAnimation = value);
            overrides.registerFloatOverride("feedingAnimation", state -> state.feedingAnimation, (state, value) -> state.feedingAnimation = value);
        });

        registerOverrides(EvokerFangsRenderState.class, overrides -> {
            overrides.registerFloatOverride("yRot", state -> state.yRot, (state, value) -> state.yRot = value);
            overrides.registerFloatOverride("biteProgress", state -> state.biteProgress, (state, value) -> state.biteProgress = value);
        });

        registerOverrides(EvokerRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isCastingSpell", state -> state.isCastingSpell, (state, value) -> state.isCastingSpell = value);
        });

        registerOverrides(ExperienceOrbRenderState.class, overrides -> {
            overrides.registerIntOverride("icon", state -> state.icon, (state, value) -> state.icon = value);
        });

        // falling block

        registerOverrides(FelineRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isCrouching", state -> state.isCrouching, (state, value) -> state.isCrouching = value);
            overrides.registerBooleanOverride("isSprinting", state -> state.isSprinting, (state, value) -> state.isSprinting = value);
            overrides.registerBooleanOverride("isSitting", state -> state.isSitting, (state, value) -> state.isSitting = value);
            overrides.registerFloatOverride("lieDownAmount", state -> state.lieDownAmount, (state, value) -> state.lieDownAmount = value);
            overrides.registerFloatOverride("lieDownAmountTail", state -> state.lieDownAmountTail, (state, value) -> state.lieDownAmountTail = value);
            overrides.registerFloatOverride("relaxStateOneAmount", state -> state.relaxStateOneAmount, (state, value) -> state.relaxStateOneAmount = value);
        });

        registerOverrides(FireworkRocketRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isShotAtAngle", state -> state.isShotAtAngle, (state, value) -> state.isShotAtAngle = value);
            overrides.registerItemStackOverride("item", state -> null, (state, value) -> {
                Minecraft.getInstance().getItemModelResolver().updateForNonLiving(state.item, value, ItemDisplayContext.GROUND, Minecraft.getInstance().player);
            });
        });

        // fishing hook

        registerOverrides(FoxRenderState.class, overrides -> {
            overrides.registerFloatOverride("headRollAngle", state -> state.headRollAngle, (state, value) -> state.headRollAngle = value);
            overrides.registerFloatOverride("crouchAmount", state -> state.crouchAmount, (state, value) -> state.crouchAmount = value);
            overrides.registerBooleanOverride("isCrouching", state -> state.isCrouching, (state, value) -> state.isCrouching = value);
            overrides.registerBooleanOverride("isSleeping", state -> state.isSleeping, (state, value) -> state.isSleeping = value);
            overrides.registerBooleanOverride("isSitting", state -> state.isSitting, (state, value) -> state.isSitting = value);
            overrides.registerBooleanOverride("isFaceplanted", state -> state.isFaceplanted, (state, value) -> state.isFaceplanted = value);
            overrides.registerBooleanOverride("isPouncing", state -> state.isPouncing, (state, value) -> state.isPouncing = value);
            overrides.registerEnumOverride("variant", Fox.Variant.class, state -> state.variant, (state, value) -> state.variant = value);
        });

        registerOverrides(FrogRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isSwimming", state -> state.isSwimming, (state, value) -> state.isSwimming = value);
            overrides.registerAnimationStateOverride("jumpAnimationState", state -> state.jumpAnimationState);
            overrides.registerAnimationStateOverride("croakAnimationState", state -> state.croakAnimationState);
            overrides.registerAnimationStateOverride("tongueAnimationState", state -> state.tongueAnimationState);
            overrides.registerAnimationStateOverride("swimIdleAnimationState", state -> state.swimIdleAnimationState);
        });

        registerOverrides(GhastRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isCharging", state -> state.isCharging, (state, value) -> state.isCharging = value);
        });

        registerOverrides(GoatRenderState.class, overrides -> {
            overrides.registerBooleanOverride("hasLeftHorn", state -> state.hasLeftHorn, (state, value) -> state.hasLeftHorn = value);
            overrides.registerBooleanOverride("hasRightHorn", state -> state.hasRightHorn, (state, value) -> state.hasRightHorn = value);
            overrides.registerFloatOverride("rammingXHeadRot", state -> state.rammingXHeadRot, (state, value) -> state.rammingXHeadRot = value);
        });

        registerOverrides(GuardianRenderState.class, overrides -> {
            overrides.registerFloatOverride("spikesAnimation", state -> state.spikesAnimation, (state, value) -> state.spikesAnimation = value);
            overrides.registerFloatOverride("tailAnimation", state -> state.tailAnimation, (state, value) -> state.tailAnimation = value);
            // vec3s
            overrides.registerFloatOverride("attackTime", state -> state.attackTime, (state, value) -> state.attackTime = value);
            overrides.registerFloatOverride("attackScale", state -> state.attackScale, (state, value) -> state.attackScale = value);
        });

        registerOverrides(HappyGhastRenderState.class, overrides -> {
            overrides.registerEnumOverride("harnessColor", DyeColor.class, state -> null, (state, value) -> {
                if (value == null) {
                    state.bodyItem = null;
                } else {
                    state.bodyItem = new ItemStack(Items.WHITE_HARNESS);
                    state.bodyItem.set(DataComponents.EQUIPPABLE, Equippable.harness(value));
                }
            });
            overrides.registerBooleanOverride("isRidden", state -> state.isRidden, (state, value) -> state.isRidden = value);
            overrides.registerBooleanOverride("isLeashHolder", state -> state.isLeashHolder, (state, value) -> state.isLeashHolder = value);
        });

        registerOverrides(HoglinRenderState.class, overrides -> {
            overrides.registerIntOverride("attackAnimationRemainingTicks", state -> state.attackAnimationRemainingTicks, (state, value) -> state.attackAnimationRemainingTicks = value);
            overrides.registerBooleanOverride("isConverting", state -> state.isConverting, (state, value) -> state.isConverting = value);
        });

        registerOverrides(HorseRenderState.class, overrides -> {
            overrides.registerEnumOverride("variant", Variant.class, state -> state.variant, (state, value) -> state.variant = value);
            overrides.registerEnumOverride("markings", Markings.class, state -> state.markings, (state, value) -> state.markings = value);
        });
        registerOverrides(HumanoidRenderState.class, overrides -> {
            if (overrides.hasModelType(m -> m instanceof HumanoidModel)) {
                overrides.registerFloatOverride("swimAmount", state -> state.swimAmount, (state, value) -> state.swimAmount = value);
                overrides.registerFloatOverride("speedValue", state -> state.speedValue, (state, value) -> state.speedValue = value, 1f);
                overrides.registerFloatOverride("maxCrossbowChargeDuration", state -> state.maxCrossbowChargeDuration, (state, value) -> state.maxCrossbowChargeDuration = value);
                overrides.registerFloatOverride("ticksUsingItem", state -> state.ticksUsingItem, (state, value) -> state.ticksUsingItem = value);
                overrides.registerEnumOverride("attackArm", HumanoidArm.class, state -> state.attackArm, (state, value) -> state.attackArm = value);
            }

            overrides.registerEnumOverride("useItemHand", InteractionHand.class, state -> state.useItemHand, (state, value) -> state.useItemHand = value);
            overrides.registerBooleanOverride("isUsingItem", state -> state.isUsingItem, (state, value) -> state.isUsingItem = value);

            overrides.registerBooleanOverride("isCrouching", state -> state.isCrouching, (state, value) -> state.isCrouching = value);
            overrides.registerBooleanOverride("isFallFlying", state -> state.isFallFlying, (state, value) -> state.isFallFlying = value);
            overrides.registerBooleanOverride("isVisuallySwimming", state -> state.isVisuallySwimming, (state, value) -> state.isVisuallySwimming = value);
            overrides.registerBooleanOverride("isPassenger", state -> state.isPassenger, (state, value) -> state.isPassenger = value);

            if (overrides.hasLayerType(layer -> layer instanceof WingsLayer)) {
                overrides.registerFloatOverride("elytraRotX", state -> state.elytraRotX, (state, value) -> state.elytraRotX = value);
                overrides.registerFloatOverride("elytraRotY", state -> state.elytraRotY, (state, value) -> state.elytraRotY = value);
                overrides.registerFloatOverride("elytraRotZ", state -> state.elytraRotZ, (state, value) -> state.elytraRotZ = value);
            }
            if (overrides.hasLayerType(layer -> layer instanceof HumanoidArmorLayer || layer instanceof CustomHeadLayer)) {
                overrides.registerItemStackOverride("helmet", state -> state.headEquipment, (state, value) -> {
                    state.headEquipment = value;

                    if (value.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof AbstractSkullBlock abstractSkullBlock) {
                        state.wornHeadType = abstractSkullBlock.getType();
                        state.wornHeadProfile = value.get(DataComponents.PROFILE);
                        state.headItem.clear();
                    } else {
                        state.wornHeadType = null;
                        state.wornHeadProfile = null;
                        state.headItem.clear();

                        if (!HumanoidArmorLayer.shouldRender(value, EquipmentSlot.HEAD)) {
                            Minecraft.getInstance().getItemModelResolver().updateForLiving(state.headItem, value, ItemDisplayContext.HEAD, Minecraft.getInstance().player);
                        }
                    }
                });
            }
            if (overrides.hasLayerType(layer -> layer instanceof HumanoidArmorLayer)) {
                overrides.registerItemStackOverride("chestplate", state -> state.chestEquipment, (state, value) -> state.chestEquipment = value);
                overrides.registerItemStackOverride("leggings", state -> state.legsEquipment, (state, value) -> state.legsEquipment = value);
                overrides.registerItemStackOverride("boots", state -> state.feetEquipment, (state, value) -> state.feetEquipment = value);
            }
        });


        registerOverrides(IllagerRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isRiding", state -> state.isRiding, (state, value) -> state.isRiding = value);
            overrides.registerBooleanOverride("isAggressive", state -> state.isAggressive, (state, value) -> state.isAggressive = value);
            overrides.registerEnumOverride("mainArm", HumanoidArm.class, state -> state.mainArm, (state, value) -> state.mainArm = value);
            overrides.registerEnumOverride("armPose", AbstractIllager.IllagerArmPose.class, state -> state.armPose, (state, value) -> state.armPose = value);
            overrides.registerIntOverride("maxCrossbowChargeDuration", state -> state.maxCrossbowChargeDuration, (state, value) -> state.maxCrossbowChargeDuration = value);
            overrides.registerFloatOverride("ticksUsingItem", state -> state.ticksUsingItem, (state, value) -> state.ticksUsingItem = value);
            overrides.registerFloatOverride("attackAnim", state -> state.attackAnim, (state, value) -> state.attackAnim = value);
        });

        // illusioner (vec3[])

        registerOverrides(IronGolemRenderState.class, overrides -> {
            overrides.registerFloatOverride("attackTicksRemaining", state -> state.attackTicksRemaining, (state, value) -> state.attackTicksRemaining = value);
            overrides.registerIntOverride("offerFlowerTick", state -> state.offerFlowerTick, (state, value) -> state.offerFlowerTick = value);
            overrides.registerEnumOverride("crackiness", Crackiness.Level.class, state -> state.crackiness, (state, value) -> state.crackiness = value);
        });

        registerOverrides(ItemClusterRenderState.class, overrides -> {
            overrides.registerItemStackOverride("item", state -> null, (state, value) -> {
                state.item.clear();
                Minecraft.getInstance().getItemModelResolver().updateForNonLiving(state.item, value, ItemDisplayContext.GROUND, Minecraft.getInstance().player);
            });
            overrides.registerIntOverride("count", state -> state.count, (state, value) -> state.count = value, 1);
            overrides.registerIntOverride("seed", state -> state.seed, (state, value) -> state.seed = value);
        });

        // item display entity

        registerOverrides(ItemEntityRenderState.class, overrides -> {
            overrides.registerFloatOverride("bobOffset", state -> state.bobOffset, (state, value) -> state.bobOffset = value);
        });

        registerOverrides(ItemFrameRenderState.class, overrides -> {
            overrides.registerEnumOverride("direction", Direction.class, state -> state.direction, (state, value) -> state.direction = value);
            overrides.registerItemStackOverride("item", state -> null, (state, value) -> {
                state.item.clear();
                Minecraft.getInstance().getItemModelResolver().updateForNonLiving(state.item, value, ItemDisplayContext.FIXED, Minecraft.getInstance().player);
            });
            overrides.registerIntOverride("rotation", state -> state.rotation, (state, value) -> state.rotation = value);
            overrides.registerBooleanOverride("isGlowFrame", state -> state.isGlowFrame, (state, value) -> state.isGlowFrame = value);
            overrides.registerIntOverride("mapId", state -> state.mapId.id(), (state, value) -> state.mapId = new MapId(value));
            // map render state
        });

        registerOverrides(LightningBoltRenderState.class, overrides -> {
            overrides.registerIntOverride("seed", state -> (int) state.seed, (state, value) -> state.seed = value);
        });

        registerOverrides(LivingEntityRenderState.class, overrides -> {
            //if (overrides.renderer instanceof ArmorStandRenderer || overrides.renderer instanceof SquidRenderer) return;
            overrides.registerFloatOverride("bodyRot", state -> state.bodyRot, (state, value) -> state.bodyRot = value);
            overrides.registerFloatOverride("yRot", state -> state.yRot, (state, value) -> state.yRot = value);
            overrides.registerFloatOverride("xRot", state -> state.xRot, (state, value) -> state.xRot = value);
            overrides.registerFloatOverride("deathTime", state -> state.deathTime, (state, value) -> state.deathTime = value);
            overrides.registerFloatOverride("walkAnimationPos", state -> state.walkAnimationPos, (state, value) -> state.walkAnimationPos = value);
            overrides.registerFloatOverride("walkAnimationSpeed", state -> state.walkAnimationSpeed, (state, value) -> state.walkAnimationSpeed = value);
            overrides.registerFloatOverride("scale", state -> state.scale, (state, value) -> state.scale = value, 1f);
            overrides.registerFloatOverride("ageScale", state -> state.ageScale, (state, value) -> state.ageScale = value, 1f);
            overrides.registerFloatOverride("ticksSinceSpearHitFeedback", state -> state.ticksSinceKineticHitFeedback, (state, value) -> state.ticksSinceKineticHitFeedback = value);
            overrides.registerBooleanOverride("isUpsideDown", state -> state.isUpsideDown, (state, value) -> state.isUpsideDown = value);
            overrides.registerBooleanOverride("isFullyFrozen", state -> state.isFullyFrozen, (state, value) -> state.isFullyFrozen = value);
            overrides.registerBooleanOverride("isBaby", state -> state.isBaby, (state, value) -> state.isBaby = value);

            // i hate this
            if (overrides.hasModelType(model -> model instanceof AbstractEquineModel || model instanceof CowModel || model instanceof SalmonModel
                                                || model instanceof TropicalFishSmallModel || model instanceof TropicalFishLargeModel || model instanceof TadpoleModel)
                || overrides.renderer instanceof SalmonRenderer || overrides.renderer instanceof TropicalFishRenderer || overrides.renderer instanceof AvatarRenderer) {
                overrides.registerBooleanOverride("isInWater", state -> state.isInWater, (state, value) -> state.isInWater = value);
            }

            overrides.registerBooleanOverride("isAutoSpinAttack", state -> state.isAutoSpinAttack, (state, value) -> state.isAutoSpinAttack = value);
            overrides.registerBooleanOverride("hasRedOverlay", state -> state.hasRedOverlay, (state, value) -> state.hasRedOverlay = value);

            // other pose types aren't checked anywhere, only sleeping is used, so just have an option for that instead
            overrides.registerBooleanOverride("isSleeping", state -> state.hasPose(Pose.SLEEPING), (state, value) -> state.pose = value ? Pose.SLEEPING : Pose.STANDING);
            overrides.registerEnumOverride("bedOrientation", Direction.class, state -> state.bedOrientation, (state, value) -> state.bedOrientation = value);

            if (overrides.hasLayerType(l -> l instanceof CustomHeadLayer) && !overrides.hasLayerType(l -> l instanceof HumanoidArmorLayer)) {
                overrides.registerEnumOverride("wornHeadType", SkullBlock.Types.class, state -> (SkullBlock.Types) state.wornHeadType, (state, value) -> state.wornHeadType = value);
                overrides.registerItemStackOverride("headItem", state -> null, (state, value) -> {
                    if (value.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof AbstractSkullBlock abstractSkullBlock) {
                        state.headItem.clear();
                        state.wornHeadType = abstractSkullBlock.getType();
                        state.wornHeadProfile = value.get(DataComponents.PROFILE);
                    } else {
                        state.headItem.clear();
                        state.wornHeadType = null;
                        state.wornHeadProfile = null;

                        if (!HumanoidArmorLayer.shouldRender(value, EquipmentSlot.HEAD)) {
                            Minecraft.getInstance().getItemModelResolver().updateForLiving(state.headItem, value, ItemDisplayContext.HEAD, Minecraft.getInstance().player);
                        }
                    }
                });
            }
            // for custom head items it seems
            overrides.registerFloatOverride("wornHeadAnimationPos", state -> state.wornHeadAnimationPos, (state, value) -> state.wornHeadAnimationPos = value);
        });

        registerOverrides(LlamaRenderState.class, overrides -> {
            overrides.registerEnumOverride("variant", Llama.Variant.class, state -> state.variant, (state, value) -> state.variant = value);
            overrides.registerBooleanOverride("hasChest", state -> state.hasChest, (state, value) -> state.hasChest = value);
            overrides.registerEnumOverride("swagColor", DyeColor.class, state -> null, (state, value) -> {
                if (value == null) {
                    state.bodyItem = null;
                } else {
                    state.bodyItem = new ItemStack(Items.WHITE_CARPET);
                    state.bodyItem.set(DataComponents.EQUIPPABLE, Equippable.llamaSwag(value));
                }
            });
            overrides.registerBooleanOverride("isTraderLlama", state -> state.isTraderLlama, (state, value) -> state.isTraderLlama = value);
        });

        registerOverrides(LlamaSpitRenderState.class, overrides -> {
            overrides.registerFloatOverride("yRot", state -> state.yRot, (state, value) -> state.yRot = value);
            overrides.registerFloatOverride("xRot", state -> state.xRot, (state, value) -> state.xRot = value);
        });

        registerOverrides(MinecartRenderState.class, overrides -> {
            overrides.registerFloatOverride("yRot", state -> state.yRot, (state, value) -> state.yRot = value);
            overrides.registerFloatOverride("xRot", state -> state.xRot, (state, value) -> state.xRot = value);
            overrides.registerIntOverride("offsetSeed", state -> (int) state.offsetSeed, (state, value) -> state.offsetSeed = value);
            overrides.registerIntOverride("hurtDir", state -> state.hurtDir, (state, value) -> state.hurtDir = value);
            overrides.registerFloatOverride("hurtTime", state -> state.hurtTime, (state, value) -> state.hurtTime = value);
            overrides.registerFloatOverride("damageTime", state -> state.damageTime, (state, value) -> state.damageTime = value);
            overrides.registerIntOverride("displayOffset", state -> state.displayOffset, (state, value) -> state.displayOffset = value);
            // block state
            overrides.registerBooleanOverride("isNewRender", state -> state.isNewRender, (state, value) -> state.isNewRender = value);
            // position stuff vec3s
        });

        registerOverrides(MinecartTntRenderState.class, overrides -> {
            overrides.registerFloatOverride("fuseRemainingInTicks", state -> state.fuseRemainingInTicks, (state, value) -> state.fuseRemainingInTicks = value);
        });

        registerOverrides(MushroomCowRenderState.class, overrides -> {
            overrides.registerEnumOverride("variant", MushroomCow.Variant.class, state -> state.variant, (state, value) -> state.variant = value);
        });

        registerOverrides(NautilusRenderState.class, overrides -> {
            overrides.registerItemStackOverride("saddle", state -> state.saddle, (state, value) -> state.saddle = value);
            overrides.registerItemStackOverride("bodyArmorItem", state -> state.bodyArmorItem, (state, value) -> state.bodyArmorItem = value);
            // variant
        });

        registerOverrides(PaintingRenderState.class, overrides -> {
            overrides.registerEnumOverride("direction", Direction.class, state -> state.direction, (state, value) -> state.direction = value);
            // variant
            // lighting
        });

        registerOverrides(PandaRenderState.class, overrides -> {
            overrides.registerEnumOverride("variant", Panda.Gene.class,state -> state.variant, (state, value) -> state.variant = value);
            overrides.registerBooleanOverride("variant", state -> state.isUnhappy, (state, value) -> state.isUnhappy = value);
            overrides.registerBooleanOverride("variant", state -> state.isSneezing, (state, value) -> state.isSneezing = value);
            overrides.registerIntOverride("variant", state -> state.sneezeTime, (state, value) -> state.sneezeTime = value);
            overrides.registerBooleanOverride("isEating", state -> state.isEating, (state, value) -> state.isEating = value);
            overrides.registerBooleanOverride("isScared", state -> state.isScared, (state, value) -> state.isScared = value);
            overrides.registerBooleanOverride("isSitting", state -> state.isSitting, (state, value) -> state.isSitting = value);
            overrides.registerFloatOverride("sitAmount", state -> state.sitAmount, (state, value) -> state.sitAmount = value);
            overrides.registerFloatOverride("lieOnBackAmount", state -> state.lieOnBackAmount, (state, value) -> state.lieOnBackAmount = value);
            overrides.registerFloatOverride("rollAmount", state -> state.rollAmount, (state, value) -> state.rollAmount = value);
            overrides.registerFloatOverride("rollTime", state -> state.rollTime, (state, value) -> state.rollTime = value);
        });

        registerOverrides(ParrotRenderState.class, overrides -> {
            overrides.registerEnumOverride("variant", Parrot.Variant.class,state -> state.variant, (state, value) -> state.variant = value);
            overrides.registerFloatOverride("flapAngle", state -> state.flapAngle, (state, value) -> state.flapAngle = value);
            overrides.registerEnumOverride("pose", ParrotModel.Pose.class, state -> state.pose, (state, value) -> state.pose = value);
        });

        registerOverrides(PhantomRenderState.class, overrides -> {
            overrides.registerFloatOverride("flapTime", state -> state.flapTime, (state, value) -> state.flapTime = value);
            overrides.registerIntOverride("size", state -> state.size, (state, value) -> state.size = value);
        });

        registerOverrides(PhantomRenderState.class, overrides -> {
            overrides.registerFloatOverride("flapTime", state -> state.flapTime, (state, value) -> state.flapTime = value);
            overrides.registerIntOverride("size", state -> state.size, (state, value) -> state.size = value);
        });

        registerOverrides(PiglinRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isBrute", state -> state.isBrute, (state, value) -> state.isBrute = value);
            overrides.registerBooleanOverride("isConverting", state -> state.isConverting, (state, value) -> state.isConverting = value);
            overrides.registerFloatOverride("maxCrossbowChageDuration", state -> state.maxCrossbowChageDuration, (state, value) -> state.maxCrossbowChageDuration = value);
            overrides.registerEnumOverride("armPose", PiglinArmPose.class, state -> state.armPose, (state, value) -> state.armPose = value);
        });

        registerOverrides(PigRenderState.class, overrides -> {
            overrides.registerBooleanOverride("hasSaddle", state -> state.saddle != null, (state, value) -> {
                if (value) {
                    state.saddle = new ItemStack(Items.SADDLE);
                } else {
                    state.saddle = ItemStack.EMPTY;
                }
            });
            // variant
        });

        registerOverrides(PolarBearRenderState.class, overrides -> {
            overrides.registerFloatOverride("standScale", state -> state.standScale, (state, value) -> state.standScale = value);
        });

        registerOverrides(PufferfishRenderState.class, overrides -> {
            overrides.registerIntOverride("puffState", state -> state.puffState, (state, value) -> state.puffState = value);
        });

        registerOverrides(RabbitRenderState.class, overrides -> {
            overrides.registerFloatOverride("jumpCompletion", state -> state.jumpCompletion, (state, value) -> state.jumpCompletion = value);
            overrides.registerBooleanOverride("isToast", state -> state.isToast, (state, value) -> state.isToast = value);
            overrides.registerEnumOverride("variant", Rabbit.Variant.class, state -> state.variant, (state, value) -> state.variant = value);
        });

        registerOverrides(RavagerRenderState.class, overrides -> {
            overrides.registerFloatOverride("stunnedTicksRemaining", state -> state.stunnedTicksRemaining, (state, value) -> state.stunnedTicksRemaining = value);
            overrides.registerFloatOverride("attackTicksRemaining", state -> state.attackTicksRemaining, (state, value) -> state.attackTicksRemaining = value);
            overrides.registerFloatOverride("roarAnimation", state -> state.roarAnimation, (state, value) -> state.roarAnimation = value);
        });

        registerOverrides(SalmonRenderState.class, overrides -> {
            overrides.registerEnumOverride("variant", Salmon.Variant.class, state -> state.variant, (state, value) -> state.variant = value);
        });

        registerOverrides(SheepRenderState.class, overrides -> {
            overrides.registerFloatOverride("headEatPositionScale",  state -> state.headEatPositionScale, (state, value) -> state.headEatPositionScale = value);
            overrides.registerFloatOverride("headEatAngleScale",  state -> state.headEatAngleScale, (state, value) -> state.headEatAngleScale = value);
            overrides.registerBooleanOverride("isSheared",  state -> state.isSheared, (state, value) -> state.isSheared = value);
            overrides.registerEnumOverride("woolColor", DyeColor.class, state -> state.woolColor, (state, value) -> state.woolColor = value);
            overrides.registerBooleanOverride("isJebSheep",  state -> state.isJebSheep, (state, value) -> state.isJebSheep = value);
        });

        registerOverrides(ShulkerBulletRenderState.class, overrides -> {
            overrides.registerFloatOverride("xRot", state -> state.xRot, (state, value) -> state.xRot = value);
            overrides.registerFloatOverride("yRot", state -> state.yRot, (state, value) -> state.yRot = value);
        });

        registerOverrides(ShulkerRenderState.class, overrides -> {
            // vec3 renderOffset
            overrides.registerEnumOverride("color", DyeColor.class, state -> state.color, (state, value) -> state.color = value);
            overrides.registerFloatOverride("peekAmount", state -> state.peekAmount, (state, value) -> state.peekAmount = value);
            overrides.registerFloatOverride("yHeadRot", state -> state.yHeadRot, (state, value) -> state.yHeadRot = value);
            overrides.registerFloatOverride("yBodyRot", state -> state.yBodyRot, (state, value) -> state.yBodyRot = value);
            overrides.registerEnumOverride("yBodyRot", Direction.class, state -> state.attachFace, (state, value) -> state.attachFace = value);
        });

        registerOverrides(SkeletonRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isAggressive", state -> state.isAggressive, (state, value) -> state.isAggressive = value);
            overrides.registerBooleanOverride("isShaking", state -> state.isShaking, (state, value) -> state.isShaking = value);
            overrides.registerBooleanOverride("isHoldingBow", state -> state.isHoldingBow, (state, value) -> state.isHoldingBow = value);
        });

        registerOverrides(SlimeRenderState.class, overrides -> {
            overrides.registerFloatOverride("squish", state -> state.squish, (state, value) -> state.squish = value);
            overrides.registerIntOverride("size", state -> state.size, (state, value) -> state.size = value, 1);
        });

        registerOverrides(SnifferRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isSearching", state -> state.isSearching, (state, value) -> state.isSearching = value);
            overrides.registerAnimationStateOverride("diggingAnimationState", state -> state.diggingAnimationState);
            overrides.registerAnimationStateOverride("sniffingAnimationState", state -> state.sniffingAnimationState);
            overrides.registerAnimationStateOverride("risingAnimationState", state -> state.risingAnimationState);
            overrides.registerAnimationStateOverride("feelingHappyAnimationState", state -> state.feelingHappyAnimationState);
            overrides.registerAnimationStateOverride("scentingAnimationState", state -> state.scentingAnimationState);
        });

        registerOverrides(SnowGolemRenderState.class, overrides -> {
            overrides.registerBooleanOverride("hasPumpkin", state -> state.hasPumpkin, (state, value) -> state.hasPumpkin = value);
        });

        registerOverrides(SquidRenderState.class, overrides -> {
            overrides.registerFloatOverride("tentacleAngle", state -> state.tentacleAngle, (state, value) -> state.tentacleAngle = value);
            overrides.registerFloatOverride("xBodyRot", state -> state.xBodyRot, (state, value) -> state.xBodyRot = value);
            overrides.registerFloatOverride("zBodyRot", state -> state.zBodyRot, (state, value) -> state.zBodyRot = value);
        });

        registerOverrides(StriderRenderState.class, overrides -> {
            overrides.registerBooleanOverride("hasSaddle", state -> state.saddle != null, (state, value) -> {
                if (value) {
                    state.saddle = new ItemStack(Items.SADDLE);
                } else {
                    state.saddle = ItemStack.EMPTY;
                }
            });
            overrides.registerBooleanOverride("isSuffocating", state -> state.isSuffocating, (state, value) -> state.isSuffocating = value);
            overrides.registerBooleanOverride("isRidden", state -> state.isRidden, (state, value) -> state.isRidden = value);
        });

        // text display

        registerOverrides(ThrownItemRenderState.class, overrides -> {
            overrides.registerItemStackOverride("item", state -> null, (state, value) -> {
                state.item.clear();
                Minecraft.getInstance().getItemModelResolver().updateForLiving(state.item, value, ItemDisplayContext.GROUND, Minecraft.getInstance().player);
            });
        });

        registerOverrides(ThrownTridentRenderState.class, overrides -> {
            overrides.registerFloatOverride("xRot", state -> state.xRot, (state, value) -> state.xRot = value);
            overrides.registerFloatOverride("yRot", state -> state.yRot, (state, value) -> state.yRot = value);
            overrides.registerBooleanOverride("isFoil", state -> state.isFoil, (state, value) -> state.isFoil = value);
        });

        registerOverrides(TippableArrowRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isTipped", state -> state.isTipped, (state, value) -> state.isTipped = value);
        });

        registerOverrides(TntRenderState.class, overrides -> {
            overrides.registerFloatOverride("fuseRemainingInTicks", state -> state.fuseRemainingInTicks, (state, value) -> state.fuseRemainingInTicks = value);
            // block state
        });

        registerOverrides(TropicalFishRenderState.class, overrides -> {
            overrides.registerEnumOverride("pattern", TropicalFish.Pattern.class, state -> state.pattern, (state, value) -> state.pattern = value);
            overrides.registerIntOverride("baseColor", state -> state.baseColor, (state, value) -> state.baseColor = value, -1);
            overrides.registerIntOverride("patternColor", state -> state.patternColor, (state, value) -> state.patternColor = value, -1);
        });

        registerOverrides(TurtleRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isOnLand", state -> state.isOnLand, (state, value) -> state.isOnLand = value);
            overrides.registerBooleanOverride("isLayingEgg", state -> state.isLayingEgg, (state, value) -> state.isLayingEgg = value);
            overrides.registerBooleanOverride("hasEgg", state -> state.hasEgg, (state, value) -> state.hasEgg = value);
        });

        registerOverrides(VexRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isCharging", state -> state.isCharging, (state, value) -> state.isCharging = value);
        });

        registerOverrides(VillagerRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isUnhappy", state -> state.isUnhappy, (state, value) -> state.isUnhappy = value);
            // villagerData (profession/type)
        });

        registerOverrides(WardenRenderState.class, overrides -> {
            overrides.registerFloatOverride("tendrilAnimation", state -> state.tendrilAnimation, (state, value) -> state.tendrilAnimation = value);
            overrides.registerFloatOverride("heartAnimation", state -> state.heartAnimation, (state, value) -> state.heartAnimation = value);
            overrides.registerAnimationStateOverride("roarAnimationState", state -> state.roarAnimationState);
            overrides.registerAnimationStateOverride("sniffAnimationState", state -> state.sniffAnimationState);
            overrides.registerAnimationStateOverride("emergeAnimationState", state -> state.emergeAnimationState);
            overrides.registerAnimationStateOverride("diggingAnimationState", state -> state.diggingAnimationState);
            overrides.registerAnimationStateOverride("attackAnimationState", state -> state.attackAnimationState);
            overrides.registerAnimationStateOverride("sonicBoomAnimationState", state -> state.sonicBoomAnimationState);
        });

        registerOverrides(WitchRenderState.class, overrides -> {
            overrides.registerIntOverride("entityId", state -> state.entityId, (state, value) -> state.entityId = value);
            overrides.registerBooleanOverride("isHoldingItem", state -> state.isHoldingItem, (state, value) -> state.isHoldingItem = value);
            overrides.registerBooleanOverride("isHoldingPotion", state -> state.isHoldingPotion, (state, value) -> state.isHoldingPotion = value);
        });

        registerOverrides(WitherRenderState.class, overrides -> {
            overrides.registerFloatOverride("xHeadRot 1", state -> state.xHeadRots[0], (state, value) -> state.xHeadRots[0] = value);
            overrides.registerFloatOverride("xHeadRot 1", state -> state.xHeadRots[1], (state, value) -> state.xHeadRots[1] = value);
            overrides.registerFloatOverride("yHeadRot 1", state -> state.yHeadRots[0], (state, value) -> state.yHeadRots[0] = value);
            overrides.registerFloatOverride("yHeadRot 1", state -> state.yHeadRots[1], (state, value) -> state.yHeadRots[1] = value);
            overrides.registerFloatOverride("invulnerableTicks", state -> state.invulnerableTicks, (state, value) -> state.invulnerableTicks = value);
            overrides.registerBooleanOverride("isPowered", state -> state.isPowered, (state, value) -> state.isPowered = value);
        });

        registerOverrides(WitherSkullRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isDangerous", state -> state.isDangerous, (state, value) -> state.isDangerous = value);
            overrides.registerFloatOverride("animationPos", state -> state.modelState.animationPos, (state, value) -> state.modelState.animationPos = value);
            overrides.registerFloatOverride("yRot", state -> state.modelState.yRot, (state, value) -> state.modelState.yRot = value);
            overrides.registerFloatOverride("xRot", state -> state.modelState.xRot, (state, value) -> state.modelState.xRot = value);
        });

        registerOverrides(WolfRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isAngry", state -> state.isAngry, (state, value) -> state.isAngry = value);
            overrides.registerBooleanOverride("isSitting", state -> state.isSitting, (state, value) -> state.isSitting = value);
            overrides.registerFloatOverride("tailAngle", state -> state.tailAngle, (state, value) -> state.tailAngle = value, (float) (Math.PI / 5));
            overrides.registerFloatOverride("headRollAngle", state -> state.headRollAngle, (state, value) -> state.headRollAngle = value);
            overrides.registerFloatOverride("shakeAnim", state -> state.shakeAnim, (state, value) -> state.shakeAnim = value);
            overrides.registerFloatOverride("wetShade", state -> state.wetShade, (state, value) -> state.wetShade = value, 1);
            // texture
            overrides.registerEnumOverride("collarColor", DyeColor.class, state -> state.collarColor, (state, value) -> state.collarColor = value);
            overrides.registerBooleanOverride("hasWolfArmor", state -> !state.bodyArmorItem.isEmpty(), (state, value) -> {
                if (value) {
                    state.bodyArmorItem = new ItemStack(Items.WOLF_ARMOR);
                } else {
                    state.bodyArmorItem = ItemStack.EMPTY;
                }
            });
        });

        registerOverrides(ZombieRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isAggressive", state -> state.isAggressive, (state, value) -> state.isAggressive = value);
            overrides.registerBooleanOverride("isConverting", state -> state.isConverting, (state, value) -> state.isConverting = value);
        });

        // zombie villager

        registerOverrides(ZombifiedPiglinRenderState.class, overrides -> {
            overrides.registerBooleanOverride("isAggressive", state -> state.isAggressive, (state, value) -> state.isAggressive = value);
        });

    }

    @SuppressWarnings("unchecked")
    public static <T extends EntityRenderState> void registerOverrides(Class<T> clazz, Consumer<EntityTypeSpecificOverrides<T>> overrides) {
        REGISTERED_OVERRIDES.put(clazz, (Consumer<EntityTypeSpecificOverrides<?>>) (Consumer<?>) overrides);
    }

    @Nullable
    public static <S extends EntityRenderState> EntityTypeSpecificOverrides<S> getOverrides(S renderState) {
        EntityTypeSpecificOverrides<S> overrides = new EntityTypeSpecificOverrides<>(renderState);

        boolean overridesApplied = false;

        for (Class<?> clazz = renderState.getClass(); EntityRenderState.class.isAssignableFrom(clazz); clazz = clazz.getSuperclass()) {
            Consumer<EntityTypeSpecificOverrides<?>> overrideRegistry = REGISTERED_OVERRIDES.get(clazz);
            if (overrideRegistry != null) {
                overridesApplied = true;
                overrideRegistry.accept(overrides);
            }
        }

        for (OptionalOverride<S, ?> override : overrides.getOverrides()) {
            override.copyFromRenderState(renderState);
        }
        return overridesApplied ? overrides : null;
    }

    private final BooleanOverride<S> invisible;
    private final EntityRenderer<?, ?> renderer;
    private final Class<S> stateClass;

    public EntityTypeSpecificOverrides(S renderState) {
        // noinspection unchecked
        this.stateClass = (Class<S>) renderState.getClass();
        this.invisible = new BooleanOverride<>("dontRender");
        this.overrides.put("dontRender", invisible);
        this.renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(renderState);
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

    protected void registerFloatOverride(String key, Function<S, Float> getter, BiConsumer<S, Float> setter, float defaultValue) {
        overrides.put(key, new FloatOverride<>(key, getter, setter, defaultValue));
    }

    protected void registerIntOverride(String key, Function<S, Integer> getter, BiConsumer<S, Integer> setter) {
        overrides.put(key, new IntegerOverride<>(key, getter, setter));
    }

    protected void registerIntOverride(String key, Function<S, Integer> getter, BiConsumer<S, Integer> setter, int defaultValue) {
        overrides.put(key, new IntegerOverride<>(key, getter, setter, defaultValue));
    }

    protected void registerBooleanOverride(String key, Function<S, Boolean> getter, BiConsumer<S, Boolean> setter) {
        overrides.put(key, new BooleanOverride<>(key, getter, setter));
    }

    protected void registerDoubleOverride(String key, Function<S, Double> getter, BiConsumer<S, Double> setter) {
        overrides.put(key, new DoubleOverride<>(key, getter, setter));
    }

    protected <E extends Enum<E>> void registerEnumOverride(String key, Class<E> enumClass, Function<S, E> getter, BiConsumer<S, @Nullable E> setter) {
        overrides.put(key, new EnumOverride<>(key, enumClass, getter, setter));
    }

    protected void registerItemStackOverride(String key, Function<S, ItemStack> getter, BiConsumer<S, ItemStack> setter) {
        overrides.put(key, new ItemStackOverride<>(key, getter, setter));
    }

    protected void registerAnimationStateOverride(String key, Function<S, AnimationState> getter) {
        registerIntOverride(key, s -> 0, (s, value) -> getter.apply(s).start(value));
    }

    protected void registerRotationsOverrides(String key, Function<S, Rotations> getter, BiConsumer<S, Rotations> setter) {
        registerFloatOverride(key + "X", state -> getter.apply(state).x(), (state, value) -> {
            Rotations current = getter.apply(state);
            setter.accept(state, new Rotations(value, current.y(), current.z()));
        });
        registerFloatOverride(key + "Y", state -> getter.apply(state).y(), (state, value) -> {
            Rotations current = getter.apply(state);
            setter.accept(state, new Rotations(value, current.y(), current.z()));
        });
        registerFloatOverride(key + "Z", state -> getter.apply(state).z(), (state, value) -> {
            Rotations current = getter.apply(state);
            setter.accept(state, new Rotations(value, current.y(), current.z()));
        });
    }

    public boolean hasLayerType(Predicate<RenderLayer<?, ?>> predicate) {
        if (renderer instanceof LivingEntityRenderer<?, ?, ?> livingEntityRenderer) {
            List<RenderLayer<?, ?>> layers = ((LivingEntityRendererAccessor) livingEntityRenderer).wikirenderer$getLayers();
            for (RenderLayer<?, ?> layer : layers) {
                if (predicate.test(layer)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean hasModelType(Predicate<EntityModel<?>> predicate) {
        if (renderer instanceof LivingEntityRenderer<?, ?, ?> livingEntityRenderer) {
            EntityModel<?> model = livingEntityRenderer.getModel();
            return predicate.test(model);
        } else {
            return false;
        }
    }

}
