package com.marcosmoreiradev.docupodcaststudio.application.batch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipFile;

/** Finds DOCX/PDF inputs without following links, junctions or copying unrelated files. */
public final class DiscoverDocumentVideoBatchSourcesUseCase {
    public BatchSourceInventory discover(Path sourceRoot) throws IOException {
        Path root = sourceRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root) || Files.isSymbolicLink(root)) {
            throw new IOException("La carpeta de documentos no existe o es un enlace: " + root);
        }
        List<BatchSourceCandidate> documents = new ArrayList<>();
        AtomicInteger ignored = new AtomicInteger();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(root) && (Files.isSymbolicLink(dir) || attrs.isOther())) {
                    ignored.incrementAndGet();
                    return FileVisitResult.SKIP_SUBTREE;
                }
                ensureInside(root, dir);
                return FileVisitResult.CONTINUE;
            }

            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                ensureInside(root, file);
                if (!attrs.isRegularFile() || Files.isSymbolicLink(file)) {
                    ignored.incrementAndGet();
                    return FileVisitResult.CONTINUE;
                }
                String name = file.getFileName().toString();
                String lower = name.toLowerCase(Locale.ROOT);
                if (name.startsWith("~$") || !(lower.endsWith(".docx") || lower.endsWith(".pdf"))) {
                    ignored.incrementAndGet();
                    return FileVisitResult.CONTINUE;
                }
                Path relative = root.relativize(file.toAbsolutePath().normalize());
                String format = lower.endsWith(".docx") ? "DOCX" : "PDF";
                documents.add(new BatchSourceCandidate(file.toAbsolutePath().normalize(), portable(relative), format,
                        sha256(file), attrs.size(), "DOCX".equals(format) ? countDocxMedia(file) : 0));
                return FileVisitResult.CONTINUE;
            }
        });
        documents.sort(Comparator.comparing(BatchSourceCandidate::relativePath, this::naturalCompare));
        return new BatchSourceInventory(root, documents, ignored.get(),
                documents.stream().mapToLong(BatchSourceCandidate::bytes).sum(),
                documents.stream().mapToInt(BatchSourceCandidate::embeddedMediaCount).sum());
    }

    private static void ensureInside(Path root, Path candidate) throws IOException {
        if (!candidate.toAbsolutePath().normalize().startsWith(root)) {
            throw new IOException("Ruta fuera de la carpeta seleccionada: " + candidate);
        }
    }

    private static String portable(Path path) { return path.toString().replace('\\', '/'); }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[64 * 1024];
                for (int read; (read = input.read(buffer)) >= 0;) if (read > 0) digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static int countDocxMedia(Path file) {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            return (int) zip.stream().filter(entry -> !entry.isDirectory() && entry.getName().startsWith("word/media/"))
                    .count();
        } catch (IOException invalidPackage) {
            return 0;
        }
    }

    private int naturalCompare(String left, String right) {
        int a = 0, b = 0;
        while (a < left.length() && b < right.length()) {
            char ca = Character.toLowerCase(left.charAt(a));
            char cb = Character.toLowerCase(right.charAt(b));
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                int a0 = a, b0 = b;
                while (a < left.length() && Character.isDigit(left.charAt(a))) a++;
                while (b < right.length() && Character.isDigit(right.charAt(b))) b++;
                String an = left.substring(a0, a).replaceFirst("^0+(?!$)", "");
                String bn = right.substring(b0, b).replaceFirst("^0+(?!$)", "");
                int cmp = Integer.compare(an.length(), bn.length());
                if (cmp == 0) cmp = an.compareTo(bn);
                if (cmp != 0) return cmp;
            } else {
                if (ca != cb) return Character.compare(ca, cb);
                a++; b++;
            }
        }
        return Integer.compare(left.length(), right.length());
    }
}
