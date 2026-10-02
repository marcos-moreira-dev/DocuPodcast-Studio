package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.application.WorkspaceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentListenPlan;
import com.marcosmoreiradev.docupodcaststudio.application.document.ListeningSessionReadiness;
import com.marcosmoreiradev.docupodcaststudio.application.document.PrepareListeningSessionRequest;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackBufferPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.TableNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptValidationIssue;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptValidationIssueLevel;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Coordinates the document-root prepared-reading brain.
 *
 * <p>The user-facing product starts from the source document. The historical
 * {@code NarrationScriptDocument} remains only as the internal compatibility payload used by
 * TTS, playback, render and project round-trip code.</p>
 */
public final class DocumentExperienceController {
    private final WorkspaceApplicationServices applicationServices;

    public DocumentExperienceController(WorkspaceApplicationServices applicationServices) {
        this.applicationServices = Objects.requireNonNull(applicationServices, "applicationServices");
    }

    public ListeningSessionReadiness listeningSession(ReadableDocument document,
                                                       NarrationScriptDocument projection,
                                                       PlaybackManifest manifest,
                                                       boolean audioJobRunning,
                                                       boolean projectSaved,
                                                       AudioJobStatusDto audioJobStatus,
                                                       PlaybackBufferPolicy bufferPolicy) {
        return applicationServices.project().document().prepareListeningSession().prepare(
                new PrepareListeningSessionRequest(document, projection, manifest, audioJobRunning, projectSaved, audioJobStatus, bufferPolicy));
    }

    public DocumentListenPlan listeningPlan(ReadableDocument document,
                                            NarrationScriptDocument projection,
                                            PlaybackManifest manifest,
                                            boolean audioJobRunning,
                                            boolean projectSaved,
                                            AudioJobStatusDto audioJobStatus,
                                            PlaybackBufferPolicy bufferPolicy) {
        return listeningSession(document, projection, manifest, audioJobRunning, projectSaved, audioJobStatus, bufferPolicy).plan();
    }

    public Optional<String> readinessProblem(ReadableDocument document) {
        return readinessProblem(document, null);
    }

    public Optional<String> readinessProblem(ReadableDocument document, ReadingProfile profile) {
        if (document == null) {
            return Optional.of("Abre un documento fuente antes de preparar la lectura narrada.");
        }
        boolean readableTable = profile != null && !profile.tablePolicy().skips() && document.tableNoticeCount() > 0;
        if (document.narratableBlockCount() == 0 && !readableTable) {
            return Optional.of("El documento no tiene bloques narrables. Revisa el perfil de lectura o la clasificación manual.");
        }
        return Optional.empty();
    }

    public NarrationScriptDocument buildNarrationProjection(ProjectSession session, ReadableDocument document) {
        return buildNarrationProjection(session, document, false);
    }

    public NarrationScriptDocument buildNarrationProjection(ProjectSession session, ReadableDocument document, boolean readAfterColon) {
        return buildNarrationProjection(session, document, readAfterColon,
                session.project().readingProfile());
    }

    public NarrationScriptDocument buildNarrationProjection(ProjectSession session,
                                                             ReadableDocument document,
                                                             boolean readAfterColon,
                                                             ReadingProfile profile) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(document, "document");
        return applicationServices.project().script()
                .buildPreparedReadingProjection()
                .build(document, session.project().metadata().language(), readAfterColon,
                        profile == null ? session.project().readingProfile() : profile)
                .narrationScript();
    }

    public List<ScriptValidationIssue> validate(NarrationScriptDocument projection) {
        Objects.requireNonNull(projection, "projection");
        return applicationServices.project().script().validateNarrationScript().validate(projection);
    }

    public String projectionReadyMessage(NarrationScriptDocument projection, List<ScriptValidationIssue> issues) {
        Objects.requireNonNull(projection, "projection");
        List<ScriptValidationIssue> normalizedIssues = issues == null ? List.of() : issues;
        long warnings = normalizedIssues.stream()
                .filter(issue -> issue.level() == ScriptValidationIssueLevel.WARNING)
                .count();
        long errors = normalizedIssues.stream()
                .filter(issue -> issue.level() == ScriptValidationIssueLevel.ERROR)
                .count();
        return "Lectura preparada: %d fragmentos, %d palabras, %d advertencias, %d errores."
                .formatted(projection.segmentCount(), projection.wordCount(), warnings, errors);
    }
}
