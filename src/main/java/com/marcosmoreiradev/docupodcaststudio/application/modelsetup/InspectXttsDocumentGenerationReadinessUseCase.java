package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Decides whether Voz IA avanzada is safe to use for document audio jobs.
 *
 * <p>This use case intentionally sits above the setup/readiness inspection: a model can be present,
 * selectable and still not be product-ready for document chunks if the runtime has never generated a
 * WAV proof. That is the difference between "downloaded" and "usable".</p>
 */
public final class InspectXttsDocumentGenerationReadinessUseCase {
    private final InspectXttsSetupReadinessUseCase setupInspector;
    private final InspectXttsSmokeTestUseCase smokeInspector;

    public InspectXttsDocumentGenerationReadinessUseCase() {
        this(new InspectXttsSetupReadinessUseCase(), new InspectXttsSmokeTestUseCase());
    }

    public InspectXttsDocumentGenerationReadinessUseCase(InspectXttsSetupReadinessUseCase setupInspector,
                                                         InspectXttsSmokeTestUseCase smokeInspector) {
        this.setupInspector = Objects.requireNonNull(setupInspector, "setupInspector");
        this.smokeInspector = Objects.requireNonNull(smokeInspector, "smokeInspector");
    }

    public XttsDocumentGenerationReadinessReport inspect(OperationalSettings settings, Path applicationRoot) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        XttsSetupReadinessReport setup = setupInspector.inspect(current, root);
        XttsSmokeTestReport smoke = smokeInspector.inspect(root);
        ArrayList<String> blocking = new ArrayList<>();
        ArrayList<String> warnings = new ArrayList<>();
        if (!setup.canBeSelectedAsEngine()) {
            blocking.addAll(setup.missingRequirements());
            if (blocking.isEmpty()) {
                blocking.add(setup.userMessage().isBlank()
                        ? "Voz IA avanzada todavía no tiene runtime/modelo/voz neutral completos."
                        : setup.userMessage());
            }
            return new XttsDocumentGenerationReadinessReport(setup, smoke, false, false, blocking, warnings,
                    "Voz IA avanzada aún no está preparada para generar fragmentos de audio del documento.",
                    "Completa Preparar/Descargar/Importar en Configuración y luego genera una prueba WAV real.");
        }
        if (!smoke.generatedWavProof()) {
            blocking.add("Falta prueba WAV real de Voz IA avanzada generada desde el runtime local.");
            if (!smoke.issues().isEmpty()) {
                blocking.addAll(smoke.issues());
            }
            return new XttsDocumentGenerationReadinessReport(setup, smoke, false, false, blocking, warnings,
                    "Voz IA avanzada está descargada/configurada, pero todavía no demostró que puede generar audio real.",
                    "Pulsa Probar en Configuración y genera un WAV corto antes de usar documentos largos.");
        }
        if (!smoke.playbackConfirmed()) {
            warnings.add("La prueba WAV existe, pero falta confirmar reproducción dentro de la app.");
            warnings.add("Puedes generar documento; si no escuchas la prueba, revisa salida de audio antes de un trabajo largo.");
            return new XttsDocumentGenerationReadinessReport(setup, smoke, true, false, blocking, warnings,
                    "Voz IA avanzada ya generó un WAV real; falta confirmar reproducción dentro de la app.",
                    "Pulsa Reproducir prueba en Configuración para cerrar la verificación.");
        }
        warnings.addAll(setup.warnings());
        return new XttsDocumentGenerationReadinessReport(setup, smoke, true, true, blocking, warnings,
                "Voz IA avanzada lista para generar fragmentos de audio del documento.",
                warnings.isEmpty() ? "Sin acción pendiente." : "Puedes usarla; revisa advertencias antes de RC final.");
    }
}
