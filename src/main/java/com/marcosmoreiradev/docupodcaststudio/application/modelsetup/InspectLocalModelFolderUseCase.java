package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Performs the local, non-destructive verification step for imported model folders.
 *
 * <p>This use case is intentionally offline-first. Downloading from the internet must be handled by
 * a future manifest-driven adapter, while this use case lets the product verify a folder that the
 * user already downloaded, copied or imported manually.</p>
 */
public final class InspectLocalModelFolderUseCase {
    private static final int MAX_WALK_DEPTH = 4;
    private static final int MAX_DISCOVERED_FILES = 40;

    public ModelInspectionResult inspect(ModelFolderContract contract, Path folder) throws IOException {
        Objects.requireNonNull(contract, "contract");
        if (folder == null || !Files.isDirectory(folder)) {
            return new ModelInspectionResult(
                    contract.engineId(),
                    folder,
                    ModelInspectionStatus.MISSING_FOLDER,
                    List.of("Carpeta local no encontrada: " + contract.recommendedFolder()),
                    List.of(),
                    false,
                    "Selecciona o importa una carpeta local para " + contract.displayName() + "."
            );
        }

        List<Path> files;
        try (var stream = Files.walk(folder, MAX_WALK_DEPTH)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> folder.relativize(path).toString()))
                    .toList();
        }

        List<String> missing = contract.requirements().stream()
                .filter(requirement -> files.stream().noneMatch(requirement::matches))
                .map(requirement -> requirement.label() + " (" + requirement.displayMarkers() + ")")
                .toList();
        List<String> discovered = files.stream()
                .limit(MAX_DISCOVERED_FILES)
                .map(path -> folder.relativize(path).toString().replace('\\', '/'))
                .toList();
        boolean checksumPresent = files.stream().anyMatch(InspectLocalModelFolderUseCase::looksLikeChecksumManifest);

        if (!missing.isEmpty()) {
            return new ModelInspectionResult(
                    contract.engineId(),
                    folder,
                    ModelInspectionStatus.MISSING_REQUIRED_FILES,
                    missing,
                    discovered,
                    checksumPresent,
                    "Faltan archivos requeridos para habilitar " + contract.displayName() + "."
            );
        }

        if (!checksumPresent) {
            return new ModelInspectionResult(
                    contract.engineId(),
                    folder,
                    ModelInspectionStatus.CHECKSUM_NOT_PROVIDED,
                    List.of(),
                    discovered,
                    false,
                    "El modelo parece completo, pero no tiene manifiesto de checksum. Puedes probarlo, pero conviene verificarlo si el catálogo lo ofrece."
            );
        }

        return new ModelInspectionResult(
                contract.engineId(),
                folder,
                ModelInspectionStatus.READY,
                List.of(),
                discovered,
                true,
                "Modelo local listo y con manifiesto de verificación disponible."
        );
    }

    private static boolean looksLikeChecksumManifest(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.equals("sha256sums.txt")
                || name.equals("checksums.txt")
                || name.equals("model-manifest.json")
                || name.endsWith(".sha256");
    }
}
