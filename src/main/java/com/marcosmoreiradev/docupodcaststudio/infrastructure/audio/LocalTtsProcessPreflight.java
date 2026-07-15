package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Performs cheap validation of the configured local TTS command before launching a job. */
public final class LocalTtsProcessPreflight {
    private final LocalTtsProcessConfiguration configuration;

    public LocalTtsProcessPreflight(LocalTtsProcessConfiguration configuration) {
        this.configuration = java.util.Objects.requireNonNull(configuration, "configuration");
    }

    public LocalTtsPreflightReport check() {
        if (!configuration.enabled()) {
            return LocalTtsPreflightReport.notReady(
                    "No hay comando TTS configurado.",
                    List.of("Define -D" + LocalTtsProcessConfiguration.PROPERTY_COMMAND + " o " + LocalTtsProcessConfiguration.ENV_COMMAND + "."),
                    List.of("Ejemplo: tts-worker --text-file {textFile} --output-file {outputFile}")
            );
        }
        List<String> issues = new ArrayList<>();
        List<String> hints = new ArrayList<>();
        String template = configuration.commandTemplate();
        List<String> tokens = LocalTtsProcessConfiguration.splitCommand(template);
        if (tokens.isEmpty()) {
            issues.add("El comando TTS queda vacío después de tokenizarlo.");
        } else {
            validateExecutableToken(tokens.get(0), issues, hints);
        }
        if (!containsAny(template, "{textFile}", "{input}")) {
            issues.add("El comando no declara placeholder de entrada {textFile} o {input}.");
        }
        if (!containsAny(template, "{outputFile}", "{output}")) {
            issues.add("El comando no declara placeholder de salida {outputFile} o {output}.");
        }
        if (!template.contains("{language}")) {
            hints.add("El comando no usa {language}; se asumirá que el motor fija el idioma por su cuenta.");
        }
        if (!template.contains("{voice}") && !template.contains("{voiceProfileId}")) {
            hints.add("El comando no usa {voice}; se asumirá una voz por defecto del motor.");
        }
        if (configuration.timeoutSeconds() < 5) {
            hints.add("El timeout configurado es bajo para TTS real: " + configuration.timeoutSeconds() + " segundos.");
        }
        if (issues.isEmpty()) {
            return LocalTtsPreflightReport.ready("Preflight TTS local correcto para " + configuration.displayName() + ".", hints);
        }
        return LocalTtsPreflightReport.notReady("Preflight TTS local falló para " + configuration.displayName() + ".", issues, hints);
    }

    private static void validateExecutableToken(String executable, List<String> issues, List<String> hints) {
        String normalized = executable == null ? "" : executable.strip();
        if (normalized.isBlank()) {
            issues.add("El ejecutable del comando está vacío.");
            return;
        }
        boolean pathLike = normalized.contains("/") || normalized.contains("\\") || normalized.startsWith(".");
        if (!pathLike) {
            hints.add("El ejecutable se resolverá por PATH: " + normalized + ".");
            return;
        }
        try {
            Path path = Path.of(normalized).toAbsolutePath().normalize();
            if (!Files.exists(path)) {
                issues.add("El ejecutable configurado no existe: " + path + ".");
            } else if (!Files.isRegularFile(path)) {
                issues.add("El ejecutable configurado no es un archivo regular: " + path + ".");
            } else if (!Files.isExecutable(path)) {
                hints.add("El archivo existe pero no aparece como ejecutable para el sistema: " + path + ".");
            }
        } catch (InvalidPathException ex) {
            issues.add("La ruta del ejecutable TTS es inválida: " + ex.getMessage());
        }
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
