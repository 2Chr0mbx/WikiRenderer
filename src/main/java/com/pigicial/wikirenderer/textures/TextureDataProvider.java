package com.pigicial.wikirenderer.textures;

import com.pigicial.wikirenderer.screen.WikiRendererUI;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.util.Translate;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Map;

public interface TextureDataProvider {

    @NotNull
    Map<String, MinecraftTexturesPayload> getTextureData();

    default void buildTextureGrabSection(RenderScreen screen, FlowLayout layout) {
        Map<String, MinecraftTexturesPayload> foundTextures = this.getTextureData();
        if (foundTextures.isEmpty()) return;

        WikiRendererUI.sectionHeader(layout, "player_textures", true);

        for (Map.Entry<String, MinecraftTexturesPayload> entry : foundTextures.entrySet()) {
            String textureContext = entry.getKey(); // player or equipment
            MinecraftTexturesPayload payload = entry.getValue();

            for (Map.Entry<MinecraftProfileTexture.Type, MinecraftProfileTexture> textureEntry : payload.textures().entrySet()) {
                MinecraftProfileTexture.Type type = textureEntry.getKey();
                MinecraftProfileTexture texture = textureEntry.getValue();

                try (WikiRendererUI.RowBuilder builder = WikiRendererUI.row(layout)) {
                    Component contextText = Translate.gui("texture_context." + textureContext);
                    Component typeText = Translate.gui("texture_type." + type.name().toLowerCase());
                    Component mergedText = Component.literal(contextText.getString() + " " + typeText.getString()); // jank

                    builder.row.child(UIComponents.label(mergedText).margins(Insets.of(0, 0, 0, 10)));

                    builder.row.child(UIComponents.button(Translate.gui("open_url"), button -> Util.getPlatform().openUri(texture.getUrl())));
                    if (!GraphicsEnvironment.isHeadless()) {
                        builder.row.child(UIComponents.button(Translate.gui("copy_texture_id"), button -> {
                            screen.notify(Translate.gui("copied_texture_id_to_clipboard"));

                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(texture.getHash()), (clipboard, contents) -> {});
                        }));

                        builder.row.child(UIComponents.button(Translate.gui("copy_json"), button -> {
                            screen.notify(Translate.gui("copied_json_to_clipboard"));

                            String json = PlayerTextureUtils.GSON.toJson(payload);
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(json), (clipboard, contents) -> {});
                        }));
                    }
                }
            }
        }
    }
}
