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
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
        VoiceLibrary voiceLibrary
) {
    public AudioGenerationRequest(PreparedReadingProjection projection, Path projectDirectory, String jobName) {
        this(Objects.requireNonNull(projection, "projection").narrationScript(), null, projectDirectory, jobName, "es", "VOC-NARRATOR", null);
    }

    public AudioGenerationRequest(PreparedReadingProjection projection, RenderUnitPlan renderUnitPlan,
                                  Path projectDirectory, String jobName) {
        this(Objects.requireNonNull(projection, "projection").narrationScript(), renderUnitPlan, projectDirectory, jobName, "es", "VOC-NARRATOR", null);
    }

    public AudioGenerationRequest(NarrationScriptDocument script, Path projectDirectory, String jobName) {
        this(script, null, projectDirectory, jobName, "es", "VOC-NARRATOR", null);
    }

    public AudioGenerationRequest(NarrationScriptDocument script, Path projectDirectory, String jobName,
                                  String language, String voiceProfileId) {
        this(script, null, projectDirectory, jobName, language, voiceProfileId, null);
    }

    public AudioGenerationRequest(NarrationScriptDocument script, RenderUnitPlan renderUnitPlan,
                                  Path projectDirectory, String jobName) {
        this(script, renderUnitPlan, projectDirectory, jobName, "es", "VOC-NARRATOR", null);
    }

    public AudioGenerationRequest(NarrationScriptDocument script, RenderUnitPlan renderUnitPlan,
                                  Path projectDirectory, String jobName, VoiceLibrary voiceLibrary) {
        this(script, renderUnitPlan, projectDirectory, jobName, "es", "VOC-NARRATOR", voiceLibrary);
    }

    public AudioGenerationRequest {
        script = Objects.requireNonNull(script, "script");
        projectDirectory = Objects.requireNonNull(projectDirectory, "projectDirectory").toAbsolutePath().normalize();
        jobName = jobName == null || jobName.isBlank() ? script.title() : jobName.strip();
        language = language == null || language.isBlank() ? "es" : language.strip();
        voiceProfileId = voiceProfileId == null || voiceProfileId.isBlank() ? "VOC-NARRATOR" : voiceProfileId.strip();
        voiceLibrary = voiceLibrary == null ? null : voiceLibrary;
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
        return generationUnits(script, renderUnitPlan);
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
        if (voiceLibrary == null) {
            return Optional.empty();
        }
        String voiceId = unit.effectiveVoiceProfileId(voiceProfileId);
        if (voiceId.isBlank()) {
            return Optional.empty();
        }
        VoiceReferenceTone requestedTone = VoiceReferenceTone.fromLayerTargetId(unit.performanceStyleId())
                .orElse(VoiceReferenceTone.NEUTRAL);
        Optional<VoiceReferenceSample> sample = voiceLibrary.referenceSampleSetByVoiceId(voiceId)
                .flatMap(set -> set.sampleFor(requestedTone).or(set::neutralSample));
        if (sample.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(resolveSamplePath(sample.get()));
    }

    private Path resolveSamplePath(VoiceReferenceSample sample) throws IOException {
        return VoiceReferenceSamplePathResolver.fromCurrentApplicationRoot()
                .resolve(projectDirectory, sample, "muestra de voz");
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
}
