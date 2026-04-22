package com.pigicial.wikirenderer.render.export.ffmpeg;

import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.render.export.ExportPathSpec;
import com.pigicial.wikirenderer.render.export.FileIO;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FFmpegDispatcher {

    public static String resolvedFFmpegPath = null;
    public static Boolean ffmpegDetected = null;
    public static boolean tryCustomPathAgain = false;
    public static CustomPathState customPathState = CustomPathState.NOT_CHECKED;
    public static boolean activelyCheckingFfmpeg = false;

    public static boolean wasFFmpegDetected() {
        return ffmpegDetected != null;
    }

    public static boolean ffmpegAvailable() {
        return ffmpegDetected != null && ffmpegDetected;
    }

    public static CompletableFuture<Boolean> detectFFmpeg() {
        if (ffmpegDetected != null && !tryCustomPathAgain) {
            return CompletableFuture.completedFuture(ffmpegDetected);
        }

        if (activelyCheckingFfmpeg) {
            return CompletableFuture.completedFuture(ffmpegDetected != null && ffmpegDetected);
        }

        activelyCheckingFfmpeg = true;
        return CompletableFuture.supplyAsync(() -> {
            String path = findFFmpegPath();
            try {
                Process process = new ProcessBuilder(path, "-version")
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();

                process.onExit().join();
                String output = new String(process.getInputStream().readAllBytes());

                if (customPathState == CustomPathState.CHECKING) {
                    customPathState = CustomPathState.FOUND;
                }

                WikiRenderer.LOGGER.info("FFmpeg detected at {}, version: {}", path, output.split(" ")[2]);
                resolvedFFmpegPath = path;
                return true;
            } catch (Exception exception) {
                WikiRenderer.LOGGER.info("Did not detect FFmpeg for reason: {}", exception.getMessage());
                if (customPathState == CustomPathState.CHECKING) {
                    customPathState = CustomPathState.NOT_FOUND;
                }
                return false;
            }
        }, Util.backgroundExecutor()).whenComplete((result, throwable) -> {
            ffmpegDetected = (throwable == null && result);
            activelyCheckingFfmpeg = false;
        });
    }

    public static String findFFmpegPath() {
        GlobalProperties globalProperties = GlobalProperties.get();
        if (globalProperties.useCustomFFmpegPath.get()) {
            customPathState = CustomPathState.CHECKING;
            File file = new File(globalProperties.customFFmpegPath);
            return file.getAbsolutePath();
        }

        customPathState = CustomPathState.NOT_CHECKED;
        String os = System.getProperty("os.name").toLowerCase();
        String binName = os.contains("win") ? "ffmpeg.exe" : "ffmpeg";

        String cmd = os.contains("win") ? "where" : "which";
        try {
            Process p = new ProcessBuilder(cmd, "ffmpeg").start();
            String foundPath = new String(p.getInputStream().readAllBytes()).trim();
            if (!foundPath.isEmpty()) {
                return foundPath.split("\n")[0].trim();
            }
        } catch (IOException ignored) {}

        String[] commonDirectories = {
                "/usr/local/bin/",
                "/opt/homebrew/bin/",
                "/usr/bin/",
                "/bin/",
                "C:\\ffmpeg\\bin\\",
                "C:\\Program Files\\ffmpeg\\bin\\"
        };

        for (String directory : commonDirectories) {
            File file = new File(directory, binName);
            if (file.exists() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }

        return binName;
    }

    public static String getResolvedOrFallbackFFmpegPath() {
        return resolvedFFmpegPath == null ? "ffmpeg" : resolvedFFmpegPath;
    }

    public static CompletableFuture<File> exportAnimation(ExportPathSpec target, Path sourcePath, Format format, AnimationHandler handler, @Nullable String cropFilter) {
        target.resolveOffset().toFile().mkdirs();

        String ffmpegPath = FFmpegDispatcher.getResolvedOrFallbackFFmpegPath();
        List<String> args = new ArrayList<>(List.of(new String[]{
                ffmpegPath,
                "-y",
                "-threads",
                String.valueOf(Math.max(1, Runtime.getRuntime().availableProcessors())),
                "-f", "image2",
                "-framerate", String.valueOf(GlobalProperties.get().exportFramerate.get()),
                "-i", "seq_%d.png"
        }));

        boolean hasCrop = cropFilter != null && !cropFilter.isBlank();
        if (format == Format.GIF) {
            args.add("-filter_complex");
            String chain1 = "format=rgba,split[split1][split2];[split1]drawbox=c=white@0.2:t=fill[bg];[bg][split2]overlay,";
            String chain2 = hasCrop ? "[0:v]" + cropFilter + "," + chain1 + "split[v1][v2];" : "[0:v]" + chain1 + "split[v1][v2];";
            args.add(chain2 + "[v1]palettegen=reserve_transparent=1:stats_mode=full[p];[v2][p]paletteuse=alpha_threshold=1:dither=bayer:bayer_scale=5");
        } else if (hasCrop) {
            // standard cropping for other formats
            args.add("-vf");
            args.add(cropFilter);
        }

        if (format.arguments.length != 0) {
            args.addAll(Arrays.asList(format.arguments));
        }

        File animationFile = target.resolveFile(format.extension);
        args.add(animationFile.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(args)
                .redirectErrorStream(true)
                .directory(sourcePath.toFile());

        try {
            Process process = pb.start();

            // Start a thread to read the output and parse frame/fps
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    parseFFmpegProgress(handler, line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                FileIO.deleteSequenceFilesFromPath(sourcePath);
                return CompletableFuture.completedFuture(animationFile);
            } else {
                throw new RuntimeException("FFmpeg failed with exit code " + exitCode);
            }
        } catch (Exception e) {
            WikiRenderer.LOGGER.error("Could not launch ffmpeg", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public static void parseFFmpegProgress(AnimationHandler handler, String line) {
        if (line.contains("frame=") && line.contains("fps=")) {
            try {
                String frame = extractValue(line, "frame=");
                String fps = extractValue(line, "fps=");
                handler.setFFmpegData(frame, fps);

            } catch (Exception ignored) {
                // FFmpeg lines can be messy, ignore malformed status updates
            }
        }
    }

    private static String extractValue(String line, String key) {
        int start = line.indexOf(key) + key.length();
        String sub = line.substring(start).trim();
        int end = sub.indexOf(" ");
        return end != -1 ? sub.substring(0, end) : sub;
    }

    public enum Format {
        APNG("apng", new String[]{"-plays", "0", "-pix_fmt", "rgba"}),
	    WEBP("webp", new String[]{"-plays", "0", "-loop", "0", "-pix_fmt", "rgba"}),
        GIF("gif", new String[]{"-plays", "0"}),
        MP4("mp4", new String[]{"-preset", "slow", "-crf", "20", "-pix_fmt", "yuv420p"}),
        MOV("mov", new String[]{"-c:v", "prores_ks", "-profile:v", "4444", "-q:v", "1", "-pix_fmt", "yuva444p10le"});

        public final String extension;
        public final String[] arguments;

        Format(String extension, String[] arguments) {
            this.extension = extension;
            this.arguments = arguments;
        }

        public Format next() {
            return switch (this) {
                case MP4 -> MOV;
                case MOV -> APNG;
                case APNG -> WEBP;
	            case WEBP -> GIF;
                case GIF -> MP4;
            };
        }
    }

}
