package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentOutlineHint;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentOutlineHintProvider;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentOutlineOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Best-effort PDF bookmark reader for simple, unencrypted outlines. */
public final class PdfBookmarkOutlineHintProvider implements DocumentOutlineHintProvider {
    private static final Pattern OBJECT_PATTERN = Pattern.compile("(?s)(\\d+)\\s+0\\s+obj\\s*<<(.*?)>>\\s*endobj");
    private static final Pattern PAGE_TYPE_PATTERN = Pattern.compile("/Type\\s*/Page\\b(?!s)");
    private static final Pattern TITLE_PATTERN = Pattern.compile("/Title\\s*\\(");
    private static final Pattern PARENT_PATTERN = Pattern.compile("/Parent\\s+(\\d+)\\s+0\\s+R");
    private static final Pattern DEST_PAGE_PATTERN = Pattern.compile("/(?:Dest|D)\\s*\\[\\s*(\\d+)\\s+0\\s+R");

    @Override
    public List<DocumentOutlineHint> hintsFor(ReadableDocument document) {
        if (document == null || document.format() != SourceDocumentFormat.PDF || document.sourcePath() == null) {
            return List.of();
        }
        try {
            String raw = Files.readString(document.sourcePath(), StandardCharsets.ISO_8859_1);
            if (!raw.startsWith("%PDF") || raw.contains("/Encrypt") || !raw.contains("/Outlines")) {
                return List.of();
            }
            return parse(raw);
        } catch (IOException | RuntimeException ignored) {
            return List.of();
        }
    }

    List<DocumentOutlineHint> parse(String raw) {
        Map<Integer, Integer> pageByObject = pageObjectMap(raw);
        if (pageByObject.isEmpty()) {
            return List.of();
        }
        Map<Integer, OutlineObject> outlineObjects = new LinkedHashMap<>();
        Matcher matcher = OBJECT_PATTERN.matcher(raw);
        while (matcher.find()) {
            int objectId = Integer.parseInt(matcher.group(1));
            String body = matcher.group(2);
            if (!TITLE_PATTERN.matcher(body).find()) {
                continue;
            }
            String title = parsePdfLiteralAfter(body, "/Title").strip();
            if (title.isBlank()) {
                continue;
            }
            OptionalInt pageObject = parseFirstInt(DEST_PAGE_PATTERN.matcher(body));
            if (pageObject.isEmpty()) {
                continue;
            }
            Integer sourcePage = pageByObject.get(pageObject.getAsInt());
            if (sourcePage == null) {
                continue;
            }
            int parentId = parseFirstInt(PARENT_PATTERN.matcher(body)).orElse(-1);
            outlineObjects.put(objectId, new OutlineObject(objectId, parentId, title, sourcePage));
        }
        if (outlineObjects.isEmpty()) {
            return List.of();
        }
        List<DocumentOutlineHint> hints = new ArrayList<>();
        for (OutlineObject object : outlineObjects.values()) {
            hints.add(new DocumentOutlineHint(
                    object.title(),
                    levelFor(object, outlineObjects),
                    Integer.toString(object.sourcePage()),
                    DocumentOutlineOrigin.PDF_BOOKMARKS));
        }
        return hints;
    }

    private static Map<Integer, Integer> pageObjectMap(String raw) {
        Map<Integer, Integer> pages = new HashMap<>();
        Matcher matcher = OBJECT_PATTERN.matcher(raw);
        int page = 1;
        while (matcher.find()) {
            int objectId = Integer.parseInt(matcher.group(1));
            String body = matcher.group(2);
            if (PAGE_TYPE_PATTERN.matcher(body).find()) {
                pages.put(objectId, page++);
            }
        }
        return pages;
    }

    private static int levelFor(OutlineObject object, Map<Integer, OutlineObject> objects) {
        int level = 1;
        int parentId = object.parentId();
        while (objects.containsKey(parentId) && level < 3) {
            level++;
            parentId = objects.get(parentId).parentId();
        }
        return level;
    }

    private static OptionalInt parseFirstInt(Matcher matcher) {
        if (!matcher.find()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(Integer.parseInt(matcher.group(1)));
    }

    private static String parsePdfLiteralAfter(String body, String key) {
        int keyIndex = body.indexOf(key);
        if (keyIndex < 0) {
            return "";
        }
        int start = body.indexOf('(', keyIndex + key.length());
        if (start < 0) {
            return "";
        }
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        int depth = 0;
        for (int i = start + 1; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (escaped) {
                value.append(ch);
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '(') {
                depth++;
                value.append(ch);
                continue;
            }
            if (ch == ')') {
                if (depth == 0) {
                    break;
                }
                depth--;
                value.append(ch);
                continue;
            }
            value.append(ch);
        }
        return value.toString().replaceAll("\\s+", " ");
    }

    private record OutlineObject(int objectId, int parentId, String title, int sourcePage) {
    }
}
