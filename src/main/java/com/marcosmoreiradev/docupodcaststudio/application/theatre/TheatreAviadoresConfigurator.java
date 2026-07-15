package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TheatreAviadoresConfigurator {

    public ConfigureResult execute(NarrationScriptDocument script, VoiceLibrary voiceLibrary,
                                   Map<String, String> assetIds) {
        List<String> voiceIds = existingVoiceIds(voiceLibrary);
        TheatreProjectLayer layer = buildLayer(script, assetIds, voiceIds);
        List<NarrativeLayerAssignment> emotions = buildEmotions(script);
        return new ConfigureResult(layer, sceneBoundariesStart(), sceneBoundariesEnd(), emotions);
    }

    public record ConfigureResult(
            TheatreProjectLayer layer,
            Map<String, String> sceneBoundariesStart,
            Map<String, String> sceneBoundariesEnd,
            List<NarrativeLayerAssignment> emotionAssignments) {
    }

    public static final List<String> REQUIRED_ASSET_FILENAMES = List.of(
            "capitan_bigote_01_frontal.png", "capitan_bigote_02_lateral_izquierdo.png",
            "capitan_bigote_03_lateral_derecho.png", "capitan_bigote_04_posterior.png",
            "capitan_bigote_05_trasero.png", "teniente_tornillo_01_frontal.png",
            "teniente_tornillo_02_lateral_izquierdo.png", "teniente_tornillo_03_lateral_derecho.png",
            "teniente_tornillo_04_posterior.png", "teniente_tornillo_05_trasero.png",
            "avion_tornillo_dorado_01.png", "obj_bidon_combustible_01.png",
            "obj_tornillos_sobrantes_01.png", "obj_mapa_01.png", "obj_compas_01.png",
            "obj_paloma_01.png", "obj_fardo_heno_01.png", "animal_vaca_01.png",
            "obj_sombrero_capitan_01.png");

    private TheatreProjectLayer buildLayer(NarrationScriptDocument script, Map<String, String> assetIds,
                                           List<String> voiceIds) {
        List<TheatreProjectLayer.Intervencion> aliases = script == null ? List.of() : script.segments().stream()
                .filter(NarrationSegment::narratable)
                .map(segment -> {
                    int seq = (int) script.segments().stream()
                            .filter(NarrationSegment::narratable)
                            .takeWhile(s -> !s.id().equals(segment.id()))
                            .count() + 1;
                    return TheatreProjectLayer.Intervencion.ofSequence(seq,
                            segment.sourceBlockIds().stream().findFirst().orElse(segment.id()));
                })
                .toList();
        List<TheatreProjectLayer.CharacterProfile> characters = List.of(
                new TheatreProjectLayer.CharacterProfile("CHR-NARRADOR", "NARRADOR", List.of("NARRADOR"),
                        "Voz externa opcional. Puede narrar desde fuera de escena o actuar como presencia diegetica."),
                new TheatreProjectLayer.CharacterProfile("CHR-CAPITAN-BIGOTE", "CAPITAN BIGOTE",
                        List.of("BIGOTE", "CAPITAN"),
                        "Piloto veterano, ceremonioso y terco. Usa chaqueta de aviador, bigote marcado y actitud de mando."),
                new TheatreProjectLayer.CharacterProfile("CHR-TENIENTE-TORNILLO", "TENIENTE TORNILLO",
                        List.of("TORNILLO", "TENIENTE"),
                        "Copiloto inventor, optimista y literal. Usa gafas, herramientas y energia mecanica."));
        List<TheatreProjectLayer.VoiceRoleAlias> voices = List.of(
                new TheatreProjectLayer.VoiceRoleAlias("VOICE-ROLE-NARRADOR", "NARRADOR", voiceIds.get(0), "CHR-NARRADOR",
                        "Asignada por el demo teatral con voz existente."),
                new TheatreProjectLayer.VoiceRoleAlias("VOICE-ROLE-BIGOTE", "CAPITAN BIGOTE", voiceIds.get(1),
                        "CHR-CAPITAN-BIGOTE", "Asignada por el demo teatral con voz existente."),
                new TheatreProjectLayer.VoiceRoleAlias("VOICE-ROLE-TORNILLO", "TENIENTE TORNILLO", voiceIds.get(2),
                        "CHR-TENIENTE-TORNILLO", "Asignada por el demo teatral con voz existente."));
        List<TheatreProjectLayer.TheatreAct> acts = List.of(
                new TheatreProjectLayer.TheatreAct("ACT-001", "Acto 1", "Vuelo comico completo del Tornillo Dorado."));
        List<TheatreProjectLayer.Scene> scenes = List.of(
                new TheatreProjectLayer.Scene("SCN-001", "Escena 1: El hangar",
                        "Amanecer. Avion al centro, herramientas y combustible visibles.", "ACT-001"),
                new TheatreProjectLayer.Scene("SCN-002", "Escena 2: En el aire",
                        "Cabina en vuelo. Mapa, compas, paloma y vaca como referencias visuales.", "ACT-001"),
                new TheatreProjectLayer.Scene("SCN-003", "Escena 3: El aterrizaje",
                        "Aterrizaje rural entre fardos de heno, sombrero y publico prudente.", "ACT-001"));
        List<TheatreProjectLayer.TheatreObject> objects = List.of(
                obj("OBJ-AVION", "Avion Tornillo Dorado", "Biplano antiguo y centro visual de la obra."),
                obj("OBJ-BIDON", "Bidon de combustible", "Utileria del hangar para la revision antes del despegue."),
                obj("OBJ-TORNILLOS", "Tornillos sobrantes",
                        "Objeto comico recurrente; el capitan insiste en que son de confianza."),
                obj("OBJ-MAPA", "Mapa de ruta", "Mapa que confunde a los aviadores porque el norte parece estar abajo."),
                obj("OBJ-COMPAS", "Compas", "Instrumento de navegacion interpretado de forma absurda."),
                obj("OBJ-PALOMA", "Paloma", "Presencia breve que funciona como juicio silencioso del vuelo."),
                obj("OBJ-FARDO", "Fardo de heno", "Elemento de aterrizaje rural para la escena final."),
                obj("OBJ-VACA", "Vaca", "Testigo rural del aterrizaje."),
                obj("OBJ-SOMBRERO", "Sombrero del capitan", "Indicador de dignidad perdida tras el aterrizaje."));
        List<TheatreProjectLayer.CharacterImage> characterImages = new ArrayList<>();
        addCharImg(characterImages, "CHR-CAPITAN-BIGOTE", "SCN-001", "Frontal",
                assetIds.get("capitan_bigote_01_frontal.png"));
        addCharImg(characterImages, "CHR-CAPITAN-BIGOTE", "SCN-001", "Lateral izquierdo",
                assetIds.get("capitan_bigote_02_lateral_izquierdo.png"));
        addCharImg(characterImages, "CHR-CAPITAN-BIGOTE", "SCN-002", "Lateral derecho",
                assetIds.get("capitan_bigote_03_lateral_derecho.png"));
        addCharImg(characterImages, "CHR-CAPITAN-BIGOTE", "SCN-003", "Posterior",
                assetIds.get("capitan_bigote_04_posterior.png"));
        addCharImg(characterImages, "CHR-CAPITAN-BIGOTE", "SCN-003", "Trasero",
                assetIds.get("capitan_bigote_05_trasero.png"));
        addCharImg(characterImages, "CHR-TENIENTE-TORNILLO", "SCN-001", "Frontal",
                assetIds.get("teniente_tornillo_01_frontal.png"));
        addCharImg(characterImages, "CHR-TENIENTE-TORNILLO", "SCN-001", "Lateral izquierdo",
                assetIds.get("teniente_tornillo_02_lateral_izquierdo.png"));
        addCharImg(characterImages, "CHR-TENIENTE-TORNILLO", "SCN-002", "Lateral derecho",
                assetIds.get("teniente_tornillo_03_lateral_derecho.png"));
        addCharImg(characterImages, "CHR-TENIENTE-TORNILLO", "SCN-003", "Posterior",
                assetIds.get("teniente_tornillo_04_posterior.png"));
        addCharImg(characterImages, "CHR-TENIENTE-TORNILLO", "SCN-003", "Trasero",
                assetIds.get("teniente_tornillo_05_trasero.png"));
        List<TheatreProjectLayer.ObjectImage> objectImages = new ArrayList<>();
        addObjImg(objectImages, "OBJ-AVION", "SCN-001", "Principal", assetIds.get("avion_tornillo_dorado_01.png"));
        addObjImg(objectImages, "OBJ-BIDON", "SCN-001", "Utileria", assetIds.get("obj_bidon_combustible_01.png"));
        addObjImg(objectImages, "OBJ-TORNILLOS", "SCN-001", "Utileria", assetIds.get("obj_tornillos_sobrantes_01.png"));
        addObjImg(objectImages, "OBJ-MAPA", "SCN-002", "Utileria", assetIds.get("obj_mapa_01.png"));
        addObjImg(objectImages, "OBJ-COMPAS", "SCN-002", "Utileria", assetIds.get("obj_compas_01.png"));
        addObjImg(objectImages, "OBJ-PALOMA", "SCN-002", "Referencia", assetIds.get("obj_paloma_01.png"));
        addObjImg(objectImages, "OBJ-VACA", "SCN-002", "Referencia", assetIds.get("animal_vaca_01.png"));
        addObjImg(objectImages, "OBJ-FARDO", "SCN-003", "Utileria", assetIds.get("obj_fardo_heno_01.png"));
        addObjImg(objectImages, "OBJ-SOMBRERO", "SCN-003", "Utileria", assetIds.get("obj_sombrero_capitan_01.png"));
        List<TheatreProjectLayer.IntervencionVisual> fragmentImages = List.of();
        return new TheatreProjectLayer(
                aliases, characters, voices, characterImages, fragmentImages, acts, scenes,
                List.of(
                        new TheatreProjectLayer.SpatialPosition("SCN-001", "T1", "CHR-NARRADOR", 0.50, 0.12,
                                "Voz externa del inicio."),
                        new TheatreProjectLayer.SpatialPosition("SCN-001", "T2", "CHR-CAPITAN-BIGOTE", 0.45, 0.70,
                                "Frente centro."),
                        new TheatreProjectLayer.SpatialPosition("SCN-001", "T3", "CHR-TENIENTE-TORNILLO", 0.72, 0.58,
                                "Centro derecha.")),
                List.of(
                        new TheatreProjectLayer.TheatreAction("SCN-001", "T2", "T3", "CHR-CAPITAN-BIGOTE",
                                "El capitan ordena la revision al teniente.", true),
                        new TheatreProjectLayer.TheatreAction("SCN-001", "T3", "T2", "CHR-TENIENTE-TORNILLO",
                                "El teniente responde con literalidad mecanica.", true)),
                textActionPlacements(),
                objectImages, objects);
    }

    private static List<TheatreProjectLayer.TextActionPlacement> textActionPlacements() {
        String[][] data = {
                { "1", "SCN-001", "CHR-NARRADOR" }, { "2", "SCN-001", "CHR-NARRADOR" },
                { "3", "SCN-001", "CHR-CAPITAN-BIGOTE" }, { "4", "SCN-001", "CHR-TENIENTE-TORNILLO" },
                { "5", "SCN-001", "CHR-CAPITAN-BIGOTE" }, { "6", "SCN-001", "CHR-TENIENTE-TORNILLO" },
                { "7", "SCN-001", "CHR-CAPITAN-BIGOTE" }, { "8", "SCN-002", "CHR-NARRADOR" },
                { "9", "SCN-002", "CHR-TENIENTE-TORNILLO" }, { "10", "SCN-002", "CHR-CAPITAN-BIGOTE" },
                { "11", "SCN-002", "CHR-TENIENTE-TORNILLO" }, { "12", "SCN-002", "CHR-CAPITAN-BIGOTE" },
                { "13", "SCN-002", "CHR-NARRADOR" }, { "14", "SCN-002", "CHR-TENIENTE-TORNILLO" },
                { "15", "SCN-002", "CHR-CAPITAN-BIGOTE" }, { "16", "SCN-002", "CHR-TENIENTE-TORNILLO" },
                { "17", "SCN-002", "CHR-CAPITAN-BIGOTE" }, { "18", "SCN-003", "CHR-NARRADOR" },
                { "19", "SCN-003", "CHR-TENIENTE-TORNILLO" }, { "20", "SCN-003", "CHR-CAPITAN-BIGOTE" },
                { "21", "SCN-003", "CHR-NARRADOR" }, { "22", "SCN-003", "CHR-TENIENTE-TORNILLO" },
                { "23", "SCN-003", "CHR-CAPITAN-BIGOTE" }, { "24", "SCN-003", "CHR-NARRADOR" },
        };
        Map<String, String> scn1Locs = Map.of("CHR-CAPITAN-BIGOTE", "fondo centro", "CHR-TENIENTE-TORNILLO",
                "fondo derecha", "CHR-NARRADOR", "extra diegetico");
        Map<String, String> scn2Locs = Map.of("CHR-CAPITAN-BIGOTE", "centro izquierda", "CHR-TENIENTE-TORNILLO",
                "centro derecha", "CHR-NARRADOR", "extra diegetico");
        Map<String, String> scn3Locs = Map.of("CHR-CAPITAN-BIGOTE", "frente izquierda", "CHR-TENIENTE-TORNILLO",
                "frente derecha", "CHR-NARRADOR", "extra diegetico");
        ArrayList<TheatreProjectLayer.TextActionPlacement> result = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            String id = "INTERVENCION-" + data[i][0];
            String sceneId = data[i][1];
            String characterId = data[i][2];
            Map<String, String> locs = "SCN-001".equals(sceneId) ? scn1Locs
                    : "SCN-002".equals(sceneId) ? scn2Locs : scn3Locs;
            String origin = locs.get(characterId);
            String dest = (i + 1 < data.length && data[i + 1][1].equals(sceneId)) ? locs.get(data[i + 1][2]) : origin;
            result.add(new TheatreProjectLayer.TextActionPlacement(id, sceneId, characterId, origin, dest, "", locs));
        }
        return List.copyOf(result);
    }

    private static List<String> existingVoiceIds(VoiceLibrary library) {
        VoiceLibrary source = library == null ? VoiceLibrary.defaults() : library;
        List<String> usable = source.voices().stream()
                .filter(VoiceProfile::usableForTts)
                .map(VoiceProfile::id)
                .filter(id -> id != null && !id.isBlank())
                .toList();
        if (usable.isEmpty()) {
            return List.of("VOC-NARRATOR", "VOC-NARRATOR", "VOC-NARRATOR");
        }
        return List.of(
                firstExisting(usable, "VOC-PRESET-HOMBRE-ADULTO-PERSONAJE-NARRATIVO", 0),
                firstExisting(usable, "VOC-PRESET-HOMBRE-60-VETERANO-ECUADOR", Math.min(1, usable.size() - 1)),
                firstExisting(usable, "VOC-PRESET-HOMBRE-20-IDEALISTA-ECUADOR", Math.min(2, usable.size() - 1)));
    }

    private static String firstExisting(List<String> voiceIds, String preferred, int fallbackIndex) {
        if (voiceIds.contains(preferred)) {
            return preferred;
        }
        return voiceIds.get(Math.max(0, Math.min(fallbackIndex, voiceIds.size() - 1)));
    }

    private static Map<String, String> sceneBoundariesStart() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("SCN-001", "INTERVENCION-1");
        map.put("SCN-002", "INTERVENCION-7");
        map.put("SCN-003", "INTERVENCION-16");
        return map;
    }

    private static Map<String, String> sceneBoundariesEnd() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("SCN-001", "INTERVENCION-6");
        map.put("SCN-002", "INTERVENCION-15");
        map.put("SCN-003", "INTERVENCION-21");
        return map;
    }

    private List<NarrativeLayerAssignment> buildEmotions(NarrationScriptDocument script) {
        if (script == null || script.empty()) {
            return List.of();
        }
        List<NarrativeLayerAssignment> assignments = new ArrayList<>();
        for (NarrationSegment segment : script.segments()) {
            if (!segment.narratable()) continue;
            VoiceReferenceTone tone = toneFor(segment);
            assignments.add(new NarrativeLayerAssignment(
                    "NLA-EMOTION-DEMO-" + segment.id(),
                    NarrativeLayerKind.EMOTION,
                    new ScriptTextRange(segment.id(), 0, segment.narrationText().length()),
                    tone.layerTargetId(),
                    "Tono " + tone.displayName(),
                    "Emocion teatral configurada por el demo Aviadores Comicos."));
        }
        return List.copyOf(assignments);
    }

    private static VoiceReferenceTone toneFor(NarrationSegment segment) {
        String characterId = segment.characterId() == null ? "" : segment.characterId().strip();
        String text = segment.narrationText() == null ? "" : segment.narrationText().strip().toUpperCase(Locale.ROOT);
        if ("CHR-CAPITAN-BIGOTE".equals(characterId)) {
            return text.contains("NO SON SOBRANTES") || text.contains("EXCELENTE")
                    ? VoiceReferenceTone.DRAMATIC
                    : VoiceReferenceTone.SERIOUS;
        }
        if ("CHR-TENIENTE-TORNILLO".equals(characterId)) {
            return text.contains("CUAL ERA") || text.contains("TRES TORNILLOS")
                    ? VoiceReferenceTone.HAPPY
                    : VoiceReferenceTone.ENTHUSIASTIC;
        }
        if ("CHR-NARRADOR".equals(characterId)) {
            return VoiceReferenceTone.CALM;
        }
        if (text.startsWith("(") || text.startsWith("ACOTACION")) {
            return VoiceReferenceTone.CALM;
        }
        return VoiceReferenceTone.NEUTRAL;
    }

    private static TheatreProjectLayer.TheatreObject obj(String id, String name, String notes) {
        return new TheatreProjectLayer.TheatreObject(id, name, notes);
    }

    private static void addCharImg(List<TheatreProjectLayer.CharacterImage> images, String characterId,
                                   String sceneId, String view, String assetId) {
        if (assetId != null && !assetId.isBlank()) {
            images.add(new TheatreProjectLayer.CharacterImage(characterId, sceneId, view, assetId,
                    "Asset visual del demo teatral."));
        }
    }

    private static void addObjImg(List<TheatreProjectLayer.ObjectImage> images, String objectId,
                                  String sceneId, String view, String assetId) {
        if (assetId != null && !assetId.isBlank()) {
            images.add(new TheatreProjectLayer.ObjectImage(objectId, sceneId, view, assetId,
                    "Asset de utileria del demo teatral."));
        }
    }
}
