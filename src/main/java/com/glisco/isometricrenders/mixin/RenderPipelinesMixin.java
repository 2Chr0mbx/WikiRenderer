package com.glisco.isometricrenders.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Set specific render pipelines' depth test to default, just so the renderables can properly render in the screen.
// Easily the most cursed Mixin I've ever seen, let alone written, in my life.
@Mixin(RenderPipelines.class)
public class RenderPipelinesMixin {
	// TODO(Ravel): remapper for com.llamalad7.mixinextras.expression.Expression is not implemented
// TODO(Ravel): remapper for com.llamalad7.mixinextras.expression.Expression is not implemented
// GUI
	@Definition(id = "withFragmentShader", method = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;withFragmentShader(Ljava/lang/String;)Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;", remap = false)
	@Definition(id = "withDepthTestFunction", method = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;withDepthTestFunction(Lcom/mojang/blaze3d/platform/DepthTestFunction;)Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;", remap = false)
	@Expression("?.withFragmentShader('core/gui').?(?).?(?, ?).withDepthTestFunction(?)")
	@WrapOperation(method = "<clinit>", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
	private static RenderPipeline.Builder noSetGui(RenderPipeline.Builder instance, DepthTestFunction depthTestFunction, Operation<RenderPipeline.Builder> original) {
		return instance;
	}

// POSITION_TEX_COLOR_SNIPPET
	@Definition(id = "withFragmentShader", method = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;withFragmentShader(Ljava/lang/String;)Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;", remap = false)
	@Definition(id = "withDepthTestFunction", method = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;withDepthTestFunction(Lcom/mojang/blaze3d/platform/DepthTestFunction;)Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;", remap = false)
	@Expression("?.withFragmentShader('core/position_tex_color').?(?).?(?).?(?, ?).withDepthTestFunction(?)")
	@WrapOperation(method = "<clinit>", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
	private static RenderPipeline.Builder noSetPositionTexColor(RenderPipeline.Builder instance, DepthTestFunction depthTestFunction, Operation<RenderPipeline.Builder> original) {
		return instance;
	}

// GUI_TEXT_SNIPPET
	@Definition(id = "builder", method = "Lcom/mojang/blaze3d/pipeline/RenderPipeline;builder([Lcom/mojang/blaze3d/pipeline/RenderPipeline$Snippet;)Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;", remap = false)
	@Definition(id = "withDepthTestFunction", method = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;withDepthTestFunction(Lcom/mojang/blaze3d/platform/DepthTestFunction;)Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;", remap = false)
	@Definition(id = "buildSnippet", method = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;buildSnippet()Lcom/mojang/blaze3d/pipeline/RenderPipeline$Snippet;", remap = false)
	@Expression("@(builder(?).withDepthTestFunction(?)).buildSnippet()")
	@WrapOperation(method = "<clinit>", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
	private static RenderPipeline.Builder noSetGuiTextSnippet(RenderPipeline.Builder instance, DepthTestFunction depthTestFunction, Operation<RenderPipeline.Builder> original) {
		return instance;
	}

// GUI_TEXT (why does it disable depth test again after the snippet? that's annoying)
	@Definition(id = "withLocation", method = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;withLocation(Ljava/lang/String;)Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;", remap = false)
	@Definition(id = "withDepthTestFunction", method = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;withDepthTestFunction(Lcom/mojang/blaze3d/platform/DepthTestFunction;)Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;", remap = false)
	@Expression("?.withLocation('pipeline/gui_text').?(?).?(?).?(?).?(?).withDepthTestFunction(?)")
	@WrapOperation(method = "<clinit>", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
	private static RenderPipeline.Builder noSetGuiText(RenderPipeline.Builder instance, DepthTestFunction depthTestFunction, Operation<RenderPipeline.Builder> original) {
		return instance;
	}

	/*
	// CUTOUT_TERRAIN: Apply withBlend(TRANSLUCENT) during the static init chain
	@Definition(id = "withLocation", method = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;withLocation(Ljava/lang/String;)Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;", remap = false)
	@Definition(id = "withShaderDefine", method = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;withShaderDefine(Ljava/lang/String;F)Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;", remap = false)
	@Expression("?.withLocation('pipeline/cutout_terrain').withShaderDefine(?, ?)")
	@WrapOperation(method = "<clinit>", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
	private static RenderPipeline.Builder applyTranslucentToCutout(RenderPipeline.Builder instance, String define, float value, Operation<RenderPipeline.Builder> original) {
		// 1. Let the original call (.withShaderDefine) happen
		RenderPipeline.Builder builder = original.call(instance, define, 0f);

		// 2. Return the builder with the extra withBlend() call attached
		// This follows your "return instance" style but actually modifies the pipeline DNA
		return builder.withBlend(BlendFunction.TRANSLUCENT);
	}
	 */
}
