package com.marcosmoreiradev.docupodcaststudio.localmedia;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/** Structured, secret-safe lifecycle diagnostics for processes started by this adapter module. */
final class OwnedProcessDiagnostics {
    private static final System.Logger LOG = System.getLogger("docupodcast.process");

    private OwnedProcessDiagnostics() { }

    static Instant started(String kind, Process process, List<String> command, String owner,
                           String terminationCondition, String containment) {
        Instant started = process.info().startInstant().orElseGet(Instant::now);
        LOG.log(System.Logger.Level.INFO,
                "process.start kind={0} pid={1} parentPid={2} executable={3} arguments={4} "
                        + "startedByDocuPodcast=true owner={5} startedAt={6} expectedTermination={7} containment={8}",
                safeKind(kind), process.pid(), parentPid(process), executable(command),
                Math.max(0, command == null ? 0 : command.size() - 1), safe(owner), started,
                safe(terminationCondition), safe(containment));
        return started;
    }

    static void resident(String kind, Process process, String owner, String reason) {
        if (process == null) return;
        LOG.log(System.Logger.Level.INFO,
                "process.resident kind={0} pid={1} parentPid={2} alive={3} owner={4} reason={5}",
                safeKind(kind), process.pid(), parentPid(process), process.isAlive(), safe(owner), safe(reason));
    }

    static void stopped(String kind, Process process, String owner, Instant started,
                        String reason, Integer exitCode, boolean forced) {
        if (process == null) return;
        long elapsed = started == null ? -1L
                : Math.max(0L, Duration.between(started, Instant.now()).toMillis());
        LOG.log(System.Logger.Level.INFO,
                "process.stop kind={0} pid={1} owner={2} reason={3} exitCode={4} elapsedMs={5} forced={6} alive={7}",
                safeKind(kind), process.pid(), safe(owner), safe(reason),
                exitCode == null ? "unavailable" : exitCode, elapsed, forced, process.isAlive());
    }

    static String kind(List<String> command) {
        String executable = executable(command).toLowerCase(Locale.ROOT);
        if (executable.contains("ffmpeg")) return "FFMPEG";
        if (executable.contains("piper")) return "PIPER";
        if (executable.contains("python")) return "PYTHON_HELPER";
        if (executable.contains("powershell") || executable.endsWith("cmd.exe")) return "SCRIPT";
        return "LOCAL_HELPER";
    }

    private static long parentPid(Process process) {
        return process.toHandle().parent().map(ProcessHandle::pid).orElse(-1L);
    }

    private static String executable(List<String> command) {
        if (command == null || command.isEmpty() || command.getFirst() == null) return "unknown";
        String raw = command.getFirst();
        try {
            Path name = Path.of(raw).getFileName();
            return name == null ? "unknown" : name.toString();
        } catch (RuntimeException invalid) {
            return "unknown";
        }
    }

    private static String safeKind(String value) {
        String normalized = safe(value).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_-]", "_");
        return normalized.isBlank() ? "UNKNOWN" : normalized;
    }

    private static String safe(String value) {
        if (value == null) return "";
        return value.replace('\r', ' ').replace('\n', ' ').strip();
    }
}
