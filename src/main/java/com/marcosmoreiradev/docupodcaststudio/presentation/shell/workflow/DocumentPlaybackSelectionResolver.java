package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentSentenceSpan;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentSentenceSplitter;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;

import java.util.List;
import java.util.Optional;

/** Resolves playback to the selected sentence/unit instead of always starting at the first cue of a segment. */
public final class DocumentPlaybackSelectionResolver {
    public Optional<PlaybackCue> cueForSelection(PlaybackManifest manifest,
                                                NarrationSegment segment,
                                                DocumentTextRange selectedRange,
                                                String selectedPreview) {
        if (manifest == null || manifest.emptyManifest() || segment == null) return Optional.empty();
        Optional<PlaybackCue> selectedUnit = selectedUnitCue(manifest, segment, selectedRange, selectedPreview);
        return selectedUnit.isPresent() ? selectedUnit : manifest.cueForSegment(segment.id());
    }

    private Optional<PlaybackCue> selectedUnitCue(PlaybackManifest manifest,
                                                  NarrationSegment segment,
                                                  DocumentTextRange selectedRange,
                                                  String selectedPreview) {
        if (selectedRange == null || selectedRange.collapsed()) return Optional.empty();
        boolean belongsToSegment = segment.sourceBlockIds().stream().anyMatch(selectedRange.blockId()::equals);
        if (!belongsToSegment) return Optional.empty();
        ScriptTextRange target = scriptRange(segment, selectedRange, selectedPreview);
        List<DocumentSentenceSpan> spans = DocumentSentenceSplitter.split(segment.id(), segment.narrationText());
        for (DocumentSentenceSpan span : spans) {
            if (span.range().startOffset() < target.endOffset() && target.startOffset() < span.range().endOffset()) {
                String unitId = segment.id() + "-U" + String.format("%03d", span.index() + 1);
                Optional<PlaybackCue> cue = manifest.cueForUnit(unitId);
                if (cue.isPresent()) return cue;
            }
        }
        return Optional.empty();
    }

    private static ScriptTextRange scriptRange(NarrationSegment segment, DocumentTextRange documentRange, String selectedPreview) {
        int start = Math.min(documentRange.startOffset(), segment.narrationText().length());
        int end = Math.min(Math.max(documentRange.endOffset(), start), segment.narrationText().length());
        String preview = selectedPreview == null ? "" : selectedPreview.strip();
        if (!preview.isBlank()) {
            int index = segment.narrationText().indexOf(preview);
            if (index >= 0) { start = index; end = index + preview.length(); }
        }
        return new ScriptTextRange(segment.id(), start, end);
    }
}
