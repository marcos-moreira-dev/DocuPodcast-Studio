package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectPiperSetupReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectXttsDocumentGenerationReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.PiperSetupReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.XttsDocumentGenerationReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Lists the audio origins that Documento may honestly expose to the user. */
public final class ListAudioEngineAvailabilityUseCase {
    private final OperationalSettingsRepository settingsRepository;
    private final Path applicationRoot;
    private final InspectXttsDocumentGenerationReadinessUseCase xttsReadiness;
    private final InspectPiperSetupReadinessUseCase piperReadiness;

    public ListAudioEngineAvailabilityUseCase(OperationalSettingsRepository settingsRepository, Path applicationRoot) {
        this(settingsRepository, applicationRoot,
                new InspectXttsDocumentGenerationReadinessUseCase(),
                new InspectPiperSetupReadinessUseCase());
    }

    public ListAudioEngineAvailabilityUseCase(OperationalSettingsRepository settingsRepository,
                                              Path applicationRoot,
                                              InspectXttsDocumentGenerationReadinessUseCase xttsReadiness,
                                              InspectPiperSetupReadinessUseCase piperReadiness) {
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.applicationRoot = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        this.xttsReadiness = Objects.requireNonNull(xttsReadiness, "xttsReadiness");
        this.piperReadiness = Objects.requireNonNull(piperReadiness, "piperReadiness");
    }

    public List<AudioEngineAvailability> list() {
        OperationalSettings settings = loadSettings();
        XttsDocumentGenerationReadinessReport xtts = xttsReadiness.inspect(settings, applicationRoot);
        PiperSetupReadinessReport piper = piperReadiness.inspect(settings, applicationRoot);
        return List.of(
                AudioEngineAvailability.localSimple(piper.ready(), describePiperForUser(piper),
                        piper.ready() ? "Sin acción pendiente; Voz local simple recibirá el dispositivo solicitado si su runtime lo soporta." : "Prepara Voz local simple desde Configuración."),
                AudioEngineAvailability.advanced(xtts.canGenerateDocumentAudio(), xtts.userMessage(), xtts.recommendedAction()),
                AudioEngineAvailability.testMode(),
                AudioEngineAvailability.computerAudio()
        );
    }

    private static String describePiperForUser(PiperSetupReadinessReport piper) {
        String base = piper == null ? "" : Objects.toString(piper.userMessage(), "").strip();
        String device = "Voz local simple/Piper recibe el dispositivo solicitado; la aceleración real depende del binario local.";
        return base.isBlank() ? device : base + " " + device;
    }

    private OperationalSettings loadSettings() {
        try {
            return settingsRepository.load();
        } catch (IOException ex) {
            return OperationalSettings.defaults();
        }
    }
}
