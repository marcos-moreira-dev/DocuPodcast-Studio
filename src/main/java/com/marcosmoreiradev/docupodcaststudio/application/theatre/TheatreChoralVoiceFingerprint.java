package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stable source identity for application-generated theatre choral audio. */
public final class TheatreChoralVoiceFingerprint {
    public static final String GENERATED_PREFIX = "generated:sha256:";

    private TheatreChoralVoiceFingerprint() {
    }

    public static String compute(DocuPodcastProject project, NarrationSegment segment, List<String> participants) {
        Map<String, String> voiceByCharacter = new LinkedHashMap<>();
        for (TheatreProjectLayer.VoiceRoleAlias alias : project.theatre().voiceRoleAliases()) {
            if (!alias.characterId().isBlank()) {
                voiceByCharacter.putIfAbsent(alias.characterId(), alias.voiceProfileId());
            }
        }
        ArrayList<String> ordered = new ArrayList<>(participants == null ? List.of() : participants);
        ordered.sort(Comparator.naturalOrder());
        StringBuilder source = new StringBuilder();
        source.append("text=").append(normalize(segment == null ? "" : segment.narrationText())).append('\n');
        source.append("style=").append(normalize(segment == null ? "" : segment.performanceStyleId())).append('\n');
        for (String characterId : ordered) {
            String voiceId = voiceByCharacter.getOrDefault(characterId, "");
            VoiceProfile voice = project.voiceLibrary().voiceById(voiceId).orElse(null);
            source.append("participant=").append(characterId)
                    .append("|voice=").append(voiceId)
                    .append("|engine=").append(voice == null ? "" : voice.engineType())
                    .append("|sample=").append(voice == null ? "" : voice.sampleAssetId())
                    .append("|model=").append(voice == null ? "" : voice.modelAssetId())
                    .append('\n');
        }
        return GENERATED_PREFIX + sha256(source.toString());
    }

    public static boolean isGenerated(String fingerprint) {
        return fingerprint != null && fingerprint.startsWith(GENERATED_PREFIX);
    }

    public static boolean isCurrent(TheatreProjectLayer.ChoralVoiceAssignment assignment,
                                    DocuPodcastProject project,
                                    NarrationSegment segment) {
        if (assignment == null || !isGenerated(assignment.sourceFingerprint())) {
            return true;
        }
        return assignment.sourceFingerprint().equals(compute(project, segment, assignment.participantCharacterIds()));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().replace("\r\n", "\n").replace('\r', '\n');
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no esta disponible.", ex);
        }
    }
}
