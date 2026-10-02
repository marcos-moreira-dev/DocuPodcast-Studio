package com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar;

import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.*;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreInterventionState;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreInteractionTargetPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreStageZone;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lenient Markdown parser for the Theatre Grammar v1 template. */
public final class TheatreGrammarMarkdownParser {
    private static final Pattern ACT_HEADING = Pattern.compile("^##\\s+Acto\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCENE_HEADING = Pattern.compile("^###\\s+Escena\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHARACTER = Pattern.compile("^-\\s*personaje\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern OBJECT = Pattern.compile("^-\\s*objeto\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MARKDOWN_LINK = Pattern.compile("!?\\[[^]]*]\\(([^)]+)\\)");
    private static final Pattern SPEECH_LINE = Pattern.compile("^([\\p{Lu}][\\p{L}\\p{N}\\s.'_-]{1,48}):\\s+(.+)$");
    private static final Pattern STAGE_DIRECTION_LINE = Pattern.compile("^(?:ACOTACI[ÓO]N|DIDASCALIA):\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern METADATA_LINE = Pattern.compile("^>\\s+(.+)$");
    private static final Pattern IMAGE_LINE = Pattern.compile("^-\\s+(\\S+)(?:\\s*\\|\\s*(.+))?$");
    private static final Pattern COMMENT_BLOCK = Pattern.compile("<!--\\s*VOZ_CATALOGO:\\s*(.*?)-->", Pattern.DOTALL);
    private static final Pattern TONE_COMMENT = Pattern.compile("<!--\\s*TONO_CATALOGO:\\s*(.*?)-->", Pattern.DOTALL);

    private TheatreGrammarMarkdownParser() {
    }

    public static ImportPlan parse(Path sourceFile) throws IOException {
        return parse(Files.readString(sourceFile, StandardCharsets.UTF_8));
    }

    public static ImportPlan parse(String markdown) {
        String safe = markdown == null ? "" : markdown;
        boolean strictV2 = safe.matches("(?s).*?(?:grammarVersion|grammar_version)\\s*[:=]\\s*theatre-v2.*")
                || safe.contains("DocuPodcast Teatro Grammar v2");
        ArrayList<ActBuilder> acts = new ArrayList<>();
        ArrayList<ProfilePlan> characters = new ArrayList<>();
        ArrayList<ProfilePlan> objects = new ArrayList<>();
        ArrayList<String> links = new ArrayList<>();
        ArrayList<InterventionPlan> interventions = new ArrayList<>();
        String title = "";
        ActBuilder currentAct = null;
        SceneBuilder currentScene = null;
        int sceneIndex = 0;
        int interventionIndex = 0;
        String voiceCatalog = extractComment(safe, COMMENT_BLOCK);
        String toneCatalog = extractComment(safe, TONE_COMMENT);
        boolean lastWasProfile = false;
        String lastProfileType = "";
        boolean lastLineWasSpeech = false;

        // Catalog comments and example code are declarations, never spoken text.
        String content = safe.replaceAll("(?s)<!--.*?-->", "");
        boolean fenced = false;
        for (String rawLine : content.split("\\R")) {
            String line = rawLine.strip();
            if (line.startsWith("```") || line.startsWith("~~~")) { fenced = !fenced; continue; }
            if (fenced) continue;
            if (line.isBlank()) {
                lastWasProfile = false;
                lastLineWasSpeech = false;
                continue;
            }
            if (title.isBlank() && line.startsWith("# ")) {
                title = line.substring(2).strip();
                lastWasProfile = false;
                lastLineWasSpeech = false;
                continue;
            }
            Matcher linkMatcher = MARKDOWN_LINK.matcher(line);
            while (linkMatcher.find()) {
                links.add(linkMatcher.group(1).strip());
            }
            Matcher imageLineMatcher = IMAGE_LINE.matcher(line);
            if (imageLineMatcher.matches()
                    && lastWasProfile
                    && !CHARACTER.matcher(line).matches()
                    && !OBJECT.matcher(line).matches()) {
                ImageRef imgRef = parseImageRef(imageLineMatcher.group(1), imageLineMatcher.group(2));
                if ("character".equals(lastProfileType) && !characters.isEmpty()) {
                    ProfilePlan last = characters.remove(characters.size() - 1);
                    List<ImageRef> imgs = new ArrayList<>(last.imagenes());
                    imgs.add(imgRef);
                    characters.add(new ProfilePlan(last.name(), last.notes(), last.voz(), last.tono(), List.copyOf(imgs), last.id(), last.aliases()));
                } else if ("object".equals(lastProfileType) && !objects.isEmpty()) {
                    ProfilePlan last = objects.remove(objects.size() - 1);
                    List<ImageRef> imgs = new ArrayList<>(last.imagenes());
                    imgs.add(imgRef);
                    objects.add(new ProfilePlan(last.name(), last.notes(), last.voz(), last.tono(), List.copyOf(imgs), last.id(), last.aliases()));
                }
                lastLineWasSpeech = false;
                continue;
            }
            lastWasProfile = false;
            Matcher actMatcher = ACT_HEADING.matcher(line);
            if (actMatcher.matches()) {
                HeadingSpec heading = parseHeading(actMatcher.group(1));
                currentAct = new ActBuilder(heading.name(), heading.id());
                acts.add(currentAct);
                currentScene = null;
                lastLineWasSpeech = false;
                continue;
            }
            Matcher sceneMatcher = SCENE_HEADING.matcher(line);
            if (sceneMatcher.matches()) {
                if (currentAct == null) {
                    currentAct = new ActBuilder("Acto 1");
                    acts.add(currentAct);
                }
                HeadingSpec heading = parseHeading(sceneMatcher.group(1));
                currentScene = new SceneBuilder(heading.name(), heading.id());
                currentAct.scenes.add(currentScene);
                sceneIndex = 0;
                lastLineWasSpeech = false;
                continue;
            }
            Matcher characterMatcher = CHARACTER.matcher(line);
            if (characterMatcher.matches()) {
                parseProfile(characterMatcher.group(1)).ifPresent(characters::add);
                lastWasProfile = true;
                lastProfileType = "character";
                lastLineWasSpeech = false;
                continue;
            }
            Matcher objectMatcher = OBJECT.matcher(line);
            if (objectMatcher.matches()) {
                parseProfile(objectMatcher.group(1)).ifPresent(objects::add);
                lastWasProfile = true;
                lastProfileType = "object";
                lastLineWasSpeech = false;
                continue;
            }
            if (line.toLowerCase(Locale.ROOT).startsWith("notas:")) {
                String notes = line.substring("notas:".length()).strip();
                if (currentScene != null) {
                    currentScene.notes = notes;
                } else if (currentAct != null) {
                    currentAct.notes = notes;
                }
                lastLineWasSpeech = false;
                continue;
            }
            Matcher metadataMatcher = METADATA_LINE.matcher(line);
            if (metadataMatcher.matches()) {
                String metadata = metadataMatcher.group(1);
                if (!lastLineWasSpeech && currentScene != null && isSceneMetadata(metadata)) {
                    mergeSceneMetadata(currentScene, metadata);
                } else if (!interventions.isEmpty()) {
                    InterventionPlan last = interventions.remove(interventions.size() - 1);
                    interventions.add(mergeMetadata(last, metadata, strictV2));
                } else if (currentScene != null) {
                    mergeSceneMetadata(currentScene, metadata);
                }
                continue;
            }
            Matcher stageDirectionMatcher = STAGE_DIRECTION_LINE.matcher(line);
            if (stageDirectionMatcher.matches()) {
                String sceneName = currentScene == null ? "" : currentScene.name;
                interventionIndex++;
                interventions.add(new InterventionPlan("ACOTACION", sceneName, "", "", "", List.of(),
                        "NEUTRAL", "", false, "", false, List.of(), "", interventionIndex, sceneIndex,
                        "", TheatreInterventionState.InheritanceMode.RESET, "", List.of(), List.of(),
                        List.of(), "", "").withSpokenText(stageDirectionMatcher.group(1)));
                sceneIndex++;
                lastLineWasSpeech = true;
                continue;
            }
            Matcher speechMatcher = SPEECH_LINE.matcher(line);
            if (speechMatcher.matches()) {
                String characterName = speechMatcher.group(1).strip();
                String sceneName = currentScene == null ? "" : currentScene.name;
                String defaultTone = "";
                for (ProfilePlan cp : characters) {
                    if (cp.name().equalsIgnoreCase(characterName)) {
                        defaultTone = cp.tono();
                        break;
                    }
                }
                interventionIndex++;
                interventions.add(new InterventionPlan(characterName, sceneName, "", "", "", List.of(),
                        defaultTone, "", true, "", false, List.of(), "", interventionIndex, sceneIndex)
                        .withSpokenText(speechMatcher.group(2)));
                sceneIndex++;
                lastLineWasSpeech = true;
                continue;
            }
            lastLineWasSpeech = false;
        }

        return new ImportPlan(
                title.isBlank() ? "Obra teatral" : title,
                acts.stream().map(ActBuilder::build).toList(),
                List.copyOf(characters),
                List.copyOf(objects),
                List.copyOf(links),
                List.copyOf(interventions),
                voiceCatalog,
                toneCatalog,
                strictV2 ? "theatre-v2" : "theatre-v1");
    }

    private static ImageRef parseImageRef(String imgPath, String rest) {
        String sceneId = "";
        String angle = "";
        if (rest != null) {
            for (String part : rest.split("\\|")) {
                String kv = part.strip();
                int eq = kv.indexOf('=');
                if (eq > 0) {
                    String key = kv.substring(0, eq).strip().toLowerCase(Locale.ROOT);
                    String val = kv.substring(eq + 1).strip();
                    if ("escena".equals(key) || "scn".equals(key)) sceneId = val;
                    if ("angulo".equals(key) || "angle".equals(key) || "view".equals(key)) angle = val;
                } else if (!sceneId.isEmpty()) {
                    angle = kv;
                } else if (kv.contains("SCN-") || kv.startsWith("ESC-")) {
                    sceneId = kv;
                } else {
                    angle = kv;
                }
            }
        }
        return new ImageRef(imgPath, sceneId, angle);
    }

    private static String extractComment(String markdown, Pattern pattern) {
        Matcher m = pattern.matcher(markdown);
        return m.find() ? m.group(1).strip() : "";
    }

    private static InterventionPlan mergeMetadata(InterventionPlan plan, String metadataText, boolean strictV2) {
        String origin = plan.origin();
        String destination = plan.destination();
        String interactionTarget = plan.interactionTarget();
        List<String> images = new ArrayList<>(plan.images());
        String tono = plan.tono();
        String cameraCue = plan.cameraCue();
        boolean applyCamera = plan.applyCamera();
        String stageBackdrop = plan.stageBackdrop();
        boolean clearStageBackdrop = plan.clearStageBackdrop();
        List<String> simultaneousVoiceNames = new ArrayList<>(plan.simultaneousVoiceNames());
        String aiContextText = plan.aiContextText();
        String interventionId = plan.interventionId();
        TheatreInterventionState.InheritanceMode inheritanceMode = plan.inheritanceMode();
        String inheritsFrom = plan.inheritsFromInterventionId();
        List<TheatreInterventionState.CharacterState> characterStates = new ArrayList<>(plan.characterStates());
        List<TheatreInterventionState.ObjectState> objectStates = new ArrayList<>(plan.objectStates());
        List<TheatreInterventionState.StageEvent> stageEvents = new ArrayList<>(plan.stageEvents());
        String microexpression = plan.microexpression();
        String emoji = plan.emoji();
        for (String part : metadataText.split("\\|")) {
            String kv = part.strip();
            int eq = kv.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = kv.substring(0, eq).strip().toLowerCase(Locale.ROOT);
            String value = kv.substring(eq + 1).strip();
            switch (key) {
                case "origen" -> origin = value;
                case "destino" -> destination = value;
                case "interaccion" -> interactionTarget = TheatreInteractionTargetPolicy.canonicalize(value);
                case "imagen" -> images.add(value);
                case "tono", "emocion", "emoción", "emotion" -> tono = value;
                case "plano", "tipo_plano", "camara", "camera", "camera_cue" -> cameraCue = value;
                case "aplicar_plano", "usar_plano", "apply_camera", "use_camera" ->
                        applyCamera = parseBoolean(value, true);
                case "fondo", "fondo_escenario", "backdrop", "stage_backdrop" -> {
                    if (isClearBackdropValue(value)) {
                        clearStageBackdrop = true;
                        stageBackdrop = "";
                    } else {
                        stageBackdrop = value;
                        clearStageBackdrop = false;
                    }
                }
                case "quitar_fondo", "sin_fondo", "clear_backdrop", "no_fondo" -> {
                    clearStageBackdrop = parseBoolean(value, true);
                    if (clearStageBackdrop) {
                        stageBackdrop = "";
                    }
                }
                case "voces", "voces_simultaneas", "choral_voices", "simultaneous_voices" ->
                        simultaneousVoiceNames = parseNameList(value);
                case "contexto_ia", "contexto_textual", "contexto", "ai_context" -> aiContextText = value;
                case "id", "intervencion_id", "intervention_id" -> interventionId = value;
                case "hereda", "inherits" -> {
                    if (value.equalsIgnoreCase("ninguna") || value.equalsIgnoreCase("reset")) {
                        inheritanceMode = TheatreInterventionState.InheritanceMode.RESET;
                        inheritsFrom = "";
                    } else if (value.equalsIgnoreCase("anterior") || value.equalsIgnoreCase("previous")) {
                        inheritanceMode = TheatreInterventionState.InheritanceMode.PREVIOUS;
                        inheritsFrom = "";
                    } else {
                        inheritanceMode = TheatreInterventionState.InheritanceMode.EXPLICIT;
                        inheritsFrom = value;
                    }
                }
                case "presentes", "characters" -> characterStates.addAll(parsePresentCharacters(value));
                case "ausentes" -> characterStates.addAll(parseAbsentCharacters(value));
                case "orientaciones" -> characterStates.addAll(parseCharacterProperty(value, "orientation"));
                case "miradas" -> characterStates.addAll(parseCharacterProperty(value, "gaze"));
                case "variantes" -> characterStates.addAll(parseCharacterProperty(value, "variant"));
                case "vestuarios" -> characterStates.addAll(parseCharacterProperty(value, "costume"));
                case "objetos", "objects" -> objectStates.addAll(parseObjects(value));
                case "eventos", "events" -> stageEvents.addAll(parseEvents(value));
                case "microexpresion", "microexpression" -> microexpression = value;
                case "emoji" -> emoji = value;
                default -> {
                    if (strictV2) throw new IllegalArgumentException("Unknown theatre-v2 intervention property: " + key);
                }
            }
        }
        return new InterventionPlan(plan.characterName(), plan.sceneName(), origin, destination, interactionTarget,
                List.copyOf(images), tono, cameraCue, applyCamera, stageBackdrop, clearStageBackdrop,
                List.copyOf(simultaneousVoiceNames), aiContextText, plan.sequenceIndex(), plan.sceneIndex(),
                interventionId, inheritanceMode, inheritsFrom, mergeCharacterStates(characterStates),
                mergeObjectStates(objectStates), List.copyOf(stageEvents), microexpression, emoji, plan.spokenText());
    }

    private static List<TheatreInterventionState.CharacterState> parsePresentCharacters(String value) {
        ArrayList<TheatreInterventionState.CharacterState> result = new ArrayList<>();
        for (String token : value.split(",")) {
            String[] pair = token.strip().split("@", 2);
            if (pair[0].isBlank()) continue;
            String zone = pair.length > 1 ? canonicalZone(pair[1]) : "";
            result.add(new TheatreInterventionState.CharacterState(pair[0].strip(), TheatreInterventionState.Presence.PRESENT,
                    zone, "", "", "", ""));
        }
        return result;
    }

    private static List<TheatreInterventionState.CharacterState> parseAbsentCharacters(String value) {
        return parseNameList(value).stream().map(id -> new TheatreInterventionState.CharacterState(id,
                TheatreInterventionState.Presence.ABSENT, "", "", "", "", "")).toList();
    }

    private static List<TheatreInterventionState.CharacterState> parseCharacterProperty(String value, String property) {
        ArrayList<TheatreInterventionState.CharacterState> result = new ArrayList<>();
        for (String token : value.split(",")) {
            String[] pair = token.strip().split("@", 2);
            if (pair.length != 2 || pair[0].isBlank() || pair[1].isBlank()) throw new IllegalArgumentException("Invalid " + property + " mapping: " + token);
            result.add(new TheatreInterventionState.CharacterState(pair[0].strip(), TheatreInterventionState.Presence.INHERIT, "",
                    property.equals("orientation") ? pair[1].strip() : "", property.equals("gaze") ? pair[1].strip() : "",
                    property.equals("variant") ? pair[1].strip() : "", property.equals("costume") ? pair[1].strip() : ""));
        }
        return result;
    }

    private static List<TheatreInterventionState.ObjectState> parseObjects(String value) {
        ArrayList<TheatreInterventionState.ObjectState> result = new ArrayList<>();
        for (String token : value.split(",")) {
            String[] pair = token.strip().split("@", 2);
            String id = pair[0].strip();
            String state = pair.length > 1 ? pair[1].strip() : "";
            if (id.isBlank()) continue;
            if (state.equalsIgnoreCase("absent") || state.equalsIgnoreCase("ausente")) {
                result.add(new TheatreInterventionState.ObjectState(id, TheatreInterventionState.Presence.ABSENT, "", "", ""));
            } else if (state.toLowerCase(Locale.ROOT).startsWith("portado:")) {
                result.add(new TheatreInterventionState.ObjectState(id, TheatreInterventionState.Presence.PRESENT, "", state.substring(state.indexOf(':') + 1).strip(), "CARRY"));
            } else {
                result.add(new TheatreInterventionState.ObjectState(id, TheatreInterventionState.Presence.PRESENT, canonicalZone(state), "", ""));
            }
        }
        return result;
    }

    private static List<TheatreInterventionState.StageEvent> parseEvents(String value) {
        ArrayList<TheatreInterventionState.StageEvent> result = new ArrayList<>();
        for (String raw : value.split(";")) {
            String token = raw.strip(); if (token.isBlank()) continue;
            String[] at = token.split("@", 2); String[] parts = at[0].split(":");
            TheatreInterventionState.EventType type;
            try { type = TheatreInterventionState.EventType.valueOf(parts[0].strip().toUpperCase(Locale.ROOT)); }
            catch (RuntimeException ex) { throw new IllegalArgumentException("Invalid theatre event: " + token); }
            String character = parts.length > 1 ? parts[1].strip() : "";
            String object = parts.length > 2 ? parts[2].strip() : "";
            String target = parts.length > 3 ? parts[3].strip() : "";
            String destination = at.length > 1 ? canonicalZone(at[1]) : "";
            result.add(new TheatreInterventionState.StageEvent(type, character, object, target, "", destination));
        }
        return result;
    }

    private static String canonicalZone(String value) {
        if (value == null || value.isBlank()) return "";
        try { return TheatreStageZone.parse(value).grammarValue(); }
        catch (RuntimeException ex) { throw new IllegalArgumentException("Invalid theatre stage zone: " + value); }
    }

    private static List<TheatreInterventionState.CharacterState> mergeCharacterStates(List<TheatreInterventionState.CharacterState> values) {
        LinkedHashMap<String, TheatreInterventionState.CharacterState> merged = new LinkedHashMap<>();
        for (var value : values) {
            var old = merged.get(value.characterId());
            if (old == null) { merged.put(value.characterId(), value); continue; }
            merged.put(value.characterId(), new TheatreInterventionState.CharacterState(value.characterId(),
                    value.presence() == TheatreInterventionState.Presence.INHERIT ? old.presence() : value.presence(),
                    value.position().isBlank() ? old.position() : value.position(),
                    value.orientation().isBlank() ? old.orientation() : value.orientation(),
                    value.gazeTarget().isBlank() ? old.gazeTarget() : value.gazeTarget(),
                    value.visualVariantId().isBlank() ? old.visualVariantId() : value.visualVariantId(),
                    value.costume().isBlank() ? old.costume() : value.costume()));
        }
        return List.copyOf(merged.values());
    }

    private static List<TheatreInterventionState.ObjectState> mergeObjectStates(List<TheatreInterventionState.ObjectState> values) {
        LinkedHashMap<String, TheatreInterventionState.ObjectState> merged = new LinkedHashMap<>();
        for (var value : values) merged.put(value.objectId(), value);
        return List.copyOf(merged.values());
    }

    private static List<String> parseNameList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        ArrayList<String> result = new ArrayList<>();
        for (String part : value.split(",")) {
            String name = part.strip();
            if (!name.isBlank()) {
                result.add(name);
            }
        }
        return List.copyOf(result);
    }

    private static boolean isSceneMetadata(String metadataText) {
        for (String part : metadataText.split("\\|")) {
            int eq = part.strip().indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = part.strip().substring(0, eq).strip().toLowerCase(Locale.ROOT);
            if (key.equals("texto_inicio")
                    || key.equals("texto_fin")
                    || key.equals("texto_inicial")
                    || key.equals("texto_final")
                    || key.equals("inicio_texto")
                    || key.equals("fin_texto")
                    || key.equals("mapa_espacial")
                    || key.equals("mapa")
                    || key.equals("fondo_escenario")
                    || key.equals("fondo")
                    || key.equals("backdrop")
                    || key.equals("stage_backdrop")
                    || key.equals("scene_id")
                    || key.equals("escena_id")) {
                return true;
            }
        }
        return false;
    }

    private static void mergeSceneMetadata(SceneBuilder scene, String metadataText) {
        if (scene == null) {
            return;
        }
        for (String part : metadataText.split("\\|")) {
            String kv = part.strip();
            int eq = kv.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = kv.substring(0, eq).strip().toLowerCase(Locale.ROOT);
            String value = kv.substring(eq + 1).strip();
            switch (key) {
                case "texto_inicio", "texto_inicial", "inicio_texto" -> scene.textStartIndex = parsePositiveInt(value);
                case "texto_fin", "texto_final", "fin_texto" -> scene.textEndIndex = parsePositiveInt(value);
                case "mapa_espacial", "mapa" -> scene.spatialMap = value;
                case "fondo_escenario", "fondo", "backdrop", "stage_backdrop" ->
                        scene.stageBackdrop = isClearBackdropValue(value) ? "" : value;
                case "nota", "notas" -> scene.notes = value;
                case "scene_id", "escena_id" -> scene.id = value;
            }
        }
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        if (normalized.equals("true") || normalized.equals("si") || normalized.equals("yes") || normalized.equals("1")) {
            return true;
        }
        if (normalized.equals("false") || normalized.equals("no") || normalized.equals("0")) {
            return false;
        }
        return defaultValue;
    }

    private static boolean isClearBackdropValue(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        return normalized.equals("sin_fondo")
                || normalized.equals("sin fondo")
                || normalized.equals("none")
                || normalized.equals("null")
                || normalized.equals("sin")
                || normalized.equals("no")
                || normalized.equals("false");
    }

    private static int parsePositiveInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            int parsed = Integer.parseInt(value.strip());
            return Math.max(0, parsed);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static Optional<ProfilePlan> parseProfile(String text) {
        String[] parts = text.split("\\|");
        String name = parts.length == 0 ? "" : parts[0].strip();
        if (name.isBlank()) {
            return Optional.empty();
        }
        String notes = "";
        String voz = "";
        String tono = "";
        String scene = "";
        String angle = "";
        String image = "";
        String id = "";
        List<String> aliases = List.of();
        List<ImageRef> imagenes = List.of();
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].strip();
            int eq = part.indexOf('=');
            if (eq < 0) continue;
            String key = part.substring(0, eq).strip().toLowerCase(Locale.ROOT);
            String value = part.substring(eq + 1).strip();
            switch (key) {
                case "nota" -> notes = value;
                case "foto", "imagen" -> image = value;
                case "escena", "scn" -> scene = value;
                case "angulo", "angle", "view" -> angle = value;
                case "voz" -> voz = value;
                case "tono", "emocion", "emoción", "emotion" -> tono = value;
                case "id" -> id = value;
                case "alias", "aliases" -> aliases = parseNameList(value);
            }
        }
        if (!image.isBlank()) {
            imagenes = List.of(new ImageRef(image, scene, angle));
        }
        return Optional.of(new ProfilePlan(name, notes, voz, tono, imagenes, id, aliases));
    }

    private static final class ActBuilder {
        private final String name;
        private final String id;
        private String notes = "";
        private final ArrayList<SceneBuilder> scenes = new ArrayList<>();

        private ActBuilder(String name) { this(name, ""); }
        private ActBuilder(String name, String id) {
            this.name = name == null || name.isBlank() ? "Acto" : name;
            this.id = id == null ? "" : id;
        }

        private ActPlan build() {
            return new ActPlan(name, notes, scenes.stream().map(SceneBuilder::build).toList(), id);
        }
    }

    private static final class SceneBuilder {
        private final String name;
        private String notes = "";
        private int textStartIndex = 0;
        private int textEndIndex = 0;
        private String spatialMap = "";
        private String stageBackdrop = "";
        private String id = "";

        private SceneBuilder(String name) { this(name, ""); }
        private SceneBuilder(String name, String id) {
            this.name = name == null || name.isBlank() ? "Escena" : name;
            this.id = id == null ? "" : id;
        }

        private ScenePlan build() {
            return new ScenePlan(name, notes, textStartIndex, textEndIndex, spatialMap, stageBackdrop, id);
        }
    }

    private static HeadingSpec parseHeading(String raw) {
        String name = raw == null ? "" : raw.strip(); String id = "";
        String[] parts = name.split("\\|"); name = parts[0].strip();
        for (int i = 1; i < parts.length; i++) {
            int eq = parts[i].indexOf('=');
            if (eq > 0 && parts[i].substring(0, eq).strip().equalsIgnoreCase("id")) id = parts[i].substring(eq + 1).strip();
        }
        return new HeadingSpec(name, id);
    }
    private record HeadingSpec(String name, String id) { }
}
