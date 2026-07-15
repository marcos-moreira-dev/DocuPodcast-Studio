package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.util.List;
import java.util.Objects;

/** Builds the canonical tools/models/scripts packaging contract used by productization scripts. */
public final class BuildRuntimeBundleManifestUseCase {
    public RuntimeBundleManifest execute(ApplicationRuntimeLayout layout) {
        Objects.requireNonNull(layout, "layout");
        return new RuntimeBundleManifest(layout, List.of(
                dir("tools-root", RuntimePathRole.TOOLS_ROOT, "tools", true, true,
                        "Raiz autocontenida para motores y utilidades locales.",
                        "Debe viajar con la app portable o instalarse junto a la app."),
                dir("models-root", RuntimePathRole.MODELS_ROOT, "models", true, true,
                        "Raiz local para modelos TTS y voces.",
                        "Debe viajar con la app portable o quedar configurable."),
                dir("scripts-root", RuntimePathRole.SCRIPTS_ROOT, "scripts", true, true,
                        "Scripts de diagnostico, TTS, preflight y release.",
                        "Se empaquetan como soporte operativo auditable."),
                dir("examples-root", RuntimePathRole.EXAMPLES_ROOT, "src/main/resources/examples", true, true,
                        "Ejemplos internos de producto usados por el menu Ejemplos.",
                        "En app instalada pueden ir dentro del jar; en repo quedan como recursos."),
                file("ffmpeg-exe", RuntimePathRole.FFMPEG_EXECUTABLE, "tools/ffmpeg/bin/ffmpeg.exe", true, false,
                        "Ejecutable FFmpeg local obligatorio para media, normalizacion y video.",
                        "Debe viajar con la app final. No depende de rutas globales del sistema."),
                file("ffprobe-exe", RuntimePathRole.FFPROBE_EXECUTABLE, "tools/ffmpeg/bin/ffprobe.exe", true, false,
                        "Ejecutable FFprobe local obligatorio para diagnostico y validacion de video final.",
                        "Debe viajar con la app final junto a ffmpeg.exe. No depende de rutas globales del sistema."),
                file("piper-exe", RuntimePathRole.PIPER_EXECUTABLE, "tools/piper/piper.exe", false, false,
                        "Motor Piper local como fallback liviano de voz.",
                        "Colocar binario Piper compatible o dejar mock diagnostico."),
                dir("piper-voices", RuntimePathRole.MODELS_ROOT, "models/tts/piper/voices", false, true,
                        "Voces Piper .onnx y configuracion asociada.",
                        "Copiar voces autorizadas o descargadas por canal externo."),
                dir("xtts-wrapper", RuntimePathRole.XTTS_WRAPPER, "tools/xtts-wrapper", true, true,
                        "Wrapper Python local para Coqui/XTTS.",
                        "Preparar entorno con scripts\\20-preparar-python-portable-coqui.bat."),
                file("xtts-python", RuntimePathRole.XTTS_PYTHON, "tools/xtts-wrapper/.venv/Scripts/python.exe", false, false,
                        "Python local de XTTS; nunca Python global.",
                        "Se genera al preparar Coqui/XTTS local."),
                dir("xtts-models", RuntimePathRole.MODELS_ROOT, "models/tts/xtts", false, true,
                        "Modelos XTTS locales y voz de referencia por defecto.",
                        "Agregar modelos autorizados; la app no descarga modelos automaticamente."),
                dir("tts-scripts", RuntimePathRole.SCRIPTS_ROOT, "scripts/tts", true, true,
                        "Scripts puente para Piper, XTTS y preflights.",
                        "Deben permanecer ASCII-safe para Windows PowerShell 5.1." )
        ));
    }

    private static RuntimeBundleItem dir(String id, RuntimePathRole role, String path, boolean required, boolean placeholder,
                                         String purpose, String hint) {
        return new RuntimeBundleItem(id, role, RuntimeBundleItemKind.DIRECTORY, path, required, placeholder, purpose, hint);
    }

    private static RuntimeBundleItem file(String id, RuntimePathRole role, String path, boolean required, boolean placeholder,
                                          String purpose, String hint) {
        return new RuntimeBundleItem(id, role, RuntimeBundleItemKind.FILE, path, required, placeholder, purpose, hint);
    }
}
