package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentRectangle;
import com.marcosmoreiradev.docupodcaststudio.application.video.NarratedFrameBinding;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Writes the exact immutable decisions later consumed by preflight and the renderer. */
public final class NarratedFrameBindingManifestWriter {
    public static final String TSV_PATH = "exports/frame-binding-manifest.tsv";
    public static final String JSON_PATH = "exports/frame-binding-manifest.json";
    private static final String LEGACY_TSV_PATH = "exports/narrated-frame-bindings.tsv";
    private static final String LEGACY_JSON_PATH = "exports/narrated-frame-bindings.json";
    public static final String DIAGNOSTIC_TSV_PATH = "exports/frame-binding-manifest-diagnostic.tsv";
    public static final String DIAGNOSTIC_JSON_PATH = "exports/frame-binding-manifest-diagnostic.json";

    public Result write(SimpleVideoPlan plan, Path projectDirectory) throws IOException {
        Path root = projectDirectory.toAbsolutePath().normalize();
        Path tsv = safe(root, TSV_PATH);
        Path json = safe(root, JSON_PATH);
        Files.createDirectories(tsv.getParent());
        ArrayList<IndexedBinding> bindings = indexed(plan);
        String tsvText = tsv(bindings);
        String jsonText = json(bindings);
        Files.writeString(tsv, tsvText, StandardCharsets.UTF_8);
        Files.writeString(json, jsonText, StandardCharsets.UTF_8);
        Files.writeString(safe(root, LEGACY_TSV_PATH), tsvText, StandardCharsets.UTF_8);
        Files.writeString(safe(root, LEGACY_JSON_PATH), jsonText, StandardCharsets.UTF_8);
        return new Result(tsv, json, bindings.size());
    }

    public Result writeDiagnostic(SimpleVideoPlan plan, Path projectDirectory) throws IOException {
        Path root = projectDirectory.toAbsolutePath().normalize();
        Path tsv = safe(root, DIAGNOSTIC_TSV_PATH);
        Path json = safe(root, DIAGNOSTIC_JSON_PATH);
        Files.createDirectories(tsv.getParent());
        ArrayList<IndexedBinding> bindings = indexed(plan);
        Files.writeString(tsv, tsv(bindings), StandardCharsets.UTF_8);
        Files.writeString(json, json(bindings), StandardCharsets.UTF_8);
        return new Result(tsv, json, bindings.size());
    }

    private static ArrayList<IndexedBinding> indexed(SimpleVideoPlan plan) {
        ArrayList<IndexedBinding> bindings = new ArrayList<>();
        for (int index = 0; index < plan.frames().size(); index++) {
            NarratedFrameBinding binding = plan.frames().get(index).visualBinding();
            if (binding != null) bindings.add(new IndexedBinding(index, binding));
        }
        return bindings;
    }

    private static String tsv(List<IndexedBinding> bindings) {
        StringBuilder out = new StringBuilder("frameId\ttimelineIndex\tsegmentId\tsourceBlockId\tsourceBlockIds\tregionId\tpage\treadingOrder"
                + "\tsourceBlockType\tpresentationMode\tvisualSource\tsourceBBox\tcropBBox"
                + "\timageRelativePath\taudioPath\tdurationMillis\trenderedTextHash"
                + "\texpectedSourceTextHash\tsourceTextStart\tsourceTextEnd\tsourceTextPreview"
                + "\tsourceLineIndices\tsourceWordIndices\texpectedFragmentBboxes\tfragmentCovered"
                + "\tqualityClassification\tdiagnosticReason\n");
        for (IndexedBinding indexed : bindings) {
            NarratedFrameBinding value = indexed.binding();
            List<String> fields = new ArrayList<>();
            fields.add(value.frameId());
            fields.add(Integer.toString(indexed.timelineIndex()));
            fields.add(value.segmentId());
            fields.add(value.sourceBlockId());
            fields.add(String.join(",", value.sourceBlockIds()));
            fields.add(value.regionId());
            fields.add(Integer.toString(value.pageNumber()));
            fields.add(Integer.toString(value.readingOrder()));
            fields.add(value.sourceBlockType());
            fields.add(value.presentationMode().name());
            fields.add(value.visualSource().name());
            fields.add(rectangle(value.sourceBBox()));
            fields.add(rectangle(value.cropBBox()));
            fields.add(value.imageRelativePath());
            fields.add(value.audioPath());
            fields.add(Long.toString(value.durationMillis()));
            fields.add(value.renderedTextHash());
            fields.add(value.expectedSourceTextHash());
            fields.add(Integer.toString(value.sourceTextStart()));
            fields.add(Integer.toString(value.sourceTextEnd()));
            fields.add(value.sourceTextPreview());
            fields.add(value.sourceLineIndices().stream().map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(",")));
            fields.add(value.sourceWordIndices().stream().map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(",")));
            fields.add(value.expectedFragmentBboxes().stream()
                    .map(NarratedFrameBindingManifestWriter::rectangle)
                    .collect(java.util.stream.Collectors.joining(";")));
            fields.add(Boolean.toString(value.fragmentCovered()));
            fields.add(value.quality().name());
            fields.add(value.reason());
            out.append(fields.stream().map(NarratedFrameBindingManifestWriter::tsvField)
                    .collect(java.util.stream.Collectors.joining("\t"))).append('\n');
        }
        return out.toString();
    }

    private static String json(List<IndexedBinding> bindings) {
        StringBuilder out = new StringBuilder("{\n  \"version\": 2,\n  \"bindings\": [\n");
        for (int index = 0; index < bindings.size(); index++) {
            IndexedBinding indexed = bindings.get(index);
            NarratedFrameBinding value = indexed.binding();
            out.append("    {")
                    .append(field("frameId", value.frameId())).append(',')
                    .append("\"timelineIndex\":").append(indexed.timelineIndex()).append(',')
                    .append(field("segmentId", value.segmentId())).append(',')
                    .append(field("sourceBlockId", value.sourceBlockId())).append(',')
                    .append(field("sourceBlockIds", String.join(",", value.sourceBlockIds()))).append(',')
                    .append(field("regionId", value.regionId())).append(',')
                    .append("\"page\":").append(value.pageNumber()).append(',')
                    .append("\"readingOrder\":").append(value.readingOrder()).append(',')
                    .append(field("sourceBlockType", value.sourceBlockType())).append(',')
                    .append(field("presentationMode", value.presentationMode().name())).append(',')
                    .append(field("visualSource", value.visualSource().name())).append(',')
                    .append(field("sourceBBox", rectangle(value.sourceBBox()))).append(',')
                    .append(field("cropBBox", rectangle(value.cropBBox()))).append(',')
                    .append(field("imageRelativePath", value.imageRelativePath())).append(',')
                    .append(field("audioPath", value.audioPath())).append(',')
                    .append("\"durationMillis\":").append(value.durationMillis()).append(',')
                    .append(field("renderedTextHash", value.renderedTextHash())).append(',')
                    .append(field("expectedSourceTextHash", value.expectedSourceTextHash())).append(',')
                    .append("\"sourceTextStart\":").append(value.sourceTextStart()).append(',')
                    .append("\"sourceTextEnd\":").append(value.sourceTextEnd()).append(',')
                    .append(field("sourceTextPreview", value.sourceTextPreview())).append(',')
                    .append(field("sourceLineIndices", value.sourceLineIndices().stream()
                            .map(String::valueOf).collect(java.util.stream.Collectors.joining(",")))).append(',')
                    .append(field("sourceWordIndices", value.sourceWordIndices().stream()
                            .map(String::valueOf).collect(java.util.stream.Collectors.joining(",")))).append(',')
                    .append(field("expectedFragmentBboxes", value.expectedFragmentBboxes().stream()
                            .map(NarratedFrameBindingManifestWriter::rectangle)
                            .collect(java.util.stream.Collectors.joining(";")))).append(',')
                    .append("\"fragmentCovered\":").append(value.fragmentCovered()).append(',')
                    .append(field("qualityClassification", value.quality().name())).append(',')
                    .append(field("diagnosticReason", value.reason())).append('}');
            if (index + 1 < bindings.size()) out.append(',');
            out.append('\n');
        }
        return out.append("  ]\n}\n").toString();
    }

    private static String rectangle(DocumentContentRectangle value) {
        return value == null ? "" : String.format(Locale.ROOT, "%.4f,%.4f,%.4f,%.4f",
                value.xMin(), value.yMin(), value.xMax(), value.yMax());
    }

    private static String tsvField(String value) {
        return value == null ? "" : value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }

    private static String field(String name, String value) {
        return "\"" + name + "\":\"" + jsonString(value) + "\"";
    }

    private static String jsonString(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\")
                .replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }

    private static Path safe(Path root, String relative) throws IOException {
        Path result = root.resolve(relative).normalize();
        if (!result.startsWith(root)) throw new IOException("Ruta de manifest fuera del proyecto.");
        return result;
    }

    public record Result(Path tsv, Path json, int bindingCount) { }

    private record IndexedBinding(int timelineIndex, NarratedFrameBinding binding) { }
}
