package net.fabricmc.fabric.impl.client.indigo.renderer.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.renderer.v1.render.BlockVertexConsumerProvider;
import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoLuminanceFix;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

public class WorldMesherRenderContext extends AbstractTerrainRenderContext {

    private final BlockAndTintGetter blockView;
    private final BlockVertexConsumerProvider bufferFunc;

    private final RandomSource random = RandomSource.createNewThreadLocalInstance();

    public WorldMesherRenderContext(BlockAndTintGetter blockView, BlockVertexConsumerProvider bufferFunc) {
        this.blockView = blockView;
        this.bufferFunc = bufferFunc;

        this.blockInfo.prepareForWorld(blockView, true);
    }

    public void tessellateBlock(BlockState blockState, BlockPos blockPos, final BlockStateModel model, PoseStack matrixStack) {
        try {
            Vec3 offset = blockState.getOffset(blockPos);
            matrixStack.translate(offset.x, offset.y, offset.z);

            matrices = matrixStack.last();

            random.setSeed(blockState.getSeed(blockPos));

            prepare(blockPos, blockState);
            model.emitQuads(getEmitter(), blockInfo.blockView, blockInfo.blockPos, blockInfo.blockState, random, blockInfo::shouldCullSide);
        } catch (Throwable throwable) {
            CrashReport crashReport = CrashReport.forThrowable(throwable, "Tessellating block in WorldMesher mesh");
            CrashReportCategory crashReportSection = crashReport.addCategory("Block being tessellated");
            CrashReportCategory.populateBlockDetails(crashReportSection, blockView, blockPos, blockState);
            throw new ReportedException(crashReport);
        }
    }

    @Override
    protected @NonNull LightDataProvider createLightDataProvider(@NonNull BlockRenderInfo blockInfo) {
        // TODO: Use a cache whenever vanilla would use a cache (BrightnessCache.enabled)
        return new LightDataProvider() {
            @Override
            public int light(@NonNull BlockPos pos, @NonNull BlockState state) {
                return LevelRenderer.getLightColor(LevelRenderer.BrightnessGetter.DEFAULT, blockInfo.blockView, state, pos);
            }

            @Override
            public float ao(@NonNull BlockPos pos, @NonNull BlockState state) {
                return AoLuminanceFix.INSTANCE.apply(blockInfo.blockView, pos, state);
            }
        };
    }

    @Override
    protected @NonNull VertexConsumer getVertexConsumer(@NonNull ChunkSectionLayer layer) {
        return this.bufferFunc.getBuffer(layer);
    }
}
