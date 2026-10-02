package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import java.text.Normalizer;
import java.util.regex.Pattern;

/** Normalizes segment text before sending it to external TTS engines. */
public final class TtsTextPreprocessor {
    private static final Pattern CODE_FENCE = Pattern.compile("(?m)^\\s*```.*$");
    private static final Pattern MARKDOWN_IMAGE = Pattern.compile("!\\[([^\\]]*)]\\([^)]*\\)");
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[([^\\]]+)]\\([^)]*\\)");
    private static final Pattern URL = Pattern.compile("https?://\\S+|www\\.\\S+");
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");
    private static final Pattern HEADING_OR_QUOTE = Pattern.compile("(?m)^\\s{0,4}(#{1,6}|>|[-*+]\\s+|\\d+\\.\\s+)");
    private static final Pattern INLINE_MARKUP = Pattern.compile("[`*_~]{1,3}");
    private static final Pattern WHITESPACE = Pattern.compile("[ \\t\\x0B\\f\\r]+");
    private static final int MAX_CHARS = 8_000;

    private TtsTextPreprocessor() {
    }

    public static String sanitize(String raw) {
        String text = raw == null ? "" : raw;
        text = Normalizer.normalize(text, Normalizer.Form.NFKC);
        text = text.replace('\u00A0', ' ');
        text = CODE_FENCE.matcher(text).replaceAll(" ");
        text = MARKDOWN_IMAGE.matcher(text).replaceAll("$1");
        text = MARKDOWN_LINK.matcher(text).replaceAll("$1");
        text = URL.matcher(text).replaceAll(" ");
        text = HEADING_OR_QUOTE.matcher(text).replaceAll("");
        text = INLINE_MARKUP.matcher(text).replaceAll("");
        text = CONTROL.matcher(text).replaceAll(" ");
        text = stripUnsupportedSurrogates(text);
        text = WHITESPACE.matcher(text).replaceAll(" ");
        text = text.replaceAll("\\n{3,}", "\\n\\n");
        text = text.strip();
        if (text.length() > MAX_CHARS) {
            text = text.substring(0, MAX_CHARS).strip() + "...";
        }
        return text;
    }
    public static boolean changed(String original, String sanitized) {
        String raw = original == null ? "" : original.strip();
        String clean = sanitized == null ? "" : sanitized.strip();
        return !raw.equals(clean);
    }

    private static String stripUnsupportedSurrogates(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (i + 1 < text.length() && Character.isLowSurrogate(text.charAt(i + 1))) {
                    int codePoint = Character.toCodePoint(c, text.charAt(i + 1));
                    if (!isEmojiLike(codePoint)) {
                        builder.appendCodePoint(codePoint);
                    } else {
                        builder.append(' ');
                    }
                    i++;
                } else {
                    builder.append(' ');
                }
            } else if (Character.isLowSurrogate(c)) {
                builder.append(' ');
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private static boolean isEmojiLike(int codePoint) {
        return (codePoint >= 0x1F000 && codePoint <= 0x1FAFF) || (codePoint >= 0x2600 && codePoint <= 0x27BF);
    }
}
