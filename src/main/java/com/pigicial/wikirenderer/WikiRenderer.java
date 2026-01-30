package com.pigicial.wikirenderer;

import com.pigicial.wikirenderer.command.WikiRendererCommand;
import com.pigicial.wikirenderer.util.AreaSelectionHelper;
import com.pigicial.wikirenderer.util.OrthographicSort;
import com.pigicial.wikirenderer.util.FileIO;
import com.pigicial.wikirenderer.util.ParticleRestriction;
import com.pigicial.wikirenderer.components.AreaSelectionComponent;
import com.pigicial.wikirenderer.components.IOStateComponent;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.hud.Hud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class WikiRenderer implements ClientModInitializer {

	public static final String MOD_ID = "wikirenderer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String VERSION = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata().getVersion().getFriendlyString();

    public static ParticleRestriction<?> particleRestriction = ParticleRestriction.always();

    public static boolean inSpriteEntityDraw = false;
    public static boolean inRenderableDraw = false;
    public static boolean inRenderableTick = false;
    public static boolean skipWorldRender = false;

    public static RenderTarget mainTargetOverride = null;

	public static ProjectionType prevProjectionType = null;
	public static GpuBufferSlice prevProjectionMatrix = null;

	public static GpuBufferSlice renderableDrawProjectionBuffer = null;
    public static OrthographicSort orthographicSorting = null;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(WikiRendererCommand::register);
        WikiRendererKeybinds.registerKeyBinds();

        String ioStateId = "io-state";
        String areaSelectionHintId = "area-selection-hint";

        Identifier hudId = Identifier.fromNamespaceAndPath(MOD_ID, "hud");
        Hud.add(hudId, () -> Containers.verticalFlow(Sizing.content(), Sizing.content()).positioning(Positioning.absolute(20, 20)));

        HudElementRegistry.addLast(hudId, (matrixStack, tickDelta) -> {
            Minecraft client = Minecraft.getInstance();
            FlowLayout isometricHud = (FlowLayout) Hud.getComponent(hudId);
            if (isometricHud == null) return;

            IOStateComponent ioState = isometricHud.childById(IOStateComponent.class, ioStateId);
            if ((ioState == null) == (FileIO.taskCount() > 0 && client.screen == null)) {
                if (FileIO.taskCount() > 0 && client.screen == null) {
                    isometricHud.child(new IOStateComponent().positioning(Positioning.absolute(20, 20)).id(ioStateId));
                } else {
                    isometricHud.removeChild(ioState);
                }
            }

            AreaSelectionComponent selectionHint = isometricHud.childById(AreaSelectionComponent.class, areaSelectionHintId);
            if ((selectionHint == null) == AreaSelectionHelper.shouldDraw()) {
                if (AreaSelectionHelper.shouldDraw()) {
                    isometricHud.child(new AreaSelectionComponent().id(areaSelectionHintId));
                } else {
                    isometricHud.removeChild(selectionHint);
                }
            }
        });
    }

    public static void skipNextWorldRender() {
        skipWorldRender = true;
    }

	public static void beginRenderableDraw(PerspectiveProjectionMatrixBuffer matrixStore, Matrix4f projectionMatrix) {
		prevProjectionType = RenderSystem.getProjectionType();
		prevProjectionMatrix = RenderSystem.getProjectionMatrixBuffer();
		renderableDrawProjectionBuffer = matrixStore.getBuffer(projectionMatrix);
		RenderSystem.setProjectionMatrix(renderableDrawProjectionBuffer, ProjectionType.ORTHOGRAPHIC);
		inRenderableDraw = true;
    }

    public static void setSortingMethod(Matrix4f projectionMatrix, Matrix4fStack modelViewStack) {
        orthographicSorting = new OrthographicSort(projectionMatrix, modelViewStack);
    }

	public static void endRenderableDraw() {
		RenderSystem.setProjectionMatrix(prevProjectionMatrix, prevProjectionType);
		prevProjectionType = null;
		prevProjectionMatrix = null;
		renderableDrawProjectionBuffer = null;
		inRenderableDraw = false;
        orthographicSorting = null;
    }

    public static void beginRenderableTick() {
        inRenderableTick = true;
    }

    public static void endRenderableTick() {
        inRenderableTick = false;
    }
}
