package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.IsometricRenders;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RenderPipelineOverrider {

    private static final Map<String, RenderPipeline> MODIFIED_PIPELINES = new HashMap<>();

    public static RenderPipeline getDepthTestingVariant(RenderPipeline original) {
        String location = original.getLocation().toString();
        if (MODIFIED_PIPELINES.containsKey(location)) {
            return MODIFIED_PIPELINES.get(location);
        } else {
            RenderPipeline.Builder builder = RenderPipeline.builder()
                    .withLocation(Objects.requireNonNull(Identifier.tryBuild(IsometricRenders.MOD_ID, original.getLocation().getPath() + "_no_depth")))
                    .withFragmentShader(original.getFragmentShader())
                    .withVertexShader(original.getVertexShader())
                    .withVertexFormat(original.getVertexFormat(), original.getVertexFormatMode())
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST) // default
                    .withPolygonMode(original.getPolygonMode())
                    .withCull(original.isCull())
                    .withColorWrite(original.isWriteColor())
                    .withDepthWrite(original.isWriteDepth())
                    .withDepthBias(original.getDepthBiasScaleFactor(), original.getDepthBiasConstant());

            ShaderDefines shaderDefines = original.getShaderDefines();
            shaderDefines.flags().forEach(builder::withShaderDefine);
            for (Map.Entry<String, String> entry : shaderDefines.values().entrySet()) {
                builder.withShaderDefine(entry.getKey(), Float.parseFloat(entry.getValue()));
            }

            for (RenderPipeline.UniformDescription uniform : original.getUniforms()) {
                builder.withUniform(uniform.name(), uniform.type());
            }

            original.getSamplers().forEach(builder::withSampler);
            original.getBlendFunction().ifPresent(builder::withBlend);

            RenderPipeline modifiedPipeline = builder.build();
            MODIFIED_PIPELINES.put(location, modifiedPipeline);
            return modifiedPipeline;
        }

    }
}
