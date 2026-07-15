package com.marcosmoreiradev.docupodcaststudio.application.fragment;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.DocumentFragment;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetRole;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetSource;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Builds the first canonical fragment projection without changing persisted project schema. */
public final class BuildFragmentWorkspaceProjectionUseCase {
    public static final String FRAGMENT_ID_METADATA_KEY = "fragmentId";

    public FragmentWorkspaceProjection build(
            ReadableDocument document,
            NarrationScriptDocument script,
            DocuPodcastProject project,
            StoryboardDocument storyboard,
            List<AudioJobSnapshot> audioJobs,
            PlaybackManifest manifest
    ) {
        Map<String, NarrationSegment> segmentsById = segmentsById(script);
        Map<String, FragmentId> fragmentIdBySegmentId = fragmentIdBySegmentId(script);
        Map<String, String> firstSegmentByBlockId = firstSegmentByBlockId(script);
        Map<String, AudioSegmentSnapshot> audioBySegmentId = audioBySegmentId(audioJobs);

        ArrayList<DocumentFragment> fragments = new ArrayList<>();
        LinkedHashMap<FragmentId, DocumentFragment> byFragmentId = new LinkedHashMap<>();
        int order = 0;
        if (document != null) {
            for (DocumentBlock block : document.blocks()) {
                if (block.type().name().equals("EMPTY") || block.type().name().equals("IGNORED")) {
                    continue;
                }
                if (block.nonNarratableSourceVisual()) {
                    continue;
                }
                String segmentId = firstSegmentByBlockId.getOrDefault(block.id(), "");
                FragmentId fragmentId = FragmentId.fromBlockId(block.id());
                DocumentFragment fragment = new DocumentFragment(
                        fragmentId,
                        order++,
                        block.text(),
                        block.sourceLocatorLabel(document.format()),
                        new DocumentTextRange(block.id(), 0, block.text().length()),
                        block.id(),
                        segmentId,
                        contentHash(block.text()),
                        statusFor(block, segmentId, audioBySegmentId.get(segmentId)),
                        blockMetadata(block, segmentId, document)
                );
                fragments.add(fragment);
                byFragmentId.put(fragmentId, fragment);
            }
        }

        if (script != null) {
            for (NarrationSegment segment : script.segments()) {
                if (secondaryReadUnit(segment)) {
                    continue;
                }
                FragmentId fragmentId = fragmentIdBySegmentId.getOrDefault(segment.id(), FragmentId.fromSegmentId(segment.id()));
                if (byFragmentId.containsKey(fragmentId)) {
                    continue;
                }
                DocumentFragment fragment = new DocumentFragment(
                        fragmentId,
                        order++,
                        segment.narrationText(),
                        "Segmento " + segment.id(),
                        null,
                        "",
                        segment.id(),
                        contentHash(segment.narrationText()),
                        statusFor(null, segment.id(), audioBySegmentId.get(segment.id())),
                        segmentMetadata(segment)
                );
                fragments.add(fragment);
                byFragmentId.put(fragmentId, fragment);
            }
        }

        ArrayList<FragmentAssetBinding> bindings = new ArrayList<>();
        Set<FragmentId> knownFragmentIds = byFragmentId.keySet();
        addSourceVisualBindings(bindings, fragments);
        addAudioBindings(bindings, audioJobs, fragmentIdBySegmentId, knownFragmentIds);
        addStoryboardBindings(bindings, storyboard, project, fragmentIdBySegmentId, knownFragmentIds);
        addNarrativeLayerBindings(bindings, project, fragmentIdBySegmentId, knownFragmentIds);
        addTheatreBindings(bindings, project, knownFragmentIds);
        addPlaybackBindings(bindings, manifest, fragmentIdBySegmentId, knownFragmentIds);
        return new FragmentWorkspaceProjection(fragments, bindings);
    }

    private static Map<String, NarrationSegment> segmentsById(NarrationScriptDocument script) {
        LinkedHashMap<String, NarrationSegment> result = new LinkedHashMap<>();
        if (script == null) {
            return result;
        }
        for (NarrationSegment segment : script.segments()) {
            result.put(segment.id(), segment);
        }
        return result;
    }

    private static Map<String, String> firstSegmentByBlockId(NarrationScriptDocument script) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        if (script == null) {
            return result;
        }
        for (NarrationSegment segment : script.segments()) {
            if (secondaryReadUnit(segment)) {
                continue;
            }
            for (String blockId : segment.sourceBlockIds()) {
                if (blockId != null && !blockId.isBlank()) {
                    result.putIfAbsent(blockId, segment.id());
                }
            }
        }
        return result;
    }

    private static Map<String, FragmentId> fragmentIdBySegmentId(NarrationScriptDocument script) {
        LinkedHashMap<String, FragmentId> result = new LinkedHashMap<>();
        if (script == null) {
            return result;
        }
        for (NarrationSegment segment : script.segments()) {
            if (secondaryReadUnit(segment)) {
                continue;
            }
            result.put(segment.id(), fragmentIdForSegment(segment));
        }
        return result;
    }

    private static FragmentId fragmentIdForSegment(NarrationSegment segment) {
        String explicit = segment.metadata().getOrDefault(FRAGMENT_ID_METADATA_KEY, "").strip();
        if (!explicit.isBlank()) {
            return FragmentId.of(explicit);
        }
        Optional<String> sourceBlock = segment.sourceBlockIds().stream()
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
        return sourceBlock.map(FragmentId::fromBlockId).orElseGet(() -> FragmentId.fromSegmentId(segment.id()));
    }

    private static Map<String, AudioSegmentSnapshot> audioBySegmentId(List<AudioJobSnapshot> audioJobs) {
        LinkedHashMap<String, AudioSegmentSnapshot> result = new LinkedHashMap<>();
        if (audioJobs == null) {
            return result;
        }
        for (AudioJobSnapshot job : audioJobs) {
            if (job == null) {
                continue;
            }
            for (AudioSegmentSnapshot segment : job.segments()) {
                result.put(segment.segmentId(), segment);
            }
        }
        return result;
    }

    private static FragmentStatus statusFor(DocumentBlock block, String segmentId, AudioSegmentSnapshot audio) {
        if (audio != null) {
            if (audio.status() == AudioSegmentStatus.COMPLETED) {
                return FragmentStatus.AUDIO_READY;
            }
            if (audio.status() == AudioSegmentStatus.FAILED) {
                return FragmentStatus.AUDIO_FAILED;
            }
            if (audio.status() == AudioSegmentStatus.GENERATING) {
                return FragmentStatus.AUDIO_GENERATING;
            }
            return FragmentStatus.AUDIO_PENDING;
        }
        if (block != null && block.sourceVisual() && !block.narratable()) {
            return FragmentStatus.VISUAL_ONLY;
        }
        if (!normalize(segmentId).isBlank() || block != null && block.narratable()) {
            return FragmentStatus.NARRATABLE;
        }
        return FragmentStatus.SOURCE_ONLY;
    }

    private static Map<String, String> blockMetadata(DocumentBlock block, String segmentId, ReadableDocument document) {
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>(block.metadata());
        metadata.put(FRAGMENT_ID_METADATA_KEY, FragmentId.fromBlockId(block.id()).value());
        metadata.put("sourceBlockId", block.id());
        metadata.put("sourceBlockType", block.type().name());
        metadata.put("sourceFormat", document.format().name());
        if (!normalize(segmentId).isBlank()) {
            metadata.put("segmentId", segmentId);
        }
        return metadata;
    }

    private static Map<String, String> segmentMetadata(NarrationSegment segment) {
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>(segment.metadata());
        metadata.put(FRAGMENT_ID_METADATA_KEY, fragmentIdForSegment(segment).value());
        metadata.put("segmentId", segment.id());
        return metadata;
    }

    private static boolean secondaryReadUnit(NarrationSegment segment) {
        return segment != null && Boolean.parseBoolean(segment.metadata().getOrDefault("secondaryReadUnit", "false"));
    }

    private static void addSourceVisualBindings(ArrayList<FragmentAssetBinding> bindings, List<DocumentFragment> fragments) {
        for (DocumentFragment fragment : fragments) {
            String path = firstPresent(fragment.metadata(), "embeddedImagePath", "sourceImagePath", "imageRelativePath", "sourceVisualPath");
            String assetId = firstPresent(fragment.metadata(), "assetId", "sourceImageAssetId", "imageAssetId");
            if (path.isBlank() && assetId.isBlank()) {
                continue;
            }
            bindings.add(new FragmentAssetBinding(
                    bindingId("SRC", fragment.fragmentId().value()),
                    fragment.fragmentId(),
                    assetId,
                    FragmentAssetRole.DOCUMENT_IMAGE,
                    FragmentAssetSource.SOURCE_DOCUMENT,
                    path,
                    "READY",
                    fragment.sourceBlockId(),
                    Map.of("source", "document-metadata")
            ));
        }
    }

    private static void addAudioBindings(ArrayList<FragmentAssetBinding> bindings,
                                         List<AudioJobSnapshot> audioJobs,
                                         Map<String, FragmentId> fragmentIdBySegmentId,
                                         Set<FragmentId> knownFragmentIds) {
        if (audioJobs == null) {
            return;
        }
        for (AudioJobSnapshot job : audioJobs) {
            if (job == null) {
                continue;
            }
            for (AudioSegmentSnapshot segment : job.segments()) {
                FragmentId fragmentId = fragmentIdBySegmentId.getOrDefault(segment.segmentId(), FragmentId.fromSegmentId(segment.segmentId()));
                if (!knownFragmentIds.contains(fragmentId)) {
                    continue;
                }
                bindings.add(new FragmentAssetBinding(
                        bindingId("AUD", job.jobId(), segment.segmentId()),
                        fragmentId,
                        "",
                        FragmentAssetRole.AUDIO_TTS,
                        FragmentAssetSource.AUDIO_JOB,
                        segment.audioRelativePath(),
                        segment.status().name(),
                        segment.segmentId(),
                        Map.of(
                                "jobId", job.jobId(),
                                "attempts", Integer.toString(segment.attempts()),
                                "durationSeconds", Double.toString(segment.durationSeconds())
                        )
                ));
            }
        }
    }

    private static void addStoryboardBindings(ArrayList<FragmentAssetBinding> bindings,
                                              StoryboardDocument storyboard,
                                              DocuPodcastProject project,
                                              Map<String, FragmentId> fragmentIdBySegmentId,
                                              Set<FragmentId> knownFragmentIds) {
        if (storyboard == null) {
            return;
        }
        for (StoryboardBinding binding : storyboard.bindings()) {
            FragmentId fragmentId = fragmentIdBySegmentId.getOrDefault(binding.segmentId(), FragmentId.fromSegmentId(binding.segmentId()));
            if (!knownFragmentIds.contains(fragmentId)) {
                continue;
            }
            String assetPath = assetPath(project, binding.imageAssetId());
            bindings.add(new FragmentAssetBinding(
                    bindingId("STB", binding.id()),
                    fragmentId,
                    binding.imageAssetId(),
                    FragmentAssetRole.MAIN_IMAGE,
                    FragmentAssetSource.STORYBOARD,
                    assetPath,
                    "READY",
                    binding.segmentId(),
                    Map.of(
                            "displayMode", binding.displayMode().name(),
                            "caption", binding.caption()
                    )
            ));
        }
    }

    private static void addNarrativeLayerBindings(ArrayList<FragmentAssetBinding> bindings,
                                                  DocuPodcastProject project,
                                                  Map<String, FragmentId> fragmentIdBySegmentId,
                                                  Set<FragmentId> knownFragmentIds) {
        if (project == null) {
            return;
        }
        for (NarrativeLayerAssignment assignment : project.narrativeLayerAssignments()) {
            String segmentId = assignment.textRange().segmentId();
            FragmentId fragmentId = fragmentIdBySegmentId.getOrDefault(segmentId, FragmentId.fromSegmentId(segmentId));
            if (!knownFragmentIds.contains(fragmentId)) {
                continue;
            }
            bindings.add(new FragmentAssetBinding(
                    bindingId("LAY", assignment.id()),
                    fragmentId,
                    assignment.targetId(),
                    roleFor(assignment.kind()),
                    FragmentAssetSource.NARRATIVE_LAYER,
                    assetPath(project, assignment.targetId()),
                    "READY",
                    assignment.id(),
                    Map.of(
                            "kind", assignment.kind().name(),
                            "displayName", assignment.displayName(),
                            "notes", assignment.notes()
                    )
            ));
        }
    }

    private static void addTheatreBindings(ArrayList<FragmentAssetBinding> bindings,
                                           DocuPodcastProject project,
                                           Set<FragmentId> knownFragmentIds) {
        if (project == null || project.theatre() == null) {
            return;
        }
        TheatreProjectLayer theatre = project.theatre();
        for (TheatreProjectLayer.Intervencion intervention : theatre.intervenciones()) {
            FragmentId fragmentId = FragmentId.fromBlockId(intervention.blockId());
            if (!knownFragmentIds.contains(fragmentId)) {
                continue;
            }
            bindings.add(new FragmentAssetBinding(
                    bindingId("THEATRE", intervention.id()),
                    fragmentId,
                    "",
                    FragmentAssetRole.THEATRE_INTERVENTION,
                    FragmentAssetSource.THEATRE,
                    "",
                    "READY",
                    intervention.id(),
                    Map.of(
                            "blockId", intervention.blockId(),
                            "sequenceIndex", Integer.toString(intervention.sequenceIndex())
                    )
            ));
        }
        for (TheatreProjectLayer.IntervencionVisual visual : theatre.intervencionesVisuales()) {
            Optional<TheatreProjectLayer.Intervencion> intervention = theatre.intervenciones().stream()
                    .filter(candidate -> candidate.id().equals(visual.intervencionId()))
                    .findFirst();
            if (intervention.isEmpty()) {
                continue;
            }
            FragmentId fragmentId = FragmentId.fromBlockId(intervention.get().blockId());
            if (!knownFragmentIds.contains(fragmentId)) {
                continue;
            }
            bindings.add(new FragmentAssetBinding(
                    bindingId("THEATRE-VIS", visual.intervencionId(), visual.assetId()),
                    fragmentId,
                    visual.assetId(),
                    FragmentAssetRole.THEATRE_VISUAL,
                    FragmentAssetSource.THEATRE,
                    assetPath(project, visual.assetId()),
                    "READY",
                    visual.intervencionId(),
                    Map.of("notes", visual.notes())
            ));
        }
    }

    private static void addPlaybackBindings(ArrayList<FragmentAssetBinding> bindings,
                                            PlaybackManifest manifest,
                                            Map<String, FragmentId> fragmentIdBySegmentId,
                                            Set<FragmentId> knownFragmentIds) {
        if (manifest == null || manifest.emptyManifest()) {
            return;
        }
        for (PlaybackCue cue : manifest.cues()) {
            FragmentId fragmentId = fragmentIdBySegmentId.getOrDefault(cue.segmentId(), FragmentId.fromSegmentId(cue.segmentId()));
            if (!knownFragmentIds.contains(fragmentId)) {
                continue;
            }
            bindings.add(new FragmentAssetBinding(
                    bindingId("CUE", cue.unitId()),
                    fragmentId,
                    cue.audioClipId(),
                    FragmentAssetRole.PLAYBACK_CUE,
                    FragmentAssetSource.PLAYBACK_MANIFEST,
                    cue.audioRelativePath(),
                    cue.hasAudio() ? "READY" : "NO_AUDIO",
                    cue.unitId(),
                    Map.of(
                            "segmentId", cue.segmentId(),
                            "startSeconds", Double.toString(cue.startSeconds()),
                            "endSeconds", Double.toString(cue.endSeconds())
                    )
            ));
        }
    }

    private static FragmentAssetRole roleFor(NarrativeLayerKind kind) {
        if (kind == null) {
            return FragmentAssetRole.NOTE;
        }
        return switch (kind) {
            case VOICE -> FragmentAssetRole.VOICE_TRACK;
            case HUMAN_AUDIO -> FragmentAssetRole.AUDIO_IMPORTED;
            case AMBIENT_AUDIO -> FragmentAssetRole.AMBIENT_AUDIO;
            case IMAGE -> FragmentAssetRole.MAIN_IMAGE;
            case BRIDGE_IMAGE -> FragmentAssetRole.BRIDGE_TO_NEXT_FRAGMENT;
            case EMOTION -> FragmentAssetRole.EMOTION;
            case NOTE -> FragmentAssetRole.NOTE;
        };
    }

    private static String assetPath(DocuPodcastProject project, String assetId) {
        if (project == null || assetId == null || assetId.isBlank()) {
            return "";
        }
        return project.assets().byId(assetId)
                .map(ProjectAssetReference::relativePath)
                .orElse("");
    }

    private static String firstPresent(Map<String, String> metadata, String... keys) {
        if (metadata == null) {
            return "";
        }
        for (String key : keys) {
            String value = metadata.getOrDefault(key, "").strip();
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String bindingId(String prefix, String... parts) {
        StringBuilder builder = new StringBuilder(prefix);
        for (String part : parts) {
            String token = normalize(part).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "-").replaceAll("^-|-$", "");
            if (!token.isBlank()) {
                builder.append('-').append(token);
            }
        }
        return builder.toString();
    }

    private static String contentHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalize(text).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format(Locale.ROOT, "%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
