package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptValidationIssueLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ValidateNarrationScriptUseCaseTest {
    @Test
    void reportsEmptyScriptAsError() {
        NarrationScriptDocument script = NarrationScriptDocument.create("Vacío", "es", "Doc", List.of());

        var issues = new ValidateNarrationScriptUseCase().validate(script);

        assertEquals(1, issues.size());
        assertEquals(ScriptValidationIssueLevel.ERROR, issues.get(0).level());
    }

    @Test
    void warnsAboutLongSegments() {
        String longText = "x".repeat(1301);
        NarrationScriptDocument script = NarrationScriptDocument.create("Largo", "es", "Doc", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Largo", longText, List.of("B001"))
        ));

        var issues = new ValidateNarrationScriptUseCase().validate(script);

        assertTrue(issues.stream().anyMatch(issue -> issue.code().equals("SEGMENT_LONG")));
    }
}
