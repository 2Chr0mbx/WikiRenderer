package com.pigicial.wikirenderer.command.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pigicial.wikirenderer.command.arguments.ItemGroupArgumentType;
import com.pigicial.wikirenderer.command.arguments.NamespaceArgumentType;
import com.pigicial.wikirenderer.command.arguments.RenderTaskArgumentType;
import com.pigicial.wikirenderer.command.arguments.TagArgumentType;
import com.pigicial.wikirenderer.render.batch.BatchRenderTask;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
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
                .then(literal("namespace")
                        .then(argument("namespace", NamespaceArgumentType.namespace())
                                .then(argument("task", new RenderTaskArgumentType())
                                        .executes(context -> {
                                            this.renderNamespace(context);
                                            return 0;
                                        }))))
                .then(literal("creative_tab")
                        .then(argument("itemgroup", ItemGroupArgumentType.itemGroup())
                                .then(argument("task", new RenderTaskArgumentType())
                                        .executes(context -> {
                                            this.renderCreativeTab(context, access);
                                            return 0;
                                        }))))
                .then(literal("tag")
                        .then(argument("tag", new TagArgumentType(access))
                                .then(argument("task", new RenderTaskArgumentType())
                                        .executes(context -> {
                                            this.renderTagContents(context);
                                            return 0;
                                        }))));
    }

    private void renderCreativeTab(CommandContext<FabricClientCommandSource> context, CommandBuildContext access) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        BatchRenderTask task = RenderTaskArgumentType.getTask("task", context);
        CreativeModeTab creativeTab = ItemGroupArgumentType.getItemGroup("itemgroup", context);
        if (creativeTab.getDisplayItems().isEmpty()) {
            CreativeModeTabs.tryRebuildTabContents(access.enabledFeatures(), true, level.registryAccess());
        }

        List<ItemStack> items = new ArrayList<>(creativeTab.getDisplayItems());

        String name = "creative-tab_" + Objects.requireNonNull(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(creativeTab)).toShortLanguageKey();
        task.action.accept(name, items);
    }

    private void renderNamespace(CommandContext<FabricClientCommandSource> context) {
        NamespaceArgumentType.Namespace namespace = NamespaceArgumentType.getNamespace("namespace", context);
        RenderTaskArgumentType.getTask("task", context).action.accept("namespace_" + namespace.name(), namespace.getContent());
    }

    private void renderTagContents(CommandContext<FabricClientCommandSource> context) {
        TagArgumentType.TagArgument tag = TagArgumentType.getTag("tag", context);
        RenderTaskArgumentType.getTask("task", context).action.accept(
                "tag_" + tag.id().getNamespace() + "/" + tag.id().getPath(),
                tag.entries().stream()
                        .map(Holder::value)
                        .map(Item::getDefaultInstance)
                        .filter(item -> !item.isEmpty())
                        .toList()
        );
    }

}
