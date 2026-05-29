package com.pigicial.wikirenderer.render.export.animation.gifski;

import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.render.export.ExportPathSpec;
import com.pigicial.wikirenderer.render.export.FileIO;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GifskiDispatcher {
    private static boolean gifskiPathMade = false;
    private static boolean activelySettingUpGifski = false;
    private static String cachedTempPath = null;

    public static boolean wasGifskiCopiedToTempPath() {
        return gifskiPathMade;
    }

    public static boolean isGifskiAvailable() {
        return cachedTempPath != null;
    }

    public static String getGifskiPathMade() {
        return cachedTempPath;
    }

    public static CompletableFuture<String> createGifskiTemporaryPath() {
        if (cachedTempPath != null || activelySettingUpGifski) {
            return CompletableFuture.completedFuture(cachedTempPath);
        }

        activelySettingUpGifski = true;
        return CompletableFuture.supplyAsync(() -> {
            String os = System.getProperty("os.name").toLowerCase();
            String resourcePath;
            String fileName = "gifski";

            if (os.contains("win")) {
                resourcePath = "/assets/wikirenderer/gifski/win/gifski.exe";
                fileName = "gifski.exe";
            } else if (os.contains("mac")) {
                resourcePath = "/assets/wikirenderer/gifski/mac/gifski";
            } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                resourcePath = "/assets/wikirenderer/gifski/linux/gifski";
            } else {
                gifskiPathMade = false;
                activelySettingUpGifski = false;
                throw new UnsupportedOperationException("what goofy operating system do you even own: " + os);
            }

            InputStream is = GifskiDispatcher.class.getResourceAsStream(resourcePath);
            if (is == null) {
                gifskiPathMade = false;
                activelySettingUpGifski = false;
                throw new RuntimeException("Could not find Gifski binary in JAR at: " + resourcePath);
            }

            try {
                Path tempFile = Files.createTempFile("minecraft_wikirenderer_mod_bundled_gifski_", "_" + fileName);
                tempFile.toFile().deleteOnExit();

                try (is) {
                    Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }

                if (!tempFile.toFile().setExecutable(true)) { // required for mac/linux
                    System.err.println("Warning: Failed to set executable permissions on " + tempFile);
                }

                cachedTempPath = tempFile.toFile().getAbsolutePath();
                gifskiPathMade = true;
                activelySettingUpGifski = false;
                return cachedTempPath;
            } catch (IOException e) {
                WikiRenderer.LOGGER.warn("Could not generate temporary gifski binary", e);
                activelySettingUpGifski = false;
                gifskiPathMade = false;
                return null;
            }
        });
    }

    public static CompletableFuture<File> exportAnimation(ExportPathSpec target, Path sourcePath) {
        target.resolveOffset().toFile().mkdirs();

        String gifskiPath = GifskiDispatcher.getGifskiPathMade();
        assert gifskiPath != null : "rur row";

        File animationFile = target.resolveFile("gif");
        GlobalProperties globalProperties = GlobalProperties.get();

        List<String> args = new ArrayList<>(List.of(new String[]{
                gifskiPath,
                "-o",
                "\"" + animationFile.getAbsolutePath() + "\"",
                "\"" + sourcePath.toFile().getAbsolutePath() + "/seq_*.png\"",
                "--fps", String.valueOf(globalProperties.exportFramerate.get()),
                "--quality", String.valueOf(globalProperties.gifskiQuality.get()),
        }));

        WikiRenderer.LOGGER.info("Starting Gifski process using command " + String.join(" ", args));

        ProcessBuilder pb = new ProcessBuilder(args)
                .redirectErrorStream(true)
                .directory(sourcePath.toFile());

        try {
            Process process = pb.start();

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                FileIO.deleteSequenceFilesFromPath(sourcePath);
                return CompletableFuture.completedFuture(animationFile);
            } else {
                throw new RuntimeException("Gifski failed with exit code " + exitCode);
            }
        } catch (Exception e) {
            WikiRenderer.LOGGER.error("Could not launch Gifski", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
