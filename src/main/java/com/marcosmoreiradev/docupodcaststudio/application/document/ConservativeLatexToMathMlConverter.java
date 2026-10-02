package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deterministic LaTeX subset used before MathCAT. Unsupported constructs are
 * rejected rather than guessed, so a complex formula stays reviewable.
 */
public final class ConservativeLatexToMathMlConverter {
    private static final Map<String, String> IDENTIFIERS = Map.ofEntries(
            Map.entry("alpha", "α"), Map.entry("beta", "β"), Map.entry("gamma", "γ"),
            Map.entry("delta", "δ"), Map.entry("epsilon", "ε"), Map.entry("theta", "θ"),
            Map.entry("lambda", "λ"), Map.entry("mu", "μ"), Map.entry("pi", "π"),
            Map.entry("rho", "ρ"), Map.entry("sigma", "σ"), Map.entry("tau", "τ"),
            Map.entry("phi", "φ"), Map.entry("omega", "ω"),
            Map.entry("Gamma", "Γ"), Map.entry("Delta", "Δ"), Map.entry("Theta", "Θ"),
            Map.entry("Lambda", "Λ"), Map.entry("Sigma", "Σ"), Map.entry("Phi", "Φ"),
            Map.entry("Omega", "Ω"), Map.entry("infty", "∞"));
    private static final Map<String, String> OPERATORS = Map.ofEntries(
            Map.entry("cdot", "·"), Map.entry("times", "×"), Map.entry("div", "÷"),
            Map.entry("pm", "±"), Map.entry("mp", "∓"), Map.entry("le", "≤"),
            Map.entry("leq", "≤"), Map.entry("ge", "≥"), Map.entry("geq", "≥"),
            Map.entry("neq", "≠"), Map.entry("approx", "≈"), Map.entry("to", "→"),
            Map.entry("rightarrow", "→"), Map.entry("leftarrow", "←"),
            Map.entry("sum", "∑"), Map.entry("prod", "∏"), Map.entry("int", "∫"),
            Map.entry("partial", "∂"), Map.entry("nabla", "∇"), Map.entry("in", "∈"));

    public String convert(String latex) {
        String source = normalize(latex);
        if (source.isBlank()) throw new IllegalArgumentException("La fórmula LaTeX está vacía.");
        Parser parser = new Parser(source);
        String body = parser.sequence('\0');
        parser.skipWhitespace();
        if (!parser.finished()) {
            throw new IllegalArgumentException("La fórmula contiene sintaxis no admitida cerca de "
                    + parser.preview() + ".");
        }
        return "<math xmlns=\"http://www.w3.org/1998/Math/MathML\"><mrow>"
                + body + "</mrow></math>";
    }

    private static String normalize(String value) {
        String source = value == null ? "" : value.strip();
        if (source.startsWith("$$") && source.endsWith("$$") && source.length() >= 4) {
            return source.substring(2, source.length() - 2).strip();
        }
        if (source.startsWith("$") && source.endsWith("$") && source.length() >= 2) {
            return source.substring(1, source.length() - 1).strip();
        }
        if (source.startsWith("\\[") && source.endsWith("\\]")) {
            return source.substring(2, source.length() - 2).strip();
        }
        if (source.startsWith("\\(") && source.endsWith("\\)")) {
            return source.substring(2, source.length() - 2).strip();
        }
        return source;
    }

    private static final class Parser {
        private final String source;
        private int cursor;

        private Parser(String source) {
            this.source = source;
        }

        private String sequence(char stop) {
            ArrayList<String> nodes = new ArrayList<>();
            while (!finished()) {
                skipWhitespace();
                if (finished() || stop != '\0' && source.charAt(cursor) == stop) break;
                if (source.charAt(cursor) == '}') {
                    throw new IllegalArgumentException("Llave de cierre sin apertura.");
                }
                String base = atom();
                String sub = null;
                String sup = null;
                while (!finished()) {
                    skipWhitespace();
                    if (finished()) break;
                    char marker = source.charAt(cursor);
                    if (marker != '_' && marker != '^') break;
                    cursor++;
                    String script = script();
                    if (marker == '_') sub = script;
                    else sup = script;
                }
                if (sub != null && sup != null) {
                    base = "<msubsup>" + base + wrapRow(sub) + wrapRow(sup) + "</msubsup>";
                } else if (sub != null) {
                    base = "<msub>" + base + wrapRow(sub) + "</msub>";
                } else if (sup != null) {
                    base = "<msup>" + base + wrapRow(sup) + "</msup>";
                }
                nodes.add(base);
            }
            if (stop != '\0') {
                if (finished() || source.charAt(cursor) != stop) {
                    throw new IllegalArgumentException("Falta una llave de cierre.");
                }
                cursor++;
            }
            return String.join("", nodes);
        }

        private String atom() {
            char current = source.charAt(cursor);
            if (current == '{') {
                cursor++;
                return wrapRow(sequence('}'));
            }
            if (current == '\\') return command();
            if (Character.isDigit(current) || current == '.') return number();
            if (Character.isLetter(current)) return identifier();
            cursor++;
            if ("+-=<>(),[]|:;".indexOf(current) >= 0) {
                return "<mo>" + xml(Character.toString(current)) + "</mo>";
            }
            throw new IllegalArgumentException("Símbolo LaTeX no admitido: " + current);
        }

        private String command() {
            cursor++;
            int start = cursor;
            while (!finished() && Character.isLetter(source.charAt(cursor))) cursor++;
            String name;
            if (start == cursor && !finished()) {
                name = Character.toString(source.charAt(cursor++));
            } else {
                name = source.substring(start, cursor);
            }
            if (name.equals("left") || name.equals("right")) {
                skipWhitespace();
                if (finished()) return "";
                char delimiter = source.charAt(cursor++);
                return delimiter == '.' ? "" : "<mo>" + xml(Character.toString(delimiter)) + "</mo>";
            }
            if (name.equals(",") || name.equals(";") || name.equals("!") || name.equals(" ")) return "";
            if (name.equals("frac")) {
                return "<mfrac>" + wrapRow(requiredGroup()) + wrapRow(requiredGroup()) + "</mfrac>";
            }
            if (name.equals("sqrt")) {
                return "<msqrt>" + requiredGroup() + "</msqrt>";
            }
            if (name.equals("text") || name.equals("operatorname")) {
                return "<mtext>" + xml(requiredGroupText()) + "</mtext>";
            }
            if (name.equals("mathrm") || name.equals("mathbf") || name.equals("mathit")) {
                return wrapRow(requiredGroup());
            }
            String identifier = IDENTIFIERS.get(name);
            if (identifier != null) return "<mi>" + identifier + "</mi>";
            String operator = OPERATORS.get(name);
            if (operator != null) return "<mo>" + operator + "</mo>";
            throw new IllegalArgumentException("Comando LaTeX no admitido: \\" + name);
        }

        private String requiredGroup() {
            skipWhitespace();
            if (finished() || source.charAt(cursor) != '{') {
                throw new IllegalArgumentException("Se esperaba un grupo entre llaves.");
            }
            cursor++;
            return sequence('}');
        }

        private String requiredGroupText() {
            skipWhitespace();
            if (finished() || source.charAt(cursor) != '{') {
                throw new IllegalArgumentException("Se esperaba texto entre llaves.");
            }
            cursor++;
            int start = cursor;
            int depth = 1;
            while (!finished() && depth > 0) {
                char value = source.charAt(cursor++);
                if (value == '{') depth++;
                else if (value == '}') depth--;
            }
            if (depth != 0) throw new IllegalArgumentException("Falta una llave de cierre.");
            return source.substring(start, cursor - 1);
        }

        private String script() {
            skipWhitespace();
            if (finished()) throw new IllegalArgumentException("Subíndice o exponente incompleto.");
            if (source.charAt(cursor) == '{') {
                cursor++;
                return sequence('}');
            }
            return atom();
        }

        private String number() {
            int start = cursor;
            boolean decimal = false;
            while (!finished()) {
                char value = source.charAt(cursor);
                if (Character.isDigit(value)) cursor++;
                else if (value == '.' && !decimal) {
                    decimal = true;
                    cursor++;
                } else break;
            }
            return "<mn>" + xml(source.substring(start, cursor)) + "</mn>";
        }

        private String identifier() {
            int start = cursor;
            while (!finished() && Character.isLetter(source.charAt(cursor))) cursor++;
            return "<mi>" + xml(source.substring(start, cursor)) + "</mi>";
        }

        private void skipWhitespace() {
            while (!finished() && Character.isWhitespace(source.charAt(cursor))) cursor++;
        }

        private boolean finished() {
            return cursor >= source.length();
        }

        private String preview() {
            return source.substring(cursor, Math.min(source.length(), cursor + 16));
        }
    }

    private static String wrapRow(String value) {
        return "<mrow>" + value + "</mrow>";
    }

    private static String xml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
