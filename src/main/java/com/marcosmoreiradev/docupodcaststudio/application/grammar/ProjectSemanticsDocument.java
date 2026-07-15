package com.marcosmoreiradev.docupodcaststudio.application.grammar;

import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Machine-readable sidecar derived from the last imported Markdown grammar. */
public record ProjectSemanticsDocument(
        int schemaVersion,
        ProjectMode projectMode,
        ProjectGrammarKind grammarKind,
        String grammarVersion,
        String sourceFileName,
        Instant importedAt,
        List<FragmentBinding> fragmentBindings,
        Map<String, String> narrativeSummary,
        Map<String, String> theatreSummary,
        List<GrammarDiagnostic> diagnostics
) {
    public ProjectSemanticsDocument {
        schemaVersion = schemaVersion <= 0 ? 1 : schemaVersion;
        projectMode = Objects.requireNonNull(projectMode, "projectMode");
        grammarKind = Objects.requireNonNull(grammarKind, "grammarKind");
        grammarVersion = normalize(grammarVersion).isBlank() ? grammarKind.grammarVersion() : normalize(grammarVersion);
        sourceFileName = normalize(sourceFileName);
        importedAt = importedAt == null ? Instant.now() : importedAt;
        fragmentBindings = fragmentBindings == null ? List.of() : List.copyOf(fragmentBindings);
        narrativeSummary = narrativeSummary == null ? Map.of() : Map.copyOf(narrativeSummary);
        theatreSummary = theatreSummary == null ? Map.of() : Map.copyOf(theatreSummary);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public record FragmentBinding(
            String fragmentId,
            String segmentId,
            String blockId,
            String title,
            String semanticType,
            Map<String, String> metadata
    ) {
        public FragmentBinding {
            fragmentId = normalize(fragmentId);
            segmentId = normalize(segmentId);
            blockId = normalize(blockId);
            title = normalize(title);
            semanticType = normalize(semanticType);
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
