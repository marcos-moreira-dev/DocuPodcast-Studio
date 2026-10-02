package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectPiperSetupReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectXttsDocumentGenerationReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.PiperSetupReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.XttsDocumentGenerationReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsRepository;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;

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
    private final ListVoiceEngineOperationalStatesUseCase operationalStates;

    /** Product constructor: readiness comes from the same registry used to synthesize audio. */
    public ListAudioEngineAvailabilityUseCase(MediaEnginePlatform platform) {
        this.settingsRepository = null;
        this.applicationRoot = Path.of(".").toAbsolutePath().normalize();
        this.xttsReadiness = null;
        this.piperReadiness = null;
        this.operationalStates = new ListVoiceEngineOperationalStatesUseCase(platform);
    }

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
        this.operationalStates = null;
    }

    public List<AudioEngineAvailability> list() {
        if (operationalStates != null) {
            java.util.ArrayList<AudioEngineAvailability> result = new java.util.ArrayList<>();
            operationalStates.list().stream().map(AudioEngineAvailability::from).forEach(result::add);
            result.add(AudioEngineAvailability.computerAudio());
            return List.copyOf(result);
        }
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
