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

    default void buildTimingsSection(FlowLayout layout) {
        List<Integer> textureTimings = this.getTicksToFullyAnimate();
        if (textureTimings == null || textureTimings.isEmpty()) return;

        DecimalFormat df = new DecimalFormat("###.##");
        String timingsKey = textureTimings.size() == 1 ? "texture_timings_data_single" : "texture_timings_data_multiple";

        long seamlessLoopTicks = AnimationTimingUtil.getSeamlessLoopDuration(textureTimings);
        Component seamlessLoopTicksText = Component.literal(df.format(seamlessLoopTicks)).withStyle(ChatFormatting.GREEN);
        Component seamlessLoopSecondsText = Component.literal(df.format(seamlessLoopTicks / 20D) + "s").withStyle(ChatFormatting.AQUA);

        WikiRendererUI.text(layout, "texture_timings", true);
        WikiRendererUI.text(layout, timingsKey + "_1", false);
        WikiRendererUI.text(layout, Translate.gui(timingsKey + "_2", seamlessLoopSecondsText, seamlessLoopTicksText), 0);
        WikiRendererUI.text(layout, timingsKey + "_3", false);
    }
}
