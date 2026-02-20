package com.pigicial.wikirenderer.command.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.pigicial.wikirenderer.mixin.access.LevelAccessor;
import com.pigicial.wikirenderer.render.entity.EntityRenderBoundsUtil;
import com.pigicial.wikirenderer.render.entity.EntityRenderable;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.screen.ScreenSchedulerAndSaver;
import com.pigicial.wikirenderer.util.Translate;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class RenderEntitySubCommand extends WikiRendererSubCommand {
    private static final SuggestionProvider<FabricClientCommandSource> CLIENT_SUMMONABLE_ENTITIES;

    static {
        CLIENT_SUMMONABLE_ENTITIES = (context, builder) -> SharedSuggestionProvider.suggestResource(
                BuiltInRegistries.ENTITY_TYPE.stream().filter(EntityType::canSummon),
                builder,
                EntityType::getKey,
                entityType -> Component.translatable(Util.makeDescriptionId("entity", EntityType.getKey(entityType)))
        );
    }

    @Override
    public String getName() {
        return "entity";
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> register(LiteralArgumentBuilder<FabricClientCommandSource> source, CommandBuildContext access) {
        return source.executes(context -> {
                    RenderEntitySubCommand.renderTargetedEntity(context);
                    return 0;
                })
                .then(argument("entity", ResourceArgument.resource(access, Registries.ENTITY_TYPE))
                        .suggests(CLIENT_SUMMONABLE_ENTITIES)
                        .executes(context -> {
                            this.renderEntity(context, false);
                            return 0;
                        })
                        .then(argument("nbt", CompoundTagArgument.compoundTag())
                                .executes(context -> {
                                    this.renderEntity(context, true);
                                    return 0;
                                })));
    }

    private void renderEntity(CommandContext<FabricClientCommandSource> context, boolean useNbt) {
        Holder.Reference<?> typeReference = context.getArgument("entity", Holder.Reference.class);
        if (typeReference.value() instanceof EntityType<?> entityTypeReference) { // basically to avoid the intellij warning, this should be always true
            CompoundTag entityNbt = useNbt ? CompoundTagArgument.getCompoundTag(context, "nbt") : null;

            EntityRenderable renderable = EntityRenderable.of(entityTypeReference, entityNbt);
            if (renderable != null) {
                ScreenSchedulerAndSaver.schedule(new RenderScreen(renderable));
            }
        }
    }

    // Based on the standard attackRange.getClosesetHit code, but instead uses custom entity render bounding box data for better accuracy
    public static void renderTargetedEntity(CommandContext<FabricClientCommandSource> context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        AttackRange attackRange = new AttackRange(0, 20, 0, 20, 0.2f, 1);
        Entity targetEntity = getClosestHit(player, attackRange);
        if (targetEntity == null) {
            Translate.commandError(context, "no_entity");
            return;
        }

        ScreenSchedulerAndSaver.schedule(new RenderScreen(EntityRenderable.copyAsRenderable(targetEntity)));
    }

    public static Entity getClosestHit(Player source, AttackRange attackRange) {
        Collection<EntityHitResult> collection = getHitEntitiesAlong(source, attackRange);

        Entity closestEntity = null;
        Vec3 eyePosition = source.getEyePosition(1);

        double lowestDistance = Double.MAX_VALUE;
        for (EntityHitResult hitEntity : collection) {
            double distance = eyePosition.distanceToSqr(hitEntity.getLocation());
            if (distance < lowestDistance) {
                lowestDistance = distance;
                closestEntity = hitEntity.getEntity();
            }
        }

        return closestEntity;
    }

    private static Collection<EntityHitResult> getHitEntitiesAlong(Player playerSource, AttackRange attackRange) {
        Vec3 headLookAngle = playerSource.getHeadLookAngle();
        Vec3 eyePosition = playerSource.getEyePosition();
        Vec3 from = eyePosition.add(headLookAngle.scale(attackRange.effectiveMinRange(playerSource)));
        double movementComponent = playerSource.getKnownMovement().dot(headLookAngle);
        Vec3 to = eyePosition.add(headLookAngle.scale(attackRange.effectiveMaxRange(playerSource) + Math.max(0.0, movementComponent)));
        return getHitEntitiesAlong(playerSource, from, to, attackRange.hitboxMargin());
    }

    private static Collection<EntityHitResult> getHitEntitiesAlong(Player source, Vec3 from, Vec3 to, float entityMargin) {
        Level level = source.level();
        AABB searchArea = AABB.ofSize(from, entityMargin, entityMargin, entityMargin).expandTowards(to.subtract(from)).inflate(1.0);
        return getManyEntityHitResult(level, source, from, to, searchArea, 0, ClipContext.Block.VISUAL, true);
    }

    public static Collection<EntityHitResult> getManyEntityHitResult(
            Level level, Player source, Vec3 from, Vec3 to, AABB targetSearchArea, float entityMargin, ClipContext.Block clipType, boolean includeFromEntity
    ) {
        List<EntityHitResult> collector = new ArrayList<>();

        AABB expandedTargetSearchArea = new AABB(
                targetSearchArea.minX - 3, targetSearchArea.minY - 3, targetSearchArea.minZ - 3,
                targetSearchArea.maxX + 3, targetSearchArea.maxY + 3, targetSearchArea.maxZ + 3
        );

        for (Entity entity : ((LevelAccessor) level).wikirenderer$getEntities().getAll()) {
            if (entity == source) continue;

            // accurate enough check to remove most entities without instead checking for the more expensive rendered bounding box data
            if (!expandedTargetSearchArea.contains(entity.position())) continue;

            AABB entityBB = EntityRenderBoundsUtil.getPositionOffsetBasedBounds(entity);
            if (entityBB == null) continue;

            if (includeFromEntity && entityBB.contains(from)) {
                collector.add(new EntityHitResult(entity, from));
            } else {
                Optional<Vec3> exactHit = entityBB.clip(from, to);
                if (exactHit.isPresent()) {
                    collector.add(new EntityHitResult(entity, exactHit.get()));
                } else if (!(entityMargin <= 0.0)) {
                    Optional<Vec3> outsideHit = entityBB.inflate(entityMargin).clip(from, to);
                    if (outsideHit.isPresent()) {
                        Vec3 outsideHitPosition = outsideHit.get();
                        Vec3 towardsTarget = entityBB.getCenter();
                        BlockHitResult hitResult = level.clipIncludingBorder(new ClipContext(outsideHitPosition, towardsTarget, clipType, ClipContext.Fluid.NONE, source));
                        if (hitResult.getType() != HitResult.Type.MISS) {
                            towardsTarget = hitResult.getLocation();
                        }

                        Optional<Vec3> surfaceHit = entity.getBoundingBox().clip(outsideHitPosition, towardsTarget);
                        surfaceHit.ifPresent(vec3 -> collector.add(new EntityHitResult(entity, vec3)));
                    }
                }
            }
        }

        return collector;
    }
}
