package com.marcosmoreiradev.docupodcaststudio.application.observability;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Exports a local, sanitized diagnostic archive. It never uploads data. */
public interface SupportBundleExporter {
    ExportResult export(ExportRequest request) throws IOException;

    record ExportRequest(Path destination, Path logDirectory, Path projectRoot,
                         List<Path> manifests, Map<String, String> diagnostics) {
        public ExportRequest {
            manifests = manifests == null ? List.of() : List.copyOf(manifests);
            diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
        }
    }

    record ExportResult(Path archive, long bytes, List<String> omittedEntries) {
        public ExportResult {
            omittedEntries = omittedEntries == null ? List.of() : List.copyOf(omittedEntries);
        }
    }
}
