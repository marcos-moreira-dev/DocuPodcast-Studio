package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.playback.SegmentAudioPlayer;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/** Best-effort JSONL recorder for playback events. It never controls playback behavior. */
public final class PlaybackDiagnosticRecorder {
    public Path recordPlaybackEvent(Path projectFile,
                                    String event,
                                    PlaybackCue cue,
                                    Optional<Path> audioFile,
                                    SegmentAudioPlayer player,
                                    double playbackRate,
                                    boolean sequentialQueueActive,
                                    String runtimeQueueLabel,
                                    String reason,
                                    Consumer<String> suspiciousEarlyStop) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("reason", safe(reason));
        values.put("playbackRate", format(playbackRate));
        values.put("playerPlaying", Boolean.toString(player != null && player.playing()));
        double position = player == null ? -1.0 : player.currentPositionSeconds();
        values.put("playerPositionSeconds", format(position));
        values.put("sequentialQueueActive", Boolean.toString(sequentialQueueActive));
        values.put("runtimeQueue", safe(runtimeQueueLabel));
        if (cue != null) {
            values.put("cueUnitId", cue.unitId());
            values.put("segmentId", cue.segmentId());
            values.put("audioRelativePath", cue.audioRelativePath());
            values.put("expectedDurationSeconds", format(cue.durationSeconds()));
            boolean completionSignal = event != null && (event.contains("advance") || event.contains("finished"));
            boolean suspicious = completionSignal && position >= 0.0 && position + 0.30 < cue.durationSeconds();
            values.put("suspiciousEarlyStop", Boolean.toString(suspicious));
            if (suspicious && suspiciousEarlyStop != null) {
                suspiciousEarlyStop.accept("posible corte temprano");
            }
        }
        audioFile.ifPresent(path -> describeWav(path, values));
        return record(projectFile, event, values);
    }

    public Path record(Path projectFile, String event, Map<String, String> values) {
        Path file = diagnosticFile(projectFile);
        try {
            Files.createDirectories(file.getParent());
            LinkedHashMap<String, String> data = new LinkedHashMap<>();
            data.put("timestamp", Instant.now().toString());
            data.put("event", safe(event));
            if (values != null) {
                values.forEach((key, value) -> data.put(safe(key), safe(value)));
            }
            Files.writeString(file, json(data) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException ignored) {
            // Playback diagnostics are support evidence only. Audio playback must not fail because logging failed.
        }
        return file;
    }

    private static void describeWav(Path path, Map<String, String> values) {
        Path normalized = path.toAbsolutePath().normalize();
        values.put("wavPath", normalized.toString());
        try {
            values.put("wavBytes", Files.isRegularFile(normalized) ? Long.toString(Files.size(normalized)) : "missing");
        } catch (IOException ex) {
            values.put("wavBytes", "unreadable: " + ex.getMessage());
        }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    public Path diagnosticFile(Path projectFile) {
        if (projectFile != null && projectFile.toAbsolutePath().normalize().getParent() != null) {
            return projectFile.toAbsolutePath().normalize().getParent()
                    .resolve("diagnostics/playback-diagnostics.jsonl")
                    .normalize();
        }
        return Path.of("target/docupodcast-playback-diagnostics/playback-diagnostics.jsonl")
                .toAbsolutePath()
                .normalize();
    }

    private static String json(Map<String, String> values) {
        StringBuilder out = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) {
                out.append(',');
            }
            out.append('"').append(escape(entry.getKey())).append('"')
                    .append(':')
                    .append('"').append(escape(entry.getValue())).append('"');
            first = false;
        }
        return out.append('}').toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String escape(String value) {
        String text = safe(value);
        StringBuilder escaped = new StringBuilder(text.length() + 8);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
