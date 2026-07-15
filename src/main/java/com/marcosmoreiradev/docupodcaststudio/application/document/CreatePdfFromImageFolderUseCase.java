package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Creates a PDF source from a folder of image files ordered naturally by filename. */
public final class CreatePdfFromImageFolderUseCase {
    private final ImageFolderPdfBuilder builder;

    public CreatePdfFromImageFolderUseCase(ImageFolderPdfBuilder builder) {
        this.builder = Objects.requireNonNull(builder, "builder");
    }

    public Path create(Path sourceFolder, Path targetPdf) throws IOException {
        if (sourceFolder == null || !Files.isDirectory(sourceFolder)) {
            throw new IOException("Selecciona una carpeta valida con imagenes.");
        }
        if (targetPdf == null) {
            throw new IOException("No se encontro una ruta de salida para el PDF.");
        }
        List<Path> images = new ArrayList<>();
        try (var stream = Files.list(sourceFolder)) {
            stream.filter(Files::isRegularFile)
                    .filter(CreatePdfFromImageFolderUseCase::isSupportedImage)
                    .sorted(Comparator.comparing(path -> naturalKey(path.getFileName().toString())))
                    .forEach(images::add);
        }
        if (images.isEmpty()) {
            throw new IOException("La carpeta no contiene imagenes compatibles.");
        }
        Files.createDirectories(targetPdf.toAbsolutePath().normalize().getParent());
        return builder.build(List.copyOf(images), targetPdf.toAbsolutePath().normalize());
    }

    private static boolean isSupportedImage(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".png")
                || name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".webp")
                || name.endsWith(".bmp");
    }

    private static String naturalKey(String value) {
        StringBuilder key = new StringBuilder();
        String safe = value == null ? "" : value.toLowerCase(Locale.ROOT);
        int index = 0;
        while (index < safe.length()) {
            char c = safe.charAt(index);
            if (Character.isDigit(c)) {
                int start = index;
                while (index < safe.length() && Character.isDigit(safe.charAt(index))) {
                    index++;
                }
                String number = safe.substring(start, index).replaceFirst("^0+(?!$)", "");
                key.append(String.format(Locale.ROOT, "%06d", number.length())).append(number);
            } else {
                key.append(c);
                index++;
            }
        }
        return key.toString();
    }
}
