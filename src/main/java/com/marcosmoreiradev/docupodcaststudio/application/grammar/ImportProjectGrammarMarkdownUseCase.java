package com.marcosmoreiradev.docupodcaststudio.application.grammar;

import com.marcosmoreiradev.docupodcaststudio.application.narrative.NarrativeVideoGrammarMarkdownParser;
import com.marcosmoreiradev.docupodcaststudio.application.narrative.NarrativeVideoImportPlan;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreBuiltInCameraCatalog;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreCameraApplicationPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar.TheatreGrammarMarkdownParser;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.ImportPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.InterventionPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Parses Markdown grammar contracts and materializes their derived project semantics sidecar. */
public final class ImportProjectGrammarMarkdownUseCase {
    private final ProjectSemanticsRepository semanticsRepository;

    public ImportProjectGrammarMarkdownUseCase(ProjectSemanticsRepository semanticsRepository) {
        this.semanticsRepository = Objects.requireNonNull(semanticsRepository, "semanticsRepository");
    }

    public NarrativeParseResult parseNarrative(Path sourceFile) throws IOException {
        NarrativeVideoImportPlan plan = NarrativeVideoGrammarMarkdownParser.parse(sourceFile);
        List<GrammarDiagnostic> diagnostics = narrativeDiagnostics(plan);
        diagnostics.addAll(plan.warnings().stream()
                .map(warning -> GrammarDiagnostic.warning("NARRATIVE_WARNING", warning))
                .toList());
        GrammarImportReport report = new GrammarImportReport(
                ProjectGrammarKind.NARRATIVE_VIDEO,
                ProjectGrammarKind.NARRATIVE_VIDEO.grammarVersion(),
                fileName(sourceFile),
                plan.fragmentCount(),
                0,
                plan.warnings().size(),
                false,
                diagnostics);
        return new NarrativeParseResult(plan, report);
    }

    public TheatreParseResult parseTheatre(Path sourceFile) throws IOException {
        var configured = new com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar.TheatreGrammarVoiceConfiguration()
                .enrich(sourceFile, TheatreGrammarMarkdownParser.parse(sourceFile));
        ImportPlan plan = configured.plan();
        List<GrammarDiagnostic> diagnostics = theatreDiagnostics(plan);
        diagnostics.addAll(configured.diagnostics());
        int created = plan.acts().size() + plan.characters().size() + plan.objects().size() + plan.interventions().size();
        GrammarImportReport report = new GrammarImportReport(
                ProjectGrammarKind.THEATRE_PRODUCTION,
                ProjectGrammarKind.THEATRE_PRODUCTION.grammarVersion(),
                fileName(sourceFile),
                created,
                0,
                0,
                false,
                diagnostics);
        return new TheatreParseResult(plan, report);
    }

    public GrammarImportReport materializeNarrative(
            DocuPodcastProject project,
            Path projectFile,
            Path sourceFile,
            NarrativeVideoImportPlan plan,
            NarrationScriptDocument script,
            GrammarImportReport parseReport) throws IOException {
        GrammarImportReport report = parseReport == null ? parseNarrative(sourceFile).report() : parseReport;
        if (projectFile == null) {
            return report.withAdditionalDiagnostics(List.of(GrammarDiagnostic.warning(
                    "SEMANTICS_NOT_MATERIALIZED",
                    "Guarda el proyecto para escribir project-semantics.json.")));
        }
        Path projectDirectory = projectFile.toAbsolutePath().normalize().getParent();
        if (projectDirectory == null) {
            return report.withAdditionalDiagnostics(List.of(GrammarDiagnostic.warning(
                    "SEMANTICS_NOT_MATERIALIZED",
                    "No se pudo resolver la carpeta del proyecto para project-semantics.json.")));
        }
        ProjectSemanticsDocument document = new ProjectSemanticsDocument(
                1,
                project.metadata().mode(),
                ProjectGrammarKind.NARRATIVE_VIDEO,
                ProjectGrammarKind.NARRATIVE_VIDEO.grammarVersion(),
                fileName(sourceFile),
                Instant.now(),
                narrativeBindings(plan, script),
                narrativeSummary(plan),
                Map.of(),
                report.diagnostics());
        semanticsRepository.save(projectDirectory, document);
        return report.withMaterialized(true);
    }

    public GrammarImportReport materializeTheatre(
            DocuPodcastProject project,
            Path projectFile,
            Path sourceFile,
            ImportPlan plan,
            NarrationScriptDocument script,
            GrammarImportReport parseReport) throws IOException {
        GrammarImportReport report = parseReport == null ? parseTheatre(sourceFile).report() : parseReport;
        if (projectFile == null) {
            return report.withAdditionalDiagnostics(List.of(GrammarDiagnostic.warning(
                    "SEMANTICS_NOT_MATERIALIZED",
                    "Guarda el proyecto para escribir project-semantics.json.")));
        }
        Path projectDirectory = projectFile.toAbsolutePath().normalize().getParent();
        if (projectDirectory == null) {
            return report.withAdditionalDiagnostics(List.of(GrammarDiagnostic.warning(
                    "SEMANTICS_NOT_MATERIALIZED",
                    "No se pudo resolver la carpeta del proyecto para project-semantics.json.")));
        }
        ProjectSemanticsDocument document = new ProjectSemanticsDocument(
                1,
                project.metadata().mode(),
                ProjectGrammarKind.THEATRE_PRODUCTION,
                ProjectGrammarKind.THEATRE_PRODUCTION.grammarVersion(),
                fileName(sourceFile),
                Instant.now(),
                theatreBindings(plan, script),
                Map.of(),
                theatreSummary(plan),
                report.diagnostics());
        semanticsRepository.save(projectDirectory, document);
        return report.withMaterialized(true);
    }

    private static List<ProjectSemanticsDocument.FragmentBinding> narrativeBindings(
            NarrativeVideoImportPlan plan,
            NarrationScriptDocument script) {
        if (plan == null) {
            return List.of();
        }
        List<NarrationSegment> segments = script == null ? List.of() : script.segments();
        ArrayList<ProjectSemanticsDocument.FragmentBinding> bindings = new ArrayList<>();
        int index = 0;
        for (NarrativeVideoImportPlan.FragmentPlan fragment : plan.fragments()) {
            NarrationSegment segment = index < segments.size() ? segments.get(index) : null;
            String segmentId = segment == null ? "" : segment.id();
            String blockId = firstBlockId(segment);
            LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
            metadata.put("order", Integer.toString(fragment.order()));
            metadata.put("tone", fragment.tone());
            metadata.put("visualPrompt", fragment.visualPrompt());
            metadata.put("bridgePrompt", fragment.bridgePrompt());
            metadata.put("hardCut", Boolean.toString(fragment.hardCut()));
            metadata.put("notes", fragment.notes());
            bindings.add(new ProjectSemanticsDocument.FragmentBinding(
                    fragmentId(segment, "NARRATIVE-" + fragment.order()),
                    segmentId,
                    blockId,
                    fragment.title(),
                    "narrativeFragment",
                    metadata));
            index++;
        }
        return bindings;
    }

    private static List<ProjectSemanticsDocument.FragmentBinding> theatreBindings(
            ImportPlan plan,
            NarrationScriptDocument script) {
        if (plan == null) {
            return List.of();
        }
        List<NarrationSegment> segments = script == null ? List.of() : script.segments();
        ArrayList<ProjectSemanticsDocument.FragmentBinding> bindings = new ArrayList<>();
        for (InterventionPlan intervention : plan.interventions()) {
            int index = Math.max(0, intervention.sequenceIndex() - 1);
            NarrationSegment segment = segments.stream().filter(s -> intervention.stableInterventionId()
                    .equals(s.metadata().get("theatreGrammarInterventionId"))).findFirst().orElseGet(() ->
                    intervention.spokenText().isBlank() && index < segments.size() ? segments.get(index) : null);
            String segmentId = segment == null ? "" : segment.id();
            String blockId = firstBlockId(segment);
            LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
            metadata.put("characterName", intervention.characterName());
            metadata.put("sceneName", intervention.sceneName());
            metadata.put("origin", intervention.origin());
            metadata.put("destination", intervention.destination());
            metadata.put("interactionTarget", intervention.interactionTarget());
            metadata.put("tone", intervention.tono());
            metadata.put("imageCount", Integer.toString(intervention.images().size()));
            metadata.put("cameraCue", intervention.cameraCue());
            if (!intervention.applyCamera()) {
                metadata.put(TheatreCameraApplicationPolicy.APPLY_CAMERA_METADATA, "false");
            }
            metadata.put("stageBackdrop", intervention.stageBackdrop());
            metadata.put("clearStageBackdrop", Boolean.toString(intervention.clearStageBackdrop()));
            metadata.put("simultaneousVoices", String.join(", ", intervention.simultaneousVoiceNames()));
            metadata.put("aiContextText", intervention.aiContextText());
            bindings.add(new ProjectSemanticsDocument.FragmentBinding(
                    fragmentId(segment, "THEATRE-" + intervention.sequenceIndex()),
                    segmentId,
                    blockId,
                    "Intervencion " + intervention.sequenceIndex(),
                    "theatreIntervention",
                    metadata));
        }
        return bindings;
    }

    private static List<GrammarDiagnostic> narrativeDiagnostics(NarrativeVideoImportPlan plan) {
        ArrayList<GrammarDiagnostic> diagnostics = new ArrayList<>();
        if (plan == null || plan.fragments().isEmpty()) {
            diagnostics.add(GrammarDiagnostic.error("NARRATIVE_NO_FRAGMENTS",
                    "No se encontraron fragmentos narrativos.", ""));
            return diagnostics;
        }
        for (NarrativeVideoImportPlan.FragmentPlan fragment : plan.fragments()) {
            if (fragment.text().isBlank()) {
                diagnostics.add(GrammarDiagnostic.warning("NARRATIVE_EMPTY_TEXT",
                        "Fragmento sin texto narrado: " + fragment.title()));
            }
            if (fragment.visualPrompt().isBlank()) {
                diagnostics.add(GrammarDiagnostic.warning("NARRATIVE_EMPTY_VISUAL",
                        "Fragmento sin prompt visual principal: " + fragment.title()));
            }
            String hardCut = Boolean.toString(fragment.hardCut());
            if (!hardCut.equals("true") && !hardCut.equals("false")) {
                diagnostics.add(GrammarDiagnostic.warning("NARRATIVE_HARD_CUT",
                        "Corte fuerte no reconocido en " + fragment.title()));
            }
        }
        return diagnostics;
    }

    private static List<GrammarDiagnostic> theatreDiagnostics(ImportPlan plan) {
        ArrayList<GrammarDiagnostic> diagnostics = new ArrayList<>();
        if (plan == null) {
            diagnostics.add(GrammarDiagnostic.error("THEATRE_EMPTY", "La gramatica teatral esta vacia.", ""));
            return diagnostics;
        }
        if (plan.acts().isEmpty()) {
            diagnostics.add(GrammarDiagnostic.warning("THEATRE_NO_ACTS", "La gramatica teatral no declara actos."));
        }
        if (plan.characters().isEmpty()) {
            diagnostics.add(GrammarDiagnostic.warning("THEATRE_NO_CHARACTERS", "La gramatica teatral no declara personajes."));
        }
        java.util.Set<String> characters = plan.characters().stream()
                .map(profile -> profile.name().toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        java.util.Set<String> scenes = plan.acts().stream()
                .flatMap(act -> act.scenes().stream())
                .map(scene -> scene.name().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        for (InterventionPlan intervention : plan.interventions()) {
            if (!intervention.stageDirection() && !intervention.characterName().isBlank()
                    && !characters.contains(intervention.characterName().toUpperCase(Locale.ROOT))) {
                diagnostics.add(GrammarDiagnostic.warning("THEATRE_UNKNOWN_CHARACTER",
                        "Intervencion con personaje no declarado: " + intervention.characterName()));
            }
            if (!intervention.sceneName().isBlank()
                    && !scenes.contains(intervention.sceneName().toLowerCase(Locale.ROOT))) {
                diagnostics.add(GrammarDiagnostic.warning("THEATRE_UNKNOWN_SCENE",
                        "Intervencion en escena no declarada: " + intervention.sceneName()));
            }
            if (!intervention.tono().isBlank() && !knownTone(intervention.tono())) {
                diagnostics.add(GrammarDiagnostic.warning("THEATRE_UNKNOWN_TONE",
                        "Tono no reconocido: " + intervention.tono()));
            }
            if (!intervention.cameraCue().isBlank() && !knownCamera(intervention.cameraCue())) {
                diagnostics.add(GrammarDiagnostic.warning("THEATRE_UNKNOWN_CAMERA",
                        "Plano teatral no reconocido: " + intervention.cameraCue()));
            }
            if (intervention.clearStageBackdrop() && !intervention.stageBackdrop().isBlank()) {
                diagnostics.add(GrammarDiagnostic.warning("THEATRE_BACKDROP_CONFLICT",
                        "Intervencion declara fondo y quitar_fondo a la vez: " + intervention.sequenceIndex()));
            }
            for (String image : intervention.images()) {
                if (image == null || image.isBlank()) {
                    diagnostics.add(GrammarDiagnostic.warning("THEATRE_EMPTY_IMAGE",
                            "Referencia de imagen vacia en intervencion " + intervention.sequenceIndex()));
                }
            }
        }
        return diagnostics;
    }

    private static Map<String, String> narrativeSummary(NarrativeVideoImportPlan plan) {
        if (plan == null) {
            return Map.of();
        }
        long visualPrompts = plan.fragments().stream().filter(fragment -> !fragment.visualPrompt().isBlank()).count();
        long bridgePrompts = plan.fragments().stream().filter(fragment -> !fragment.bridgePrompt().isBlank()).count();
        return Map.of(
                "title", plan.title(),
                "fragmentCount", Integer.toString(plan.fragmentCount()),
                "visualPromptCount", Long.toString(visualPrompts),
                "bridgePromptCount", Long.toString(bridgePrompts));
    }

    private static Map<String, String> theatreSummary(ImportPlan plan) {
        if (plan == null) {
            return Map.of();
        }
        int sceneCount = plan.acts().stream().mapToInt(act -> act.scenes().size()).sum();
        long cameraCueCount = plan.interventions().stream().filter(intervention -> !intervention.cameraCue().isBlank()).count();
        long backdropCueCount = plan.interventions().stream().filter(intervention -> !intervention.stageBackdrop().isBlank()).count()
                + plan.acts().stream().flatMap(act -> act.scenes().stream()).filter(scene -> !scene.stageBackdrop().isBlank()).count();
        long backdropClearCount = plan.interventions().stream().filter(InterventionPlan::clearStageBackdrop).count();
        long contextOverrideCount = plan.interventions().stream().filter(intervention -> !intervention.aiContextText().isBlank()).count();
        return Map.of(
                "title", plan.title(),
                "actCount", Integer.toString(plan.acts().size()),
                "sceneCount", Integer.toString(sceneCount),
                "characterCount", Integer.toString(plan.characters().size()),
                "objectCount", Integer.toString(plan.objects().size()),
                "interventionCount", Integer.toString(plan.interventions().size()),
                "cameraCueCount", Long.toString(cameraCueCount),
                "backdropCueCount", Long.toString(backdropCueCount),
                "backdropClearCount", Long.toString(backdropClearCount),
                "contextOverrideCount", Long.toString(contextOverrideCount));
    }

    private static String fragmentId(NarrationSegment segment, String fallback) {
        String blockId = firstBlockId(segment);
        if (!blockId.isBlank()) {
            return FragmentId.fromBlockId(blockId).value();
        }
        if (segment != null && !segment.id().isBlank()) {
            return FragmentId.fromSegmentId(segment.id()).value();
        }
        return "FRG-" + fallback;
    }

    private static String firstBlockId(NarrationSegment segment) {
        if (segment == null || segment.sourceBlockIds().isEmpty()) {
            return "";
        }
        return segment.sourceBlockIds().get(0);
    }

    private static boolean knownTone(String tone) {
        try {
            VoiceReferenceTone.valueOf(tone.strip().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean knownCamera(String cameraCue) {
        return TheatreBuiltInCameraCatalog.contains(cameraCue);
    }

    private static String fileName(Path path) {
        return path == null || path.getFileName() == null ? "" : path.getFileName().toString();
    }

    public record NarrativeParseResult(NarrativeVideoImportPlan plan, GrammarImportReport report) {
    }

    public record TheatreParseResult(ImportPlan plan, GrammarImportReport report) {
    }
}
