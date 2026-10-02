package com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.TheatrePackageScanner;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageEntry;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageInventory;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.SimpleJsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Scanner for {@code docupodcast-theatre.json} plus convention-based additions under assets/. */
public final class JsonTheatrePackageScanner implements com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.OfficialTheatrePackageAccess {
    public static final String MANIFEST_FILE = "docupodcast-theatre.json";
    @Override public com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.OfficialTheatrePackageAccess.Source open(Path source) throws IOException {
        return TheatrePackageSource.open(source);
    }

    public String presentationMode(Path root) throws IOException {
        Object parsed = SimpleJsonParser.parse(Files.readString(root.resolve(MANIFEST_FILE), StandardCharsets.UTF_8));
        if (!(parsed instanceof Map<?, ?> map)) throw new IOException("Manifiesto teatral inválido");
        Object mode = map.get("presentationMode");
        if (mode == null) return "fragments";
        if (!(mode instanceof String value) || !Set.of("fragments", "characters", "scenery", "none").contains(value))
            throw new IOException("presentationMode debe ser fragments, characters, scenery o none");
        return (String) mode;
    }
    public Path grammarFile(Path root) throws IOException {
        Object parsed = SimpleJsonParser.parse(Files.readString(root.resolve(MANIFEST_FILE), StandardCharsets.UTF_8));
        if (!(parsed instanceof Map<?, ?> map)) throw new IOException("Manifiesto teatral inválido");
        Object declared = map.get("grammar");
        String relative = declared instanceof String s && !s.isBlank() ? s : "obra.teatro.md";
        return resolveInside(root.toRealPath(), relative);
    }
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "webp", "wav", "mp3", "flac", "m4a", "ogg", "mp4");

    @Override
    @SuppressWarnings("unchecked")
    public TheatrePackageInventory scan(Path sourceRoot) throws IOException {
        if (sourceRoot == null) throw new IOException("La carpeta de obra es obligatoria.");
        Path root = sourceRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) throw new IOException("La carpeta de obra no existe: " + root);
        Path realRoot = root.toRealPath();
        Path manifest = realRoot.resolve(MANIFEST_FILE);
        if (!Files.isRegularFile(manifest)) {
            throw new IOException("Falta " + MANIFEST_FILE + " en la carpeta de obra.");
        }
        Object parsed;
        try {
            String manifestText = Files.readString(manifest, StandardCharsets.UTF_8);
            if (!manifestText.strip().startsWith("{") || !manifestText.strip().endsWith("}")) {
                throw new IllegalArgumentException("JSON object is incomplete");
            }
            parsed = SimpleJsonParser.parse(manifestText);
        } catch (RuntimeException invalidJson) {
            throw new IOException("No se pudo leer " + MANIFEST_FILE + ": JSON inválido.", invalidJson);
        }
        if (!(parsed instanceof Map<?, ?> raw)) throw new IOException("El manifiesto teatral debe ser un objeto JSON.");
        Map<String, Object> json = (Map<String, Object>) raw;
        int schemaVersion = integer(json.get("schemaVersion"), 1);
        if (schemaVersion != 1 && schemaVersion != 2) throw new IOException("Versión de manifiesto teatral no soportada: " + schemaVersion);
        String packageId = requiredString(json.get("packageId"), "packageId");
        String packageVersion = optionalString(json.get("packageVersion"));
        if (schemaVersion == 2) {
            String grammarVersion = requiredString(json.get("grammarVersion"), "grammarVersion");
            if (!grammarVersion.equals("theatre-v2")) throw new IOException("grammarVersion debe ser theatre-v2.");
            String grammar = optionalString(json.get("grammar"));
            if (grammar.isBlank()) grammar = "obra.teatro.md";
            Path grammarFile = resolveInside(realRoot, grammar);
            if (!Files.isRegularFile(grammarFile)) throw new IOException("Falta la gramática declarada: " + grammar);
        }

        LinkedHashMap<String, TheatrePackageEntry> entriesByPath = new LinkedHashMap<>();
        Object assets = json.get("assets");
        if (assets instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> assetRaw)) throw new IOException("Cada asset debe ser un objeto JSON.");
                Map<String, Object> asset = (Map<String, Object>) assetRaw;
                TheatrePackageEntry entry = explicitEntry(realRoot, asset);
                TheatrePackageEntry previous = entriesByPath.put(entry.relativePath(), entry);
                if (previous != null) throw new IOException("Ruta de asset duplicada: " + entry.relativePath());
                if (schemaVersion == 2 && asset.get("bindings") instanceof List<?> bindings) {
                    int index = 0;
                    for (Object binding : bindings) {
                        if (!(binding instanceof Map<?, ?> use)) throw new IOException("Binding de asset inválido");
                        Map<String, Object> declaration = new LinkedHashMap<>();
                        for (String key : List.of("path", "kind", "sha256", "size")) if (asset.containsKey(key)) declaration.put(key, asset.get(key));
                        // A use may change its role and target, never the physical file or identity.
                        for (var field : use.entrySet()) {
                            String key = field.getKey().toString();
                            if (Set.of("path", "logicalId", "id", "sha256", "size", "bindings").contains(key))
                                throw new IOException("Campo reservado en binding: " + key);
                            declaration.put(key, field.getValue());
                        }
                        declaration.put("logicalId", entry.logicalId() + "-USE-" + (++index));
                        declaration.put("packageAssetLogicalId", entry.logicalId());
                        TheatrePackageEntry extra = explicitEntry(realRoot, declaration);
                        entriesByPath.put(entry.relativePath() + "#" + extra.logicalId(), extra);
                    }
                }
            }
        }

        Path assetsRoot = realRoot.resolve("assets");
        if (schemaVersion == 1 && Files.isDirectory(assetsRoot)) {
            try (var paths = Files.walk(assetsRoot)) {
                for (Path file : paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)).toList()) {
                    String relative = normalizedRelative(realRoot, file);
                    if (!supported(file) || entriesByPath.containsKey(relative)) continue;
                    TheatrePackageEntry conventional = conventionalEntry(realRoot, file, relative);
                    if (conventional != null) entriesByPath.put(relative, conventional);
                }
            }
        }
        try {
            return TheatrePackageInventory.create(schemaVersion, packageId, packageVersion, realRoot.toString(),
                    new ArrayList<>(entriesByPath.values()));
        } catch (IllegalArgumentException invalidManifest) {
            throw new IOException("El manifiesto teatral contiene IDs o rutas inválidas: "
                    + invalidManifest.getMessage(), invalidManifest);
        }
    }

    private static TheatrePackageEntry explicitEntry(Path root, Map<String, Object> asset) throws IOException {
        String path = requiredString(asset.get("path"), "assets[].path").replace('\\', '/');
        Path file = resolveInside(root, path);
        TheatrePackageAssetKind kind = parseKind(requiredString(first(asset, "kind", "type"), "assets[].kind"));
        String logicalId = optionalString(first(asset, "logicalId", "id"));
        Map<String, String> metadata = scalarMetadata(asset, Set.of("path", "kind", "type", "logicalId", "id", "sha256", "size"));
        if (logicalId.isBlank()) {
            TheatrePackageEntry derived = conventionalEntry(root, file, path);
            if (derived == null) throw new IOException("El asset requiere logicalId: " + path);
            logicalId = derived.logicalId();
            HashMap<String, String> merged = new HashMap<>(derived.metadata());
            merged.putAll(metadata);
            metadata = Map.copyOf(merged);
        }
        TheatrePackageEntry result = entry(file, path, logicalId, kind, metadata);
        String declaredHash = optionalString(asset.get("sha256"));
        if (!declaredHash.isBlank() && !declaredHash.equalsIgnoreCase(result.sha256())) {
            throw new IOException("Hash SHA-256 incorrecto para " + path);
        }
        long declaredSize = longValue(asset.get("size"), -1L);
        if (declaredSize >= 0 && declaredSize != result.size()) {
            throw new IOException("Tamaño incorrecto para " + path + ": esperado " + declaredSize + ", real " + result.size());
        }
        return result;
    }

    private static TheatrePackageEntry conventionalEntry(Path root, Path file, String relative) throws IOException {
        String[] parts = relative.split("/");
        if (parts.length < 3 || !parts[0].equalsIgnoreCase("assets")) return null;
        String group = slug(parts[1]);
        String base = slug(withoutExtension(parts[parts.length - 1]));
        Map<String, String> metadata;
        String id;
        TheatrePackageAssetKind kind;
        switch (group) {
            case "personajes", "characters" -> {
                if (parts.length < 4) return null;
                String characterId = slug(parts[2]);
                id = "character:" + characterId + ":view:" + base;
                kind = TheatrePackageAssetKind.CHARACTER_IMAGE;
                metadata = Map.of("characterId", characterId, "view", base);
            }
            case "objetos", "objects" -> {
                if (parts.length < 4) return null;
                String objectId = slug(parts[2]);
                id = "object:" + objectId + ":view:" + base;
                kind = TheatrePackageAssetKind.OBJECT_IMAGE;
                metadata = Map.of("objectId", objectId, "view", base);
            }
            case "fondos", "backdrops" -> {
                id = "backdrop:" + base;
                kind = TheatrePackageAssetKind.BACKDROP;
                metadata = Map.of("backdropId", base);
            }
            case "mapas", "maps" -> {
                id = "spatial-map:" + base;
                kind = TheatrePackageAssetKind.SPATIAL_MAP;
                metadata = Map.of("sceneId", base);
            }
            case "intervenciones", "interventions" -> {
                id = "intervention:" + base + ":image";
                kind = TheatrePackageAssetKind.INTERVENTION_IMAGE;
                metadata = Map.of("interventionId", interventionId(base));
            }
            case "puentes", "bridges" -> {
                String[] endpoints = base.split("__", 2);
                if (endpoints.length != 2) return null;
                id = "bridge:" + endpoints[0] + ":" + endpoints[1];
                kind = TheatrePackageAssetKind.INTERMEDIATE_FRAME;
                metadata = Map.of("fromInterventionId", interventionId(endpoints[0]),
                        "toInterventionId", interventionId(endpoints[1]));
            }
            case "audio" -> {
                if (parts.length < 4) return null;
                String sceneId = slug(parts[2]);
                id = "scene:" + sceneId + ":intervention:" + base + ":audio";
                kind = TheatrePackageAssetKind.HUMAN_AUDIO;
                metadata = Map.of("sceneId", sceneId, "interventionId", interventionId(base));
            }
            case "voces", "voices" -> {
                id = "voice-sample:" + base;
                kind = TheatrePackageAssetKind.VOICE_SAMPLE;
                metadata = Map.of("voiceId", base);
            }
            case "video", "videos" -> {
                id = "video:" + base;
                kind = TheatrePackageAssetKind.VIDEO;
                metadata = Map.of();
            }
            default -> { return null; }
        }
        return entry(file, relative, id, kind, metadata);
    }

    private static TheatrePackageEntry entry(Path file, String relative, String id,
                                              TheatrePackageAssetKind kind, Map<String, String> metadata) throws IOException {
        if (!Files.isRegularFile(file)) throw new IOException("Asset inexistente: " + relative);
        return new TheatrePackageEntry(id, kind, relative, sha256(file), Files.size(file), metadata);
    }

    private static Path resolveInside(Path root, String relative) throws IOException {
        if (relative.isBlank() || Path.of(relative).isAbsolute()) throw new IOException("Ruta de asset inválida: " + relative);
        Path candidate = root.resolve(relative).normalize();
        if (!candidate.startsWith(root)) throw new IOException("Asset fuera de la carpeta de obra: " + relative);
        if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Asset declarado inexistente: " + relative);
        if (Files.isSymbolicLink(candidate)) throw new IOException("No se permiten enlaces simbólicos en paquetes teatrales: " + relative);
        Path real = candidate.toRealPath();
        if (!real.startsWith(root)) throw new IOException("Asset enlazado fuera de la carpeta de obra: " + relative);
        return real;
    }

    private static String normalizedRelative(Path root, Path file) throws IOException {
        Path real = file.toRealPath();
        if (!real.startsWith(root)) throw new IOException("Asset fuera de la carpeta de obra: " + file);
        return root.relativize(real).toString().replace('\\', '/');
    }

    private static boolean supported(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && SUPPORTED_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private static TheatrePackageAssetKind parseKind(String value) throws IOException {
        String normalized = value.strip().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        try { return TheatrePackageAssetKind.valueOf(normalized); }
        catch (IllegalArgumentException ex) { throw new IOException("Tipo de asset teatral no soportado: " + value, ex); }
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[64 * 1024];
                for (int read; (read = input.read(buffer)) >= 0;) {
                    if (read > 0) digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private static Map<String, String> scalarMetadata(Map<String, Object> source, Set<String> ignored) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (!ignored.contains(key) && value != null && !(value instanceof Map<?, ?>) && !(value instanceof List<?>)) {
                result.put(key, String.valueOf(value));
            }
        });
        return Map.copyOf(result);
    }

    private static Object first(Map<String, Object> map, String first, String second) {
        Object value = map.get(first);
        return value == null ? map.get(second) : value;
    }

    private static String requiredString(Object value, String field) throws IOException {
        String normalized = optionalString(value);
        if (normalized.isBlank()) throw new IOException(field + " es obligatorio.");
        return normalized;
    }

    private static String optionalString(Object value) { return value == null ? "" : String.valueOf(value).strip(); }
    private static int integer(Object value, int fallback) { return value instanceof Number number ? number.intValue() : fallback; }
    private static long longValue(Object value, long fallback) { return value instanceof Number number ? number.longValue() : fallback; }
    private static String withoutExtension(String value) { int dot = value.lastIndexOf('.'); return dot < 0 ? value : value.substring(0, dot); }
    private static String slug(String value) {
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized.isBlank() ? "item" : normalized;
    }
    private static String interventionId(String value) {
        String normalized = value.strip().toUpperCase(Locale.ROOT).replace('_', '-');
        if (normalized.matches("I-?\\d+")) normalized = normalized.replaceFirst("I-?", "INTERVENCION-");
        if (normalized.matches("INTERVENCION-\\d+")) {
            String digits = normalized.substring("INTERVENCION-".length());
            try { return "INTERVENCION-" + Integer.parseInt(digits); }
            catch (NumberFormatException ignored) { return normalized; }
        }
        return value.strip();
    }
}
