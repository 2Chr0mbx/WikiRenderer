package com.pigicial.wikirenderer.command.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

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

    public static void renderTargetedEntity(CommandContext<FabricClientCommandSource> context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        AttackRange attackRange = new AttackRange(0, 10, 0, 10, 0.3f, 10);
        HitResult closestHit = attackRange.getClosesetHit(player, 1.0f, e -> {

            if (e instanceof LivingEntity livingEntity) {
                if (livingEntity.isInvisible() || (livingEntity instanceof ArmorStand armorStand && armorStand.isMarker())) {
                    for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                        if (!livingEntity.getItemBySlot(equipmentSlot).isEmpty()) {
                            return true;
                        }
                    }
                    return false;
                }
            }

            return e.isPickable();
        });

        if (!(closestHit instanceof EntityHitResult entityHitResult)) {
            Translate.commandError(context, "no_entity");
            return;
        }

        Entity targetEntity = entityHitResult.getEntity();
        ScreenSchedulerAndSaver.schedule(new RenderScreen(
                EntityRenderable.copyAsRenderable(targetEntity)
        ));
    }
}
