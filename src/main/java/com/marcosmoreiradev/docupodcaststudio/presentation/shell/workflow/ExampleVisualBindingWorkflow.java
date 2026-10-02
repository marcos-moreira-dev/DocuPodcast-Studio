package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.WorkspaceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.decisions.UserVisibleDecision;
import com.marcosmoreiradev.docupodcaststudio.application.examples.ExampleVisualBindingDescriptor;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Imports bundled demo visuals and anchors them to document fragments as IMAGE layers. */
public final class ExampleVisualBindingWorkflow {
    private final NarrativeLayerCoordinator layerCoordinator = new NarrativeLayerCoordinator();

    public Result bind(WorkspaceApplicationServices services,
                       ProjectSession session,
                       Path projectFile,
                       ReadableDocument document,
                       NarrationScriptDocument script,
                       List<Path> visualAssets,
                       List<ExampleVisualBindingDescriptor> bindings) throws IOException {
        Objects.requireNonNull(services, "services");
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(projectFile, "projectFile");
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(script, "script");
        Map<String, Path> assetsByName = assetsByName(visualAssets);
        int imported = 0;
        int assigned = 0;
        int skipped = 0;
        for (ExampleVisualBindingDescriptor binding : bindings == null ? List.<ExampleVisualBindingDescriptor>of() : bindings) {
            Path asset = assetsByName.get(binding.assetFileName());
            Optional<NarrationSegment> segment = segmentForBinding(script, document, binding);
            if (asset == null || segment.isEmpty()) {
                skipped++;
                continue;
            }
            var importedImage = services.generation().storyboard().importImageAsset().importImage(session.project(), projectFile, asset);
            imported++;
            session.replaceProject(importedImage.project(), true);
            Optional<DocumentBlock> block = firstSourceBlock(document, segment.get());
            if (block.isEmpty()) {
                skipped++;
                continue;
            }
            DocumentTextRange documentRange = new DocumentTextRange(block.get().id(), 0, block.get().text().length());
            ScriptTextRange scriptRange = new ScriptTextRange(segment.get().id(), 0, segment.get().narrationText().length());
            NarrativeLayerCoordinator.AssignmentOutcome outcome = layerCoordinator.assign(
                    session,
                    NarrativeLayerKind.IMAGE,
                    scriptRange,
                    documentRange,
                    document,
                    block.get().id(),
                    block.get().preview(60),
                    importedImage.imageAsset().id());
            if (outcome.assigned()) {
                assigned++;
            } else {
                skipped++;
            }
        }
        return new Result(imported, assigned, skipped);
    }

    private static Map<String, Path> assetsByName(List<Path> assets) {
        Map<String, Path> result = new LinkedHashMap<>();
        for (Path asset : assets == null ? List.<Path>of() : assets) {
            if (asset != null && asset.getFileName() != null) {
                result.put(asset.getFileName().toString(), asset);
            }
        }
        return result;
    }

    private static Optional<NarrationSegment> segmentForBinding(NarrationScriptDocument script,
                                                                ReadableDocument document,
                                                                ExampleVisualBindingDescriptor binding) {
        if (binding != null && binding.hasAnchorText()) {
            Optional<NarrationSegment> anchored = segmentByAnchor(script, document, binding.anchorText());
            if (anchored.isPresent()) {
                return anchored;
            }
        }
        return segmentAt(script, binding == null ? -1 : binding.fragmentIndex());
    }

    private static Optional<NarrationSegment> segmentByAnchor(NarrationScriptDocument script, ReadableDocument document, String anchorText) {
        String needle = normalizeForSearch(anchorText);
        if (script == null || document == null || needle.isBlank()) {
            return Optional.empty();
        }
        for (NarrationSegment segment : script.segments()) {
            for (String blockId : segment.sourceBlockIds()) {
                Optional<DocumentBlock> block = document.blockById(blockId);
                if (block.isPresent() && containsAnchor(block.get().text(), needle)) {
                    return Optional.of(segment);
                }
            }
            if (containsAnchor(segment.narrationText(), needle)) {
                return Optional.of(segment);
            }
        }
        return Optional.empty();
    }

    private static boolean containsAnchor(String text, String normalizedAnchor) {
        String haystack = normalizeForSearch(text);
        return !normalizedAnchor.isBlank() && (haystack.startsWith(normalizedAnchor) || haystack.contains(normalizedAnchor));
    }

    private static String normalizeForSearch(String value) {
        return value == null ? "" : value
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .strip()
                .toLowerCase(java.util.Locale.ROOT);
    }

    private static Optional<NarrationSegment> segmentAt(NarrationScriptDocument script, int oneBasedIndex) {
        if (script == null || oneBasedIndex < 1 || oneBasedIndex > script.segments().size()) {
            return Optional.empty();
        }
        return Optional.of(script.segments().get(oneBasedIndex - 1));
    }

    private static Optional<DocumentBlock> firstSourceBlock(ReadableDocument document, NarrationSegment segment) {
        return segment.sourceBlockIds().stream().map(document::blockById).flatMap(Optional::stream).findFirst();
    }

    public record Result(int imported, int assigned, int skipped) {
        public boolean hasFallbacks() {
            return skipped > 0;
        }

        public String message() {
            if (hasFallbacks()) {
                return "Demo visual creado con advertencias: " + assigned + " imagenes asociadas y " + skipped
                        + " sin asociar. Puedes completar las pendientes desde el panel Imagen.";
            }
            return "Demo visual creado: " + assigned + " imagenes copiadas al proyecto y asociadas a fragmentos.";
        }

        public Optional<UserVisibleDecision> userDecision(String exampleTitle) {
            if (!hasFallbacks()) {
                return Optional.empty();
            }
            return Optional.of(UserVisibleDecision.defensiveFallback(
                    "Demo creado con visuales pendientes",
                    "El demo \"" + normalizeTitle(exampleTitle) + "\" se creó, pero " + skipped
                            + " imagen(es) no pudieron asociarse automáticamente. Puedes completarlas desde Documento > Imagen.",
                    "imported=" + imported + "; assigned=" + assigned + "; skipped=" + skipped));
        }

        private static String normalizeTitle(String title) {
            return title == null || title.isBlank() ? "seleccionado" : title.strip();
        }
    }
}
