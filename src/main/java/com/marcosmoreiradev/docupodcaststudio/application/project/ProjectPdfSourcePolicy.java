package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;

import java.util.Objects;

/** Product policy for using PDF sources in each official project mode. */
public final class ProjectPdfSourcePolicy {
    public PdfSourceUse resolve(ProjectMode mode) {
        ProjectMode resolved = Objects.requireNonNullElse(mode, ProjectMode.defaultMode());
        return switch (resolved) {
            case DOCUMENTARY_STUDIO -> new PdfSourceUse(
                    resolved,
                    true,
                    true,
                    false,
                    "PDF visual de primera clase para estudio documental; seleccion por regiones y lectura/OCR gradual.");
            case NARRATIVE_VIDEO -> creativeScriptMode(resolved,
                    "PDF permitido como referencia de solo lectura; para guion narrativo editable usa DOCX o Markdown.");
            case THEATRE_PRODUCTION -> creativeScriptMode(resolved,
                    "PDF permitido como referencia de solo lectura; para obra teatral editable usa DOCX o Markdown.");
        };
    }

    private static PdfSourceUse creativeScriptMode(ProjectMode mode, String guidance) {
        return new PdfSourceUse(mode, true, false, true, guidance);
    }

    public record PdfSourceUse(
            ProjectMode mode,
            boolean visualRenderingAvailable,
            boolean advancedStudyWorkflow,
            boolean recommendEditableScriptSource,
            String guidance
    ) {
        public PdfSourceUse {
            mode = Objects.requireNonNullElse(mode, ProjectMode.defaultMode());
            guidance = guidance == null ? "" : guidance.strip();
        }
    }
}
