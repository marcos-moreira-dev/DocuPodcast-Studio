package com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar;

import com.marcosmoreiradev.docupodcaststudio.application.grammar.GrammarDiagnostic;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.ImportPlan;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Resolves references against the original grammar, before it is copied into a project. */
public final class TheatreGrammarAssetImporter {
    @FunctionalInterface public interface ImportAsset { String apply(Path source) throws IOException; }
    public record Result(Map<String, String> assetIds, List<GrammarDiagnostic> diagnostics) { }

    public Result importAssets(Path source, ImportPlan plan, ImportAsset importer) throws IOException {
        Path root = source.toAbsolutePath().getParent().toRealPath();
        Set<String> paths = new LinkedHashSet<>();
        plan.characters().forEach(p -> p.imagenes().forEach(i -> paths.add(i.path())));
        plan.objects().forEach(p -> p.imagenes().forEach(i -> paths.add(i.path())));
        plan.acts().forEach(a -> a.scenes().forEach(s -> { paths.add(s.spatialMap()); paths.add(s.stageBackdrop()); }));
        plan.interventions().forEach(i -> { paths.addAll(i.images()); paths.add(i.stageBackdrop()); });
        Map<String, String> ids = new LinkedHashMap<>();
        Map<Path, String> imported = new HashMap<>();
        List<GrammarDiagnostic> diagnostics = new ArrayList<>();
        for (String reference : paths) {
            if (reference == null || reference.isBlank()) continue;
            try {
                Path relative = Path.of(reference.replace('\\', '/'));
                Path resolved = root.resolve(relative).normalize();
                if (relative.isAbsolute() || !resolved.startsWith(root)) throw new IOException("Ruta fuera de la carpeta de la obra");
                if (!Files.isRegularFile(resolved)) throw new IOException("Archivo no encontrado");
                Path real = resolved.toRealPath();
                if (!real.startsWith(root)) throw new IOException("Enlace fuera de la carpeta de la obra");
                String id = imported.get(real);
                if (id == null) { id = importer.apply(real); imported.put(real, id); }
                ids.put(reference, id);
            } catch (IOException | RuntimeException failure) {
                diagnostics.add(GrammarDiagnostic.warning("THEATRE_ASSET_UNRESOLVED", reference + ": " + failure.getMessage()));
            }
        }
        return new Result(Map.copyOf(ids), List.copyOf(diagnostics));
    }
}
