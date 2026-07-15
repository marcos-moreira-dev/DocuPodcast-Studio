package com.marcosmoreiradev.docupodcaststudio.domain.voice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Official advanced voice presets bundled with DocuPodcast. */
public final class OfficialAdvancedVoicePresetCatalog {
    public static final String PRESET_ROOT = "samples/voices/advanced-presets";
    public static final String PRIMARY_PRESET_ID = "VOC-PRESET-HOMBRE-ADULTO-PERSONAJE-NARRATIVO";

    private static final Instant CREATED_AT = Instant.parse("2026-06-09T00:00:00Z");

    private static final List<PresetDefinition> PRESETS = List.of(
            new PresetDefinition("VOC-PRESET-HOMBRE-20-IDEALISTA-ECUADOR", "Hombre 20 idealista ecuatoriano diálogo", "hombre_20_idealista_ecuador_dialogo"),
            new PresetDefinition("VOC-PRESET-HOMBRE-20-PICARO-ECUADOR", "Hombre 20 pícaro ecuatoriano diálogo", "hombre_20_picaro_ecuador_dialogo"),
            new PresetDefinition("VOC-PRESET-HOMBRE-35-ASTUTO-ECUADOR", "Hombre 35 astuto ecuatoriano diálogo", "hombre_35_astuto_ecuador_dialogo"),
            new PresetDefinition("VOC-PRESET-HOMBRE-35-TERCO-COMICO-ECUADOR", "Hombre 35 terco cómico ecuatoriano diálogo", "hombre_35_terco_comico_ecuador_dialogo"),
            new PresetDefinition("VOC-PRESET-HOMBRE-40-BUROCRATA-ECUADOR", "Hombre 40 burócrata ecuatoriano diálogo", "hombre_40_burocrata_ecuador_dialogo"),
            new PresetDefinition("VOC-PRESET-HOMBRE-40-POPULAR-ECUADOR", "Hombre 40 popular ecuatoriano diálogo", "hombre_40_popular_ecuador_dialogo"),
            new PresetDefinition("VOC-PRESET-HOMBRE-45-TRABAJADOR-ECUADOR", "Hombre 45 trabajador ecuatoriano diálogo", "hombre_45_trabajador_ecuador_dialogo"),
            new PresetDefinition("VOC-PRESET-HOMBRE-50-PODER-GRINGO", "Hombre 50 poder gringo diálogo", "hombre_50_poder_gringo_dialogo"),
            new PresetDefinition("VOC-PRESET-HOMBRE-60-VETERANO-ECUADOR", "Hombre 60 veterano ecuatoriano diálogo", "hombre_60_veterano_ecuador_dialogo"),
            new PresetDefinition(PRIMARY_PRESET_ID, "Hombre adulto narrativo", "hombre_adulto_personaje_narrativo"),
            new PresetDefinition("VOC-PRESET-HOMBRE-MADURO-NARRATIVO", "Hombre maduro narrativo", "hombre_maduro_narrativo"),
            new PresetDefinition("VOC-PRESET-HOMBRE-40-FIRME-LATAM-PERSONAJE", "Hombre 40 firme LATAM personaje", "hombre_40_firme_latam_personaje"),
            new PresetDefinition("VOC-PRESET-HOMBRE-40-CONVERSACIONAL-LATAM-DIALOGO", "Hombre 40 conversacional LATAM dialogo", "hombre_40_conversacional_latam_dialogo"),
            new PresetDefinition("VOC-PRESET-MUJER-20-ENTUSIASTA-LATAM-PERSONAJE", "Mujer 20 entusiasta LATAM personaje", "mujer_20_entusiasta_latam_personaje"),
            new PresetDefinition("VOC-PRESET-MUJER-20-SUAVE-COLOMBIANA-NEUTRAL-DIALOGO", "Mujer 20 suave colombiana neutral dialogo", "mujer_20_suave_colombiana_neutral_dialogo"),
            new PresetDefinition("VOC-PRESET-HOMBRE-35-INTENSO-EXPLOSIVO-PERSONAJE", "Hombre 35 intenso explosivo personaje", "hombre_35_intenso_explosivo_personaje"),
            new PresetDefinition("VOC-PRESET-HOMBRE-45-RESIGNADO-OSCURO-PERSONAJE", "Hombre 45 resignado oscuro personaje", "hombre_45_resignado_oscuro_personaje"),
            new PresetDefinition("VOC-PRESET-HOMBRE-45-MORALISTA-COMICO-PERSONAJE", "Hombre 45 moralista comico personaje", "hombre_45_moralista_comico_personaje"),
            new PresetDefinition("VOC-PRESET-HOMBRE-35-ASPIRACIONAL-EMOTIVO-PERSONAJE", "Hombre 35 aspiracional emotivo personaje", "hombre_35_aspiracional_emotivo_personaje"),
            new PresetDefinition("VOC-PRESET-HOMBRE-40-INDIGNADO-TESTIGO-PERSONAJE", "Hombre 40 indignado testigo personaje", "hombre_40_indignado_testigo_personaje"),
            new PresetDefinition("VOC-PRESET-MUJER-40-DRAMATICA-INDIGNADA-PERSONAJE", "Mujer 40 dramatica indignada personaje", "mujer_40_dramatica_indignada_personaje"),
            new PresetDefinition("VOC-PRESET-MUJER-60-SABIA-MEMORIA-LATAM-PERSONAJE", "Mujer 60 sabia memoria LATAM personaje", "mujer_60_sabia_memoria_latam_personaje"),
            new PresetDefinition("VOC-PRESET-NINO-10-TERCO-CARICATURESCO-PERSONAJE", "Nino 10 terco caricaturesco personaje", "nino_10_terco_caricaturesco_personaje"),
            new PresetDefinition("VOC-PRESET-MUJER-25-CANTORA-ECUADOR", "Mujer 25 cantora ecuatoriana diálogo", "mujer_25_cantora_ecuador_dialogo"),
            new PresetDefinition("VOC-PRESET-MUJER-40-BUROCRATA-ECUADOR", "Mujer 40 burócrata ecuatoriana diálogo", "mujer_40_burocrata_ecuador_dialogo"),
            new PresetDefinition("VOC-PRESET-MUJER-40-POPULAR-ECUADOR", "Mujer 40 popular ecuatoriana diálogo", "mujer_40_popular_ecuador_dialogo"),
            new PresetDefinition("VOC-PRESET-MUJER-ADULTA-CALIDA-NARRATIVA", "Mujer adulta cálida narrativa", "mujer_adulta_calida_narrativo")
    );

    private static final Map<VoiceReferenceTone, String> TONE_FILES = toneFiles();

    private OfficialAdvancedVoicePresetCatalog() {
    }

    public static List<VoiceProfile> profiles() {
        return PRESETS.stream().map(OfficialAdvancedVoicePresetCatalog::profile).toList();
    }

    public static List<VoiceReferenceSampleSet> sampleSets() {
        return PRESETS.stream().map(OfficialAdvancedVoicePresetCatalog::sampleSet).toList();
    }

    public static List<String> voiceIds() {
        return PRESETS.stream().map(PresetDefinition::id).toList();
    }

    public static boolean isOfficialPreset(VoiceProfile voice) {
        return voice != null && "true".equalsIgnoreCase(voice.metadata().getOrDefault("officialPreset", ""));
    }

    private static VoiceProfile profile(PresetDefinition preset) {
        return new VoiceProfile(
                preset.id(),
                preset.displayName(),
                VoiceProfileType.PREDEFINED,
                VoiceEngineType.XTTS,
                "es",
                sampleId(preset, VoiceReferenceTone.NEUTRAL),
                "",
                VoiceQualityPreset.HUMAN_REFERENCE,
                true,
                "Voz avanzada prediseñada incluida con DocuPodcast; sus muestras son referencias oficiales para narración y teatro.",
                Map.of(
                        "builtInAdvancedReference", "true",
                        "officialPreset", "true",
                        "sourceFolder", preset.folder()
                )
        );
    }

    private static VoiceReferenceSampleSet sampleSet(PresetDefinition preset) {
        ArrayList<VoiceReferenceSample> samples = new ArrayList<>();
        for (Map.Entry<VoiceReferenceTone, String> entry : TONE_FILES.entrySet()) {
            VoiceReferenceTone tone = entry.getKey();
            samples.add(new VoiceReferenceSample(
                    sampleId(preset, tone),
                    preset.id(),
                    tone,
                    PRESET_ROOT + "/" + preset.folder() + "/" + entry.getValue(),
                    VoiceSampleOrigin.APP_DEFAULT,
                    VoiceFileOwnership.APP_RESOURCE,
                    0,
                    CREATED_AT,
                    "Muestra oficial prediseñada: " + preset.displayName() + " · " + tone.displayName() + "."
            ));
        }
        return VoiceReferenceSampleSet.forAdvancedVoice(preset.id(), samples);
    }

    private static String sampleId(PresetDefinition preset, VoiceReferenceTone tone) {
        return "VOICE-SAMPLE-" + preset.id() + "-" + tone.name();
    }

    private static Map<VoiceReferenceTone, String> toneFiles() {
        LinkedHashMap<VoiceReferenceTone, String> files = new LinkedHashMap<>();
        files.put(VoiceReferenceTone.NEUTRAL, "neutral.wav");
        files.put(VoiceReferenceTone.HAPPY, "feliz.wav");
        files.put(VoiceReferenceTone.SAD, "triste.wav");
        files.put(VoiceReferenceTone.ANGRY, "enojada.wav");
        files.put(VoiceReferenceTone.SERIOUS, "seria.wav");
        files.put(VoiceReferenceTone.CALM, "calmada.wav");
        files.put(VoiceReferenceTone.WORRIED, "preocupada.wav");
        files.put(VoiceReferenceTone.ENTHUSIASTIC, "entusiasmada.wav");
        files.put(VoiceReferenceTone.BORED, "aburrida.wav");
        files.put(VoiceReferenceTone.EUPHORIC, "euforica.wav");
        files.put(VoiceReferenceTone.MELANCHOLIC, "melancolica.wav");
        files.put(VoiceReferenceTone.NERVOUS, "nerviosa.wav");
        files.put(VoiceReferenceTone.AFRAID, "asustada.wav");
        files.put(VoiceReferenceTone.SURPRISED, "sorprendida.wav");
        files.put(VoiceReferenceTone.DOUBTFUL, "dudosa.wav");
        files.put(VoiceReferenceTone.TIRED, "cansada.wav");
        files.put(VoiceReferenceTone.PLEADING, "suplicante.wav");
        files.put(VoiceReferenceTone.AUTHORITATIVE, "autoritaria.wav");
        files.put(VoiceReferenceTone.IRONIC, "ironica.wav");
        files.put(VoiceReferenceTone.SARCASTIC, "sarcastica.wav");
        files.put(VoiceReferenceTone.MYSTERIOUS, "misteriosa.wav");
        files.put(VoiceReferenceTone.SOLEMN, "solemne.wav");
        files.put(VoiceReferenceTone.HEROIC, "heroica.wav");
        files.put(VoiceReferenceTone.DRAMATIC, "dramatica.wav");
        files.put(VoiceReferenceTone.TENSE, "tensa.wav");
        files.put(VoiceReferenceTone.RUSHED, "apurada.wav");
        files.put(VoiceReferenceTone.CONFUSED, "confundida.wav");
        files.put(VoiceReferenceTone.TENDER, "tierna.wav");
        files.put(VoiceReferenceTone.COLD, "fria.wav");
        files.put(VoiceReferenceTone.MOCKING, "burlona.wav");
        files.put(VoiceReferenceTone.DISTRUSTFUL, "desconfiada.wav");
        files.put(VoiceReferenceTone.REPENTANT, "arrepentida.wav");
        files.put(VoiceReferenceTone.VULNERABLE, "vulnerable.wav");
        files.put(VoiceReferenceTone.HOPEFUL, "esperanzada.wav");
        files.put(VoiceReferenceTone.RESIGNED, "resignada.wav");
        files.put(VoiceReferenceTone.THREATENING, "amenazante.wav");
        files.put(VoiceReferenceTone.DEFIANT, "desafiante.wav");
        files.put(VoiceReferenceTone.SEDUCTIVE_NON_EXPLICIT, "sutil.wav");
        return Collections.unmodifiableMap(files);
    }

    private record PresetDefinition(String id, String displayName, String folder) {
    }
}
