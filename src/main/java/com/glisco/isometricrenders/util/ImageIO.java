package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.property.GlobalProperties;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageIO {

    private static final AtomicInteger TASK_COUNT = new AtomicInteger(0);

    public static CompletableFuture<File> save(NativeImage image, ExportPathSpec path) {
        CompletableFuture<File> future = new CompletableFuture<>();

        TASK_COUNT.incrementAndGet();
        ForkJoinPool.commonPool().submit(() -> {
            File imageFile = path.resolveFile("png");

            imageFile.getParentFile().mkdirs();

            try {
                image.writeToFile(imageFile);
                IsometricRenders.LOGGER.info("Image " + imageFile.getAbsolutePath() + " saved");
                future.complete(imageFile);
            } catch (IOException e) {
                IsometricRenders.LOGGER.warn("Could not save image " + imageFile.getAbsolutePath(), e);
                future.completeExceptionally(e);
            } finally {
                TASK_COUNT.decrementAndGet();
            }
        });

        return future;
    }

    public static int taskCount() {
        return TASK_COUNT.get();
    }

    public static Component progressText() {
        int jobs = taskCount();
        if (jobs == 0) return Translate.gui("exporter.idle");
        return Translate.gui("exporter.jobs", jobs);
    }

    public static Path next(Path input) {
        String filename = input.getFileName().toString();

        int separatorIndex = filename.lastIndexOf('.');
        if (separatorIndex == -1) separatorIndex = filename.length();

        String name = filename.substring(0, separatorIndex);
        String extension = filename.substring(separatorIndex);

        Path path = input.getParent();

        Path currentPath = path.resolve(join(name, extension, 0));
        Path lastPath = currentPath;

        for (int i = 1; Files.exists(currentPath); i++) {
            lastPath = currentPath;
            currentPath = path.resolve(join(name, extension, i));
        }

        return GlobalProperties.overwriteLatest.get() ? lastPath : currentPath;
    }

    private static String join(String filename, String extension, int index) {
        return index == 0
                ? filename + extension
                : filename + "_" + index + (extension.isEmpty() ? "" : "_" + extension);
    }

}
