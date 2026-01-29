package com.pigicial.wikirenderer.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.opengl.*;

import java.util.ArrayList;
import java.util.List;

public class MemoryGuard {

    private final float maximumLoadFactor;
    private int availableVramMB = 0;
    private int availableRamMB = 0;

    public MemoryGuard(float maximumLoadFactor) {
        this.maximumLoadFactor = maximumLoadFactor;
    }

    public void update() {
        int[] data = new int[4];
        GLCapabilities capabilities = GL.getCapabilities();

        if (capabilities.GL_ATI_meminfo) GL11.glGetIntegerv(ATIMeminfo.GL_TEXTURE_FREE_MEMORY_ATI, data);
        if (capabilities.GL_NVX_gpu_memory_info) GL11.glGetIntegerv(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX, data);
        GL11.glGetError();

        this.availableVramMB = data[0] / 1024;
        this.availableRamMB = (int) ((Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()) / 1024 / 1024);
    }

    public boolean isSupported() {
        return (GL.getCapabilities().GL_ATI_meminfo || GL.getCapabilities().GL_NVX_gpu_memory_info);
    }

    public List<Component> getStatusTooltip(int memoryMB) {
        List<Component> tooltip = new ArrayList<>();

        tooltip.add(this.usageText("vram", memoryMB, this.availableVramMB(), this.canFitInVram(memoryMB)));
        tooltip.add(this.usageText("ram", memoryMB, this.availableRamMB(), this.canFitInRam(memoryMB)));

        if (!this.isSupported()) {
            tooltip.add(Component.empty());
            tooltip.add(Translate.gui("no_vram_info_warning").withStyle(ChatFormatting.YELLOW));
        }

        if (!this.canFit(memoryMB)) {
            tooltip.add(Translate.gui("vram_ignore").withStyle(ChatFormatting.GRAY));
        }

        return tooltip;
    }

    private MutableComponent usageText(String key, int usage, int available, boolean fits) {
        if (available == 0) available = 1;

        if (fits) {
            return Translate.gui(
                    key,
                    Component.literal(usage + "").withStyle(ChatFormatting.GRAY),
                    Component.literal(available + "").withStyle(ChatFormatting.GRAY),
                    Component.literal(usage * 100 / available + "%").withStyle(ChatFormatting.GRAY)
            );
        } else {
            return Translate.gui(
                    key,
                    Component.literal(usage + "").withStyle(ChatFormatting.RED),
                    Component.literal(available + "").withStyle(ChatFormatting.GRAY),
                    Component.literal(usage * 100 / available + "%").withStyle(ChatFormatting.RED)
            );
        }
    }

    public int availableVramMB() {
        return this.availableVramMB;
    }

    public int availableRamMB() {
        return this.availableRamMB;
    }

    public boolean canFit(int memoryMB) {
        return canFitInVram(memoryMB) && canFitInRam(memoryMB);
    }

    public boolean canFitInRam(int memoryMB) {
        return memoryMB / (float) this.availableRamMB <= this.maximumLoadFactor;
    }

    public boolean canFitInVram(int memoryMB) {
        return memoryMB / (float) this.availableVramMB <= this.maximumLoadFactor;
    }

}
