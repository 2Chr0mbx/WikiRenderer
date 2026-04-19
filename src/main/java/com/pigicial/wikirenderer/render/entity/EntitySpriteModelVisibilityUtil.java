package com.pigicial.wikirenderer.render.entity;

import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.mixin.access.ModelPartAccessor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class EntitySpriteModelVisibilityUtil {

    public static void hideNonHeadParts(LivingEntityRenderer<?, ?, ?> livingEntityRenderer, List<Runnable> toggleCallbacks) {
        EntityModel<?> model = livingEntityRenderer.getModel();
        EntitySpriteModelVisibilityUtil.hideNonHeadParts(model, toggleCallbacks);
    }

    public static void hideNonHeadParts(EntityModel<?> model, List<Runnable> toggleCallbacks) {
        ModelPart root = model.root();

        Function<String, @Nullable ModelPart> partLookup = root.createPartLookup();
        if (partLookup.apply("head") != null) {
            EntitySpriteModelVisibilityUtil.hideNonHeadParts(toggleCallbacks, root, "head");
        } else if (partLookup.apply("center_head") != null) {
            // wither
            EntitySpriteModelVisibilityUtil.hideNonHeadParts(toggleCallbacks, root, "center_head");
        } else if (partLookup.apply("body") != null) {
            EntitySpriteModelVisibilityUtil.hideNonHeadParts(toggleCallbacks, root, "body");
        }
    }

    public static void hideNonHeadParts(List<Runnable> toggleCallbacks, ModelPart part, String headType) {
        Map<String, ModelPart> childParts = ((ModelPartAccessor) (Object) part).wikirenderer$getChildren();
        childParts.forEach((identifier, modelPart) -> {
            if (!identifier.equals(headType)) {
                boolean previouslySkippedDraw = modelPart.skipDraw;
                modelPart.skipDraw = true;
                toggleCallbacks.add(() -> modelPart.skipDraw = previouslySkippedDraw);
                hideNonHeadParts(toggleCallbacks, modelPart, headType);
            }
        });
    }

    public static void hideOrShowNonHeadParts(EntityModel<?> model, List<Runnable> toggleCallbacks) {
        if (model == null) return;
        if (WikiRenderer.inSpriteEntityDraw) {
            if (toggleCallbacks.isEmpty()) {
                EntitySpriteModelVisibilityUtil.hideNonHeadParts(model, toggleCallbacks);
            }
        } else {
            toggleCallbacks.forEach(Runnable::run);
            toggleCallbacks.clear();
        }
    }
}
