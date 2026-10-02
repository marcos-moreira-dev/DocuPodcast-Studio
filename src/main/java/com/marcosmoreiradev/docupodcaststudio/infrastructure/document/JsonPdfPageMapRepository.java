package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageMapRepository;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarrationBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfObjectNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageGeometry;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageMap;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageMapManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageNode;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageNodeKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfReviewState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfSemanticTextLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfTextStructure;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.SimpleJsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Atomic UTF-8 JSON sidecar at document/page-maps, separate from V3 pages. */
public final class JsonPdfPageMapRepository implements PdfPageMapRepository {
    public static final String MANIFEST_RELATIVE_PATH = "document/page-maps/manifest.json";
    private final AtomicUtf8JsonFileWriter writer = new AtomicUtf8JsonFileWriter();

    @Override
    public Optional<PdfPageMapManifest> loadManifest(Path projectRoot) throws IOException {
        Path path = manifestPath(root(projectRoot));
        if (!Files.isRegularFile(path)) return Optional.empty();
        Map<String, Object> map = object(parse(path), "page-map manifest");
        return Optional.of(new PdfPageMapManifest(integer(map, "schemaVersion"),
                string(map, "sourceSha256"), string(map, "builderSignature"),
                integers(map.get("pageNumbers")), Instant.parse(string(map, "updatedAt"))));
    }

    @Override
    public Optional<PdfPageMap> loadPage(Path projectRoot, int pageNumber) throws IOException {
        Path path = pagePath(root(projectRoot), pageNumber);
        if (!Files.isRegularFile(path)) return Optional.empty();
        Map<String, Object> map = object(parse(path), "page-map");
        ArrayList<PdfPageNode> nodes = new ArrayList<>();
        for (Object value : array(map.get("nodes"))) nodes.add(readNode(object(value, "node")));
        ArrayList<PdfNarrationBinding> bindings = new ArrayList<>();
        for (Object value : array(map.get("narrationBindings"))) {
            Map<String, Object> binding = object(value, "binding");
            bindings.add(new PdfNarrationBinding(integer(binding, "pageNumber"), strings(binding.get("sourceRegionIds")),
                    PdfSemanticTextLayer.valueOf(string(binding, "sourceLayer")),
                    PdfObjectNarrationPolicy.valueOf(string(binding, "policy")),
                    string(binding, "interpretationId"), longNumber(binding, "sourceRevision"),
                    string(binding, "sourceFingerprint")));
        }
        return Optional.of(new PdfPageMap(integer(map, "schemaVersion"), integer(map, "pageNumber"),
                longNumber(map, "sourcePageRevision"), string(map, "builderSignature"),
                readGeometry(object(map.get("pageGeometry"), "pageGeometry")), nodes, bindings));
    }

    @Override
    public synchronized void savePage(Path projectRoot, String sourceSha256, PdfPageMap pageMap) throws IOException {
        Path root = root(projectRoot);
        writer.write(pagePath(root, pageMap.pageNumber()), pageJson(pageMap));
        List<Integer> pages = loadManifest(root)
                .filter(value -> value.sourceSha256().equals(sourceSha256)
                        && value.builderSignature().equals(pageMap.builderSignature()))
                .map(PdfPageMapManifest::pageNumbers).orElse(List.of());
        ArrayList<Integer> updated = new ArrayList<>(pages);
        updated.add(pageMap.pageNumber());
        PdfPageMapManifest manifest = new PdfPageMapManifest(PdfPageMapManifest.CURRENT_SCHEMA_VERSION,
                sourceSha256, pageMap.builderSignature(), updated, Instant.now());
        writer.write(manifestPath(root), manifestJson(manifest));
    }

    private static PdfPageNode readNode(Map<String, Object> node) throws IOException {
        ArrayList<PdfPageGeometry> parts = new ArrayList<>();
        for (Object value : array(node.get("geometryParts"))) parts.add(readGeometry(object(value, "geometryPart")));
        Map<String, Object> text = object(node.get("text"), "text");
        Map<String, Object> evidence = object(node.get("evidence"), "evidence");
        Map<String, Object> review = object(node.get("reviewState"), "reviewState");
        return new PdfPageNode(string(node, "id"), string(node, "legacyRegionId"),
                string(node, "parentId"), PdfPageNodeKind.valueOf(string(node, "kind")),
                string(node, "semanticType"), integer(node, "order"),
                readGeometry(object(node.get("geometry"), "geometry")), parts,
                new PdfTextStructure(string(text, "literalText"), integer(text, "startOffset"),
                        integer(text, "endOffset"), integer(text, "sentenceIndex")),
                new PdfEvidence(string(evidence, "origin"), decimal(evidence, "confidence"),
                        string(evidence, "extractorSignature")),
                new PdfReviewState(bool(review, "humanOverride"), string(review, "state")),
                strings(node.get("childIds")));
    }

    private static PdfPageGeometry readGeometry(Map<String, Object> geometry) throws IOException {
        return new PdfPageGeometry(decimal(geometry, "xMin"), decimal(geometry, "yMin"),
                decimal(geometry, "xMax"), decimal(geometry, "yMax"),
                decimal(geometry, "pageWidth"), decimal(geometry, "pageHeight"),
                string(geometry, "coordinateSpace"));
    }

    private static String manifestJson(PdfPageMapManifest manifest) {
        return "{\n" + field("schemaVersion", manifest.schemaVersion(), true)
                + field("sourceSha256", manifest.sourceSha256(), true)
                + field("builderSignature", manifest.builderSignature(), true)
                + fieldRaw("pageNumbers", arrayNumbers(manifest.pageNumbers()), true)
                + field("updatedAt", manifest.updatedAt().toString(), false) + "}\n";
    }

    private static String pageJson(PdfPageMap page) {
        return "{\n" + field("schemaVersion", page.schemaVersion(), true)
                + field("pageNumber", page.pageNumber(), true)
                + field("sourcePageRevision", page.sourcePageRevision(), true)
                + field("builderSignature", page.builderSignature(), true)
                + fieldRaw("pageGeometry", geometryJson(page.pageGeometry()), true)
                + fieldRaw("nodes", arrayJson(page.nodes().stream().map(JsonPdfPageMapRepository::nodeJson).toList()), true)
                + fieldRaw("narrationBindings", arrayJson(page.narrationBindings().stream()
                        .map(JsonPdfPageMapRepository::bindingJson).toList()), false) + "}\n";
    }

    private static String nodeJson(PdfPageNode node) {
        return "{" + compact("id", node.id(), true) + compact("legacyRegionId", node.legacyRegionId(), true)
                + compact("parentId", node.parentId(), true) + compact("kind", node.kind().name(), true)
                + compact("semanticType", node.semanticType(), true) + compactRaw("order", Integer.toString(node.order()), true)
                + compactRaw("geometry", geometryJson(node.geometry()), true)
                + compactRaw("geometryParts", arrayJson(node.geometryParts().stream().map(JsonPdfPageMapRepository::geometryJson).toList()), true)
                + compactRaw("text", textJson(node.text()), true) + compactRaw("evidence", evidenceJson(node.evidence()), true)
                + compactRaw("reviewState", reviewJson(node.reviewState()), true)
                + compactRaw("childIds", arrayStrings(node.childIds()), false) + "}";
    }

    private static String geometryJson(PdfPageGeometry value) {
        return "{" + compactRaw("xMin", number(value.xMin()), true) + compactRaw("yMin", number(value.yMin()), true)
                + compactRaw("xMax", number(value.xMax()), true) + compactRaw("yMax", number(value.yMax()), true)
                + compactRaw("pageWidth", number(value.pageWidth()), true) + compactRaw("pageHeight", number(value.pageHeight()), true)
                + compact("coordinateSpace", value.coordinateSpace(), false) + "}";
    }

    private static String textJson(PdfTextStructure value) {
        return "{" + compact("literalText", value.literalText(), true)
                + compactRaw("startOffset", Integer.toString(value.startOffset()), true)
                + compactRaw("endOffset", Integer.toString(value.endOffset()), true)
                + compactRaw("sentenceIndex", Integer.toString(value.sentenceIndex()), false) + "}";
    }

    private static String evidenceJson(PdfEvidence value) {
        return "{" + compact("origin", value.origin(), true)
                + compactRaw("confidence", number(value.confidence()), true)
                + compact("extractorSignature", value.extractorSignature(), false) + "}";
    }

    private static String reviewJson(PdfReviewState value) {
        return "{" + compactRaw("humanOverride", Boolean.toString(value.humanOverride()), true)
                + compact("state", value.state(), false) + "}";
    }

    private static String bindingJson(PdfNarrationBinding value) {
        return "{" + compactRaw("pageNumber", Integer.toString(value.pageNumber()), true)
                + compactRaw("sourceRegionIds", arrayStrings(value.sourceRegionIds()), true)
                + compact("sourceLayer", value.sourceLayer().name(), true) + compact("policy", value.policy().name(), true)
                + compact("interpretationId", value.interpretationId(), true)
                + compactRaw("sourceRevision", Long.toString(value.sourceRevision()), true)
                + compact("sourceFingerprint", value.sourceFingerprint(), false) + "}";
    }

    private static Path root(Path root) { return root.toAbsolutePath().normalize(); }
    private static Path directory(Path root) { return root.resolve("document/page-maps").normalize(); }
    private static Path manifestPath(Path root) { return directory(root).resolve("manifest.json"); }
    private static Path pagePath(Path root, int number) { return directory(root).resolve("page-%06d.json".formatted(number)); }
    private static Object parse(Path path) throws IOException { return SimpleJsonParser.parse(Files.readString(path, StandardCharsets.UTF_8)); }
    @SuppressWarnings("unchecked") private static Map<String,Object> object(Object value, String field) throws IOException { if (value instanceof Map<?,?> map) return (Map<String,Object>) map; throw new IOException(field + " must be an object"); }
    @SuppressWarnings("unchecked") private static List<Object> array(Object value) throws IOException { if (value instanceof List<?> list) return (List<Object>) list; throw new IOException("value must be an array"); }
    private static String string(Map<String,Object> map, String key) throws IOException { Object value=map.get(key); if (value instanceof String text) return text; throw new IOException(key + " must be text"); }
    private static int integer(Map<String,Object> map, String key) throws IOException { return Math.toIntExact(longNumber(map,key)); }
    private static long longNumber(Map<String,Object> map, String key) throws IOException { Object value=map.get(key); if(value instanceof Number number)return number.longValue(); throw new IOException(key+" must be a number"); }
    private static double decimal(Map<String,Object> map, String key) throws IOException { Object value=map.get(key); if(value instanceof Number number)return number.doubleValue(); throw new IOException(key+" must be a number"); }
    private static boolean bool(Map<String,Object> map, String key) throws IOException { Object value=map.get(key); if(value instanceof Boolean flag)return flag; throw new IOException(key+" must be boolean"); }
    private static List<String> strings(Object value) throws IOException { ArrayList<String> values=new ArrayList<>(); for(Object item:array(value)){if(!(item instanceof String text))throw new IOException("array item must be text"); values.add(text);} return List.copyOf(values); }
    private static List<Integer> integers(Object value) throws IOException { ArrayList<Integer> values=new ArrayList<>(); for(Object item:array(value)){if(!(item instanceof Number number))throw new IOException("array item must be number"); values.add(number.intValue());} return List.copyOf(values); }
    private static String field(String key, String value, boolean comma){return "  "+quote(key)+": "+quote(value)+(comma?",":"")+"\n";}
    private static String field(String key, long value, boolean comma){return fieldRaw(key,Long.toString(value),comma);}
    private static String fieldRaw(String key,String value,boolean comma){return "  "+quote(key)+": "+value+(comma?",":"")+"\n";}
    private static String compact(String key,String value,boolean comma){return compactRaw(key,quote(value),comma);}
    private static String compactRaw(String key,String value,boolean comma){return quote(key)+":"+value+(comma?",":"");}
    private static String arrayJson(List<String> values){return "["+String.join(",",values)+"]";}
    private static String arrayStrings(List<String> values){return arrayJson(values.stream().map(JsonPdfPageMapRepository::quote).toList());}
    private static String arrayNumbers(List<Integer> values){return arrayJson(values.stream().map(String::valueOf).toList());}
    private static String number(double value){return Double.toString(value);}
    private static String quote(String value){String text=value==null?"":value; return "\""+text.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t")+"\"";}
}
