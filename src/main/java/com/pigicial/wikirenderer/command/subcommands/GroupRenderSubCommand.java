package com.pigicial.wikirenderer.command.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pigicial.wikirenderer.command.arguments.*;
import com.pigicial.wikirenderer.render.batch.BatchRenderable;
import com.pigicial.wikirenderer.render.batch.ItemBatchRenderTask;
import com.pigicial.wikirenderer.render.entity.EntityRenderable;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.screen.ScreenSchedulerAndSaver;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class GroupRenderSubCommand extends WikiRendererSubCommand {
    @Override
    public String getName() {
        return "group";
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> register(LiteralArgumentBuilder<FabricClientCommandSource> source, CommandBuildContext access) {
        return source
                .then(literal("item")
                        .then(literal("namespace")
                                .then(argument("namespace", ItemNamespaceArgumentType.namespace())
                                        .then(argument("task", new ItemBatchRenderTaskArgumentType())
                                                .executes(context -> {
                                                    this.renderItemNamespace(context);
                                                    return 0;
                                                }))))
                        .then(literal("creative_tab")
                                .then(argument("itemgroup", ItemGroupArgumentType.itemGroup())
                                        .then(argument("task", new ItemBatchRenderTaskArgumentType())
                                                .executes(context -> {
                                                    this.renderCreativeTab(context, access);
                                                    return 0;
                                                }))))
                        .then(literal("tag")
                                .then(argument("tag", new ItemTagArgumentType(access))
                                        .then(argument("task", new ItemBatchRenderTaskArgumentType())
                                                .executes(context -> {
                                                    this.renderItemTagContents(context);
                                                    return 0;
                                                })))))
                .then(literal("entity")
                        .then(literal("namespace")
                                .then(argument("namespace", EntityNamespaceArgumentType.namespace())
                                        .executes(context -> {
                                            this.renderEntityNamespace(context, false);
                                            return 0;
                                        })
                                        .then(argument("nbt", CompoundTagArgument.compoundTag())
                                                .executes(context -> {
                                                    this.renderEntityNamespace(context, true);
                                                    return 0;
                                                }))))
                        .then(literal("tag")
                                .then(argument("tag", new EntityTagArgumentType(access))
                                        .executes(context -> {
                                            this.renderEntityTagContents(context, false);
                                            return 0;
                                        })
                                        .then(argument("nbt", CompoundTagArgument.compoundTag())
                                                .executes(context -> {
                                                    this.renderEntityTagContents(context, true);
                                                    return 0;
                                                })))));
    }

    private void renderCreativeTab(CommandContext<FabricClientCommandSource> context, CommandBuildContext access) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        ItemBatchRenderTask task = ItemBatchRenderTaskArgumentType.getTask("task", context);
        CreativeModeTab creativeTab = ItemGroupArgumentType.getItemGroup("itemgroup", context);
        if (creativeTab.getDisplayItems().isEmpty()) {
            CreativeModeTabs.tryRebuildTabContents(access.enabledFeatures(), true, level.registryAccess());
        }

        List<ItemStack> items = new ArrayList<>(creativeTab.getDisplayItems());

        String name = "creative-tab_" + Objects.requireNonNull(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(creativeTab)).toShortLanguageKey();
        task.action.accept(name, items);
    }

    private void renderItemNamespace(CommandContext<FabricClientCommandSource> context) {
        ItemNamespaceArgumentType.Namespace namespace = ItemNamespaceArgumentType.getNamespace("namespace", context);
        ItemBatchRenderTaskArgumentType.getTask("task", context).action.accept("namespace_" + namespace.name(), namespace.getContent());
    }

    private void renderItemTagContents(CommandContext<FabricClientCommandSource> context) {
        ItemTagArgumentType.TagArgument tag = ItemTagArgumentType.getTag("tag", context);
        ItemBatchRenderTaskArgumentType.getTask("task", context).action.accept(
                "tag_" + tag.id().getNamespace() + "/" + tag.id().getPath(),
                tag.entries().stream()
                        .map(Holder::value)
                        .map(Item::getDefaultInstance)
                        .filter(item -> !item.isEmpty())
                        .toList()
        );
    }

    private void renderEntityTagContents(CommandContext<FabricClientCommandSource> context, boolean useNbt) {
        EntityTagArgumentType.TagArgument tag = EntityTagArgumentType.getTag("tag", context);
        String source = "tag_" + tag.id().getNamespace() + "/" + tag.id().getPath();

        List<EntityRenderable> renderables = tag.entries()
                .stream()
                .map(Holder::value)
                .filter(EntityType::canSummon)
                .map(type -> {
                    CompoundTag entityNbt = useNbt ? CompoundTagArgument.getCompoundTag(context, "nbt") : null;
                    return EntityRenderable.of(type, entityNbt);
                })
                .filter(Objects::nonNull)
                .toList();

        ScreenSchedulerAndSaver.schedule(new RenderScreen(BatchRenderable.of(source, renderables)));
    }

    private void renderEntityNamespace(CommandContext<FabricClientCommandSource> context, boolean useNbt) {
        EntityNamespaceArgumentType.Namespace namespace = EntityNamespaceArgumentType.getNamespace("namespace", context);
        String source = "namespace_" + namespace.name();

        List<EntityRenderable> renderables = namespace.getContent()
                .stream()
                .map(type -> {
                    CompoundTag entityNbt = useNbt ? CompoundTagArgument.getCompoundTag(context, "nbt") : null;
                    return EntityRenderable.of(type, entityNbt);
                })
                .filter(Objects::nonNull)
                .toList();

        ScreenSchedulerAndSaver.schedule(new RenderScreen(BatchRenderable.of(source, renderables)));
    }

}
