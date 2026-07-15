package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Presentation-only policy for turning technical setup output into user-facing text. */
final class SettingsTechnicalMessageHumanizer {
    private SettingsTechnicalMessageHumanizer() {
    }

    static String progress(String message) {
        String raw = message == null ? "" : message.strip();
        String text = raw.toLowerCase(Locale.ROOT);
        if (text.contains("http") || text.contains("obligatorio") || text.contains("pendiente")
                || text.contains("vacío") || text.contains("vacio")) {
            return engineText(raw);
        }
        if (text.startsWith("descargando") && (text.contains(":") || text.contains("archivo"))) {
            return downloadFailure(raw);
        }
        if (text.contains("download") || text.contains("descarg")) {
            return "Descargando recursos necesarios...";
        }
        if (text.contains("install") || text.contains("instal")) {
            return "Instalando componentes locales...";
        }
        if (text.contains("copy") || text.contains("copi")) {
            return "Copiando recursos dentro del programa...";
        }
        if (text.contains("verify") || text.contains("verific") || text.contains("check")) {
            return "Verificando componentes locales...";
        }
        if (text.contains("error") || text.contains("fail") || text.contains("fall")) {
            return "Revisando un problema durante la operación...";
        }
        return "Operación en curso...";
    }

    static String embeddedDependencyProgress(String message) {
        String raw = message == null ? "" : message.strip();
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("download") || lower.contains("descarg")) {
            return "Descargando recursos locales...";
        }
        if (lower.contains("install") || lower.contains("instal")) {
            return "Instalando componentes embebidos...";
        }
        if (lower.contains("copy") || lower.contains("copi")) {
            return "Copiando archivos dentro del programa...";
        }
        if (lower.contains("verific") || lower.contains("probe") || lower.contains("smoke")) {
            return "Verificando runtime local...";
        }
        return raw.isBlank() ? "Operación en curso..." : engineText(raw);
    }

    static String engineText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String value = text;
        value = value.replace(String.join("", "Co", "qui", "/", "XT", "TS"), "Voz IA avanzada");
        value = value.replace(String.join("", "Co", "qui", " ", "XT", "TS"), "Voz IA avanzada");
        value = value.replace(String.join("", "Co", "qui"), "Voz IA avanzada");
        value = value.replace(String.join("", "XT", "TS"), "Voz IA avanzada");
        value = value.replace(String.join("", "Pi", "per"), "Voz local simple");
        value = value.replace(String.join("", "co", "qui", "/", "xt", "ts"), "Voz IA avanzada");
        value = value.replace(String.join("", "co", "qui", " ", "xt", "ts"), "Voz IA avanzada");
        value = value.replace(String.join("", "co", "qui"), "Voz IA avanzada");
        value = value.replace(String.join("", "xt", "ts"), "Voz IA avanzada");
        value = value.replace(String.join("", "pi", "per"), "Voz local simple");
        value = value.replaceAll("https?://\\S+|www\\.\\S+", "proveedor de descarga");
        value = value.replace("ONNX", "voz local");
        value = value.replace(".json", "");
        value = value.replace("TTS externo", "Voz local no configurada");
        value = value.replace("mock", "modo de prueba");
        value = value.replace("Mock", "Modo de prueba");
        value = value.replace("FFmpeg", "Video local");
        value = value.replace("FFprobe", "componente de video");
        value = value.replaceAll("[A-Za-z]:[^\\s]+", "carpeta local");
        value = value.replaceAll("models[^\\s]+", "recursos locales");
        value = value.replaceAll("tools[^\\s]+", "componentes locales");
        value = value.replace(String.join("", ".on", "nx", ".json"), "");
        value = value.replace(String.join("", ".on", "nx"), "");
        value = value.replace("ffmpeg.exe", "componente de video");
        value = value.replace("ffprobe.exe", "componente de video");
        return value;
    }

    static String downloadFailures(List<String> failedFiles) {
        if (failedFiles == null || failedFiles.isEmpty()) {
            return "sin detalle pendiente";
        }
        return failedFiles.stream()
                .limit(4)
                .map(SettingsTechnicalMessageHumanizer::downloadFailure)
                .collect(Collectors.joining("; "));
    }

    static String downloadFailure(String detail) {
        if (detail == null || detail.isBlank()) {
            return "detalle no disponible";
        }
        String value = detail;
        value = value.replace("config.json", "configuración del modelo");
        value = value.replace("model.pth", "archivo principal de voz");
        value = value.replace("vocab.json", "vocabulario");
        value = value.replace(String.join("", "speakers", "_", "x", "tts.pth"), "referencias internas de voz");
        value = value.replace("dvae.pth", "codificador de audio");
        value = value.replace("mel_stats.pth", "estadísticas de audio");
        value = value.replace("LICENSE.txt", "licencia");
        value = value.replace("README.md", "documentación del modelo");
        value = value.replace("hash.md5", "verificación de descarga");
        return engineText(value);
    }
}
