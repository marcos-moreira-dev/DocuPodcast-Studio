package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.application.reading.PreparedReadingProjection;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceReferenceSamplePathResolver;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User-level request to generate mock or real audio from a prepared-reading payload.
 *
 * <p>The {@code NarrationScriptDocument} field is retained as the internal compatibility payload
 * consumed by existing audio jobs. Product callers can enter through {@link PreparedReadingProjection}
 * and should keep user-facing language centered on documents, fragments and audio.</p>
 */
public record AudioGenerationRequest(
        NarrationScriptDocument script,
        RenderUnitPlan renderUnitPlan,
        Path projectDirectory,
        String jobName,
        String language,
        String voiceProfileId,
        VoiceLibrary voiceLibrary,
        VoiceReferenceSamplePathResolver voiceSamplePathResolver,
        String acousticRuntimeId,
        String voiceEngineId
) {
    private static final ConcurrentHashMap<SampleFileKey, String> SAMPLE_FINGERPRINT_CACHE =
            new ConcurrentHashMap<>();

    public AudioGenerationRequest(PreparedReadingProjection projection, Path projectDirectory, String jobName) {
        this(Objects.requireNonNull(projection, "projection").narrationScript(), null, projectDirectory, jobName, "es", "VOC-NARRATOR", null, null);
    }

    public AudioGenerationRequest(PreparedReadingProjection projection, RenderUnitPlan renderUnitPlan,
                                  Path projectDirectory, String jobName) {
        this(Objects.requireNonNull(projection, "projection").narrationScript(), renderUnitPlan, projectDirectory, jobName, "es", "VOC-NARRATOR", null, null);
    }

    public AudioGenerationRequest(NarrationScriptDocument script, Path projectDirectory, String jobName) {
        this(script, null, projectDirectory, jobName, "es", "VOC-NARRATOR", null, null);
    }

    public AudioGenerationRequest(NarrationScriptDocument script, Path projectDirectory, String jobName,
                                  String language, String voiceProfileId) {
        this(script, null, projectDirectory, jobName, language, voiceProfileId, null, null);
    }

    public AudioGenerationRequest(NarrationScriptDocument script, RenderUnitPlan renderUnitPlan,
                                  Path projectDirectory, String jobName) {
        this(script, renderUnitPlan, projectDirectory, jobName, "es", "VOC-NARRATOR", null, null);
    }

    public AudioGenerationRequest(NarrationScriptDocument script, RenderUnitPlan renderUnitPlan,
                                  Path projectDirectory, String jobName, VoiceLibrary voiceLibrary) {
        this(script, renderUnitPlan, projectDirectory, jobName, "es", "VOC-NARRATOR", voiceLibrary, null);
    }

    public AudioGenerationRequest(NarrationScriptDocument script, RenderUnitPlan renderUnitPlan,
                                  Path projectDirectory, String jobName, String language,
                                  String voiceProfileId, VoiceLibrary voiceLibrary) {
        this(script, renderUnitPlan, projectDirectory, jobName, language, voiceProfileId,
                voiceLibrary, null, "unspecified");
    }

    public AudioGenerationRequest(NarrationScriptDocument script, RenderUnitPlan renderUnitPlan,
                                  Path projectDirectory, String jobName, String language,
                                  String voiceProfileId, VoiceLibrary voiceLibrary,
                                  VoiceReferenceSamplePathResolver voiceSamplePathResolver) {
        this(script, renderUnitPlan, projectDirectory, jobName, language, voiceProfileId,
                voiceLibrary, voiceSamplePathResolver, "unspecified");
    }

    public AudioGenerationRequest(NarrationScriptDocument script, RenderUnitPlan renderUnitPlan,
                                  Path projectDirectory, String jobName, String language,
                                  String voiceProfileId, VoiceLibrary voiceLibrary,
                                  VoiceReferenceSamplePathResolver voiceSamplePathResolver,
                                  String acousticRuntimeId) {
        this(script, renderUnitPlan, projectDirectory, jobName, language, voiceProfileId,
                voiceLibrary, voiceSamplePathResolver, acousticRuntimeId, "");
    }

    public AudioGenerationRequest {
        script = Objects.requireNonNull(script, "script");
        projectDirectory = Objects.requireNonNull(projectDirectory, "projectDirectory").toAbsolutePath().normalize();
        jobName = jobName == null || jobName.isBlank() ? script.title() : jobName.strip();
        language = language == null || language.isBlank() ? "es" : language.strip();
        voiceProfileId = voiceProfileId == null || voiceProfileId.isBlank() ? "VOC-NARRATOR" : voiceProfileId.strip();
        acousticRuntimeId = acousticRuntimeId == null || acousticRuntimeId.isBlank()
                ? "unspecified" : acousticRuntimeId.strip();
        voiceEngineId = voiceEngineId == null ? "" : voiceEngineId.strip().toLowerCase(java.util.Locale.ROOT)
                .replace('_', '-');
        voiceLibrary = voiceLibrary == null ? null : voiceLibrary;
        voiceSamplePathResolver = voiceSamplePathResolver == null
                ? VoiceReferenceSamplePathResolver.fromCurrentApplicationRoot()
                : voiceSamplePathResolver;
        if (script.empty()) {
            throw new IllegalArgumentException("La lectura preparada no tiene fragmentos para generar audio.");
        }
        if (generationUnits(script, renderUnitPlan).isEmpty()) {
            throw new IllegalArgumentException("No hay unidades de voz sintetizable para generar audio.");
        }
    }

    public boolean usesRenderUnitPlan() {
        return renderUnitPlan != null;
    }

    public List<AudioGenerationUnit> generationUnits() {
        return generationUnits(script, renderUnitPlan).stream()
                .map(this::withEffectiveVoiceFingerprint)
                .toList();
    }

    public int generationUnitCount() {
        return generationUnits().size();
    }

    /**
     * Resolves the reference WAV that should drive an advanced voice unit.
     *
     * <p>Documento stores voice and theatrical tone as project layers. VOZ-TTS5B connects those
     * layers to real generation by resolving {@link AudioGenerationUnit#voiceProfileId()} plus
     * {@link AudioGenerationUnit#performanceStyleId()} against the project {@link VoiceLibrary}.
     * Missing expressive tones fall back to the same voice's Neutral sample; local/simple engines
     * may ignore the returned value when their command template has no speaker placeholder.</p>
     */
    public Optional<Path> referenceSamplePathFor(AudioGenerationUnit unit) throws IOException {
        Objects.requireNonNull(unit, "unit");
        String voiceId = unit.effectiveVoiceProfileId(voiceProfileId);
        Optional<VoiceReferenceSample> sample = selectedReferenceSampleFor(unit);
        if (sample.isEmpty()) {
            if (legacyAdvancedDefault(voiceId)) {
                return voiceSamplePathResolver.resolveDefaultAdvancedReference();
            }
            return Optional.empty();
        }
        return Optional.of(resolveSamplePath(sample.get()));
    }

    /** Exact spoken text paired with the selected reference sample, when known. */
    public Optional<String> referenceTranscriptFor(AudioGenerationUnit unit) {
        Objects.requireNonNull(unit, "unit");
        return selectedReferenceSampleFor(unit)
                .map(VoiceReferenceSample::referenceTranscript)
                .filter(transcript -> !transcript.isBlank());
    }

    /** Whether this character promises an identity that must not fall back to another speaker. */
    public boolean requiresReferenceSampleFor(AudioGenerationUnit unit) {
        Objects.requireNonNull(unit, "unit");
        if (voiceLibrary == null) return false;
        String voiceId = unit.effectiveVoiceProfileId(voiceProfileId);
        if (voiceId.isBlank()) return false;
        return voiceLibrary.referenceSampleSetByVoiceId(voiceId).isPresent()
                || voiceLibrary.voiceById(voiceId)
                .map(voice -> voice.hasSample() || voice.hasModel() || voice.supportsStyleTransfer())
                .orElse(false);
    }

    public VoiceReferenceTone requestedReferenceToneFor(AudioGenerationUnit unit) {
        Objects.requireNonNull(unit, "unit");
        return VoiceReferenceTone.fromLayerTargetId(unit.performanceStyleId())
                .orElse(VoiceReferenceTone.NEUTRAL);
    }

    public Optional<VoiceReferenceTone> appliedReferenceToneFor(AudioGenerationUnit unit) {
        return selectedReferenceSampleFor(unit).map(VoiceReferenceSample::tone);
    }

    private AudioGenerationUnit withEffectiveVoiceFingerprint(AudioGenerationUnit unit) {
        String voiceId = unit.effectiveVoiceProfileId(voiceProfileId);
        VoiceReferenceTone requestedTone = requestedReferenceToneFor(unit);
        StringBuilder configuration = new StringBuilder(voiceId)
                .append('|').append(requestedTone.name())
                .append("|language=").append(language.toLowerCase(java.util.Locale.ROOT))
                .append("|runtime=").append(acousticRuntimeId.toLowerCase(java.util.Locale.ROOT));
        if (voiceLibrary == null) {
            configuration.append("|engine-default");
            return unit.withSourceFingerprint(
                    unit.sourceFingerprint().withVoiceConfiguration(configuration.toString()));
        }
        Optional<VoiceReferenceSample> sample = selectedReferenceSampleFor(unit);
        if (sample.isEmpty()) {
            Optional<Path> legacyDefault = legacyAdvancedDefault(voiceId)
                    ? voiceSamplePathResolver.resolveDefaultAdvancedReference()
                    : Optional.empty();
            if (legacyDefault.isPresent()) {
                configuration.append("|legacy-default|");
                try {
                    configuration.append(sampleFingerprint(legacyDefault.orElseThrow()));
                } catch (IOException unavailable) {
                    configuration.append("unavailable");
                }
            } else {
                configuration.append("|no-reference-sample");
            }
        } else {
            VoiceReferenceSample selected = sample.orElseThrow();
            configuration.append('|').append(selected.tone().name())
                    .append('|').append(selected.id())
                    .append('|').append(selected.fileUri());
            if (!selected.referenceTranscript().isBlank()) {
                configuration.append("|transcript=")
                        .append(textFingerprint(selected.referenceTranscript()));
            }
            try {
                Path path = resolveSamplePath(selected);
                configuration.append('|').append(sampleFingerprint(path));
            } catch (IOException | RuntimeException unavailable) {
                configuration.append("|unavailable");
            }
        }
        return unit.withSourceFingerprint(
                unit.sourceFingerprint().withVoiceConfiguration(configuration.toString()));
    }

    private Path resolveSamplePath(VoiceReferenceSample sample) throws IOException {
        return voiceSamplePathResolver.resolve(projectDirectory, sample, "muestra de voz");
    }

    private Optional<VoiceReferenceSample> selectedReferenceSampleFor(AudioGenerationUnit unit) {
        if (voiceLibrary == null) return Optional.empty();
        String voiceId = unit.effectiveVoiceProfileId(voiceProfileId);
        if (voiceId.isBlank()) return Optional.empty();
        VoiceReferenceTone requestedTone = requestedReferenceToneFor(unit);
        return voiceLibrary.referenceSampleSetByVoiceId(voiceId)
                .flatMap(set -> set.sampleFor(requestedTone).or(set::neutralSample));
    }

    private static String sampleFingerprint(Path path) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        SampleFileKey key = new SampleFileKey(normalized, Files.size(normalized),
                Files.getLastModifiedTime(normalized).toMillis());
        String cached = SAMPLE_FINGERPRINT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(normalized)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read > 0) {
                        digest.update(buffer, 0, read);
                    }
                }
            }
            String fingerprint = HexFormat.of().formatHex(digest.digest());
            SAMPLE_FINGERPRINT_CACHE.putIfAbsent(key, fingerprint);
            return fingerprint;
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String textFingerprint(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = (value == null ? "" : value.strip())
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static List<AudioGenerationUnit> generationUnits(NarrationScriptDocument script, RenderUnitPlan renderUnitPlan) {
        if (renderUnitPlan != null) {
            return renderUnitPlan.audioUnits().stream()
                    .filter(unit -> unit.requiresAudioGeneration())
                    .map(unit -> AudioGenerationUnit.fromRenderUnit(unit, script.segmentById(unit.segmentId())))
                    .toList();
        }
        return script.segments().stream()
                .filter(NarrationSegment::narratable)
                .map(AudioGenerationUnit::fromSegment)
                .toList();
    }

    private static boolean legacyAdvancedDefault(String voiceId) {
        return "VOC-OWN-PLACEHOLDER".equalsIgnoreCase(
                voiceId == null ? "" : voiceId.strip());
    }

    private record SampleFileKey(Path path, long size, long modifiedAtMillis) {
    }
}
