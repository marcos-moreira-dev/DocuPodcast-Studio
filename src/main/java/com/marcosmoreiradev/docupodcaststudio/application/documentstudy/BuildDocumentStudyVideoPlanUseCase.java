package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoFrame;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds documentary paragraph and silent-table frames in original DOCX order. */
public final class BuildDocumentStudyVideoPlanUseCase {
    private static final String FRAME_DIRECTORY = "exports/document-study-frames";
    private final DocumentStudyVideoContentResolver contentResolver;
    private final DocumentStudySlideCompositor compositor;

    public BuildDocumentStudyVideoPlanUseCase() {
        this(new DocumentStudyVideoContentResolver(), new DocumentStudySlideCompositor());
    }

    public BuildDocumentStudyVideoPlanUseCase(DocumentStudyVideoContentResolver contentResolver,
                                              DocumentStudySlideCompositor compositor) {
        this.contentResolver = contentResolver;
        this.compositor = compositor;
    }

    public SimpleVideoPlan build(DocuPodcastProject project,
                                 ReadableDocument document,
                                 NarrationScriptDocument script,
                                 List<AudioJobSnapshot> jobs,
                                 Path projectDirectory,
                                 DocumentTextVideoOptions options) throws IOException {
        if (project == null) throw new IllegalArgumentException("Se requiere proyecto documental.");
        if (document == null) throw new IllegalArgumentException("Se requiere el Word/DOCX abierto.");
        if (script == null || script.empty()) throw new IllegalArgumentException("Se requiere lectura preparada.");
        Path root = projectDirectory == null ? null : projectDirectory.toAbsolutePath().normalize();
        if (root == null) throw new IllegalArgumentException("Se requiere carpeta de proyecto.");
        Path frameDirectory = root.resolve(FRAME_DIRECTORY);
        Files.createDirectories(frameDirectory);
        DocumentTextVideoOptions effective = options == null ? DocumentTextVideoOptions.defaults() : options;
        DocumentStudyVideoConfiguration configuration = project.study().documentaryVideoConfiguration();
        Map<String, AudioSegmentSnapshot> audio = completedAudioBySegment(jobs, root);
        Map<String, List<NarrationSegment>> segments = segmentsByBlock(script);
        ArrayList<SimpleVideoFrame> frames = new ArrayList<>();
        int frameIndex = 1;
        for (DocumentStudyVideoContentResolver.Item item : contentResolver.resolve(document, configuration)) {
            if (!item.enabled()) continue;
            Path imageFile = frameDirectory.resolve(fileName(item));
            String imageRelativePath = portableRelativePath(root, imageFile);
            if (item.kind() == DocumentStudyVideoContentResolver.Kind.TABLE) {
                compositor.composeTable(imageFile, item.block(), configuration, effective);
                frames.add(new SimpleVideoFrame(
                        "DOC-TABLE-" + token(item.block().id()),
                        "TABLE-" + token(item.block().id()),
                        "Tabla",
                        "",
                        "",
                        imageRelativePath,
                        "",
                        0.0,
                        item.durationSeconds(),
                        true,
                        false,
                        true));
                frameIndex++;
                continue;
            }
            if (item.kind() == DocumentStudyVideoContentResolver.Kind.CLOSING) {
                var closing = configuration.closingSlide(item.block().id())
                        .orElseThrow(() -> new IOException("No existe la diapositiva final "
                                + item.block().id() + "."));
                compositor.composeClosing(imageFile, closing, project, root, effective);
                frames.add(new SimpleVideoFrame(
                        "DOC-CLOSING-" + token(closing.id()),
                        "CLOSING-" + token(closing.id()),
                        closing.title().isBlank() ? "Cierre" : closing.title(),
                        "",
                        "",
                        imageRelativePath,
                        "",
                        0.0,
                        closing.durationSeconds(),
                        true,
                        false,
                        true));
                frameIndex++;
                continue;
            }
            List<NarrationSegment> blockSegments = segments.getOrDefault(item.block().id(), List.of());
            if (blockSegments.isEmpty()) continue;
            if (item.kind() == DocumentStudyVideoContentResolver.Kind.PARAGRAPH) {
                compositor.composeParagraph(imageFile, item.block(), item.paragraphVisual(), configuration,
                        project, root, effective);
            } else {
                compositor.composeCover(imageFile, item.block(), configuration, effective);
            }
            for (NarrationSegment segment : blockSegments) {
                List<AudioSegmentSnapshot> clips = audioForSegment(audio, segment.id());
                if (clips.isEmpty()) clips = java.util.Collections.singletonList(null);
                for (AudioSegmentSnapshot clip : clips) {
                    frames.add(new SimpleVideoFrame(
                            "DOC-FRAME-" + String.format(Locale.ROOT, "%04d", frameIndex++),
                            clip == null ? segment.id() : clip.segmentId(),
                            segment.title().isBlank() ? item.block().id() : segment.title(),
                            segment.narrationText(),
                            "",
                            imageRelativePath,
                            clip == null ? "" : clip.audioRelativePath(),
                            clip == null ? 0.0 : clip.durationSeconds(),
                            0.35,
                            true,
                            clip != null && clip.completed()));
                }
            }
        }
        String title = configuration.videoTitle().isBlank() ? document.title() : configuration.videoTitle();
        return new SimpleVideoPlan("Video documental - " + title, frames, 0.35, Instant.now());
    }

    private static String fileName(DocumentStudyVideoContentResolver.Item item) {
        String prefix = switch (item.kind()) {
            case COVER -> "cover-";
            case PARAGRAPH -> "paragraph-";
            case TABLE -> "table-";
            case CLOSING -> "closing-";
        };
        return prefix + item.block().id().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-") + ".png";
    }

    private static Map<String, List<NarrationSegment>> segmentsByBlock(NarrationScriptDocument script) {
        LinkedHashMap<String, ArrayList<NarrationSegment>> mutable = new LinkedHashMap<>();
        for (NarrationSegment segment : script.segments()) {
            if (secondaryReadUnit(segment)) continue;
            for (String blockId : segment.sourceBlockIds()) {
                mutable.computeIfAbsent(blockId, ignored -> new ArrayList<>()).add(segment);
            }
        }
        LinkedHashMap<String, List<NarrationSegment>> result = new LinkedHashMap<>();
        mutable.forEach((blockId, segments) -> result.put(blockId, List.copyOf(segments)));
        return result;
    }

    private static Map<String, AudioSegmentSnapshot> completedAudioBySegment(List<AudioJobSnapshot> jobs, Path root) {
        LinkedHashMap<String, AudioSegmentSnapshot> result = new LinkedHashMap<>();
        if (jobs == null) return result;
        jobs.stream().filter(java.util.Objects::nonNull).sorted(Comparator.comparing(AudioJobSnapshot::updatedAt))
                .forEach(job -> job.segments().stream().filter(segment -> usableAudio(root, segment))
                        .forEach(segment -> result.put(segment.segmentId(), segment)));
        return result;
    }

    private static boolean usableAudio(Path root, AudioSegmentSnapshot segment) {
        if (segment == null || !segment.completed() || segment.audioRelativePath().isBlank()) return false;
        Path file = root.resolve(segment.audioRelativePath()).toAbsolutePath().normalize();
        return file.startsWith(root) && Files.isRegularFile(file);
    }

    private static List<AudioSegmentSnapshot> audioForSegment(Map<String, AudioSegmentSnapshot> audio, String segmentId) {
        ArrayList<AudioSegmentSnapshot> units = new ArrayList<>();
        for (AudioSegmentSnapshot clip : audio.values()) {
            if (segmentId.equals(clip.segmentId())) return units.isEmpty() ? List.of(clip) : List.copyOf(units);
            if (clip.segmentId().startsWith(segmentId + "-")) units.add(clip);
        }
        return List.copyOf(units);
    }

    private static boolean secondaryReadUnit(NarrationSegment segment) {
        return segment != null && Boolean.parseBoolean(segment.metadata().getOrDefault("secondaryReadUnit", "false"));
    }

    private static String portableRelativePath(Path root, Path file) {
        return root.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private static String token(String value) {
        return value.replaceAll("[^A-Za-z0-9_-]", "-");
    }
}
