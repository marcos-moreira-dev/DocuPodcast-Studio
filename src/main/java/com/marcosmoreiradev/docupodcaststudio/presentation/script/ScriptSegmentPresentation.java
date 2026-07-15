package com.marcosmoreiradev.docupodcaststudio.presentation.script;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptValidationIssue;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptValidationIssueLevel;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.CharacterProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.PerformanceStyle;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;

import java.util.List;
import java.util.Objects;

/**
 * Presentation projection for one narration segment.
 *
 * <p>The script workspace needs to show product readiness, not only raw ids: voice assignment,
 * generated audio, storyboard image, validation and playback state. Keeping this projection outside
 * JavaFX makes the status rules testable and prevents the view from becoming a second domain layer.</p>
 */
public record ScriptSegmentPresentation(
        String segmentId,
        String characterLabel,
        String voiceLabel,
        String styleLabel,
        boolean voiceReady,
        String voiceStatus,
        boolean audioReady,
        String audioStatus,
        boolean storyboardReady,
        String storyboardStatus,
        ScriptValidationIssueLevel validationLevel,
        String validationStatus,
        boolean playbackActive,
        boolean playbackPaused
) {
    public ScriptSegmentPresentation {
        segmentId = normalize(segmentId);
        characterLabel = normalize(characterLabel);
        voiceLabel = normalize(voiceLabel);
        styleLabel = normalize(styleLabel);
        voiceStatus = normalize(voiceStatus);
        audioStatus = normalize(audioStatus);
        storyboardStatus = normalize(storyboardStatus);
        validationLevel = validationLevel == null ? ScriptValidationIssueLevel.INFO : validationLevel;
        validationStatus = normalize(validationStatus);
    }

    public static ScriptSegmentPresentation from(
            NarrationSegment segment,
            VoiceLibrary voiceLibrary,
            PlaybackManifest playbackManifest,
            StoryboardDocument storyboard,
            List<ScriptValidationIssue> issues,
            PlaybackCursor cursor
    ) {
        Objects.requireNonNull(segment, "segment");
        VoiceLibrary library = voiceLibrary == null ? VoiceLibrary.defaults() : voiceLibrary;
        List<ScriptValidationIssue> segmentIssues = issues == null ? List.of() : issues.stream()
                .filter(issue -> segment.id().equals(issue.segmentId()))
                .toList();

        VoiceProfile voice = library.voiceById(segment.voiceProfileId()).orElse(null);
        CharacterProfile character = library.characterById(segment.characterId()).orElse(null);
        PerformanceStyle style = library.styleById(segment.performanceStyleId()).orElse(null);
        boolean voiceReady = character != null && voice != null && style != null && voice.usableForTts();
        String voiceStatus = voiceReady ? "Voz lista" : missingVoiceStatus(character, voice, style);

        boolean audioReady = playbackManifest != null
                && !playbackManifest.emptyManifest()
                && playbackManifest.cueForSegment(segment.id()).map(cue -> cue.hasAudio()).orElse(false);
        String audioStatus = audioReady ? "Audio generado" : "Audio pendiente";

        boolean storyboardReady = storyboard != null && storyboard.bindingForSegment(segment.id()).isPresent();
        String storyboardStatus = storyboardReady ? "Imagen asociada" : "Imagen pendiente";

        long errors = segmentIssues.stream().filter(issue -> issue.level() == ScriptValidationIssueLevel.ERROR).count();
        long warnings = segmentIssues.stream().filter(issue -> issue.level() == ScriptValidationIssueLevel.WARNING).count();
        ScriptValidationIssueLevel validationLevel = errors > 0
                ? ScriptValidationIssueLevel.ERROR
                : warnings > 0 ? ScriptValidationIssueLevel.WARNING : ScriptValidationIssueLevel.INFO;
        String validationStatus = errors > 0
                ? errors + " errores"
                : warnings > 0 ? warnings + " advertencias" : "Sin hallazgos";

        boolean sameCursorSegment = cursor != null && segment.id().equals(cursor.segmentId());
        boolean playbackActive = sameCursorSegment && !cursor.paused();
        boolean playbackPaused = sameCursorSegment && cursor.paused();

        return new ScriptSegmentPresentation(
                segment.id(),
                character == null ? segment.characterId() : character.displayName(),
                voice == null ? segment.voiceProfileId() : voice.displayName(),
                style == null ? segment.performanceStyleId() : style.displayName(),
                voiceReady,
                voiceStatus,
                audioReady,
                audioStatus,
                storyboardReady,
                storyboardStatus,
                validationLevel,
                validationStatus,
                playbackActive,
                playbackPaused
        );
    }

    public String validationCssClass() {
        return switch (validationLevel) {
            case ERROR -> "script-status-error";
            case WARNING -> "script-status-warning";
            case INFO -> "script-status-ok";
        };
    }

    public String voiceCssClass() {
        return voiceReady ? "script-status-ok" : "script-status-warning";
    }

    public String audioCssClass() {
        return audioReady ? "script-status-ok" : "script-status-pending";
    }

    public String storyboardCssClass() {
        return storyboardReady ? "script-status-ok" : "script-status-pending";
    }

    public String playbackStatus() {
        if (playbackActive) {
            return "Reproduciendo";
        }
        if (playbackPaused) {
            return "Pausado";
        }
        return audioReady ? "Preparado" : "Sin audio";
    }

    public String playbackCssClass() {
        if (playbackActive) {
            return "script-status-playing";
        }
        if (playbackPaused) {
            return "script-status-paused";
        }
        return audioReady ? "script-status-ok" : "script-status-pending";
    }

    private static String missingVoiceStatus(CharacterProfile character, VoiceProfile voice, PerformanceStyle style) {
        if (character == null) {
            return "Personaje no encontrado";
        }
        if (voice == null) {
            return "Voz no encontrada";
        }
        if (!voice.usableForTts()) {
            return "Voz pendiente de muestra";
        }
        if (style == null) {
            return "Estilo no encontrado";
        }
        return "Voz incompleta";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
