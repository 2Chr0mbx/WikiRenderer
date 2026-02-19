package com.pigicial.wikirenderer.render.item;

import com.pigicial.wikirenderer.screen.WikiRendererUI;
import com.pigicial.wikirenderer.util.AnimationTimingUtil;
import com.pigicial.wikirenderer.util.Translate;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.text.DecimalFormat;
import java.util.List;

public interface AnimationTimingsProvider {
    List<Integer> getTicksToFullyAnimate();

    default String getAnimationTimingsHeaderTranslationKey() {
        return "texture_timings";
    }

    default void buildTimingsSection(FlowLayout layout) {
        try (WikiRendererUI.RowBuilder builder = WikiRendererUI.autoNewLineRow(layout)) {
            WikiRendererUI.dynamicLabel(builder.row, () -> {
                List<Integer> textureTimings = this.getTicksToFullyAnimate();
                if (textureTimings == null || textureTimings.isEmpty()) return Component.empty();

                DecimalFormat df = new DecimalFormat("###.##");
                String timingsKey = textureTimings.size() == 1 ? "texture_timings_data_single" : "texture_timings_data_multiple";

                long seamlessLoopTicks = AnimationTimingUtil.getSeamlessLoopDuration(textureTimings);
                Component seamlessLoopTicksText = Component.literal(df.format(seamlessLoopTicks)).withStyle(ChatFormatting.GREEN);
                Component seamlessLoopSecondsText = Component.literal(df.format(seamlessLoopTicks / 20D) + "s").withStyle(ChatFormatting.AQUA);
                System.out.println("timings = " + textureTimings);
                return Translate.gui(timingsKey, seamlessLoopSecondsText, seamlessLoopTicksText);
            });
        }
    }
}
