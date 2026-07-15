package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.XttsModelPathPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDeviceArgumentMapper;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.errors.EngineUnavailableException;
import com.marcosmoreiradev.docupodcaststudio.application.process.GenerationAttemptPolicy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Configuration for a real local TTS engine executed as an internal process.
 *
 * <p>The application stays JavaFX-first and serverless. A high-quality engine such as XTTS/Piper can be
 * packaged as an executable or script and exposed through a command template with placeholders.</p>
 */
public record LocalTtsProcessConfiguration(
        String commandTemplate,
        String displayName,
        String language,
        String voiceProfileId,
        int timeoutSeconds,
        int maxRetries,
        ComputeDevicePolicy computePolicy,
        String computeDeviceId
) {
    public static final String PROPERTY_COMMAND = "docupodcast.tts.command";
    public static final String PROPERTY_DISPLAY_NAME = "docupodcast.tts.displayName";
    public static final String PROPERTY_LANGUAGE = "docupodcast.tts.language";
    public static final String PROPERTY_VOICE = "docupodcast.tts.voice";
    public static final String PROPERTY_TIMEOUT = "docupodcast.tts.timeoutSeconds";
    public static final String PROPERTY_MAX_RETRIES = "docupodcast.tts.maxRetries";
    public static final String PROPERTY_COMPUTE_POLICY = "docupodcast.compute.policy";
    public static final String PROPERTY_COMPUTE_DEVICE = "docupodcast.compute.selectedDeviceId";

    public static final String ENV_COMMAND = "DOCUPODCAST_TTS_COMMAND";
    public static final String ENV_DISPLAY_NAME = "DOCUPODCAST_TTS_DISPLAY_NAME";
    public static final String ENV_LANGUAGE = "DOCUPODCAST_TTS_LANGUAGE";
    public static final String ENV_VOICE = "DOCUPODCAST_TTS_VOICE";
    public static final String ENV_TIMEOUT = "DOCUPODCAST_TTS_TIMEOUT_SECONDS";
    public static final String ENV_MAX_RETRIES = "DOCUPODCAST_TTS_MAX_RETRIES";
    public static final String ENV_COMPUTE_POLICY = "DOCUPODCAST_COMPUTE_POLICY";
    public static final String ENV_COMPUTE_DEVICE = "DOCUPODCAST_COMPUTE_DEVICE";

    public LocalTtsProcessConfiguration {
        commandTemplate = normalize(commandTemplate);
        displayName = normalize(displayName).isBlank() ? "Motor TTS local por proceso" : normalize(displayName);
        language = normalize(language).isBlank() ? "es" : normalize(language);
        voiceProfileId = normalize(voiceProfileId).isBlank() ? "VOC-NARRATOR" : normalize(voiceProfileId);
        timeoutSeconds = timeoutSeconds <= 0 ? 180 : timeoutSeconds;
        maxRetries = Math.max(0, maxRetries);
        computePolicy = computePolicy == null ? ComputeDevicePolicy.AUTO : computePolicy;
        computeDeviceId = normalize(computeDeviceId);
    }

    public LocalTtsProcessConfiguration(String commandTemplate, String displayName, String language,
                                        String voiceProfileId, int timeoutSeconds, int maxRetries) {
        this(commandTemplate, displayName, language, voiceProfileId, timeoutSeconds, maxRetries, ComputeDevicePolicy.AUTO, "");
    }

    public LocalTtsProcessConfiguration(String commandTemplate, String displayName, String language,
                                        String voiceProfileId, int timeoutSeconds) {
        this(commandTemplate, displayName, language, voiceProfileId, timeoutSeconds, 3, ComputeDevicePolicy.AUTO, "");
    }

    public static LocalTtsProcessConfiguration fromSystem() {
        return from(System.getProperties(), System.getenv());
    }

    public static LocalTtsProcessConfiguration from(Properties properties, Map<String, String> environment) {
        Properties props = properties == null ? new Properties() : properties;
        Map<String, String> env = environment == null ? Map.of() : environment;
        String command = firstNonBlank(props.getProperty(PROPERTY_COMMAND), env.get(ENV_COMMAND));
        String displayName = firstNonBlank(props.getProperty(PROPERTY_DISPLAY_NAME), env.get(ENV_DISPLAY_NAME));
        String language = firstNonBlank(props.getProperty(PROPERTY_LANGUAGE), env.get(ENV_LANGUAGE), "es");
        String voice = firstNonBlank(props.getProperty(PROPERTY_VOICE), env.get(ENV_VOICE), "VOC-NARRATOR");
        int timeout = parsePositiveInt(firstNonBlank(props.getProperty(PROPERTY_TIMEOUT), env.get(ENV_TIMEOUT)), 180);
        int maxRetries = parseNonNegativeInt(firstNonBlank(props.getProperty(PROPERTY_MAX_RETRIES), env.get(ENV_MAX_RETRIES)), 3);
        ComputeDevicePolicy policy = ComputeDevicePolicy.from(firstNonBlank(props.getProperty(PROPERTY_COMPUTE_POLICY), env.get(ENV_COMPUTE_POLICY)));
        String device = firstNonBlank(props.getProperty(PROPERTY_COMPUTE_DEVICE), env.get(ENV_COMPUTE_DEVICE));
        return new LocalTtsProcessConfiguration(command, displayName, language, voice, timeout, maxRetries, policy, device);
    }

    public boolean enabled() {
        return !commandTemplate.isBlank();
    }

    public AudioEngineDescriptor descriptor() {
        LocalTtsPreflightReport report = preflightReport();
        if (!enabled()) {
            return AudioEngineDescriptor.process(displayName, false, "", report.userMessage());
        }
        return AudioEngineDescriptor.process(displayName, report.ready(), commandTemplate,
                report.userMessage() + " Intentos por segmento: "
                        + GenerationAttemptPolicy.robustAudioMaxAttemptsFromRetries(maxRetries)
                        + ". Dispositivo: " + computePolicy.label()
                        + (computeDeviceId.isBlank() ? "" : " (" + computeDeviceId + ")")
                        + ". La app no abre servidor ni API HTTP.");
    }

    public LocalTtsPreflightReport preflightReport() {
        return new LocalTtsProcessPreflight(this).check();
    }

    public List<String> commandFor(String segmentId, Path textFile, Path outputFile, String requestLanguage, String requestVoice) {
        return commandFor(segmentId, textFile, outputFile, requestLanguage, requestVoice, null);
    }

    public List<String> commandForVoiceTest(String segmentId, Path textFile, Path outputFile, String requestLanguage,
                                            String requestVoice, Path speakerWav) {
        return commandFor(segmentId, textFile, outputFile, requestLanguage, requestVoice, speakerWav);
    }

    public List<String> commandFor(String segmentId, Path textFile, Path outputFile, String requestLanguage,
                                   String requestVoice, Path speakerWav) {
        Objects.requireNonNull(textFile, "textFile");
        Objects.requireNonNull(outputFile, "outputFile");
        String lang = normalize(requestLanguage).isBlank() ? language : normalize(requestLanguage);
        String voice = normalize(requestVoice).isBlank() ? voiceProfileId : normalize(requestVoice);
        String speaker = speakerWav == null ? "" : speakerWav.toString();
        ArrayList<String> tokens = new ArrayList<>();
        for (String raw : splitCommand(commandTemplate)) {
            tokens.add(applyPlaceholders(raw, segmentId, textFile, outputFile, lang, voice, speaker));
        }
        tokens = new ArrayList<>(normalizeXttsModelDirectoryArgument(tokens));
        List<String> finalTokens;
        if (!speaker.isBlank()) {
            finalTokens = overrideSpeakerArgument(tokens, speaker);
        } else {
            finalTokens = List.copyOf(tokens);
        }
        return validateNoLegacyRuntimeCommand(finalTokens);
    }

    static List<String> splitCommand(String command) {
        String text = normalize(command);
        if (text.isBlank()) {
            return List.of();
        }
        ArrayList<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;
        boolean escaping = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaping) {
                current.append(c);
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                continue;
            }
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
                continue;
            }
            if (c == '"' && !inSingle) {
                inDouble = !inDouble;
                continue;
            }
            if (Character.isWhitespace(c) && !inSingle && !inDouble) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(c);
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    /**
     * Legacy/custom command templates can still contain -ModelDir paths that point directly to
     * model.pth. Normalize them here as a last Java-side safety net so even old localized scripts
     * such as "Voz IA avanzada-file-to-wav.ps1" do not receive model.pth/model.pth inputs.
     */
    static List<String> normalizeXttsModelDirectoryArgument(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        ArrayList<String> normalized = new ArrayList<>(tokens);
        for (int i = 0; i < normalized.size(); i++) {
            String token = normalized.get(i);
            if (("-ModelDir".equalsIgnoreCase(token) || "--model-dir".equalsIgnoreCase(token)) && i + 1 < normalized.size()) {
                normalized.set(i + 1, normalizeModelDirToken(normalized.get(i + 1)));
                i++;
                continue;
            }
            String lower = token.toLowerCase(Locale.ROOT);
            if (lower.startsWith("-modeldir=") || lower.startsWith("--model-dir=")) {
                int equals = token.indexOf('=');
                normalized.set(i, token.substring(0, equals + 1) + normalizeModelDirToken(token.substring(equals + 1)));
            }
        }
        return List.copyOf(normalized);
    }

    static List<String> validateNoLegacyRuntimeCommand(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        String blockedMarker = blockedLegacyRuntimeMarker(tokens);
        if (blockedMarker.isBlank()) {
            return List.copyOf(tokens);
        }
        throw new EngineUnavailableException(
                "Voz IA avanzada",
                "La voz avanzada tiene una ruta antigua guardada. Abre Configuracion inicial y vuelve a preparar el motor local.",
                "Comando TTS bloqueado antes de ejecutar proceso local. legacyMarker=" + blockedMarker);
    }

    static boolean containsBlockedLegacyRuntimePath(List<String> tokens) {
        return !blockedLegacyRuntimeMarker(tokens).isBlank();
    }

    private static String blockedLegacyRuntimeMarker(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return "";
        }
        String command = String.join(" ", tokens).replace('\\', '/').toLowerCase(Locale.ROOT);
        if (command.contains("componentes locales ia avanzada-wrapper")) {
            return "componentes locales ia avanzada-wrapper";
        }
        if (command.contains("recursos locales ia avanzada")) {
            return "recursos locales ia avanzada";
        }
        if (command.contains("/model.pth/model.pth")) {
            return "model.pth/model.pth";
        }
        return "";
    }

    private static String normalizeModelDirToken(String value) {
        String raw = normalize(value);
        if (raw.isBlank()) {
            return raw;
        }
        String unquoted = raw;
        if ((unquoted.startsWith("\"") && unquoted.endsWith("\""))
                || (unquoted.startsWith("'") && unquoted.endsWith("'"))) {
            unquoted = unquoted.substring(1, unquoted.length() - 1);
        }
        Path normalized = XttsModelPathPolicy.normalizeModelDirectory(Path.of(unquoted));
        String text = normalized.toString();
        return text;
    }

    private String applyPlaceholders(String token, String segmentId, Path textFile, Path outputFile,
                                     String language, String voice, String speakerWav) {
        return token
                .replace("{segmentId}", normalize(segmentId))
                .replace("{input}", textFile.toString())
                .replace("{textFile}", textFile.toString())
                .replace("{output}", outputFile.toString())
                .replace("{outputFile}", outputFile.toString())
                .replace("{language}", language)
                .replace("{voice}", voice)
                .replace("{voiceProfileId}", voice)
                .replace("{speakerWav}", speakerWav)
                .replace("{referenceSample}", speakerWav)
                .replace("{voiceSample}", speakerWav)
                .replace("{computePolicy}", computePolicy.name())
                .replace("{computeDevice}", computeDeviceId)
                .replace("{device}", ComputeDeviceArgumentMapper.toProcessDeviceArgument(computePolicy, computeDeviceId))
                .replace("{gpuIndex}", gpuIndex(computeDeviceId));
    }

    private static List<String> overrideSpeakerArgument(List<String> tokens, String speakerWav) {
        ArrayList<String> updated = new ArrayList<>(tokens);
        for (int i = 0; i < updated.size(); i++) {
            String token = updated.get(i);
            if (("-SpeakerWav".equalsIgnoreCase(token) || "--speaker-wav".equalsIgnoreCase(token)) && i + 1 < updated.size()) {
                updated.set(i + 1, speakerWav);
                return List.copyOf(updated);
            }
            String lower = token.toLowerCase(Locale.ROOT);
            if (lower.startsWith("-speakerwav=") || lower.startsWith("--speaker-wav=")) {
                int equals = token.indexOf('=');
                updated.set(i, token.substring(0, equals + 1) + speakerWav);
                return List.copyOf(updated);
            }
        }
        return List.copyOf(updated);
    }

    private static String gpuIndex(String deviceId) {
        return ComputeDeviceArgumentMapper.gpuIndex(deviceId);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String normalized = normalize(value);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return "";
    }

    private static int parseNonNegativeInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(normalize(value));
            return parsed >= 0 ? parsed : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static int parsePositiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(normalize(value));
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
