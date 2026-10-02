package com.marcosmoreiradev.docupodcaststudio.domain.document;

import java.util.ArrayList;
import java.util.List;

/**
 * Small deterministic sentence splitter for the document workspace. It is deliberately
 * conservative: the source text stays untouched, and spans point back to character offsets
 * in the imported block so project layers can be anchored without writing into Word.
 */
public final class DocumentSentenceSplitter {
    private DocumentSentenceSplitter() {
    }

    public static List<DocumentSentenceSpan> split(DocumentBlock block) {
        if (block == null) {
            return List.of();
        }
        return split(block.id(), block.text());
    }

    public static List<DocumentSentenceSpan> split(String blockId, String text) {
        String normalizedBlockId = blockId == null ? "" : blockId.strip();
        if (normalizedBlockId.isBlank()) {
            return List.of();
        }
        String source = text == null ? "" : text;
        if (source.isBlank()) {
            return List.of();
        }
        List<DocumentSentenceSpan> spans = new ArrayList<>();
        int start = firstNonWhitespace(source, 0);
        int index = 0;
        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            if (isSentenceTerminator(current) && !isDecimalPoint(source, i)) {
                int end = includeSentenceEnding(source, i + 1);
                index = addSpan(spans, normalizedBlockId, index, source, start, end);
                start = firstNonWhitespace(source, end);
                i = Math.max(i, start - 1);
            }
        }
        if (start < source.length()) {
            addSpan(spans, normalizedBlockId, index, source, start, source.length());
        }
        if (spans.isEmpty()) {
            addSpan(spans, normalizedBlockId, 0, source, 0, source.length());
        }
        return List.copyOf(spans);
    }

    private static int addSpan(List<DocumentSentenceSpan> spans, String blockId, int index, String source, int start, int end) {
        int trimmedStart = firstNonWhitespace(source, start);
        int trimmedEnd = lastNonWhitespace(source, end);
        if (trimmedStart >= trimmedEnd) {
            return index;
        }
        String sentence = source.substring(trimmedStart, trimmedEnd);
        if (!containsSpeakableContent(sentence)) {
            return index;
        }
        spans.add(new DocumentSentenceSpan(
                blockId + ":S" + String.format("%03d", index + 1),
                index,
                new DocumentTextRange(blockId, trimmedStart, trimmedEnd),
                sentence));
        return index + 1;
    }

    private static boolean isSentenceTerminator(char value) {
        return value == '.' || value == '!' || value == '?' || value == '…';
    }

    private static int includeSentenceEnding(String source, int end) {
        int cursor = end;
        while (cursor < source.length()) {
            char value = source.charAt(cursor);
            if (isSentenceTerminator(value) || value == '"' || value == '\''
                    || value == ')' || value == ']' || value == '»' || value == '”') {
                cursor++;
            } else {
                break;
            }
        }
        return cursor;
    }

    private static boolean isDecimalPoint(String source, int index) {
        return source.charAt(index) == '.' && index > 0 && index + 1 < source.length()
                && Character.isDigit(source.charAt(index - 1))
                && Character.isDigit(source.charAt(index + 1));
    }

    private static boolean containsSpeakableContent(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isWhitespace(current)) {
                continue;
            }
            int type = Character.getType(current);
            if (type != Character.CONNECTOR_PUNCTUATION
                    && type != Character.DASH_PUNCTUATION
                    && type != Character.START_PUNCTUATION
                    && type != Character.END_PUNCTUATION
                    && type != Character.INITIAL_QUOTE_PUNCTUATION
                    && type != Character.FINAL_QUOTE_PUNCTUATION
                    && type != Character.OTHER_PUNCTUATION) {
                return true;
            }
        }
        return false;
    }

    private static int firstNonWhitespace(String source, int start) {
        int cursor = Math.max(0, start);
        while (cursor < source.length() && Character.isWhitespace(source.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private static int lastNonWhitespace(String source, int endExclusive) {
        int cursor = Math.min(source.length(), Math.max(0, endExclusive));
        while (cursor > 0 && Character.isWhitespace(source.charAt(cursor - 1))) {
            cursor--;
        }
        return cursor;
    }
}
