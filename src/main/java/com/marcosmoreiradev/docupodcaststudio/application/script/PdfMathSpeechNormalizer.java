package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts the small mathematical notation admitted in PDF SOURCE into TTS-safe
 * Spanish. It is deliberately not a TeX parser: unsupported expressions remain
 * visible in SOURCE and are announced conservatively instead of being guessed.
 */
public final class PdfMathSpeechNormalizer {
    public static final String CONVENTION_EXPLICIT = "LATEX_TECHNICAL_V1";
    public static final String CONVENTION_LEGACY_VLM = "LEGACY_VLM_LATEX_V1";
    public static final String CONVENTION_LITERAL = "LITERAL_TEXT_V1";

    private static final Pattern INLINE_LATEX = Pattern.compile("(?<!\\$)\\$([^$\\r\\n]+)\\$(?!\\$)");
    private static final Pattern MONEY = Pattern.compile(
            "(?i)(?:US\\$|USD\\s*\\$?|\\$)\\s*\\d+(?:[.,]\\d{1,2})?");
    private static final Pattern STRONG_MATH = Pattern.compile(
            "(?i)(?:\\\\(?:frac|sqrt|sin|cos|tan|lim|pi|approx|to)|[π≤≥≈→]|"
                    + "(?:sin|cos|tan|lim)\\s*\\(?[^.!?;]{0,60}(?:[/=<>]))");
    private static final Pattern INFIX_MATH = Pattern.compile(
            "(?i)(?:(?:sin|cos|tan|lim|π|\\b[xyz]\\b).*[=<>]"
                    + "|[=<>].*(?:sin|cos|tan|lim|π|\\b[xyz]\\b))");
    private static final Pattern SIMPLE_POWER = Pattern.compile("([A-Za-z0-9π)]+)\\s*\\^\\s*\\{?([A-Za-z0-9]+)}?");
    private static final Pattern SIMPLE_FRACTION = Pattern.compile(
            "(?<![\\p{L}\\d])([A-Za-z0-9π().+-]+)\\s*/\\s*([A-Za-z0-9π().+-]+)");
    private static final Pattern TEX_FRACTION = Pattern.compile(
            "\\\\frac\\s*\\{([^{}]+)}\\s*\\{([^{}]+)}");
    private static final Pattern TEX_SQRT = Pattern.compile("\\\\sqrt\\s*\\{([^{}]+)}");

    public String normalize(PdfRegion region, String raw) {
        String text = raw == null ? "" : raw;
        String convention = convention(region, text);
        if (CONVENTION_LITERAL.equals(convention)) return text.strip();

        Matcher inline = INLINE_LATEX.matcher(text);
        StringBuffer result = new StringBuffer();
        while (inline.find()) {
            inline.appendReplacement(result, Matcher.quoteReplacement(
                    speakExpression(inline.group(1))));
        }
        inline.appendTail(result);
        String value = result.toString();
        String[] lines = value.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            if (STRONG_MATH.matcher(lines[index]).find()
                    || region != null && region.effectiveType() == PdfRegionType.MATH) {
                lines[index] = speakExpression(lines[index]);
            }
        }
        return String.join("\n", lines).replaceAll("[ \\t]+", " ")
                .replaceAll(" *\\n *", "\n").strip();
    }

    /** Normalizes an already-designated SPEECH layer without guessing its provenance. */
    public String normalizeSpeech(PdfRegion region, String raw) {
        String text = raw == null ? "" : raw.strip();
        if (text.isBlank()) return "";
        if (INLINE_LATEX.matcher(text).find()
                || STRONG_MATH.matcher(text).find()
                || INFIX_MATH.matcher(text).find()
                || text.matches("(?s).*\\d\\s*/\\s*\\d.*")
                || text.indexOf('½') >= 0
                || region != null && region.effectiveType() == PdfRegionType.MATH) {
            return speakExpression(text);
        }
        return text;
    }

    public boolean safeForTts(String speech) {
        String value = speech == null ? "" : speech;
        return !value.isBlank() && !value.matches("(?s).*[\\\\${}|].*")
                && !value.contains("?");
    }

    public String convention(PdfRegion region, String text) {
        if (region != null) {
            String explicit = region.attributes().getOrDefault(
                    "mathSourceConvention", "").strip();
            if (!explicit.isBlank()) return explicit;
            if (region.effectiveType() == PdfRegionType.MATH) {
                return CONVENTION_EXPLICIT;
            }
            if (region.evidence().origin() == PdfRegionOrigin.VLM_SEMANTIC
                    && hasTechnicalMathMarkup(text)) {
                return CONVENTION_LEGACY_VLM;
            }
        }
        return CONVENTION_LITERAL;
    }

    public String speakExpression(String expression) {
        String value = expression == null ? "" : expression.strip();
        if (value.isBlank()) return "";
        if (MONEY.matcher(value).matches()) return value;

        value = stripMathDelimiters(value);
        value = value.replaceAll("(?i)\\blim\\?+", "límite ")
                .replaceAll("limₓ\\s*→\\s*₀⁺", "límite cuando x tiende a 0 por la derecha")
                .replaceAll("limₓ\\s*→\\s*₀⁻", "límite cuando x tiende a 0 por la izquierda")
                .replaceAll("limₓ\\s*→\\s*₀", "límite cuando x tiende a 0")
                .replace("ₓ", " x ").replace("₀", " 0 ")
                .replace("⁺", " por la derecha ")
                .replace("⁻", " por la izquierda ")
                .replace("²", " al cuadrado ")
                .replace("³", " al cubo ");
        Matcher fraction = TEX_FRACTION.matcher(value);
        while (fraction.find()) {
            value = fraction.replaceFirst(Matcher.quoteReplacement(
                    fractionSpeech(fraction.group(1), fraction.group(2))));
            fraction = TEX_FRACTION.matcher(value);
        }
        Matcher root = TEX_SQRT.matcher(value);
        while (root.find()) {
            value = root.replaceFirst(Matcher.quoteReplacement(
                    "raíz cuadrada de " + speakExpression(root.group(1))));
            root = TEX_SQRT.matcher(value);
        }
        value = value.replaceAll("\\\\lim\\s*_\\s*\\{([^{}]+)}",
                        "límite cuando $1")
                .replace("\\sin", "sin").replace("\\cos", "cos")
                .replace("\\tan", "tan").replace("\\pi", "π")
                .replace("\\to", "→").replace("\\approx", "≈")
                .replace("\\cdot", " por ").replace("\\times", " por ")
                .replace("\\left", "").replace("\\right", "")
                .replace("\\,", " ").replace("\\;", " ");

        Matcher power = SIMPLE_POWER.matcher(value);
        StringBuffer powers = new StringBuffer();
        while (power.find()) {
            String exponent = power.group(2);
            String spoken = speakAtom(power.group(1)) + switch (exponent) {
                case "2" -> " al cuadrado";
                case "3" -> " al cubo";
                default -> " elevado a " + speakAtom(exponent);
            };
            power.appendReplacement(powers, Matcher.quoteReplacement(spoken));
        }
        power.appendTail(powers);
        value = powers.toString();

        Matcher simpleFraction = SIMPLE_FRACTION.matcher(value);
        StringBuffer fractions = new StringBuffer();
        while (simpleFraction.find()) {
            simpleFraction.appendReplacement(fractions, Matcher.quoteReplacement(
                    " " + fractionSpeech(simpleFraction.group(1),
                            simpleFraction.group(2)) + " "));
        }
        simpleFraction.appendTail(fractions);
        value = fractions.toString();
        value = value.replaceAll("(?i)\\bun medio\\s+(?=(?:sin|cos|tan)\\b|[xyz]\\b)",
                "un medio por ");

        value = value.replace("½", " un medio por ")
                .replace("≤", " menor o igual que ")
                .replace("≥", " mayor o igual que ")
                .replace("≠", " distinto de ")
                .replace("≈", " aproximadamente igual a ")
                .replace("→", " tiende a ")
                .replace("<", " menor que ")
                .replace(">", " mayor que ")
                .replace("=", " igual a ")
                .replace("+", " más ")
                .replace("−", " menos ")
                .replace("-", " menos ")
                .replace("×", " por ")
                .replace("·", " por ")
                .replaceAll("(?i)\\bsin\\s*(?:de\\s*)?\\(?\\s*([A-Za-z0-9π.]+)\\s*\\)?",
                        "seno de $1 ")
                .replaceAll("(?i)\\bcos\\s*(?:de\\s*)?\\(?\\s*([A-Za-z0-9π.]+)\\s*\\)?",
                        "coseno de $1 ")
                .replaceAll("(?i)\\btan\\s*(?:de\\s*)?\\(?\\s*([A-Za-z0-9π.]+)\\s*\\)?",
                        "tangente de $1 ")
                .replace("π", " pi ")
                .replaceAll("[{}$]", " ");
        value = replaceStandaloneVariables(value);
        value = replaceStandaloneNumbers(value);
        return value.replaceAll("\\s+", " ").strip();
    }

    private String fractionSpeech(String numerator, String denominator) {
        String left = speakExpression(numerator);
        String right = speakExpression(denominator);
        if ((left.equals("uno") || left.equals("1"))
                && (right.equals("dos") || right.equals("2"))) return "un medio";
        if ((right.equals("dos") || right.equals("2")) && left.equals("pi")) {
            return "pi sobre dos";
        }
        return left + " dividido para " + right;
    }

    private static String replaceStandaloneNumbers(String value) {
        return value.replaceAll("(?<!\\d)0(?!\\d)", "cero")
                .replaceAll("(?<!\\d)1(?!\\d)", "uno")
                .replaceAll("(?<!\\d)2(?!\\d)", "dos")
                .replaceAll("(?<!\\d)3(?!\\d)", "tres");
    }

    private static String replaceStandaloneVariables(String value) {
        return value.replaceAll("(?i)(?<![\\p{L}])x(?![\\p{L}])", "equis")
                .replaceAll("(?i)(?<![\\p{L}])y(?=\\s*[=<>])", "ye")
                .replaceAll("(?i)(?<=[=<>]\\s)y(?![\\p{L}])", "ye")
                .replaceAll("(?i)(?<![\\p{L}])z(?![\\p{L}])", "zeta");
    }

    private static String speakAtom(String value) {
        String atom = value == null ? "" : value.strip();
        if (atom.equals("1")) return "uno";
        if (atom.equals("2")) return "dos";
        if (atom.equals("3")) return "tres";
        if (atom.equalsIgnoreCase("x")) return "equis";
        if (atom.equalsIgnoreCase("y")) return "ye";
        if (atom.equals("π") || atom.equalsIgnoreCase("pi")) return "pi";
        return atom;
    }

    private static boolean hasTechnicalMathMarkup(String text) {
        String value = text == null ? "" : text;
        return INLINE_LATEX.matcher(value).find()
                || STRONG_MATH.matcher(value).find();
    }

    private static String stripMathDelimiters(String value) {
        String result = value.strip();
        if (result.startsWith("$$") && result.endsWith("$$") && result.length() > 4) {
            return result.substring(2, result.length() - 2).strip();
        }
        if (result.startsWith("$") && result.endsWith("$") && result.length() > 2) {
            return result.substring(1, result.length() - 1).strip();
        }
        return result.replace("\\(", "").replace("\\)", "")
                .replace("\\[", "").replace("\\]", "");
    }
}
