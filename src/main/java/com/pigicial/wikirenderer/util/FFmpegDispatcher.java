package com.pigicial.wikirenderer.util;

import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.property.GlobalProperties;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FFmpegDispatcher {

    private static Boolean ffmpegDetected = null;

    public static boolean wasFFmpegDetected() {
        return ffmpegDetected != null;
    }

    public static boolean ffmpegAvailable() {
        return ffmpegDetected != null && ffmpegDetected;
    }

    public static CompletableFuture<Boolean> detectFFmpeg() {
        if (ffmpegDetected != null) {
            return CompletableFuture.completedFuture(ffmpegDetected);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Process process = new ProcessBuilder("ffmpeg", "-version")
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();

                process.onExit().join();
                String output = new String(process.getInputStream().readAllBytes());

                WikiRenderer.LOGGER.info("FFmpeg detected, version: {}", output.split(" ")[2]);
                return true;
            } catch (IOException exception) {
                WikiRenderer.LOGGER.info("Did not detect FFmpeg for reason: {}", exception.getMessage());
                return false;
            }
        }, Util.backgroundExecutor()).whenComplete((result, throwable) -> {
            if (throwable != null) {
                ffmpegDetected = false;
                WikiRenderer.LOGGER.warn("Could not complete FFmpeg detection", throwable);
            } else {
                ffmpegDetected = result;
            }
        });
    }

    @SuppressWarnings("resource")
    public static CompletableFuture<File> assemble(ExportPathSpec target, Path sourcePath, Format format, @Nullable String cropFilter) {
        target.resolveOffset().toFile().mkdirs();

        List<String> args = new ArrayList<>(List.of(new String[]{
                "ffmpeg",
                "-y",
                "-f", "image2",
                "-framerate", String.valueOf(GlobalProperties.exportFramerate.get()),
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

        ProcessBuilder process = new ProcessBuilder(args)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .directory(sourcePath.toFile());

        try {
            return process.start().onExit().thenApply(exited -> {
                try {
                    Files.list(sourcePath)
                            .filter(path -> path.getFileName().toString().matches("seq_\\d+\\.png"))
                            .forEach(deletePath -> {
                                try {
                                    Files.delete(deletePath);
                                } catch (IOException e) {
                                    WikiRenderer.LOGGER.warn("Could not clean up sequence directory", e);
                                }
                            });
                } catch (IOException e) {
                    WikiRenderer.LOGGER.warn("Could not clean up sequence directory", e);
                }

                return animationFile;
            });
        } catch (IOException e) {
            WikiRenderer.LOGGER.error("Could not launch ffmpeg", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public enum Format {
        APNG("apng", new String[]{"-plays", "0", "-pix_fmt", "rgba"}),
	    WEBP("webp", new String[]{"-plays", "0", "-loop", "0", "-pix_fmt", "rgba"}),
        GIF("gif", new String[]{"-plays", "0"}),
        MP4("mp4", new String[]{"-preset", "slow", "-crf", "20", "-pix_fmt", "yuv420p"});

        public final String extension;
        public final String[] arguments;

        Format(String extension, String[] arguments) {
            this.extension = extension;
            this.arguments = arguments;
        }

        public Format next() {
            return switch (this) {
                case MP4 -> APNG;
                case APNG -> WEBP;
	            case WEBP -> GIF;
                case GIF -> MP4;
            };
        }
    }

}
