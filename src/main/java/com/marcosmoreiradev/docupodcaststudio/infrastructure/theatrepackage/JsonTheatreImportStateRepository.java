package com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.TheatreImportStateRepository;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatreImportState;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageEntry;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.AtomicJsonFileWriter;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.SimpleJsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Atomic repository for the generated theatre synchronization state. */
public final class JsonTheatreImportStateRepository implements TheatreImportStateRepository {
    public static final String FILE_NAME = "theatre-import-state.json";
    private final AtomicJsonFileWriter writer = new AtomicJsonFileWriter();

    @Override
    @SuppressWarnings("unchecked")
    public Optional<TheatreImportState> open(Path projectRoot) throws IOException {
        Path file = file(projectRoot);
        if (!Files.isRegularFile(file)) return Optional.empty();
        Object parsed;
        String json = Files.readString(file, StandardCharsets.UTF_8);
        try {
            parsed = SimpleJsonParser.parse(json);
        } catch (IOException | RuntimeException invalidJson) {
            throw new IOException("Estado de importación teatral ilegible: JSON inválido.", invalidJson);
        }
        if (!(parsed instanceof Map<?, ?> raw)) throw new IOException("Estado de importación teatral inválido.");
        Map<String, Object> root = (Map<String, Object>) raw;
        int schema = number(root.get("schemaVersion"), 0).intValue();
        if (schema != TheatreImportState.CURRENT_SCHEMA_VERSION) {
            throw new IOException("Versión de estado teatral no soportada: " + schema);
        }
        ArrayList<TheatrePackageEntry> entries = new ArrayList<>();
        if (root.get("entries") instanceof List<?> list) for (Object value : list) {
            if (!(value instanceof Map<?, ?> entryRaw)) throw new IOException("Entrada de estado teatral inválida.");
            Map<String, Object> entry = (Map<String, Object>) entryRaw;
            Map<String, String> metadata = new LinkedHashMap<>();
            if (entry.get("metadata") instanceof Map<?, ?> metadataRaw) {
                metadataRaw.forEach((key, item) -> metadata.put(String.valueOf(key), item == null ? "" : String.valueOf(item)));
            }
            try {
                entries.add(new TheatrePackageEntry(string(entry, "logicalId"),
                        TheatrePackageAssetKind.valueOf(string(entry, "kind")), string(entry, "relativePath"),
                        string(entry, "sha256"), number(entry.get("size"), 0).longValue(), metadata));
            } catch (RuntimeException ex) {
                throw new IOException("Entrada de estado teatral incompatible.", ex);
            }
        }
        try {
            return Optional.of(new TheatreImportState(schema, string(root, "packageId"),
                    string(root, "packageVersion"), string(root, "sourceRoot"),
                    string(root, "inventoryFingerprint"), Instant.parse(string(root, "appliedAt")), entries));
        } catch (RuntimeException ex) {
            throw new IOException("Estado de importación teatral incompatible.", ex);
        }
    }

    @Override
    public void save(TheatreImportState state, Path projectRoot) throws IOException {
        writer.write(file(projectRoot), write(state));
    }

    private static Path file(Path projectRoot) throws IOException {
        if (projectRoot == null) throw new IOException("La carpeta del proyecto es obligatoria.");
        Path root = projectRoot.toAbsolutePath().normalize();
        Files.createDirectories(root);
        return root.resolve(FILE_NAME);
    }

    private static String write(TheatreImportState state) {
        StringBuilder json = new StringBuilder(4096).append("{\n")
                .append("  \"schemaVersion\": ").append(state.schemaVersion()).append(",\n")
                .append("  \"packageId\": ").append(quote(state.packageId())).append(",\n")
                .append("  \"packageVersion\": ").append(quote(state.packageVersion())).append(",\n")
                .append("  \"sourceRoot\": ").append(quote(state.sourceRoot())).append(",\n")
                .append("  \"inventoryFingerprint\": ").append(quote(state.inventoryFingerprint())).append(",\n")
                .append("  \"appliedAt\": ").append(quote(state.appliedAt().toString())).append(",\n")
                .append("  \"entries\": [\n");
        for (int i = 0; i < state.entries().size(); i++) {
            TheatrePackageEntry entry = state.entries().get(i);
            if (i > 0) json.append(",\n");
            json.append("    {\"logicalId\":").append(quote(entry.logicalId()))
                    .append(",\"kind\":").append(quote(entry.kind().name()))
                    .append(",\"relativePath\":").append(quote(entry.relativePath()))
                    .append(",\"sha256\":").append(quote(entry.sha256()))
                    .append(",\"size\":").append(entry.size()).append(",\"metadata\":{");
            int metadataIndex = 0;
            for (var metadata : entry.metadata().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                if (metadataIndex++ > 0) json.append(',');
                json.append(quote(metadata.getKey())).append(':').append(quote(metadata.getValue()));
            }
            json.append("}}");
        }
        return json.append("\n  ]\n}\n").toString();
    }

    private static String quote(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }
    private static String string(Map<String, Object> map, String key) { Object value = map.get(key); return value == null ? "" : String.valueOf(value); }
    private static Number number(Object value, Number fallback) { return value instanceof Number number ? number : fallback; }
}
