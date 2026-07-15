package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectPiperSetupReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectXttsSetupReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.PiperSetupReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.XttsSetupReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.video.EmbeddedFfmpegLocator;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegRuntimeProbeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegRuntimeReport;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegToolDiscovery;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Builds an assisted checklist for the real GUI smoke without driving JavaFX by fake automation. */
public final class BuildGuiSmokeChecklistUseCase {
    private final InspectXttsSetupReadinessUseCase xttsInspector;
    private final InspectPiperSetupReadinessUseCase piperInspector;
    private final FfmpegRuntimeProbeUseCase ffmpegProbe;

    public BuildGuiSmokeChecklistUseCase() {
        this(new InspectXttsSetupReadinessUseCase(), new InspectPiperSetupReadinessUseCase(),
                new FfmpegRuntimeProbeUseCase(ExternalProcessRunner.unavailable("BuildGuiSmokeChecklistUseCase")));
    }

    public BuildGuiSmokeChecklistUseCase(
            InspectXttsSetupReadinessUseCase xttsInspector,
            InspectPiperSetupReadinessUseCase piperInspector,
            FfmpegRuntimeProbeUseCase ffmpegProbe
    ) {
        this.xttsInspector = Objects.requireNonNull(xttsInspector, "xttsInspector");
        this.piperInspector = Objects.requireNonNull(piperInspector, "piperInspector");
        this.ffmpegProbe = Objects.requireNonNull(ffmpegProbe, "ffmpegProbe");
    }

    public GuiSmokeChecklistReport build(OperationalSettings settings, Path applicationRoot) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        XttsSetupReadinessReport xtts = xttsInspector.inspect(current, root);
        PiperSetupReadinessReport piper = piperInspector.inspect(current, root);
        Path configuredFfmpeg = configuredFfmpeg(current);
        FfmpegToolDiscovery discovery = new EmbeddedFfmpegLocator().locate(root, configuredFfmpeg);
        FfmpegRuntimeReport ffmpeg = ffmpegProbe.inspect(discovery);
        boolean voiceReady = xtts.ready() || piper.ready();
        boolean mediaReady = ffmpeg.readyForFinalVideo();

        ArrayList<GuiSmokeStep> steps = new ArrayList<>();
        steps.add(new GuiSmokeStep("Configuración / Motores y dependencias",
                "Verificar Voz IA avanzada o Voz local simple",
                voiceReady ? "READY" : "BLOCKED",
                voiceReady ? "Hay al menos un motor de voz real listo para probar." : missingVoiceMessage(xtts, piper)));
        steps.add(new GuiSmokeStep("Vista / Voces",
                "Generar prueba de voz real con una frase corta",
                voiceReady ? "MANUAL" : "BLOCKED",
                voiceReady ? "Usa Generar prueba con esta voz y confirma que el WAV resultante contiene voz audible real." : "No se puede validar sin un motor real listo."));
        steps.add(new GuiSmokeStep("Documento",
                "Abrir un documento corto y preparar lectura",
                "MANUAL",
                "Usa un TXT/DOCX corto para reducir tiempo de prueba y confirmar selección de fragmentos."));
        steps.add(new GuiSmokeStep("Documento / Playbar",
                "Escuchar desde aquí y confirmar avance de fragmentos",
                voiceReady ? "MANUAL" : "BLOCKED",
                voiceReady ? "Debe generar/reproducir segmentos reales sin pedir comandos externos al usuario final." : "Bloqueado hasta preparar un motor de voz."));
        steps.add(new GuiSmokeStep("Exportar / Audio",
                "Exportar o localizar audio generado del documento corto",
                voiceReady ? "MANUAL" : "BLOCKED",
                voiceReady ? "Confirma que el audio queda dentro del proyecto y sobrevive al cierre/reapertura." : "Bloqueado hasta preparar un motor de voz."));
        steps.add(new GuiSmokeStep("Visuales / Video simple",
                "Verificar FFmpeg si se quiere cerrar video final",
                mediaReady ? "READY" : "MANUAL",
                mediaReady ? "FFmpeg y FFprobe listos para render final." : "FFmpeg puede importarse desde Configuración; sin esto, la app solo debe prometer paquete renderizable."));
        String message = voiceReady
                ? "Checklist de smoke GUI listo. Ejecuta los pasos manuales desde la app para validar el flujo real."
                : "Checklist generado, pero el smoke GUI queda bloqueado hasta preparar un motor de voz real.";
        return new GuiSmokeChecklistReport(root, Instant.now(), voiceReady, mediaReady, steps, message);
    }

    public Path writeMarkdown(GuiSmokeChecklistReport report, Path targetFile) throws IOException {
        Objects.requireNonNull(report, "report");
        Path target = targetFile == null
                ? report.applicationRoot().resolve("dist/release-candidate/PF6B_GUI_SMOKE_CHECKLIST.md").normalize()
                : targetFile.toAbsolutePath().normalize();
        Files.createDirectories(target.getParent());
        Files.writeString(target, toMarkdown(report), StandardCharsets.UTF_8);
        return target;
    }

    private static String toMarkdown(GuiSmokeChecklistReport report) {
        StringBuilder out = new StringBuilder();
        out.append("# DocuPodcast Studio - PF6B smoke real desde GUI\n\n");
        out.append("Generado: `").append(report.generatedAt()).append("`\n\n");
        out.append(report.userMessage()).append("\n\n");
        out.append("Resumen: ").append(report.compactSummary()).append("\n\n");
        out.append("| Área | Acción esperada | Estado | Detalle |\n");
        out.append("|---|---|---:|---|\n");
        for (GuiSmokeStep step : report.steps()) {
            out.append("| ").append(escape(step.area()))
                    .append(" | ").append(escape(step.expectedAction()))
                    .append(" | ").append(escape(step.status()))
                    .append(" | ").append(escape(step.detail()))
                    .append(" |\n");
        }
        out.append("\nCriterio: no se considera RC personal si una acción visible del flujo principal depende de placeholder o comando manual no explicado.\n");
        return out.toString();
    }

    private static String missingVoiceMessage(XttsSetupReadinessReport xtts, PiperSetupReadinessReport piper) {
        String advanced = xtts == null ? "Voz IA avanzada sin reporte" : String.join(" · ", xtts.missingRequirements());
        String simple = piper == null ? "Voz local simple sin reporte" : String.join(" · ", piper.missingRequirements());
        return "Falta preparar motor real. Voz IA avanzada: " + advanced + ". Voz local simple: " + simple + ".";
    }

    private static Path configuredFfmpeg(OperationalSettings settings) {
        String raw = settings.video().ffmpegExecutable();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Path.of(raw.strip());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace('\n', ' ');
    }
}
