package com.marcosmoreiradev.docupodcaststudio.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prevents invalid UTF-8, mojibake and implicit text encodings in application sources. */
final class Utf8TextIntegrityTest {
    private static final List<Path> TEXT_ROOTS = List.of(
            Path.of("src/main"),
            Path.of("src/test"),
            Path.of("studio-ink/src"),
            Path.of("studio-launcher/src"),
            Path.of("studio-local-media-adapters/src"),
            Path.of("studio-media-api/src"));
    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            ".java", ".css", ".fxml", ".properties", ".xml", ".json", ".md", ".txt");
    private static final List<String> MOJIBAKE_MARKERS = List.of(
            "\u00c3", "\u00c2", "\u00e2\u20ac", "\ufffd");
    private static final Pattern IMPLICIT_READ_STRING =
            Pattern.compile("\\bFiles\\.readString\\s*\\(\\s*[^,()]+\\s*\\)");
    private static final Pattern IMPLICIT_WRITE_STRING =
            Pattern.compile("\\bFiles\\.writeString\\s*\\(\\s*[^,()]+,\\s*[^,()]+\\s*\\)");
    private static final Pattern IMPLICIT_READER_WRITER =
            Pattern.compile("\\bnew\\s+(?:FileReader|FileWriter|InputStreamReader|OutputStreamWriter)\\s*"
                    + "\\([^,)]*\\)");

    @Test
    void applicationTextIsStrictUtf8WithoutMojibake() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : textFiles()) {
            try {
                String text = decodeStrict(file);
                for (String marker : MOJIBAKE_MARKERS) {
                    if (text.contains(marker)) {
                        violations.add(portable(file) + " contains mojibake marker " + printable(marker));
                    }
                }
            } catch (CharacterCodingException ex) {
                violations.add(portable(file) + " is not valid UTF-8");
            }
        }
        assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
    }

    @Test
    void applicationTextIoDeclaresItsCharset() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : mainJavaFiles()) {
            String text = decodeStrict(file);
            if (IMPLICIT_READ_STRING.matcher(text).find()) {
                violations.add(portable(file) + " uses Files.readString without an explicit charset");
            }
            if (IMPLICIT_WRITE_STRING.matcher(text).find()) {
                violations.add(portable(file) + " uses Files.writeString without an explicit charset");
            }
            if (IMPLICIT_READER_WRITER.matcher(text).find()) {
                violations.add(portable(file) + " constructs a text reader/writer without an explicit charset");
            }
        }
        assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
    }

    private static List<Path> textFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        for (Path root : TEXT_ROOTS) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .filter(Utf8TextIntegrityTest::isTextFile)
                        .forEach(files::add);
            }
        }
        return files;
    }

    private static List<Path> mainJavaFiles() throws IOException {
        return textFiles().stream()
                .filter(path -> portable(path).contains("/src/main/"))
                .filter(path -> path.toString().endsWith(".java"))
                .toList();
    }

    private static boolean isTextFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return TEXT_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private static String decodeStrict(Path file) throws IOException {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(Files.readAllBytes(file)))
                .toString();
    }

    private static String portable(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static String printable(String value) {
        return value.codePoints()
                .mapToObj(codePoint -> "U+" + String.format("%04X", codePoint))
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
