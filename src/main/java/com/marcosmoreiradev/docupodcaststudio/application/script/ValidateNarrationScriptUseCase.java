package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptValidationIssue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Validates the editable narration script before audio generation. */
public final class ValidateNarrationScriptUseCase {
    public List<ScriptValidationIssue> validate(NarrationScriptDocument script) {
        Objects.requireNonNull(script, "script");
        ArrayList<ScriptValidationIssue> issues = new ArrayList<>();
        if (script.segments().isEmpty()) {
            issues.add(ScriptValidationIssue.error("SCRIPT_EMPTY", "La lectura preparada no tiene fragmentos narrables.", ""));
            return List.copyOf(issues);
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (NarrationSegment segment : script.segments()) {
            if (!ids.add(segment.id())) {
                issues.add(ScriptValidationIssue.error("SEGMENT_DUPLICATE", "Segmento duplicado: " + segment.id(), segment.id()));
            }
            if (segment.narrationText().isBlank()) {
                issues.add(ScriptValidationIssue.error("SEGMENT_EMPTY", "El segmento no tiene texto narrable.", segment.id()));
            }
            if (segment.characterCount() > 1200) {
                issues.add(ScriptValidationIssue.warning("SEGMENT_LONG", "El segmento supera 1200 caracteres y conviene dividirlo antes del TTS.", segment.id()));
            }
            if (segment.sourceBlockIds().isEmpty()) {
                issues.add(ScriptValidationIssue.warning("SEGMENT_NO_SOURCE", "El segmento no referencia bloques del documento fuente.", segment.id()));
            }
            if (segment.voiceProfileId().isBlank()) {
                issues.add(ScriptValidationIssue.warning("SEGMENT_NO_VOICE", "El segmento no tiene voz asignada.", segment.id()));
            }
        }
        return List.copyOf(issues);
    }
}
