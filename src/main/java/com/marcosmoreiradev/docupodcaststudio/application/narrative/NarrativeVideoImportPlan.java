package com.marcosmoreiradev.docupodcaststudio.application.narrative;

import java.util.List;
import java.util.Map;

/** Parsed Markdown grammar for a Video narrativo project. */
public record NarrativeVideoImportPlan(
        String title,
        Map<String, String> metadata,
        List<FragmentPlan> fragments,
        List<String> warnings
) {
    public NarrativeVideoImportPlan(String title, List<FragmentPlan> fragments, List<String> warnings) {
        this(title, Map.of(), fragments, warnings);
    }

    public NarrativeVideoImportPlan {
        title = title == null || title.isBlank() ? "Video narrativo" : title.strip();
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        fragments = fragments == null ? List.of() : List.copyOf(fragments);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public int fragmentCount() {
        return fragments.size();
    }

    public record FragmentPlan(
            int order,
            String title,
            String text,
            String visualPrompt,
            String bridgePrompt,
            boolean hardCut,
            String notes,
            String tone
    ) {
        public FragmentPlan(int order, String title, String text, String visualPrompt, String bridgePrompt,
                            boolean hardCut, String notes) {
            this(order, title, text, visualPrompt, bridgePrompt, hardCut, notes, "");
        }

        public FragmentPlan {
            order = Math.max(1, order);
            title = normalize(title).isBlank() ? "Fragmento " + order : normalize(title);
            text = normalize(text);
            visualPrompt = normalize(visualPrompt);
            bridgePrompt = normalize(bridgePrompt);
            notes = normalize(notes);
            tone = normalize(tone);
        }

        private static String normalize(String value) {
            return value == null ? "" : value.strip();
        }
    }
}
