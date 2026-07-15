package com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar;

import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

        for (String rawLine : safe.split("\\R")) {
            String line = rawLine.strip();
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
                    characters.add(new ProfilePlan(last.name(), last.notes(), last.voz(), last.tono(), List.copyOf(imgs)));
                } else if ("object".equals(lastProfileType) && !objects.isEmpty()) {
                    ProfilePlan last = objects.remove(objects.size() - 1);
                    List<ImageRef> imgs = new ArrayList<>(last.imagenes());
                    imgs.add(imgRef);
                    objects.add(new ProfilePlan(last.name(), last.notes(), last.voz(), last.tono(), List.copyOf(imgs)));
                }
                lastLineWasSpeech = false;
                continue;
            }
            lastWasProfile = false;
            Matcher actMatcher = ACT_HEADING.matcher(line);
            if (actMatcher.matches()) {
                currentAct = new ActBuilder(actMatcher.group(1).strip());
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
                currentScene = new SceneBuilder(sceneMatcher.group(1).strip());
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
                    interventions.add(mergeMetadata(last, metadata));
                } else if (currentScene != null) {
                    mergeSceneMetadata(currentScene, metadata);
                }
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
                        defaultTone, "", true, "", false, List.of(), "", interventionIndex, sceneIndex));
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
                toneCatalog);
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

    private static InterventionPlan mergeMetadata(InterventionPlan plan, String metadataText) {
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
                case "interaccion" -> interactionTarget = value;
                case "imagen" -> images.add(value);
                case "tono" -> tono = value;
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
            }
        }
        return new InterventionPlan(plan.characterName(), plan.sceneName(), origin, destination, interactionTarget,
                List.copyOf(images), tono, cameraCue, applyCamera, stageBackdrop, clearStageBackdrop,
                List.copyOf(simultaneousVoiceNames), aiContextText, plan.sequenceIndex(), plan.sceneIndex());
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
                    || key.equals("stage_backdrop")) {
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
                case "tono" -> tono = value;
            }
        }
        if (!image.isBlank()) {
            imagenes = List.of(new ImageRef(image, scene, angle));
        }
        return Optional.of(new ProfilePlan(name, notes, voz, tono, imagenes));
    }

    private static final class ActBuilder {
        private final String name;
        private String notes = "";
        private final ArrayList<SceneBuilder> scenes = new ArrayList<>();

        private ActBuilder(String name) {
            this.name = name == null || name.isBlank() ? "Acto" : name;
        }

        private ActPlan build() {
            return new ActPlan(name, notes, scenes.stream().map(SceneBuilder::build).toList());
        }
    }

    private static final class SceneBuilder {
        private final String name;
        private String notes = "";
        private int textStartIndex = 0;
        private int textEndIndex = 0;
        private String spatialMap = "";
        private String stageBackdrop = "";

        private SceneBuilder(String name) {
            this.name = name == null || name.isBlank() ? "Escena" : name;
        }

        private ScenePlan build() {
            return new ScenePlan(name, notes, textStartIndex, textEndIndex, spatialMap, stageBackdrop);
        }
    }
}
