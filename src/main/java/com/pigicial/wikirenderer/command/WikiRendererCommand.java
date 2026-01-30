package com.pigicial.wikirenderer.command;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.mixin.access.BlockInputAccessor;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.render.area.AreaRenderable;
import com.pigicial.wikirenderer.render.batch.BatchRenderTask;
import com.pigicial.wikirenderer.render.entity.EntityRenderable;
import com.pigicial.wikirenderer.render.entity.player.ProfileFetchMode;
import com.pigicial.wikirenderer.render.entity.player.RenderablePlayerEntity;
import com.pigicial.wikirenderer.render.item.BlockStateRenderable;
import com.pigicial.wikirenderer.render.item.ItemRenderable;
import com.pigicial.wikirenderer.render.item.TooltipRenderable;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.screen.ScreenSchedulerAndSaver;
import com.pigicial.wikirenderer.util.AreaSelectionHelper;
import com.pigicial.wikirenderer.util.Translate;
import io.wispforest.owo.ui.component.EntityComponent;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class WikiRendererCommand {

    private static final SuggestionProvider<FabricClientCommandSource> CLIENT_SUMMONABLE_ENTITIES = (context, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.stream().filter(EntityType::canSummon),
            builder, EntityType::getKey, entityType -> Component.translatable(Util.makeDescriptionId("entity", EntityType.getKey(entityType)))
    );

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext access) {
        dispatcher.register(literal("wikirender")
                .executes(WikiRendererCommand::showRootNodeHelp)
                .then(literal("reopen")
                        .executes(WikiRendererCommand::reopenSavedMenu))
                .then(literal("area")
                        .then(literal("island")
                                .then(argument("chunk_cube_size", IntegerArgumentType.integer(4, 64))
                                        .then(argument("distance_limit", IntegerArgumentType.integer(1, 2000))
                                                .executes(WikiRendererCommand::renderSurroundingConnectedMiniChunks))))
                        .then(literal("pos")
                                .then(argument("start", BlockPosArgument.blockPos())
                                        .then(argument("end", BlockPosArgument.blockPos())
                                                .executes(WikiRendererCommand::renderAreaWithArguments))))
                        .executes(WikiRendererCommand::renderAreaSelection))
                .then(literal("block")
                        .executes(WikiRendererCommand::renderTargetedBlock)
                        .then(argument("block", BlockStateArgument.block(access))
                                .executes(WikiRendererCommand::renderBlockWithArgument)))
                .then(literal("entity")
                        .executes(WikiRendererCommand::renderTargetedEntity)
                        .then(argument("entity", ResourceArgument.resource(access, Registries.ENTITY_TYPE))
                                .suggests(CLIENT_SUMMONABLE_ENTITIES)
                                .executes(WikiRendererCommand::renderEntityWithoutNbt)
                                .then(argument("nbt", CompoundTagArgument.compoundTag())
                                        .executes(WikiRendererCommand::renderEntityWithNbt))))
                .then(literal("player")
                        .executes(WikiRendererCommand::renderSelf)
                        .then(literal("name")
                                .then(argument("name", StringArgumentType.string())
                                        .executes(c -> renderPlayerByName(c, false))
                                        .then(argument("nbt", CompoundTagArgument.compoundTag())
                                                .executes(c -> renderPlayerByName(c, true)))))
                        .then(literal("texture")
                                .then(argument("texture", StringArgumentType.string())
                                        .executes(c -> renderPlayerFromTexture(c, false))
                                        .then(argument("nbt", CompoundTagArgument.compoundTag())
                                                .executes(c -> renderPlayerFromTexture(c, true)))))
                        .then(literal("uuid")
                                .then(argument("uuid", UuidArgument.uuid())
                                        .executes(c -> renderPlayerByUuid(c, false))
                                        .then(argument("nbt", CompoundTagArgument.compoundTag())
                                                .executes(c -> renderPlayerByUuid(c, true))))))
                .then(literal("item")
                        .executes(WikiRendererCommand::renderHeldItem)
                        .then(argument("item", ItemArgument.item(access))
                                .executes(WikiRendererCommand::renderItemWithArgument)))
                .then(literal("tooltip")
                        .executes(WikiRendererCommand::renderHeldItemTooltip)
                        .then(argument("item", ItemArgument.item(access))
                                .executes(WikiRendererCommand::renderItemTooltipWithArgument)))
                .then(literal("namespace")
                        .then(argument("namespace", NamespaceArgumentType.namespace())
                                .then(argument("task", new RenderTaskArgumentType())
                                        .executes(WikiRendererCommand::renderNamespace))))
                .then(literal("creative_tab")
                        .then(argument("itemgroup", ItemGroupArgumentType.itemGroup())
                                .then(argument("task", new RenderTaskArgumentType())
                                        .executes(WikiRendererCommand::renderCreativeTab))))
                .then(literal("tag")
                        .then(argument("tag", new TagArgumentType(access))
                                .then(argument("task", new RenderTaskArgumentType())
                                        .executes(WikiRendererCommand::renderTagContents))))
                .then(literal("unsafe")
                        .then(literal("enable")
                                .executes(WikiRendererCommand::enableUnsafe))
                        .then(literal("disable")
                                .executes(WikiRendererCommand::disableUnsafe))));
    }

    private static int reopenSavedMenu(CommandContext<FabricClientCommandSource> context) {
        if (ScreenSchedulerAndSaver.getSavedScreen() == null) {
            Translate.commandFeedback(context, "no_saved_menu");
        } else {
            ScreenSchedulerAndSaver.schedule(ScreenSchedulerAndSaver.getSavedScreen());
        }
        return 0;
    }

    private static int showRootNodeHelp(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();

        source.sendFeedback(Translate.prefixed(Translate.make("version", Component.literal(WikiRenderer.VERSION).withStyle(ChatFormatting.DARK_GRAY)).withStyle(ChatFormatting.GRAY)));
        source.sendFeedback(Translate.prefixed(Translate.make("command_hint").withStyle(
                style -> style
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://docs.wispforest.io/isometric-renders/slash_isorender/")))
                        .applyFormat(ChatFormatting.UNDERLINE)
                        .applyFormat(ChatFormatting.GRAY)
        )));
        return 0;
    }

    private static int disableUnsafe(CommandContext<FabricClientCommandSource> context) {
        GlobalProperties.UNSAFE.set(false);
        Translate.commandFeedback(context, "unsafe_disabled");
        return 0;
    }

    private static int enableUnsafe(CommandContext<FabricClientCommandSource> context) {
        GlobalProperties.UNSAFE.set(true);
        Translate.commandFeedback(context, "unsafe_enabled");
        return 0;
    }

    private static int renderSelf(CommandContext<FabricClientCommandSource> context) {
        LocalPlayer clientPlayer = Minecraft.getInstance().player;

        ScreenSchedulerAndSaver.schedule(new RenderScreen(EntityRenderable.copyAsRenderable(clientPlayer)));
        return 0;
    }

    private static int renderPlayerByName(CommandContext<FabricClientCommandSource> context, boolean useNbt) {
        String name = StringArgumentType.getString(context, "name");
        GameProfile gameProfile = new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name);

        RenderablePlayerEntity player = new RenderablePlayerEntity(gameProfile, ProfileFetchMode.NAME);

        if (useNbt) {
            CompoundTag playerNbt = CompoundTagArgument.getCompoundTag(context, "nbt");
            try (ProblemReporter.ScopedCollector logging = new ProblemReporter.ScopedCollector(player.problemPath(), WikiRenderer.LOGGER)) {
                player.load(TagValueInput.create(logging, player.registryAccess(), playerNbt));
            }
        }

        ScreenSchedulerAndSaver.schedule(new RenderScreen(
                new EntityRenderable(null, player)
        ));

        return 0;
    }

    private static int renderPlayerByUuid(CommandContext<FabricClientCommandSource> context, boolean useNbt) {
        UUID id = context.getArgument("uuid", UUID.class);
        GameProfile gameProfile = new GameProfile(id, id.toString());

        RenderablePlayerEntity player = new RenderablePlayerEntity(gameProfile, ProfileFetchMode.UUID);

        if (useNbt) {
            CompoundTag playerNbt = CompoundTagArgument.getCompoundTag(context, "nbt");
            try (ProblemReporter.ScopedCollector logging = new ProblemReporter.ScopedCollector(player.problemPath(), WikiRenderer.LOGGER)) {
                player.load(TagValueInput.create(logging, player.registryAccess(), playerNbt));
            }

            System.out.println("player items = " + playerNbt + ", " + player.getItemInHand(InteractionHand.MAIN_HAND));
        }

        ScreenSchedulerAndSaver.schedule(new RenderScreen(
                new EntityRenderable(null, player)
        ));

        return 0;
    }

    private static int renderPlayerFromTexture(CommandContext<FabricClientCommandSource> context, boolean useNbt) {
        String texture = StringArgumentType.getString(context, "texture");
        String textureUrl = "https://textures.minecraft.net/texture/" + texture;
        String json = String.format("{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}", textureUrl);
        String base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        Multimap<String, Property> propertyMap = ArrayListMultimap.create();
        propertyMap.put("textures", new Property("textures", base64));

        byte[] textureHash = texture.getBytes(StandardCharsets.UTF_8);
        GameProfile gameProfile = new GameProfile(UUID.nameUUIDFromBytes(textureHash), "IsometricMannequin/" + texture, new PropertyMap(propertyMap));

        RenderablePlayerEntity player = new RenderablePlayerEntity(gameProfile, ProfileFetchMode.TEXTURE);

        if (useNbt) {
            CompoundTag playerNbt = CompoundTagArgument.getCompoundTag(context, "nbt");
            try (ProblemReporter.ScopedCollector logging = new ProblemReporter.ScopedCollector(player.problemPath(), WikiRenderer.LOGGER)) {
                player.load(TagValueInput.create(logging, player.registryAccess(), playerNbt));
            }
        }

        ScreenSchedulerAndSaver.schedule(new RenderScreen(
                new EntityRenderable(null, player)
        ));

        return 0;
    }

    private static int renderTagContents(CommandContext<FabricClientCommandSource> context) {
        TagArgumentType.TagArgument tag = TagArgumentType.getTag("tag", context);
        RenderTaskArgumentType.getTask("task", context).action.accept(
                "tag_" + tag.id().getNamespace() + "/" + tag.id().getPath(),
                tag.entries().stream()
                        .map(Holder::value)
                        .map(Item::getDefaultInstance)
                        .toList()
        );
        return 0;
    }

    private static int renderCreativeTab(CommandContext<FabricClientCommandSource> context) {
        BatchRenderTask task = RenderTaskArgumentType.getTask("task", context);
        withItemGroupFromContext(context, (itemStacks, name) -> task.action.accept(name, itemStacks));
        return 0;
    }

    private static int renderNamespace(CommandContext<FabricClientCommandSource> context) {
        NamespaceArgumentType.Namespace namespace = NamespaceArgumentType.getNamespace("namespace", context);
        RenderTaskArgumentType.getTask("task", context).action.accept("namespace_" + namespace.name(), namespace.getContent());
        return 0;
    }

    private static int renderItemWithArgument(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {
        ScreenSchedulerAndSaver.schedule(new RenderScreen(
                new ItemRenderable(ItemArgument.getItem(context, "item").createItemStack(1, false))
        ));
        return 0;
    }

    private static int renderHeldItem(CommandContext<FabricClientCommandSource> context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return 0;

        ItemStack mainHandItem = player.getMainHandItem();
        if (mainHandItem.isEmpty()) {
            Translate.commandError(context, "no_held_item");
            return 0;
        }

        ScreenSchedulerAndSaver.schedule(new RenderScreen(new ItemRenderable(mainHandItem)));
        return 0;
    }

    private static int renderItemTooltipWithArgument(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {
        ScreenSchedulerAndSaver.schedule(new RenderScreen(
                new TooltipRenderable(ItemArgument.getItem(context, "item").createItemStack(1, false))
        ));
        return 0;
    }

    private static int renderHeldItemTooltip(CommandContext<FabricClientCommandSource> context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return 0;

        ItemStack mainHandItem = player.getMainHandItem();
        if (mainHandItem.isEmpty()) {
            Translate.commandError(context, "no_held_item");
            return 0;
        }

        ScreenSchedulerAndSaver.schedule(new RenderScreen(new TooltipRenderable(mainHandItem)));
        return 0;
    }

    private static int renderEntityWithNbt(CommandContext<FabricClientCommandSource> context) {
        CompoundTag entityNbt = CompoundTagArgument.getCompoundTag(context, "nbt");
        Holder.Reference<EntityType<?>> entityReference = context.getArgument("entity", Holder.Reference.class);

        EntityRenderable renderable = EntityRenderable.of(entityReference.value(), entityNbt);
        if (renderable != null) {
            ScreenSchedulerAndSaver.schedule(new RenderScreen(renderable));
        }

        return 0;
    }

    private static int renderEntityWithoutNbt(CommandContext<FabricClientCommandSource> context) {
        Holder.Reference<EntityType<?>> entityReference = context.getArgument("entity", Holder.Reference.class);

        EntityRenderable renderable = EntityRenderable.of(entityReference.value(), null);
        if (renderable != null) {
            ScreenSchedulerAndSaver.schedule(new RenderScreen(renderable));
        }

        return 0;
    }

    public static int renderTargetedEntity(CommandContext<FabricClientCommandSource> context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return 0;

        AttackRange attackRange = new AttackRange(0, 10, 0, 10, 0.3f, 10);
        HitResult closesetHit = attackRange.getClosesetHit(player, 1.0f, e -> {

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

        if (!(closesetHit instanceof EntityHitResult entityHitResult)) {
            Translate.commandError(context, "no_entity");
            return 0;
        }

        Entity targetEntity = entityHitResult.getEntity();
        ScreenSchedulerAndSaver.schedule(new RenderScreen(
                EntityRenderable.copyAsRenderable(targetEntity)
        ));

        return 0;
    }

    private static int renderBlockWithArgument(CommandContext<FabricClientCommandSource> context) {
        BlockInput stateArg = context.getArgument("block", BlockInput.class);
        BlockState state = stateArg.getState();
        CompoundTag data = ((BlockInputAccessor) stateArg).wikirenderer$getTag();

        BlockStateRenderable renderable = BlockStateRenderable.of(state, data);
        if (renderable == null) return 0;

        ScreenSchedulerAndSaver.schedule(new RenderScreen(renderable));
        return 0;
    }

    private static int renderTargetedBlock(CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return 0;

        if (client.hitResult instanceof BlockHitResult blockHitResult) {
            BlockPos hitPos = blockHitResult.getBlockPos();
            BlockStateRenderable renderable = BlockStateRenderable.copyOf(client.level, hitPos);
            if (renderable != null) {
                ScreenSchedulerAndSaver.schedule(new RenderScreen(renderable));
            }
        } else {
            Translate.commandError(context, "no_block");
        }

        return 0;
    }

    private static int renderAreaWithArguments(CommandContext<FabricClientCommandSource> context) {
        WorldCoordinates startArg = context.getArgument("start", WorldCoordinates.class);
        WorldCoordinates endArg = context.getArgument("end", WorldCoordinates.class);

        BlockPos pos1 = getPosFromArgument(startArg, context.getSource());
        BlockPos pos2 = getPosFromArgument(endArg, context.getSource());

        ScreenSchedulerAndSaver.schedule(new RenderScreen(
                AreaRenderable.of(pos1, pos2)
        ));

        return 0;
    }

    private static int renderSurroundingConnectedMiniChunks(CommandContext<FabricClientCommandSource> context) {
        Integer chunkSize = context.getArgument("chunk_cube_size", Integer.class);
        Integer distanceLimit = context.getArgument("distance_limit", Integer.class);

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 0;
        }

        Translate.commandFeedback(context, "scanning_chunks", Component.literal(String.valueOf(chunkSize)), Component.literal(String.valueOf(chunkSize)), Component.literal(String.valueOf(distanceLimit)));
        AreaRenderable area = AreaRenderable.of(context, player.blockPosition(), chunkSize, distanceLimit);
        if (area == null) {
            return 0;
        }

        ScreenSchedulerAndSaver.schedule(new RenderScreen(area));
        return 0;
    }

    private static int renderAreaSelection(CommandContext<FabricClientCommandSource> context) {
        if (!AreaSelectionHelper.tryOpenScreen()) {
            Translate.commandError(context, "incomplete_selection");
        }

        return 0;
    }

    private static <S> void withItemGroupFromContext(CommandContext<S> context, BiConsumer<List<ItemStack>, String> action) {
        CreativeModeTab itemGroup = ItemGroupArgumentType.getItemGroup("itemgroup", context);
        List<ItemStack> stacks = new ArrayList<>(itemGroup.getDisplayItems());
        action.accept(stacks, "creative-tab_" + BuiltInRegistries.CREATIVE_MODE_TAB.getKey(itemGroup).toShortLanguageKey());
    }

    public static BlockPos getPosFromArgument(WorldCoordinates argument, FabricClientCommandSource source) {
        Vec3 pos = source.getPlayer().trackingPosition();

        return BlockPos.containing(argument.x().get(pos.x), argument.y().get(pos.y), argument.z().get(pos.z));
    }
}
