package com.marcosmoreiradev.docupodcaststudio.domain.storyboard;

import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Editable live-storyboard manifest associated with a narration script. */
public record StoryboardDocument(
        String id,
        String title,
        String sourceScriptId,
        List<StoryboardBinding> bindings,
        Map<String, String> layout,
        Instant createdAt,
        Instant updatedAt,
        String notes
) {
    public StoryboardDocument {
        id = normalizeToken(id, "STORYBOARD-001");
        title = title == null || title.isBlank() ? "Secuencia visual" : title.strip();
        sourceScriptId = normalizeToken(sourceScriptId, "SCRIPT-001");
        bindings = bindings == null ? List.of() : List.copyOf(bindings);
        layout = layout == null ? Map.of() : Map.copyOf(layout);
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
        notes = notes == null ? "" : notes.strip();
        validateUniqueBindings(bindings);
    }

    public static StoryboardDocument createForScript(NarrationScriptDocument script) {
        String title = script == null ? "Secuencia visual" : "Secuencia visual — " + script.title();
        String sourceScriptId = script == null ? "SCRIPT-001" : script.id();
        Instant now = Instant.now();
        return new StoryboardDocument("STORYBOARD-001", title, sourceScriptId, List.of(), Map.of(), now, now,
                "Panel visual básico. Asocia imágenes del usuario a fragmentos del documento.");
    }

    public Optional<StoryboardBinding> bindingForSegment(String segmentId) {
        if (segmentId == null || segmentId.isBlank()) {
            return Optional.empty();
        }
        return bindings.stream().filter(binding -> binding.segmentId().equals(segmentId.strip())).findFirst();
    }

    public StoryboardDocument withBinding(StoryboardBinding binding) {
        java.util.Objects.requireNonNull(binding, "binding");
        LinkedHashMap<String, StoryboardBinding> bySegment = new LinkedHashMap<>();
        for (StoryboardBinding existing : bindings) {
            bySegment.put(existing.segmentId(), existing);
        }
        bySegment.put(binding.segmentId(), binding);
        return new StoryboardDocument(id, title, sourceScriptId, List.copyOf(bySegment.values()), layout, createdAt, Instant.now(), notes);
    }

    public StoryboardDocument withoutBindingForSegment(String segmentId) {
        String target = normalizeToken(segmentId, "segmentId");
        List<StoryboardBinding> next = bindings.stream()
                .filter(binding -> !binding.segmentId().equals(target))
                .toList();
        return new StoryboardDocument(id, title, sourceScriptId, next, layout, createdAt, Instant.now(), notes);
    }

    public List<StoryboardScene> scenesFor(NarrationScriptDocument script) {
        if (script == null) {
            return List.of();
        }
        ArrayList<StoryboardScene> scenes = new ArrayList<>();
        int index = 1;
        for (NarrationSegment segment : script.segments()) {
            Optional<StoryboardBinding> binding = bindingForSegment(segment.id());
            String sceneId = "SCN-" + String.format(java.util.Locale.ROOT, "%03d", index++);
            scenes.add(new StoryboardScene(
                    sceneId,
                    segment.id(),
                    segment.title().isBlank() ? segment.id() : segment.title(),
                    segment.preview(120),
                    binding.map(StoryboardBinding::imageAssetId).orElse(""),
                    binding.map(StoryboardBinding::caption).orElse(""),
                    binding.map(StoryboardBinding::displayMode).orElse(StoryboardDisplayMode.FIT_CONTAIN),
                    binding.isPresent()
            ));
        }
        return List.copyOf(scenes);
    }


    public List<StoryboardBinding> bindingsForImage(String imageAssetId) {
        String target = imageAssetId == null ? "" : imageAssetId.strip();
        if (target.isBlank()) {
            return List.of();
        }
        return bindings.stream()
                .filter(binding -> binding.imageAssetId().equals(target))
                .toList();
    }

    public Map<String, List<StoryboardBinding>> bindingsByImageAssetId() {
        LinkedHashMap<String, List<StoryboardBinding>> result = new LinkedHashMap<>();
        for (StoryboardBinding binding : bindings) {
            java.util.ArrayList<StoryboardBinding> next = new java.util.ArrayList<>(
                    result.getOrDefault(binding.imageAssetId(), List.of()));
            next.add(binding);
            result.put(binding.imageAssetId(), List.copyOf(next));
        }
        return Map.copyOf(result);
    }

    public int bindingCount() {
        return bindings.size();
    }

    public long boundSegmentCount() {
        return bindings.stream().map(StoryboardBinding::segmentId).distinct().count();
    }

    private static void validateUniqueBindings(List<StoryboardBinding> bindings) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        LinkedHashSet<String> segments = new LinkedHashSet<>();
        for (StoryboardBinding binding : bindings) {
            if (!ids.add(binding.id())) {
                throw new IllegalArgumentException("Binding duplicado: " + binding.id());
            }
            if (!segments.add(binding.segmentId())) {
                throw new IllegalArgumentException("El segmento ya tiene una imagen asociada: " + binding.segmentId());
            }
        }
    }

    private static String normalizeToken(String value, String fallback) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            return fallback;
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("token must not contain whitespace: " + normalized);
        }
        return normalized;
    }
}
