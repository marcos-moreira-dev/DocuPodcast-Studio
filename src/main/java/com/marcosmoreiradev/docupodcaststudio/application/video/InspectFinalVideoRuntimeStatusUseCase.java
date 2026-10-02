package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Resolves provider details outside JavaFX and exposes only final-video status data. */
public final class InspectFinalVideoRuntimeStatusUseCase {
    private final Path runtimeRoot;
    private final FfmpegRuntimeProbeUseCase runtimeProbe;
    private final EmbeddedFfmpegLocator runtimeLocator;

    public InspectFinalVideoRuntimeStatusUseCase(Path runtimeRoot,
                                                 FfmpegRuntimeProbeUseCase runtimeProbe) {
        this(runtimeRoot, runtimeProbe, new EmbeddedFfmpegLocator());
    }

    InspectFinalVideoRuntimeStatusUseCase(Path runtimeRoot,
                                          FfmpegRuntimeProbeUseCase runtimeProbe,
                                          EmbeddedFfmpegLocator runtimeLocator) {
        this.runtimeRoot = Objects.requireNonNull(runtimeRoot, "runtime root")
                .toAbsolutePath().normalize();
        this.runtimeProbe = Objects.requireNonNull(runtimeProbe, "runtime probe");
        this.runtimeLocator = Objects.requireNonNull(runtimeLocator, "runtime locator");
    }

    public FinalVideoRuntimeStatus inspect(OperationalSettings.VideoRenderSettings settings,
                                           VideoEncoderPolicy requestedPolicy) {
        OperationalSettings.VideoRenderSettings current = settings == null
                ? OperationalSettings.defaults().video() : settings;
        Path configured = parsePath(current.configuredRendererExecutable());
        FfmpegRuntimeReport report = runtimeProbe.inspect(runtimeLocator.locate(runtimeRoot, configured));
        VideoEncoderPolicy requested = requestedPolicy == null ? VideoEncoderPolicy.AUTO : requestedPolicy;
        String warnings = String.join(" ", report.warnings());
        String state = report.readyForFinalVideo()
                ? "Listo para video final. FFmpeg y FFprobe están verificados."
                : "Necesita preparación." + (warnings.isBlank() ? "" : " " + warnings);
        return new FinalVideoRuntimeStatus(
                "FFmpeg",
                "FFprobe",
                report.ffmpegExecutable(),
                report.ffprobeExecutable(),
                report.readyForFinalVideo(),
                state,
                report.ffmpegVersion(),
                report.ffprobeVersion(),
                report.effectiveEncoder(requested),
                requested.name(),
                report.encoders(),
                "La instalación, importación y reparación de FFmpeg se administra exclusivamente "
                        + "en Motores y dependencias."
        );
    }

    public FinalVideoRuntimeStatus unavailable(VideoEncoderPolicy requestedPolicy) {
        VideoEncoderPolicy requested = requestedPolicy == null ? VideoEncoderPolicy.AUTO : requestedPolicy;
        return new FinalVideoRuntimeStatus(
                "FFmpeg",
                "FFprobe",
                null,
                null,
                false,
                "Configuración abierta sin servicios de diagnóstico.",
                "",
                "",
                "",
                requested.name(),
                List.of(),
                "Abre Motores y dependencias para comprobar o preparar el runtime de video."
        );
    }

    private static Path parsePath(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Path.of(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
