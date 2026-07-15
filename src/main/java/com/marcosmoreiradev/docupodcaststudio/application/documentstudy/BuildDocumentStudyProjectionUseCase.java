package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentSourceCapabilities;
import com.marcosmoreiradev.docupodcaststudio.application.fragment.FragmentWorkspaceProjection;
import com.marcosmoreiradev.docupodcaststudio.application.reading.PreparedReadingProjection;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Builds the Estudio documental projection without changing persisted project schema. */
public final class BuildDocumentStudyProjectionUseCase {
    public DocumentStudyProjection build(
            ReadableDocument document,
            PreparedReadingProjection preparedReading,
            FragmentWorkspaceProjection fragmentProjection,
            List<AudioJobSnapshot> audioJobs,
            Path projectDirectory
    ) {
        boolean documentLoaded = document != null;
        NarrationScriptDocument script = preparedReading == null ? null : preparedReading.narrationScript();
        boolean readingPrepared = script != null && !script.empty();
        List<NarrationSegment> primarySegments = primarySegments(script);
        Set<String> audioIds = completedAudioIds(audioJobs, projectDirectory);
        int audioReady = 0;
        for (NarrationSegment segment : primarySegments) {
            if (hasAudioForSegment(audioIds, segment.id())) {
                audioReady++;
            }
        }
        int audioMissing = Math.max(0, primarySegments.size() - audioReady);
        ArrayList<String> missing = new ArrayList<>();
        if (!documentLoaded) {
            missing.add("Abre una fuente documental.");
        }
        if (!readingPrepared) {
            missing.add("Prepara la lectura del documento.");
        }
        if (readingPrepared && audioMissing > 0) {
            missing.add("Genera audio para " + audioMissing + " fragmento(s) narrable(s).");
        }
        DocumentStudyReadiness readiness = new DocumentStudyReadiness(
                documentLoaded,
                readingPrepared,
                readingPrepared && audioMissing == 0 && !primarySegments.isEmpty(),
                false,
                missing);
        List<DocumentStudySupportItem> supportItems = supportItems(document);
        return new DocumentStudyProjection(
                documentLoaded ? document.title() : "Estudio documental",
                documentLoaded ? document.format() : null,
                DocumentSourceCapabilities.forFormat(documentLoaded ? document.format() : null),
                fragmentProjection == null ? 0 : fragmentProjection.fragments().size(),
                primarySegments.size(),
                secondarySegments(script).size(),
                documentLoaded ? (int) document.sourceVisualBlockCount() : 0,
                documentLoaded ? (int) document.tableNoticeCount() : 0,
                documentLoaded ? (int) document.imageNoticeCount() : 0,
                documentLoaded ? (int) document.mathNoticeCount() : 0,
                documentLoaded ? document.issues().size() : 0,
                audioReady,
                audioMissing,
                readiness,
                supportItems,
                documentLoaded ? document.issues().stream().map(issue -> issue.code()).toList() : List.of());
    }

    private static List<DocumentStudySupportItem> supportItems(ReadableDocument document) {
        if (document == null) {
            return List.of();
        }
        ArrayList<DocumentStudySupportItem> items = new ArrayList<>();
        for (DocumentBlock block : document.blocks()) {
            if (!block.sourceVisual()) {
                continue;
            }
            items.add(new DocumentStudySupportItem(
                    block.id(),
                    block.type(),
                    block.type().displayName(),
                    block.text(),
                    block.sourceLocatorLabel(document.format()),
                    block.metadata()));
        }
        return List.copyOf(items);
    }

    private static List<NarrationSegment> primarySegments(NarrationScriptDocument script) {
        if (script == null) {
            return List.of();
        }
        return script.segments().stream()
                .filter(NarrationSegment::narratable)
                .filter(segment -> !secondaryReadUnit(segment))
                .toList();
    }

    private static List<NarrationSegment> secondarySegments(NarrationScriptDocument script) {
        if (script == null) {
            return List.of();
        }
        return script.segments().stream()
                .filter(BuildDocumentStudyProjectionUseCase::secondaryReadUnit)
                .toList();
    }

    private static boolean secondaryReadUnit(NarrationSegment segment) {
        return segment != null && Boolean.parseBoolean(segment.metadata().getOrDefault("secondaryReadUnit", "false"));
    }

    private static Set<String> completedAudioIds(List<AudioJobSnapshot> jobs, Path projectDirectory) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (jobs == null) {
            return result;
        }
        for (AudioJobSnapshot job : jobs) {
            if (job == null) {
                continue;
            }
            for (AudioSegmentSnapshot segment : job.segments()) {
                if (usableAudio(projectDirectory, segment)) {
                    result.add(segment.segmentId());
                }
            }
        }
        return result;
    }

    private static boolean hasAudioForSegment(Set<String> audioIds, String segmentId) {
        if (segmentId == null || segmentId.isBlank()) {
            return false;
        }
        if (audioIds.contains(segmentId)) {
            return true;
        }
        String unitPrefix = segmentId + "-";
        return audioIds.stream().anyMatch(id -> id.startsWith(unitPrefix));
    }

    private static boolean usableAudio(Path projectDirectory, AudioSegmentSnapshot segment) {
        if (segment == null || !segment.completed() || segment.audioRelativePath().isBlank()) {
            return false;
        }
        if (projectDirectory == null) {
            return true;
        }
        Path root = projectDirectory.toAbsolutePath().normalize();
        Path audio = root.resolve(segment.audioRelativePath()).toAbsolutePath().normalize();
        return audio.startsWith(root) && Files.isRegularFile(audio);
    }
}
