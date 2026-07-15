package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Built-in theatrical camera references bundled with the application. */
public final class TheatreBuiltInCameraCatalog {
    private static final String RESOURCE_ROOT = "/images/theatre/cameras/";
    private static final List<String> IDS = List.of(
            "CERCA_CENTRO_ALTO",
            "CERCA_CENTRO_BAJO",
            TheatreProjectLayer.DEFAULT_CAMERA_ID,
            "CERCA_DERECHA_ALTO",
            "CERCA_DERECHA_BAJO",
            "CERCA_DERECHA_NIVEL",
            "CERCA_IZQUIERDA_ALTO",
            "CERCA_IZQUIERDA_BAJO",
            "CERCA_IZQUIERDA_NIVEL",
            "PANORAMICA_CENTRO_ALTO",
            "PANORAMICA_CENTRO_BAJO",
            "PANORAMICA_CENTRO_NIVEL",
            "PANORAMICA_DERECHA_ALTO",
            "PANORAMICA_DERECHA_BAJO",
            "PANORAMICA_DERECHA_NIVEL",
            "PANORAMICA_IZQUIERDA_ALTO",
            "PANORAMICA_IZQUIERDA_BAJO",
            "PANORAMICA_IZQUIERDA_NIVEL"
    );

    private TheatreBuiltInCameraCatalog() {
    }

    public static List<TheatreProjectLayer.CameraReference> references() {
        return IDS.stream()
                .map(id -> new TheatreProjectLayer.CameraReference(
                        id,
                        displayName(id),
                        assetId(id),
                        distance(id),
                        orientation(id),
                        height(id),
                        TheatreProjectLayer.DEFAULT_CAMERA_ID.equals(id),
                        "Plano teatral embebido"))
                .toList();
    }

    public static Map<String, TheatreProjectLayer.CameraReference> referencesById() {
        LinkedHashMap<String, TheatreProjectLayer.CameraReference> result = new LinkedHashMap<>();
        for (TheatreProjectLayer.CameraReference reference : references()) {
            result.put(reference.id(), reference);
        }
        return result;
    }

    public static Optional<String> resourceUri(String cameraId) {
        return resourceUrl(cameraId).map(URL::toExternalForm);
    }

    public static Optional<Path> resourcePath(String cameraId) {
        Optional<URL> url = resourceUrl(cameraId);
        if (url.isEmpty()) {
            return Optional.empty();
        }
        if ("file".equalsIgnoreCase(url.get().getProtocol())) {
            try {
                return Optional.of(Path.of(url.get().toURI()).toAbsolutePath().normalize());
            } catch (URISyntaxException ex) {
                return Optional.empty();
            }
        }
        return materializeResource(cameraId, url.get());
    }

    public static String displayName(String id) {
        String normalized = normalizeId(id);
        return normalized.isBlank() ? "TIPO DE PLANO" : normalized.replace('_', ' ');
    }

    public static boolean contains(String cameraId) {
        return referencesById().containsKey(normalizeId(cameraId));
    }

    public static String assetId(String cameraId) {
        return "BUILTIN-CAMERA-" + normalizeId(cameraId);
    }

    private static Optional<URL> resourceUrl(String cameraId) {
        String id = normalizeId(cameraId);
        if (id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(TheatreBuiltInCameraCatalog.class.getResource(RESOURCE_ROOT + id + ".png"));
    }

    private static Optional<Path> materializeResource(String cameraId, URL url) {
        try (InputStream in = url.openStream()) {
            Path cache = Path.of(System.getProperty("java.io.tmpdir"), "docupodcast-theatre-cameras");
            Files.createDirectories(cache);
            Path target = cache.resolve(normalizeId(cameraId) + ".png").toAbsolutePath().normalize();
            Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return Optional.of(target);
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    private static String normalizeId(String id) {
        return id == null ? "" : id.strip().toUpperCase(java.util.Locale.ROOT);
    }

    private static String distance(String cameraId) {
        String[] parts = normalizeId(cameraId).split("_");
        return parts.length > 0 ? parts[0] : "";
    }

    private static String orientation(String cameraId) {
        String[] parts = normalizeId(cameraId).split("_");
        return parts.length > 1 ? parts[1] : "";
    }

    private static String height(String cameraId) {
        String[] parts = normalizeId(cameraId).split("_");
        return parts.length > 2 ? parts[2] : "";
    }
}
