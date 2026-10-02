package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.video.NarratedFrameBinding;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoFrame;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Diagnostic-only audit; it never deduplicates or changes the export timeline. */
public final class NarrationFragmentAuditWriter {
    public static final String RELATIVE_PATH = "exports/fragment-binding-audit.json";

    public Report write(SimpleVideoPlan plan, Path projectRoot) throws IOException {
        ArrayList<Entry> entries = new ArrayList<>();
        NarratedFrameBinding previous = null;
        for (SimpleVideoFrame frame : plan.frames()) {
            NarratedFrameBinding current = frame.visualBinding();
            if (!frame.audioRequired() || current == null) continue;
            boolean duplicate = previous != null && duplicate(previous, current);
            entries.add(new Entry(frame.id(), frame.segmentId(), current.sourceBlockId(),
                    current.regionId(), current.sourceTextStart(), current.sourceTextEnd(),
                    current.sourceTextPreview(), current.expectedFragmentBboxes().toString(),
                    current.fragmentCovered(), current.quality().name(), current.reason(),
                    duplicate, duplicate ? previous.segmentId() : ""));
            previous = current;
        }
        Report report = new Report(List.copyOf(entries));
        Path target = projectRoot.resolve(RELATIVE_PATH).toAbsolutePath().normalize();
        if (!target.startsWith(projectRoot.toAbsolutePath().normalize())) {
            throw new IOException("Ruta de auditoria fuera del proyecto.");
        }
        Files.createDirectories(target.getParent());
        Files.writeString(target, json(report), StandardCharsets.UTF_8);
        return report;
    }

    static boolean duplicate(NarratedFrameBinding first, NarratedFrameBinding second) {
        if (first == null || second == null || first.pageNumber() != second.pageNumber()
                || !first.regionId().equals(second.regionId())) return false;
        int overlap = Math.max(0, Math.min(first.sourceTextEnd(), second.sourceTextEnd())
                - Math.max(first.sourceTextStart(), second.sourceTextStart()));
        int shortest = Math.max(1, Math.min(first.sourceTextEnd() - first.sourceTextStart(),
                second.sourceTextEnd() - second.sourceTextStart()));
        if ((double) overlap / shortest < 0.80) return false;
        return tokenOverlap(first.sourceTextPreview(), second.sourceTextPreview()) >= 0.80;
    }

    private static double tokenOverlap(String first, String second) {
        Set<String> left = tokens(first);
        Set<String> right = tokens(second);
        if (left.isEmpty() || right.isEmpty()) return 0.0;
        long common = left.stream().filter(right::contains).count();
        return (double) common / Math.min(left.size(), right.size());
    }

    private static Set<String> tokens(String value) {
        HashSet<String> result = new HashSet<>();
        for (String token : (value == null ? "" : value).toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ").strip().split("\\s+")) {
            if (!token.isBlank()) result.add(token);
        }
        return result;
    }

    private static String json(Report report) {
        StringBuilder out = new StringBuilder("{\n  \"version\":1,\n  \"spokenFrames\":")
                .append(report.spokenFrames()).append(",\n  \"fragmentCovered\":")
                .append(report.fragmentCovered()).append(",\n  \"wrongVisual\":")
                .append(report.wrongVisual()).append(",\n  \"duplicateNarration\":")
                .append(report.duplicateNarration()).append(",\n  \"entries\":[\n");
        for (int i = 0; i < report.entries().size(); i++) {
            Entry e = report.entries().get(i);
            if (i > 0) out.append(",\n");
            out.append("    {\"frameId\":\"").append(escape(e.frameId()))
                    .append("\",\"segmentId\":\"").append(escape(e.segmentId()))
                    .append("\",\"sourceBlockId\":\"").append(escape(e.sourceBlockId()))
                    .append("\",\"regionId\":\"").append(escape(e.regionId()))
                    .append("\",\"sourceTextStart\":").append(e.sourceTextStart())
                    .append(",\"sourceTextEnd\":").append(e.sourceTextEnd())
                    .append(",\"sourceTextPreview\":\"").append(escape(e.sourceTextPreview()))
                    .append("\",\"expectedFragmentBboxes\":\"").append(escape(e.expectedFragmentBboxes()))
                    .append("\",\"fragmentCovered\":").append(e.fragmentCovered())
                    .append(",\"qualityClassification\":\"").append(escape(e.qualityClassification()))
                    .append("\",\"diagnosticReason\":\"").append(escape(e.diagnosticReason()))
                    .append("\",\"duplicateNarration\":").append(e.duplicateNarration())
                    .append(",\"duplicateOf\":\"").append(escape(e.duplicateOf())).append("\"}");
        }
        return out.append("\n  ]\n}\n").toString();
    }

    private static String escape(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\")
                .replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    public record Entry(String frameId, String segmentId, String sourceBlockId, String regionId,
                        int sourceTextStart, int sourceTextEnd, String sourceTextPreview,
                        String expectedFragmentBboxes, boolean fragmentCovered,
                        String qualityClassification, String diagnosticReason,
                        boolean duplicateNarration, String duplicateOf) { }

    public record Report(List<Entry> entries) {
        public int spokenFrames() { return entries.size(); }
        public long fragmentCovered() { return entries.stream().filter(Entry::fragmentCovered).count(); }
        public long wrongVisual() { return entries.stream()
                .filter(entry -> "WRONG_VISUAL".equals(entry.qualityClassification())).count(); }
        public long duplicateNarration() { return entries.stream().filter(Entry::duplicateNarration).count(); }
    }
}
