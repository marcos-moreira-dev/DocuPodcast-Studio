package com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Resolves a folder or ZIP to the same validated package root. */
public final class TheatrePackageSource implements AutoCloseable {
    private final Path root;
    private final Path temporaryRoot;

    private TheatrePackageSource(Path root, Path temporaryRoot) { this.root = root; this.temporaryRoot = temporaryRoot; }
    public Path root() { return root; }

    public static TheatrePackageSource open(Path source) throws IOException {
        if (source == null) throw new IOException("La fuente del paquete teatral es obligatoria.");
        Path normalized = source.toAbsolutePath().normalize();
        if (Files.isDirectory(normalized)) return new TheatrePackageSource(normalized.toRealPath(), null);
        if (!Files.isRegularFile(normalized) || !normalized.getFileName().toString().toLowerCase().endsWith(".zip")) {
            throw new IOException("El paquete teatral debe ser una carpeta o un ZIP.");
        }
        Path temp = Files.createTempDirectory("docupodcast-theatre-" + UUID.randomUUID() + "-");
        Set<String> entries = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(normalized))) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                String raw = entry.getName().replace('\\', '/');
                if (raw.isBlank() || raw.startsWith("/") || raw.matches("^[A-Za-z]:.*")) throw new IOException("Ruta ZIP inválida: " + raw);
                Path target = temp.resolve(raw).normalize();
                if (!target.startsWith(temp)) throw new IOException("Traversal detectado en ZIP: " + raw);
                String key = target.toString().toLowerCase();
                if (!entries.add(key)) throw new IOException("Entrada ZIP duplicada: " + raw);
                if (entry.isDirectory()) Files.createDirectories(target);
                else { Files.createDirectories(target.getParent()); Files.copy(zip, target); }
                zip.closeEntry();
            }
        } catch (Exception ex) {
            deleteTree(temp);
            if (ex instanceof IOException io) throw io;
            throw new IOException("No se pudo extraer el paquete teatral.", ex);
        }
        Path root = locateRoot(temp);
        return new TheatrePackageSource(root, temp);
    }

    private static Path locateRoot(Path temp) throws IOException {
        if (Files.isRegularFile(temp.resolve(JsonTheatrePackageScanner.MANIFEST_FILE))) return temp;
        try (var children = Files.list(temp)) {
            var dirs = children.filter(Files::isDirectory).toList();
            if (dirs.size() == 1 && Files.isRegularFile(dirs.get(0).resolve(JsonTheatrePackageScanner.MANIFEST_FILE))) return dirs.get(0);
        }
        throw new IOException("El ZIP debe contener docupodcast-theatre.json en la raíz o en una única carpeta raíz.");
    }

    @Override public void close() throws IOException { if (temporaryRoot != null) deleteTree(temporaryRoot); }
    private static void deleteTree(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
