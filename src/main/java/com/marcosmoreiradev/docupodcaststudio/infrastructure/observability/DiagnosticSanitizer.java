package com.marcosmoreiradev.docupodcaststudio.infrastructure.observability;

import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

/** Redacts credentials, URL query strings and personal absolute paths. */
public final class DiagnosticSanitizer {
    private static final Pattern SECRET = Pattern.compile(
            "(?i)(authorization|api[-_]?key|token|password|secret)\\s*[:=]\\s*([^\\s,;]+)");
    private static final Pattern URL_QUERY = Pattern.compile("(https?://[^\\s?]+)\\?[^\\s]*");
    private static final Pattern CONTENT = Pattern.compile(
            "(?i)(prompt|documentText|sourceText|transcript)\\s*[:=]\\s*(\"[^\"]*\"|[^\\r\\n,;]+)");
    private final String userHome;
    private final String projectRoot;

    public DiagnosticSanitizer(Path projectRoot) {
        this.userHome = normalized(Path.of(System.getProperty("user.home", ".")));
        this.projectRoot = projectRoot == null ? "" : normalized(projectRoot);
    }

    public String sanitize(String value) {
        String result = Objects.toString(value, "");
        result = SECRET.matcher(result).replaceAll("$1=<redacted>");
        result = URL_QUERY.matcher(result).replaceAll("$1?<redacted>");
        result = CONTENT.matcher(result).replaceAll("$1=<redacted>");
        if (!projectRoot.isBlank()) result = replacePath(result, projectRoot, "<project>");
        if (!userHome.isBlank()) result = replacePath(result, userHome, "<user-home>");
        return result;
    }

    private static String replacePath(String source, String normalized, String replacement) {
        String result = replaceIgnoreCase(source, normalized, replacement);
        return replaceIgnoreCase(result, normalized.replace('/', '\\'), replacement);
    }

    private static String replaceIgnoreCase(String source, String target, String replacement) {
        return Pattern.compile(Pattern.quote(target), Pattern.CASE_INSENSITIVE)
                .matcher(source).replaceAll(java.util.regex.Matcher.quoteReplacement(replacement));
    }

    private static String normalized(Path path) {
        return path.toAbsolutePath().normalize().toString().replace('\\', '/');
    }
}
