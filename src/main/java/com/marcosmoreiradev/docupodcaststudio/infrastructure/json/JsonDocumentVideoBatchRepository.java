package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import com.marcosmoreiradev.docupodcaststudio.domain.export.AudioExportFormat;

import com.marcosmoreiradev.docupodcaststudio.application.batch.DocumentVideoBatchRepository;
import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextVideoBackgroundMode;
import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextVideoOptions;
import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextEffect;
import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentBackgroundImageFit;
import com.marcosmoreiradev.docupodcaststudio.domain.video.SimpleVideoResolutionPreset;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchBranding;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemStage;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemState;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.BrandingPlacement;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchItem;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchDraft;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchProject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** UTF-8 JSON repository with replace-on-save persistence for resumable batch state. */
public final class JsonDocumentVideoBatchRepository implements com.marcosmoreiradev.docupodcaststudio.application.batch.DocumentVideoBatchWorkspaceRepository {
    public void saveDraft(DocumentVideoBatchDraft draft, Path descriptor) throws IOException {
        Path absolute = descriptor.toAbsolutePath().normalize();
        if (absolute.getParent() != null) Files.createDirectories(absolute.getParent());
        atomicWrite(absolute, "{\n"
                + field("formatVersion", 1) + ",\n"
                + field("kind", "document-video-express-configuration") + ",\n"
                + field("title", draft.title()) + ",\n"
                + field("sourceRoot", draft.sourceRoot()) + ",\n"
                + field("destinationRoot", draft.destinationRoot()) + ",\n"
                + "  \"profile\": " + writeProfile(draft.profile()) + "\n}\n");
    }

    @SuppressWarnings("unchecked")
    public DocumentVideoBatchDraft openDraft(Path descriptor) throws IOException {
        Object parsed = SimpleJsonParser.parse(Files.readString(descriptor, StandardCharsets.UTF_8));
        if (!(parsed instanceof Map<?, ?> raw)) throw new IOException("Configuración Express inválida.");
        Map<String, Object> root = (Map<String, Object>) raw;
        if (!"document-video-express-configuration".equals(string(root, "kind"))) {
            throw new IOException("El archivo no es una configuración previa de Video Express.");
        }
        return new DocumentVideoBatchDraft(string(root, "title"), string(root, "sourceRoot"),
                string(root, "destinationRoot"), readProfile(map(root.get("profile"))));
    }

    @Override public void save(DocumentVideoBatchProject project, Path descriptor) throws IOException {
        Path absolute = descriptor.toAbsolutePath().normalize();
        Files.createDirectories(absolute.getParent());
        atomicWrite(absolute, write(project));
    }

    private static void atomicWrite(Path absolute, String contents) throws IOException {
        Path temporary = absolute.resolveSibling(absolute.getFileName() + ".tmp");
        Files.writeString(temporary, contents, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override @SuppressWarnings("unchecked")
    public DocumentVideoBatchProject open(Path descriptor) throws IOException {
        Object parsed = SimpleJsonParser.parse(Files.readString(descriptor, StandardCharsets.UTF_8));
        if (!(parsed instanceof Map<?, ?> raw)) throw new IOException("Descriptor de lote inválido.");
        Map<String, Object> root = (Map<String, Object>) raw;
        Map<String, Object> profileMap = map(root.get("profile"));
        DocumentVideoBatchProfile profile = readProfile(profileMap);
        List<DocumentVideoBatchItem> items = new ArrayList<>();
        Object itemValue = root.get("items");
        if (itemValue instanceof List<?> list) for (Object value : list) {
            Map<String, Object> item = map(value);
            items.add(new DocumentVideoBatchItem(string(item, "id"), integer(item, "order", items.size()),
                    string(item, "title"), string(item, "sourceRelativePath"), string(item, "copiedSourceRelativePath"),
                    string(item, "childProjectRelativePath"), string(item, "outputVideoRelativePath"), string(item, "sha256"),
                    number(item, "sourceBytes", 0), integer(item, "embeddedMediaCount", 0),
                    enumValue(BatchItemState.class, string(item, "state"), BatchItemState.PENDING),
                    enumValue(BatchItemStage.class, string(item, "stage"), BatchItemStage.DISCOVERED),
                    decimal(item, "progress", 0), string(item, "message"), instant(item, "updatedAt")));
        }
        return new DocumentVideoBatchProject(integer(root, "formatVersion", 1), string(root, "id"), string(root, "title"),
                string(root, "sourceRoot"), string(root, "projectRoot"), string(root, "descriptorRelativePath"), profile,
                items, integer(root, "ignoredFileCount", 0), instant(root, "createdAt"), instant(root, "updatedAt"));
    }

    private static DocumentVideoBatchProfile readProfile(Map<String, Object> profileMap) {
        Map<String, Object> videoMap = map(profileMap.get("video"));
        DocumentTextVideoOptions video = new DocumentTextVideoOptions(
                enumValue(SimpleVideoResolutionPreset.class, string(videoMap, "resolution"), SimpleVideoResolutionPreset.defaultPreset()),
                enumValue(DocumentTextVideoBackgroundMode.class, string(videoMap, "backgroundMode"), DocumentTextVideoBackgroundMode.SOLID_COLOR),
                string(videoMap, "backgroundColor"), string(videoMap, "backgroundImagePath"),
                string(videoMap, "textColor"), string(videoMap, "accentColor"), string(videoMap, "fontFamily"),
                stringOr(videoMap, "titleFontFamily", string(videoMap, "fontFamily")),
                integer(videoMap, "fontSize", 54), bool(videoMap, "underlineNarratedText", true),
                string(videoMap, "narratedUnderlineColor"), integer(videoMap, "narratedUnderlineThicknessPx", 4),
                decimal(videoMap, "backgroundImageOpacity", 0.35),
                enumValue(DocumentBackgroundImageFit.class, string(videoMap, "backgroundImageFit"), DocumentBackgroundImageFit.COVER),
                enumValue(DocumentTextEffect.class, string(videoMap, "textEffect"), DocumentTextEffect.NONE),
                string(videoMap, "textEffectColor"), integer(videoMap, "textEffectThicknessPx", 3));
        Map<String, Object> brandingMap = map(profileMap.get("branding"));
        BatchBranding branding = new BatchBranding(bool(brandingMap, "enabled", false),
                string(brandingMap, "sourcePath"), string(brandingMap, "projectRelativePath"),
                enumValue(BrandingPlacement.class, string(brandingMap, "placement"), BrandingPlacement.BOTTOM_RIGHT),
                integer(brandingMap, "sizePercent", 15), decimal(brandingMap, "opacity", 0.9));
        Map<String, com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentBackgroundOverride> backgrounds = new java.util.LinkedHashMap<>();
        map(profileMap.get("documentBackgrounds")).forEach((key, value) -> {
            var custom = map(value);
            backgrounds.put(key, new com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentBackgroundOverride(
                    string(custom, "imagePath"), custom.get("visibility") == null ? null : decimal(custom, "visibility", 0.35)));
        });
        return new DocumentVideoBatchProfile(video,
                decimal(profileMap, "imageSlideSeconds", 6), bool(profileMap, "interpretImages", false),
                branding,
                enumValue(com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchOutputKind.class,
                        string(profileMap, "outputKind"), com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchOutputKind.VIDEO),
                enumValue(com.marcosmoreiradev.docupodcaststudio.domain.export.AudioExportFormat.class,
                        string(profileMap, "audioFormat"), com.marcosmoreiradev.docupodcaststudio.domain.export.AudioExportFormat.MP3),
                string(profileMap, "voiceEngineId"), string(profileMap, "aiEngineId"), backgrounds);
    }

    private static String write(DocumentVideoBatchProject p) {
        StringBuilder out = new StringBuilder(8192).append("{\n")
                .append(field("formatVersion", p.formatVersion())).append(",\n")
                .append(field("id", p.id())).append(",\n").append(field("title", p.title())).append(",\n")
                .append(field("sourceRoot", p.sourceRoot())).append(",\n").append(field("projectRoot", p.projectRoot())).append(",\n")
                .append(field("descriptorRelativePath", p.descriptorRelativePath())).append(",\n")
                .append("  \"profile\": ").append(writeProfile(p.profile())).append(",\n")
                .append(field("ignoredFileCount", p.ignoredFileCount())).append(",\n")
                .append(field("createdAt", p.createdAt().toString())).append(",\n").append(field("updatedAt", p.updatedAt().toString())).append(",\n")
                .append("  \"items\": [\n");
        for (int i = 0; i < p.items().size(); i++) {
            if (i > 0) out.append(",\n");
            out.append(writeItem(p.items().get(i)));
        }
        return out.append("\n  ]\n}\n").toString();
    }

    private static String writeProfile(DocumentVideoBatchProfile p) {
        DocumentTextVideoOptions v = p.video(); BatchBranding b = p.branding();
        String backgrounds = p.documentBackgrounds().entrySet().stream().map(entry ->
                quote(entry.getKey()) + ":{" + raw("imagePath", entry.getValue().imagePath())
                        + ",\"visibility\":" + entry.getValue().visibility() + "}")
                .collect(java.util.stream.Collectors.joining(","));
        return "{\n    \"video\": {" + raw("resolution", v.resolution().name()) + "," + raw("backgroundMode", v.backgroundMode().name())
                + "," + raw("backgroundColor", v.backgroundColor()) + "," + raw("backgroundImagePath", v.backgroundImagePath())
                + "," + raw("textColor", v.textColor()) + "," + raw("accentColor", v.accentColor()) + "," + raw("fontFamily", v.fontFamily())
                + "," + raw("titleFontFamily", v.titleFontFamily())
                + ",\"fontSize\":" + v.fontSize() + ",\"underlineNarratedText\":" + v.underlineNarratedText()
                + "," + raw("narratedUnderlineColor", v.narratedUnderlineColor()) + ",\"narratedUnderlineThicknessPx\":" + v.narratedUnderlineThicknessPx()
                + ",\"backgroundImageOpacity\":" + v.backgroundImageOpacity()
                + "," + raw("backgroundImageFit", v.backgroundImageFit().name()) + "," + raw("textEffect", v.textEffect().name())
                + "," + raw("textEffectColor", v.textEffectColor()) + ",\"textEffectThicknessPx\":" + v.textEffectThicknessPx() + "},\n"
                + "    \"documentBackgrounds\": {" + backgrounds + "},\n"
                + "    " + raw("outputKind", p.outputKind().name()) + "," + raw("audioFormat", p.audioFormat().name()) + ",\n"
                + "    " + raw("voiceEngineId", p.voiceEngineId()) + "," + raw("aiEngineId", p.aiEngineId()) + ",\n"
                + "    \"imageSlideSeconds\":" + p.imageSlideSeconds() + ",\"interpretImages\":" + p.interpretImages() + ",\n"
                + "    \"branding\":{" + raw("enabled", b.enabled()) + "," + raw("sourcePath", b.sourcePath()) + "," + raw("projectRelativePath", b.projectRelativePath())
                + "," + raw("placement", b.placement().name()) + ",\"sizePercent\":" + b.sizePercent() + ",\"opacity\":" + b.opacity() + "}\n  }";
    }

    private static String writeItem(DocumentVideoBatchItem i) {
        return "    {" + raw("id", i.id()) + ",\"order\":" + i.order() + "," + raw("title", i.title()) + ","
                + raw("sourceRelativePath", i.sourceRelativePath()) + "," + raw("copiedSourceRelativePath", i.copiedSourceRelativePath()) + ","
                + raw("childProjectRelativePath", i.childProjectRelativePath()) + "," + raw("outputVideoRelativePath", i.outputVideoRelativePath()) + ","
                + raw("sha256", i.sha256()) + ",\"sourceBytes\":" + i.sourceBytes() + ",\"embeddedMediaCount\":" + i.embeddedMediaCount() + ","
                + raw("state", i.state().name()) + "," + raw("stage", i.stage().name()) + ",\"progress\":" + i.progress() + ","
                + raw("message", i.message()) + "," + raw("updatedAt", i.updatedAt().toString()) + "}";
    }

    private static String field(String key, Object value) { return "  " + raw(key, value); }
    private static String raw(String key, Object value) {
        String encoded = value instanceof Number || value instanceof Boolean ? String.valueOf(value) : quote(String.valueOf(value));
        return quote(key) + ":" + encoded;
    }
    private static String quote(String value) { return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""; }
    @SuppressWarnings("unchecked") private static Map<String, Object> map(Object value) { return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of(); }
    private static String string(Map<String, Object> map, String key) { Object value = map.get(key); return value == null ? "" : String.valueOf(value); }
    private static String stringOr(Map<String, Object> map, String key, String fallback) {
        String value = string(map, key);
        return value.isBlank() ? fallback : value;
    }
    private static int integer(Map<String, Object> map, String key, int fallback) { Object value = map.get(key); return value instanceof Number n ? n.intValue() : fallback; }
    private static long number(Map<String, Object> map, String key, long fallback) { Object value = map.get(key); return value instanceof Number n ? n.longValue() : fallback; }
    private static double decimal(Map<String, Object> map, String key, double fallback) { Object value = map.get(key); return value instanceof Number n ? n.doubleValue() : fallback; }
    private static boolean bool(Map<String, Object> map, String key, boolean fallback) { Object value = map.get(key); return value instanceof Boolean b ? b : fallback; }
    private static Instant instant(Map<String, Object> map, String key) { try { return Instant.parse(string(map, key)); } catch (RuntimeException ex) { return Instant.now(); } }
    private static <T extends Enum<T>> T enumValue(Class<T> type, String value, T fallback) { try { return Enum.valueOf(type, value); } catch (RuntimeException ex) { return fallback; } }
}
