package com.marcosmoreiradev.docupodcaststudio.domain.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionRevisionRef;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.LinkedHashSet;

/** Persistable provenance determining whether an audio chunk can be reused. */
public record AudioSourceFingerprint(
        List<PdfRegionRevisionRef> sourceRegions,
        int firstPage,
        int lastPage,
        String textSha256,
        String voiceConfigurationSha256,
        String preprocessingSha256,
        String derivedTreatmentSha256
) {
    public AudioSourceFingerprint {
        sourceRegions = sourceRegions == null ? List.of() : List.copyOf(sourceRegions);
        firstPage = Math.max(0, firstPage);
        lastPage = Math.max(firstPage, lastPage);
        textSha256 = normalize(textSha256);
        voiceConfigurationSha256 = normalize(voiceConfigurationSha256);
        preprocessingSha256 = normalize(preprocessingSha256);
        derivedTreatmentSha256 = normalize(derivedTreatmentSha256);
    }

    public AudioSourceFingerprint(List<PdfRegionRevisionRef> sourceRegions, int firstPage, int lastPage,
                                  String textSha256, String voiceConfigurationSha256,
                                  String preprocessingSha256) {
        this(sourceRegions, firstPage, lastPage, textSha256, voiceConfigurationSha256,
                preprocessingSha256, "");
    }

    public static AudioSourceFingerprint untraceable() {
        return new AudioSourceFingerprint(List.of(), 0, 0, "", "", "", "");
    }

    public static AudioSourceFingerprint pdf(List<PdfRegionRevisionRef> regions,
                                             String text,
                                             String voiceConfiguration,
                                             String preprocessing) {
        List<PdfRegionRevisionRef> safe = regions == null ? List.of() : List.copyOf(regions);
        int first = safe.stream().mapToInt(PdfRegionRevisionRef::pageNumber).min().orElse(0);
        int last = safe.stream().mapToInt(PdfRegionRevisionRef::pageNumber).max().orElse(0);
        return new AudioSourceFingerprint(safe, first, last, sha256(text),
                sha256(voiceConfiguration), sha256(preprocessing), "");
    }

    public static AudioSourceFingerprint pdf(List<PdfRegionRevisionRef> regions,
                                             String text,
                                             String voiceConfiguration,
                                             String preprocessing,
                                             String derivedTreatment) {
        List<PdfRegionRevisionRef> safe = regions == null ? List.of() : List.copyOf(regions);
        int first = safe.stream().mapToInt(PdfRegionRevisionRef::pageNumber).min().orElse(0);
        int last = safe.stream().mapToInt(PdfRegionRevisionRef::pageNumber).max().orElse(0);
        return new AudioSourceFingerprint(safe, first, last, sha256(text),
                sha256(voiceConfiguration), sha256(preprocessing),
                derivedTreatment == null || derivedTreatment.isBlank() ? "" : sha256(derivedTreatment));
    }

    public static AudioSourceFingerprint generic(String text,
                                                 String voiceConfiguration,
                                                 String preprocessing) {
        return new AudioSourceFingerprint(List.of(), 0, 0, sha256(text),
                sha256(voiceConfiguration), sha256(preprocessing), "");
    }

    /**
     * Preserves the effective acoustic identity of already-authorized chunks when
     * they are materialized as one segment WAV. Equal component hashes remain
     * byte-for-byte equal; genuinely mixed components receive an ordered aggregate
     * hash and can therefore never masquerade as either input.
     */
    public static AudioSourceFingerprint composed(List<AudioSourceFingerprint> components) {
        List<AudioSourceFingerprint> safe = components == null
                ? List.of() : components.stream().filter(java.util.Objects::nonNull).toList();
        if (safe.isEmpty()) return untraceable();
        LinkedHashSet<PdfRegionRevisionRef> regions = new LinkedHashSet<>();
        safe.forEach(value -> regions.addAll(value.sourceRegions()));
        return new AudioSourceFingerprint(List.copyOf(regions),
                safe.stream().mapToInt(AudioSourceFingerprint::firstPage).filter(value -> value > 0).min().orElse(0),
                safe.stream().mapToInt(AudioSourceFingerprint::lastPage).max().orElse(0),
                aggregateHash(safe.stream().map(AudioSourceFingerprint::textSha256).toList()),
                aggregateHash(safe.stream().map(AudioSourceFingerprint::voiceConfigurationSha256).toList()),
                aggregateHash(safe.stream().map(AudioSourceFingerprint::preprocessingSha256).toList()),
                aggregateHash(safe.stream().map(AudioSourceFingerprint::derivedTreatmentSha256).toList()));
    }

    public boolean traceable() {
        return !textSha256.isBlank() && !voiceConfigurationSha256.isBlank()
                && !preprocessingSha256.isBlank();
    }

    public boolean reusableFor(AudioSourceFingerprint current) {
        if (!traceable() || current == null || !current.traceable()) return false;
        // Region revisions, page geometry and derived visual treatments are
        // provenance, not acoustic inputs. They remain persisted for audit but
        // cannot invalidate an otherwise identical WAV.
        return textSha256.equals(current.textSha256)
                && voiceConfigurationSha256.equals(current.voiceConfigurationSha256)
                && preprocessingSha256.equals(current.preprocessingSha256);
    }

    /** Returns the same provenance with an exact effective voice/sample configuration. */
    public AudioSourceFingerprint withVoiceConfiguration(String voiceConfiguration) {
        return new AudioSourceFingerprint(sourceRegions, firstPage, lastPage, textSha256,
                sha256(voiceConfiguration), preprocessingSha256, derivedTreatmentSha256);
    }

    public String regionIds() {
        return sourceRegions.stream().map(PdfRegionRevisionRef::regionId)
                .collect(java.util.stream.Collectors.joining(","));
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(
                    (value == null ? "" : value).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String aggregateHash(List<String> values) {
        List<String> normalized = values.stream().map(AudioSourceFingerprint::normalize).toList();
        if (normalized.stream().distinct().count() == 1) return normalized.getFirst();
        return sha256(String.join("\u0000", normalized));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(java.util.Locale.ROOT);
    }
}
