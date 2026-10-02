package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Detects character dialogue from lines shaped as CHARACTER: text. */
public final class TheatreCharacterDetector {
    private static final int MAX_CHARACTER_NAME_LENGTH = 56;
    private static final int MAX_CHARACTER_NAME_WORDS = 7;

    private TheatreCharacterDetector() {
    }

    public static List<TheatreCharacterPresentation> detect(
            ReadableDocument document,
            NarrationScriptDocument script,
            List<TheatreProjectLayer.CharacterProfile> savedProfiles) {
        Map<String, TheatreProjectLayer.CharacterProfile> profilesByKey = profilesByKey(savedProfiles);
        LinkedHashMap<String, CharacterAccumulator> characters = new LinkedHashMap<>();

        List<String> sourceLines = sourceLines(document, script);
        for (String line : sourceLines) {
            Optional<DialogueLine> dialogue = dialogueLine(line);
            if (dialogue.isEmpty()) {
                continue;
            }
            DialogueLine parsed = dialogue.get();
            String key = characterKey(parsed.characterName());
            CharacterAccumulator accumulator = characters.computeIfAbsent(
                    key,
                    ignored -> new CharacterAccumulator(parsed.characterName(), profileFor(parsed.characterName(), profilesByKey)));
            accumulator.addMention(parsed.dialoguePreview());
        }

        for (TheatreProjectLayer.CharacterProfile profile : safeProfiles(savedProfiles)) {
            String key = characterKey(profile.displayName());
            characters.computeIfAbsent(key, ignored -> new CharacterAccumulator(profile.displayName(), profile));
        }

        return characters.values().stream()
                .map(CharacterAccumulator::presentation)
                .toList();
    }

    static Optional<DialogueLine> dialogueLine(String rawLine) {
        String line = normalize(rawLine);
        if (line.isBlank()) {
            return Optional.empty();
        }
        int firstColon = line.indexOf(':');
        if (firstColon <= 0 || firstColon == line.length() - 1) {
            return Optional.empty();
        }
        String candidate = cleanSpeaker(line.substring(0, firstColon));
        String dialogue = normalize(line.substring(firstColon + 1));
        if (!validCharacterName(candidate) || dialogue.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new DialogueLine(candidate, dialogue));
    }

    public static String stableCharacterId(String displayName) {
        String normalized = normalize(displayName);
        if (normalized.isBlank()) {
            return "CHR-SIN-NOMBRE";
        }
        String ascii = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (ascii.isBlank()) {
            return "CHR-SIN-NOMBRE";
        }
        return "CHR-" + (ascii.length() > 48 ? ascii.substring(0, 48).replaceAll("-+$", "") : ascii);
    }

    private static List<String> sourceLines(ReadableDocument document, NarrationScriptDocument script) {
        if (document != null && !document.blocks().isEmpty()) {
            return document.blocks().stream()
                    .filter(DocumentBlock::narratable)
                    .map(block -> block.metadata().containsKey("theatreGrammarInterventionId")
                            ? block.metadata().get("characterName") + ": " + block.text() : block.text())
                    .flatMap(text -> splitLines(text).stream())
                    .toList();
        }
        if (script != null && !script.empty()) {
            return script.segments().stream()
                    .filter(NarrationSegment::narratable)
                    .map(segment -> segment.metadata().containsKey("theatreGrammarInterventionId")
                            ? segment.metadata().get("characterName") + ": " + segment.narrationText() : segment.narrationText())
                    .flatMap(text -> splitLines(text).stream())
                    .toList();
        }
        return List.of();
    }

    private static List<String> splitLines(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return List.of();
        }
        String[] pieces = normalized.split("\\R+");
        ArrayList<String> lines = new ArrayList<>(pieces.length);
        for (String piece : pieces) {
            String line = normalize(piece);
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static Map<String, TheatreProjectLayer.CharacterProfile> profilesByKey(List<TheatreProjectLayer.CharacterProfile> savedProfiles) {
        LinkedHashMap<String, TheatreProjectLayer.CharacterProfile> result = new LinkedHashMap<>();
        for (TheatreProjectLayer.CharacterProfile profile : safeProfiles(savedProfiles)) {
            result.put(characterKey(profile.displayName()), profile);
            for (String alias : profile.aliases()) {
                result.putIfAbsent(characterKey(alias), profile);
            }
        }
        return result;
    }

    private static List<TheatreProjectLayer.CharacterProfile> safeProfiles(List<TheatreProjectLayer.CharacterProfile> savedProfiles) {
        if (savedProfiles == null || savedProfiles.isEmpty()) {
            return List.of();
        }
        return savedProfiles.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private static TheatreProjectLayer.CharacterProfile profileFor(
            String displayName,
            Map<String, TheatreProjectLayer.CharacterProfile> profilesByKey) {
        TheatreProjectLayer.CharacterProfile existing = profilesByKey.get(characterKey(displayName));
        if (existing != null) {
            return existing;
        }
        return new TheatreProjectLayer.CharacterProfile(stableCharacterId(displayName), displayName, List.of(), "");
    }

    private static boolean validCharacterName(String candidate) {
        String normalized = normalize(candidate);
        if (normalized.isBlank()
                || normalized.length() > MAX_CHARACTER_NAME_LENGTH
                || normalized.split("\\s+").length > MAX_CHARACTER_NAME_WORDS
                || !normalized.chars().anyMatch(Character::isLetter)) {
            return false;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (upper.matches("INTERVENCION-\\d+")
                || upper.matches("ESCENA\\s+\\d+.*")
                || upper.matches("ACTO\\s+\\d+.*")
                || upper.matches("CAPITULO\\s+\\d+.*")
                || upper.matches("CAPÍTULO\\s+\\d+.*")
                || upper.matches("PARTE\\s+\\d+.*")
                || upper.matches("SECUENCIA\\s+\\d+.*")) {
            return false;
        }
        String[] words = normalized.split("\\s+");
        for (String word : words) {
            String cleaned = word.replaceAll("[^\\p{L}\\p{N}]", "");
            if (cleaned.isBlank() || cleaned.chars().allMatch(Character::isDigit)) {
                continue;
            }
            int first = cleaned.codePointAt(0);
            boolean uppercaseLike = Character.isUpperCase(first) || cleaned.equals(cleaned.toUpperCase(Locale.ROOT));
            if (!uppercaseLike) {
                return false;
            }
        }
        return true;
    }

    private static String cleanSpeaker(String candidate) {
        return normalize(candidate)
                .replaceAll("^[\\-\\u2013\\u2014\\u2022\\s]+", "")
                .replaceAll("^[\"'\\u201c\\u201d\\u2018\\u2019]+", "")
                .replaceAll("[\"'\\u201c\\u201d\\u2018\\u2019]+$", "")
                .replaceAll("\\s+", " ")
                .strip();
    }

    private static String characterKey(String displayName) {
        return Normalizer.normalize(normalize(displayName), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", " ")
                .strip();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    record DialogueLine(String characterName, String dialoguePreview) {
    }

    private static final class CharacterAccumulator {
        private final TheatreProjectLayer.CharacterProfile profile;
        private final String displayName;
        private String firstLinePreview = "";
        private int mentionCount;

        private CharacterAccumulator(String displayName, TheatreProjectLayer.CharacterProfile profile) {
            this.displayName = normalize(displayName);
            this.profile = Objects.requireNonNull(profile, "profile");
        }

        private void addMention(String dialoguePreview) {
            if (firstLinePreview.isBlank()) {
                firstLinePreview = normalize(dialoguePreview);
            }
            mentionCount++;
        }

        private TheatreCharacterPresentation presentation() {
            return new TheatreCharacterPresentation(
                    profile.id(),
                    displayName.isBlank() ? profile.displayName() : displayName,
                    firstLinePreview,
                    profile.notes(),
                    mentionCount);
        }
    }
}
