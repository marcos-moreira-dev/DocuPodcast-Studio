package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationGateway;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobRepository;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioProcessDiagnosticsRepository;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.compute.InspectXttsCudaSmokeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.compute.XttsCudaSmokeReport;
import com.marcosmoreiradev.docupodcaststudio.application.errors.EngineUnavailableException;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectXttsDocumentGenerationReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.XttsDocumentGenerationReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsRepository;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsMigrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.process.DefaultExternalProcessRunner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.time.Duration;

/**
 * Audio gateway that resolves the current persisted voice settings when a job starts.
 *
 * <p>Settings can be changed from the running JavaFX app. A static gateway created at startup would
 * keep using the old motor until restart; this wrapper avoids that by rebuilding the effective engine
 * before submit/resume while keeping the same job queue and repositories.</p>
 */
public final class SettingsAwareAudioGenerationGateway implements AudioGenerationGateway {
    private final OperationalSettingsRepository settingsRepository;
    private final Path applicationRoot;
    private final InMemoryAudioJobQueue queue;
    private final AudioJobRepository jobRepository;
    private final AudioProcessDiagnosticsRepository diagnosticsRepository;
    private final ExternalProcessRunner processRunner;
    private final ConcurrentHashMap<String, AudioGenerationGateway> jobDelegates = new ConcurrentHashMap<>();

    public SettingsAwareAudioGenerationGateway(OperationalSettingsRepository settingsRepository,
                                               Path applicationRoot,
                                               InMemoryAudioJobQueue queue,
                                               AudioJobRepository jobRepository,
                                               AudioProcessDiagnosticsRepository diagnosticsRepository) {
        this(settingsRepository, applicationRoot, queue, jobRepository, diagnosticsRepository, new DefaultExternalProcessRunner());
    }

    public SettingsAwareAudioGenerationGateway(OperationalSettingsRepository settingsRepository,
                                               Path applicationRoot,
                                               InMemoryAudioJobQueue queue,
                                               AudioJobRepository jobRepository,
                                               AudioProcessDiagnosticsRepository diagnosticsRepository,
                                               ExternalProcessRunner processRunner) {
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.applicationRoot = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        this.queue = Objects.requireNonNull(queue, "queue");
        this.jobRepository = Objects.requireNonNull(jobRepository, "jobRepository");
        this.diagnosticsRepository = Objects.requireNonNull(diagnosticsRepository, "diagnosticsRepository");
        this.processRunner = processRunner == null ? new DefaultExternalProcessRunner() : processRunner;
    }

    @Override
    public AudioEngineDescriptor engineDescriptor() {
        return delegateForCurrentSettings().engineDescriptor();
    }

    @Override
    public String submit(AudioGenerationRequest request, Consumer<AudioJobStatusDto> statusConsumer) {
        AudioGenerationGateway delegate = delegateForCurrentSettings();
        String jobId = delegate.submit(request, statusConsumer);
        jobDelegates.put(jobId, delegate);
        return jobId;
    }

    @Override
    public String resume(AudioGenerationRequest request, AudioJobSnapshot snapshot, Consumer<AudioJobStatusDto> statusConsumer) {
        AudioGenerationGateway delegate = delegateForCurrentSettings();
        String jobId = delegate.resume(request, snapshot, statusConsumer);
        jobDelegates.put(jobId, delegate);
        return jobId;
    }

    @Override
    public boolean cancel(String jobId) {
        AudioGenerationGateway delegate = jobDelegates.get(jobId);
        return delegate != null && delegate.cancel(jobId);
    }

    @Override
    public List<AudioJobStatusDto> listStatuses() {
        return queue.list();
    }

    @Override
    public boolean awaitTermination(String jobId, Duration timeout) throws InterruptedException {
        AudioGenerationGateway delegate = jobDelegates.get(jobId);
        boolean terminal = delegate == null ? AudioGenerationGateway.super.awaitTermination(jobId, timeout)
                : delegate.awaitTermination(jobId, timeout);
        if (terminal) jobDelegates.remove(jobId);
        return terminal;
    }

    private AudioGenerationGateway delegateForCurrentSettings() {
        OperationalSettings settings = loadAndPersistRepairedSettings();
        String command = resolveTtsCommandTemplate(settings, applicationRoot);
        LocalTtsProcessConfiguration configuration = new LocalTtsProcessConfiguration(
                command,
                effectiveTtsDisplayName(settings, command),
                settings.tts().language(),
                settings.tts().voiceProfileId(),
                effectiveTtsTimeoutSeconds(settings),
                settings.tts().maxRetries(),
                effectiveTtsComputePolicy(settings, command, applicationRoot),
                effectiveTtsComputeDeviceId(settings, command, applicationRoot));
        if (configuration.enabled()) {
            if (usesAdvancedVoiceCommand(command)) {
                XttsDocumentGenerationReadinessReport gate = new InspectXttsDocumentGenerationReadinessUseCase()
                        .inspect(settings, applicationRoot);
                if (!gate.canGenerateDocumentAudio()) {
                    return disabledAdvancedVoiceGateway(command, gate);
                }
            }
            return new LocalTtsProcessAudioGenerationGateway(queue, jobRepository, diagnosticsRepository, configuration, processRunner);
        }
        return new MockAudioGenerationGateway(queue, jobRepository, 120L);
    }


    private AudioGenerationGateway disabledAdvancedVoiceGateway(String command, XttsDocumentGenerationReadinessReport gate) {
        String action = gate == null || gate.recommendedAction().isBlank()
                ? "Genera una prueba WAV real desde Configuración antes de usar documentos largos."
                : gate.recommendedAction();
        String message = (gate == null || gate.userMessage().isBlank()
                ? "Voz IA avanzada aún no es usable para generar fragmentos de audio del documento."
                : gate.userMessage()) + " " + action;
        return new AudioGenerationGateway() {
            @Override
            public AudioEngineDescriptor engineDescriptor() {
                return AudioEngineDescriptor.process("Voz IA avanzada", false, command == null ? "" : command, message);
            }

            @Override
            public String submit(AudioGenerationRequest request, Consumer<AudioJobStatusDto> statusConsumer) {
                throw new EngineUnavailableException("Voz IA avanzada", message, "El gateway defensivo bloqueó la generación porque la prueba real de Voz IA avanzada no está aprobada.");
            }

            @Override
            public String resume(AudioGenerationRequest request, AudioJobSnapshot snapshot, Consumer<AudioJobStatusDto> statusConsumer) {
                throw new EngineUnavailableException("Voz IA avanzada", message, "El gateway defensivo bloqueó la generación porque la prueba real de Voz IA avanzada no está aprobada.");
            }

            @Override
            public boolean cancel(String jobId) {
                return false;
            }

            @Override
            public List<AudioJobStatusDto> listStatuses() {
                return queue.list();
            }
        };
    }

    private OperationalSettings loadAndPersistRepairedSettings() {
        OperationalSettings loaded = loadSettings();
        OperationalSettings repaired = OperationalSettingsMigrationPolicy.repair(loaded);
        try {
            settingsRepository.save(repaired);
        } catch (IOException ignored) {
            // Runtime resolution must still use repaired in-memory settings if persistence fails.
        }
        return repaired;
    }

    private OperationalSettings loadSettings() {
        try {
            return settingsRepository.load();
        } catch (IOException ex) {
            return OperationalSettings.defaults();
        }
    }

    static String resolveTtsCommandTemplate(OperationalSettings settings, Path applicationRoot) {
        return XttsTtsCommandTemplate.resolve(settings, applicationRoot)
                .or(() -> PiperTtsCommandTemplate.resolve(settings, applicationRoot))
                .orElse(settings.tts().commandTemplate());
    }

    // Source contract kept for managed-runtime HF2: effectiveTtsComputePolicy(settings, command).
    static ComputeDevicePolicy effectiveTtsComputePolicy(OperationalSettings settings, String commandTemplate) {
        return effectiveTtsComputePolicy(settings, commandTemplate, Path.of(".").toAbsolutePath().normalize());
    }

    static ComputeDevicePolicy effectiveTtsComputePolicy(OperationalSettings settings, String commandTemplate, Path applicationRoot) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        if (usesAdvancedVoiceCommand(commandTemplate)) {
            if (cpuTtsRequested(current)) {
                return ComputeDevicePolicy.CPU_ONLY;
            }
            if (manualTtsDeviceRequested(current)) {
                return current.compute().policy();
            }
            XttsCudaSmokeReport cudaSmoke = inspectCudaSmoke(applicationRoot);
            if (current.compute().allowGpuForTts()
                    && current.compute().policy().canUseGpu()
                    && cudaSmoke.gpuUsableForXtts()) {
                return ComputeDevicePolicy.SPECIFIC_DEVICE;
            }
            // In AUTO/PREFER_GPU, keep document generation deterministic until a smoke confirms CUDA.
            // SPECIFIC_DEVICE is handled above as a manual request and is allowed to fail explicitly.
            return ComputeDevicePolicy.CPU_ONLY;
        }
        return current.compute().allowGpuForTts() ? current.compute().policy() : ComputeDevicePolicy.CPU_ONLY;
    }

    static String effectiveTtsComputeDeviceId(OperationalSettings settings, String commandTemplate) {
        return effectiveTtsComputeDeviceId(settings, commandTemplate, Path.of(".").toAbsolutePath().normalize());
    }

    static String effectiveTtsComputeDeviceId(OperationalSettings settings, String commandTemplate, Path applicationRoot) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        if (usesAdvancedVoiceCommand(commandTemplate)) {
            if (cpuTtsRequested(current)) {
                return "cpu";
            }
            if (manualTtsDeviceRequested(current)) {
                return current.compute().selectedDeviceId();
            }
            XttsCudaSmokeReport cudaSmoke = inspectCudaSmoke(applicationRoot);
            if (current.compute().allowGpuForTts()
                    && current.compute().policy().canUseGpu()
                    && cudaSmoke.gpuUsableForXtts()) {
                return cudaSmoke.selectedDeviceId().isBlank() ? "gpu-nvidia-0" : cudaSmoke.selectedDeviceId();
            }
            return "cpu";
        }
        return current.compute().selectedDeviceId();
    }

    private static boolean cpuTtsRequested(OperationalSettings settings) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        return !current.compute().allowGpuForTts()
                || current.compute().policy() == ComputeDevicePolicy.CPU_ONLY
                || "cpu".equalsIgnoreCase(current.compute().selectedDeviceId());
    }

    private static boolean manualTtsDeviceRequested(OperationalSettings settings) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        return current.compute().allowGpuForTts()
                && current.compute().policy() == ComputeDevicePolicy.SPECIFIC_DEVICE
                && !current.compute().selectedDeviceId().isBlank()
                && !"auto".equalsIgnoreCase(current.compute().selectedDeviceId())
                && !"cpu".equalsIgnoreCase(current.compute().selectedDeviceId());
    }

    private static XttsCudaSmokeReport inspectCudaSmoke(Path applicationRoot) {
        return new InspectXttsCudaSmokeUseCase().inspect(applicationRoot);
    }

    static int effectiveTtsTimeoutSeconds(OperationalSettings settings) {
        int configured = settings.tts().timeoutSeconds();
        String mode = settings.tts().engineMode();
        if ("xtts".equalsIgnoreCase(mode) || "coqui".equalsIgnoreCase(mode)) {
            return Math.max(configured, 900);
        }
        return configured;
    }

    static String effectiveTtsDisplayName(OperationalSettings settings, String commandTemplate) {
        String mode = settings.tts().engineMode();
        if (usesAdvancedVoiceCommand(commandTemplate) || "xtts".equalsIgnoreCase(mode) || "coqui".equalsIgnoreCase(mode)) {
            return "Voz IA avanzada";
        }
        if (usesSimpleVoiceCommand(commandTemplate) || "piper".equalsIgnoreCase(mode)) {
            return "Voz local simple";
        }
        String displayName = settings.tts().displayName();
        return displayName == null || displayName.isBlank() ? "Modo de prueba" : displayName;
    }

    static boolean usesAdvancedVoiceCommand(String commandTemplate) {
        return XttsTtsCommandTemplate.looksLikeLegacyAdvancedVoiceCommand(commandTemplate);
    }

    static boolean usesSimpleVoiceCommand(String commandTemplate) {
        String command = commandTemplate == null ? "" : commandTemplate.toLowerCase(java.util.Locale.ROOT);
        return command.contains("piper-file-to-wav") || command.contains("piper.exe");
    }
}
