package com.marcosmoreiradev.docupodcaststudio.application.engines;

import java.util.ArrayList;
import java.util.List;

/** Converts technical engine preflight reports into concise user-facing guidance. */
public final class BuildHumanEnginePreflightSummaryUseCase {
    public HumanEnginePreflightSummary fromAiEngineReport(AiEnginePreflightReport report) {
        if (report == null || report.items().isEmpty()) {
            return new HumanEnginePreflightSummary(
                    HumanEnginePreflightState.ERROR,
                    "No se pudo inspeccionar motores de voz/media.",
                    "Abre Configuración y revisa las rutas de tools y models.",
                    "Motores: error de inspección",
                    List.of("El reporte técnico no trajo elementos de preflight."));
        }
        if (report.fullAiDemoReady()) {
            return new HumanEnginePreflightSummary(
                    HumanEnginePreflightState.READY,
                    "Motores listos para voz y media.",
                    "Puedes preparar audio, importar media y continuar con pruebas reales.",
                    "Motores listos",
                    report.items().stream().map(this::detail).toList());
        }
        List<AiEnginePreflightItem> pending = report.needsConfiguration();
        String primary = pending.isEmpty()
                ? "Ejecuta una prueba corta de voz y FFmpeg desde Configuración."
                : pending.getFirst().recommendedAction();
        return new HumanEnginePreflightSummary(
                HumanEnginePreflightState.REQUIRES_PREPARATION,
                "Motores requieren preparación antes del uso real completo.",
                primary,
                "Motores: requiere preparación",
                pending.isEmpty()
                        ? report.items().stream().map(this::detail).toList()
                        : pending.stream().map(this::detail).toList());
    }

    public HumanEnginePreflightSummary fromStartupReport(StartupEnginePreflightReport report) {
        if (report == null || report.items().isEmpty()) {
            return new HumanEnginePreflightSummary(
                    HumanEnginePreflightState.ERROR,
                    "No se pudo inspeccionar el arranque de motores.",
                    "Revisa el layout de instalación o la raíz runtime configurada.",
                    "Arranque motores: error",
                    List.of("El reporte de arranque no contiene elementos."));
        }
        if (report.listeningReady()) {
            return new HumanEnginePreflightSummary(
                    HumanEnginePreflightState.READY,
                    "Arranque de motores listo para lectura con voz IA.",
                    "Mantén el smoke opt-in para validar motores reales antes del release.",
                    "Arranque motores listo",
                    report.items().stream().map(this::detail).toList());
        }
        List<StartupEnginePreflightItem> missing = report.missingRequiredItems();
        String primary = missing.isEmpty()
                ? "Revisa Configuración > Motores y ejecuta el preflight manual."
                : missing.getFirst().recommendedAction();
        return new HumanEnginePreflightSummary(
                HumanEnginePreflightState.REQUIRES_PREPARATION,
                "El arranque detectó motores pendientes de preparación.",
                primary,
                "Arranque motores: requiere preparación",
                missing.isEmpty()
                        ? report.items().stream().map(this::detail).toList()
                        : missing.stream().map(this::detail).toList());
    }

    private String detail(AiEnginePreflightItem item) {
        return item.displayName() + ": " + item.status().label() + " — " + item.userMessage();
    }

    private String detail(StartupEnginePreflightItem item) {
        return item.displayName() + ": " + item.status().label() + " — " + item.userMessage();
    }
}
