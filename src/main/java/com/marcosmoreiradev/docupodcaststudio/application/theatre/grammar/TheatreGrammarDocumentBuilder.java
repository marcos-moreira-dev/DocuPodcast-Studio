package com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar;

import com.marcosmoreiradev.docupodcaststudio.domain.document.*;
import com.marcosmoreiradev.docupodcaststudio.domain.script.*;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.ImportPlan;
import java.nio.file.Path;
import java.util.*;

/** Explicit grammar projection. Ordinary Markdown never enters this boundary. */
public final class TheatreGrammarDocumentBuilder {
    public static final String INTERVENTION_ID = "theatreGrammarInterventionId";

    public ReadableDocument build(ImportPlan plan, Path source) {
        List<DocumentBlock> blocks = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (var intervention : plan.interventions()) {
            String id = intervention.stableInterventionId();
            if (!ids.add(id)) throw new IllegalArgumentException("Intervención duplicada: " + id);
            if (intervention.spokenText().isBlank()) continue;
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put(INTERVENTION_ID, id);
            metadata.put("theatreGrammarSegmentId", "SEG-" + id);
            metadata.put("characterName", intervention.characterName());
            var profile = plan.characters().stream().filter(p -> p.name().equalsIgnoreCase(intervention.characterName())
                    || p.id().equalsIgnoreCase(intervention.characterName())
                    || p.aliases().stream().anyMatch(a -> a.equalsIgnoreCase(intervention.characterName())))
                    .findFirst();
            metadata.put("theatreGrammarCharacterId",
                    com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImportUseCase.stableEntityId(
                            profile.map(p -> p.id()).orElse(""), "CHR", profile.map(p -> p.name()).orElse(intervention.characterName())));
            metadata.put("sceneName", intervention.sceneName());
            metadata.put("origin", intervention.origin());
            metadata.put("destination", intervention.destination());
            metadata.put("interactionTarget", intervention.interactionTarget());
            metadata.put("theatreApplyCamera", Boolean.toString(intervention.applyCamera()));
            metadata.put("theatreAiContext", intervention.aiContextText());
            metadata.put("generationSource", "theatre-grammar");
            if (intervention.stageDirection()) {
                metadata.put("theatreStageDirection", "true");
                metadata.put("logicalOnly", "true");
                metadata.put("narratability", "NON_NARRATABLE");
                metadata.put("tone", "NEUTRAL");
            }
            blocks.add(new DocumentBlock("B-" + id, DocumentBlockType.PARAGRAPH,
                    intervention.spokenText(), intervention.stageDirection() ? "theatre-stage-direction" : "theatre-dialogue", metadata));
        }
        return new ReadableDocument(plan.title(), SourceDocumentFormat.MARKDOWN, source, blocks);
    }

    public static NarrationSegment segment(DocumentBlock block) {
        boolean stageDirection = Boolean.parseBoolean(block.metadata().getOrDefault("theatreStageDirection", "false"));
        return new NarrationSegment(block.metadata().get("theatreGrammarSegmentId"), NarrationSegmentType.PARAGRAPH,
                stageDirection ? "Acotación" : block.metadata().get("characterName"), block.text(), List.of(block.id()),
                block.metadata().get("theatreGrammarCharacterId"),
                "VOC-NARRATOR", "STY-NEUTRAL", block.metadata());
    }
}
