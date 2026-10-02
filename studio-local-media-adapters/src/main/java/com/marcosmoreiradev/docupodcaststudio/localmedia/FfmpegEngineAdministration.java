package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Safe FFmpeg installation, probes, preparation operations and real short render. */
final class FfmpegEngineAdministration extends AbstractLocalEngineAdministration {
    private static final EngineActionId PROBE = new EngineActionId("probe");
    private static final EngineActionId NORMALIZE = new EngineActionId("normalize");
    private static final EngineActionId EXTRACT_AUDIO = new EngineActionId("extract-audio");
    private final VideoRenderEngine renderer;
    private final LocalProcessExecutor executor = new LocalProcessExecutor();

    FfmpegEngineAdministration(VideoRenderEngine renderer, RuntimeAssetCatalog assets) {
        super(renderer, assets, List.of(
                action(EngineActionId.IMPORT, "Importar FFmpeg",
                        "Importa un ejecutable local sin permitir elegir el destino.", true,
                        file("executableFile", "ffmpeg.exe", true)),
                action(PROBE, "Comprobar codecs", "Ejecuta el probe del adaptador registrado.", false),
                action(NORMALIZE, "Normalizar audio de prueba",
                        "Normaliza un archivo al area de diagnostico administrada.", false,
                        file("sourceFile", "Audio o video de origen", true)),
                action(EXTRACT_AUDIO, "Extraer audio de prueba",
                        "Extrae audio al area de diagnostico administrada.", false,
                        file("sourceFile", "Video de origen", true)),
                action(EngineActionId.REPAIR, "Reparar FFmpeg",
                        "Limpia exclusivamente staging incompleto de FFmpeg.", true),
                action(EngineActionId.SMOKE_TEST, "Renderizar video corto",
                        "Renderiza un MP4 documental corto con imagen, texto y narración WAV.", false)));
        this.renderer = renderer;
    }

    @Override protected List<GenerationArtifact> perform(EngineActionRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        if (EngineActionId.IMPORT.equals(request.actionId())) {
            runtime.importFile(inputPath(request, "executableFile"), "tools/ffmpeg/bin/ffmpeg.exe", context);
        } else if (NORMALIZE.equals(request.actionId())) {
            Path output = runtime.target("diagnostics/ffmpeg/normalized.wav");
            runFfmpeg(inputPath(request, "sourceFile"), output,
                    List.of("-vn", "-ar", "44100", "-ac", "2", "-c:a", "pcm_s16le"), context);
            return List.of(artifact("audio", output, "normalized"));
        } else if (EXTRACT_AUDIO.equals(request.actionId())) {
            Path output = runtime.target("diagnostics/ffmpeg/extracted.wav");
            runFfmpeg(inputPath(request, "sourceFile"), output,
                    List.of("-vn", "-ar", "44100", "-ac", "2", "-c:a", "pcm_s16le"), context);
            return List.of(artifact("audio", output, "extracted"));
        } else if (EngineActionId.REPAIR.equals(request.actionId())) {
            runtime.repair(engineId().value(), context);
        } else if (EngineActionId.SMOKE_TEST.equals(request.actionId())) {
            Path directory = runtime.target("diagnostics/ffmpeg");
            Files.createDirectories(directory);
            Path cover = directory.resolve("documentary-cover.png");
            Path paragraph = directory.resolve("documentary-paragraph.png");
            Path narration = directory.resolve("documentary-narration.wav");
            writeSmokeImage(cover, "DocuPodcast Studio", "Video documental");
            writeSmokeImage(paragraph, "Prueba de exportación", "Imagen, texto y audio");
            writeToneWav(narration, 2.0);
            Path output = directory.resolve("documentary-video-smoke.mp4");
            VideoTimelinePlan plan = new VideoTimelinePlan(List.of(
                    new VideoTimelineItem("cover",
                            List.of(new TimelineVisualSource(TimelineVisualKind.STILL_IMAGE, cover, 0, 1,
                                    "cover", Map.of("kind", "cover"))), narration, 1, Map.of("text", "Portada")),
                    new VideoTimelineItem("paragraph",
                            List.of(new TimelineVisualSource(TimelineVisualKind.STILL_IMAGE, paragraph, 0, 1,
                                    "paragraph", Map.of("kind", "paragraph"))), narration, 1,
                            Map.of("text", "Fragmento documental"))),
                    List.of(), 640, 360, 12,
                    VideoEncodingPreference.CPU, Map.of("purpose", "documentary-final-video-smoke"));
            renderer.render(new VideoRenderRequest(plan, output, Map.of()), context);
            verifyMp4(output, directory, context);
            return List.of(artifact("video", output, "short-render"));
        }
        return List.of();
    }

    private void runFfmpeg(Path source, Path output, List<String> options, ExecutionContext context)
            throws IOException, InterruptedException {
        if (!Files.isRegularFile(source)) throw new IOException("No existe el archivo de origen.");
        Files.createDirectories(output.getParent());
        java.util.ArrayList<String> command = new java.util.ArrayList<>();
        command.add(assets.require(FfmpegVideoRenderEngine.ID, "executable").toString());
        command.addAll(List.of("-y", "-i", source.toAbsolutePath().normalize().toString()));
        command.addAll(options);
        command.add(output.toString());
        var result = executor.run(command, output.getParent(), context);
        if (result.exitCode() != 0 || !Files.isRegularFile(output)) {
            throw new IOException("FFmpeg no produjo el artefacto solicitado: " + tail(result.output()));
        }
    }

    private void verifyMp4(Path output, Path workDirectory, ExecutionContext context)
            throws IOException, InterruptedException {
        Path ffprobe = assets.require(FfmpegVideoRenderEngine.ID, "executable")
                .resolveSibling("ffprobe.exe");
        if (!Files.isRegularFile(ffprobe)) {
            throw new IOException("FFprobe no está disponible para validar el MP4 documental.");
        }
        var result = executor.run(List.of(ffprobe.toString(), "-v", "error",
                "-show_entries", "format=duration", "-of", "default=nw=1:nk=1",
                output.toString()), workDirectory, context);
        if (result.exitCode() != 0 || result.output().isBlank()) {
            throw new IOException("FFprobe no pudo validar el MP4 documental: " + tail(result.output()));
        }
    }

    private static void writeSmokeImage(Path target, String title, String subtitle) throws IOException {
        BufferedImage image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(91, 33, 182));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.WHITE);
            graphics.drawString(title, 225, 165);
            graphics.drawString(subtitle, 235, 195);
        } finally {
            graphics.dispose();
        }
        if (!ImageIO.write(image, "png", target.toFile())) throw new IOException("No se pudo crear el frame de prueba.");
    }

    private static void writeToneWav(Path target, double durationSeconds) throws IOException {
        float sampleRate = 44_100.0f;
        int frames = (int) Math.round(sampleRate * durationSeconds);
        byte[] pcm = new byte[frames * 2];
        for (int index = 0; index < frames; index++) {
            short sample = (short) (Math.sin(2.0 * Math.PI * 220.0 * index / sampleRate) * 1200);
            pcm[index * 2] = (byte) (sample & 0xff);
            pcm[index * 2 + 1] = (byte) ((sample >>> 8) & 0xff);
        }
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        try (AudioInputStream stream = new AudioInputStream(
                new ByteArrayInputStream(pcm), format, frames)) {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, target.toFile());
        }
    }

    private static GenerationArtifact artifact(String kind, Path output, String operation) {
        return new GenerationArtifact(kind, output.toUri(), Map.of("purpose", "smoke", "operation", operation));
    }

    private static String tail(String output) {
        String value = output == null ? "" : output.strip();
        return value.length() <= 1000 ? value : value.substring(value.length() - 1000);
    }
}
