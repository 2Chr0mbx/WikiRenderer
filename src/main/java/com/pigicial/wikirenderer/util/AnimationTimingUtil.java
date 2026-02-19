package com.pigicial.wikirenderer.util;

import com.pigicial.wikirenderer.mixin.access.ItemStackRenderStateAccessor;
import com.pigicial.wikirenderer.mixin.access.SpriteContentsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SimpleModelWrapper;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AnimationTimingUtil {

    public static List<Integer> getTicksToFullyAnimateItem(ItemStack itemStack) {
        Identifier modelIdentifier = itemStack.get(DataComponents.ITEM_MODEL);
        if (modelIdentifier == null) {
            return List.of();
        }

        ItemStackRenderState renderState = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().appendItemLayers(
                renderState,
                itemStack,
                ItemDisplayContext.GUI,
                Minecraft.getInstance().level,
                null,
                0
        );

        List<Integer> animationCompletionTimes = new ArrayList<>();
        for (ItemStackRenderState.LayerRenderState layer : ((ItemStackRenderStateAccessor) renderState).wikirenderer$getLayers()) {
            fillTimings(layer.prepareQuadList(), animationCompletionTimes);
        }

        renderState.clear();
        return animationCompletionTimes;
    }

    public static List<Integer> getTicksToFullyAnimateBlock(BlockState state) {
        BlockStateModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        List<BlockModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(), parts); // todo: dont use random

        List<Integer> animationCompletionTimes = new ArrayList<>();
        for (BlockModelPart part : parts) {
            List<BakedQuad> quads = part instanceof SimpleModelWrapper wrapper ? wrapper.quads().getAll() : part.getQuads(null);
            fillTimings(quads, animationCompletionTimes);
        }

        return animationCompletionTimes;
    }

    private static void fillTimings(Collection<BakedQuad> quads, List<Integer> animationCompletionTimes) {
        for (BakedQuad quad : quads) {
            try (TextureAtlasSprite sprite = quad.sprite()) {
                SpriteContents.AnimatedTexture animatedTexture = ((SpriteContentsAccessor) sprite.contents()).wikirender$getAnimatedTexture();
                if (animatedTexture != null) {
                    int time = 0;
                    for (SpriteContents.FrameInfo frame : animatedTexture.frames) {
                        time += frame.time();
                    }
                    animationCompletionTimes.add(time);
                }
            }
        }
    }

    public static long getSeamlessLoopDuration(List<Integer> timings) {
        if (timings.isEmpty()) {
            return 0;
        }

        long result = timings.getFirst();
        for (int time : timings) {
            result = getLowestCommonDenominator(result, time);
        }

        return result;
    }

    private static long getLowestCommonDenominator(long a, long b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        return Math.abs(a * b) / getGreatestCommonDivisor(a, b);
    }

    private static long getGreatestCommonDivisor(long a, long b) {
        while (b > 0) {
            long temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }
}
