package com.marcosmoreiradev.docupodcaststudio.domain.voice;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Reference tones used by the advanced voice workflow.
 *
 * <p>These are not text commands. Each tone represents an optional voice reference
 * sample recorded or imported by the user. If a requested tone is missing, the
 * product should fall back to the neutral sample of the same voice and notify the
 * user.</p>
 */
public enum VoiceReferenceTone {
    NEUTRAL(VoiceReferenceToneCategory.BASIC, "Neutral",
            "Hoy leeré este texto con claridad, calma y una voz natural para que cada palabra se entienda bien."),
    HAPPY(VoiceReferenceToneCategory.BASIC, "Feliz",
            "Qué alegría estar aquí; siento que este momento trae una luz nueva y una sonrisa imposible de esconder."),
    SAD(VoiceReferenceToneCategory.BASIC, "Triste",
            "A veces el silencio pesa más que las palabras, y aun así intento seguir hablando con el corazón sereno."),
    ANGRY(VoiceReferenceToneCategory.BASIC, "Enojada",
            "No puedo aceptar que esto siga ocurriendo; ya he esperado demasiado y necesito que me escuchen ahora."),
    SERIOUS(VoiceReferenceToneCategory.BASIC, "Seria",
            "Este asunto requiere atención, precisión y responsabilidad; cada detalle debe revisarse antes de decidir."),
    CALM(VoiceReferenceToneCategory.BASIC, "Calmada",
            "Respira con tranquilidad; todo puede ordenarse paso a paso si mantenemos la mente clara y la voz serena."),
    WORRIED(VoiceReferenceToneCategory.BASIC, "Preocupada",
            "Me inquieta lo que pueda pasar si no actuamos a tiempo, aunque todavía quiero creer que hay solución."),
    ENTHUSIASTIC(VoiceReferenceToneCategory.BASIC, "Entusiasmada",
            "Esto puede convertirse en algo enorme; siento que estamos a punto de descubrir una oportunidad increíble."),
    BORED(VoiceReferenceToneCategory.BASIC, "Aburrida",
            "Otra vez la misma historia, las mismas palabras y la misma espera interminable que parece no cambiar nada."),

    JOYFUL(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Alegre",
            "Me encanta ver cómo todo empieza a tomar forma; hay una energía bonita en este lugar y se nota."),
    EUPHORIC(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Eufórica",
            "No puedo creerlo; esto es mucho mejor de lo que imaginaba y siento que voy a explotar de emoción."),
    MELANCHOLIC(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Melancólica",
            "Recuerdo aquellos días con una mezcla de ternura y distancia, como si el tiempo los hubiera cubierto suavemente."),
    NERVOUS(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Nerviosa",
            "No sé si estoy lista, pero voy a intentarlo; mis manos tiemblan un poco y mi voz quiere adelantarse."),
    AFRAID(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Asustada",
            "Escuché un ruido detrás de la puerta y, por un momento, sentí que el aire se quedaba completamente quieto."),
    SURPRISED(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Sorprendida",
            "¿De verdad ocurrió eso? No esperaba esta noticia y todavía estoy tratando de entender lo que significa."),
    DOUBTFUL(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Dudosa",
            "Tal vez sea una buena idea, pero hay algo que no termina de convencerme y necesito pensarlo mejor."),
    TIRED(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Cansada",
            "He caminado demasiado por hoy; mi voz se vuelve lenta y solo quiero descansar un momento en silencio."),
    PLEADING(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Suplicante",
            "Por favor, escúchame un instante más; no te pido mucho, solo una oportunidad para explicar lo que siento."),
    AUTHORITATIVE(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Autoritaria",
            "Escuchen con atención: desde este momento cada persona cumplirá su parte sin excusas ni retrasos innecesarios."),
    IRONIC(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Irónica",
            "Claro, porque seguramente todo se arregla solo si fingimos que nada de esto era importante."),
    SARCASTIC(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Sarcástica",
            "Maravilloso, justo lo que necesitábamos: otro problema presentado como si fuera una brillante solución."),
    MYSTERIOUS(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Misteriosa",
            "Hay cosas que no deben decirse en voz alta, especialmente cuando la noche parece escuchar detrás de las paredes."),
    SOLEMN(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Solemne",
            "Hoy pronunciamos estas palabras con respeto, conscientes del peso que tienen para quienes estuvieron antes que nosotros."),
    HEROIC(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Heroica",
            "Aunque el camino sea difícil, avanzaremos con valor, porque alguien debe dar el primer paso por los demás."),
    DRAMATIC(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Dramática",
            "Si esta es la última vez que hablo, que al menos mis palabras queden grabadas en tu memoria."),
    TENSE(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Tensa",
            "Nadie se movió; todos esperaban una respuesta mientras el reloj parecía sonar más fuerte que nunca."),
    RUSHED(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Apurada",
            "Tenemos que salir ahora mismo; no hay tiempo para discutir detalles, recoge lo necesario y ven conmigo."),
    CONFUSED(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Confundida",
            "Espera, no entiendo qué acaba de pasar; hace un momento todo parecía claro y ahora nada encaja."),
    TENDER(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Tierna",
            "Ven aquí, no tengas miedo; a veces una voz suave puede hacer que el mundo parezca menos grande."),
    COLD(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Fría",
            "No confundas mi silencio con duda; simplemente ya tomé una decisión y no pienso repetirla."),
    MOCKING(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Burlona",
            "¿Eso era todo? Pensé que venías con una gran respuesta, no con esa explicación tan conveniente."),
    DISTRUSTFUL(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Desconfiada",
            "Dices que puedo confiar en ti, pero tus palabras no coinciden con lo que vi hace un momento."),
    REPENTANT(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Arrepentida",
            "Si pudiera volver atrás, elegiría mejor mis palabras y no dejaría que el orgullo hablara por mí."),
    VULNERABLE(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Vulnerable",
            "No me resulta fácil decir esto, pero necesito admitir que tengo miedo y que no puedo hacerlo sola."),
    HOPEFUL(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Esperanzada",
            "Quizás todavía haya una salida; a veces basta una pequeña señal para volver a creer."),
    RESIGNED(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Resignada",
            "Ya entendí que no todo puede cambiarse; haré lo que me toca y seguiré adelante sin pelear más."),
    THREATENING(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Amenazante",
            "Te conviene pensar muy bien tu próxima palabra, porque esta vez no voy a pasar por alto lo que hiciste."),
    DEFIANT(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Desafiante",
            "Si creen que voy a rendirme tan fácilmente, todavía no han entendido quién soy ni por qué sigo aquí."),
    SEDUCTIVE_NON_EXPLICIT(VoiceReferenceToneCategory.THEATRICAL_EXTENDED, "Sutil",
            "Habla con calma y presencia; algunas ideas se comunican mejor con una intención medida y una voz serena.");

    private final VoiceReferenceToneCategory category;
    private final String displayName;
    private final String suggestedRecordingPrompt;

    VoiceReferenceTone(VoiceReferenceToneCategory category, String displayName, String suggestedRecordingPrompt) {
        this.category = category;
        this.displayName = displayName;
        this.suggestedRecordingPrompt = suggestedRecordingPrompt;
    }

    public VoiceReferenceToneCategory category() {
        return category;
    }

    public String displayName() {
        return displayName;
    }

    public String suggestedRecordingPrompt() {
        return suggestedRecordingPrompt;
    }

    public boolean isNeutral() {
        return this == NEUTRAL;
    }

    /** Stable target id used by document-layer tone assignments. */
    public String layerTargetId() {
        return "TONE-" + name();
    }

    public static Optional<VoiceReferenceTone> fromLayerTargetId(String value) {
        String normalized = value == null ? "" : value.strip().toUpperCase(java.util.Locale.ROOT);
        if (normalized.startsWith("TONE-")) {
            normalized = normalized.substring("TONE-".length());
        }
        if (normalized.startsWith("STY-")) {
            var legacy = fromPerformanceStyleId(normalized);
            if (legacy.isPresent()) return legacy;
            normalized = normalized.substring(4);
        }
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        for (VoiceReferenceTone tone : values()) {
            String label = normalizeLabel(tone.displayName());
            String input = normalizeLabel(normalized);
            if (tone.name().equalsIgnoreCase(normalized) || label.equals(input)
                    || (label.endsWith("a") && (label.substring(0,label.length()-1)+"o").equals(input))) {
                return Optional.of(tone);
            }
        }
        return Optional.empty();
    }

    private static String normalizeLabel(String value) {
        return java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(java.util.Locale.ROOT);
    }

    private static Optional<VoiceReferenceTone> fromPerformanceStyleId(String styleId) {
        String normalized = styleId == null ? "" : styleId.strip().toUpperCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "STY-NEUTRAL" -> Optional.of(NEUTRAL);
            case "STY-HAPPY", "STY-CHEERFUL" -> Optional.of(HAPPY);
            case "STY-SAD" -> Optional.of(SAD);
            case "STY-CALM", "STY-WARM" -> Optional.of(CALM);
            case "STY-SERIOUS" -> Optional.of(SERIOUS);
            case "STY-DRAMATIC" -> Optional.of(DRAMATIC);
            default -> Optional.empty();
        };
    }

    public boolean isBasic() {
        return category == VoiceReferenceToneCategory.BASIC;
    }

    public boolean isTheatricalExtended() {
        return category == VoiceReferenceToneCategory.THEATRICAL_EXTENDED;
    }

    public static List<VoiceReferenceTone> basicTones() {
        return Arrays.stream(values()).filter(VoiceReferenceTone::isBasic).toList();
    }

    public static List<VoiceReferenceTone> theatricalExtendedTones() {
        return Arrays.stream(values()).filter(VoiceReferenceTone::isTheatricalExtended).toList();
    }
}
