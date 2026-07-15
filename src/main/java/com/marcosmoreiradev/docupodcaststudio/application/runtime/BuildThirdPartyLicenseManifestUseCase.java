package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.util.List;

/** Builds the legal inventory required before shipping a portable or installer build. */
public final class BuildThirdPartyLicenseManifestUseCase {
    public ThirdPartyLicenseManifest execute() {
        return new ThirdPartyLicenseManifest(List.of(
                component("ffmpeg", "FFmpeg", ThirdPartyComponentKind.RUNTIME_TOOL,
                        "tools/ffmpeg/bin", "GPL/LGPL según build usado", "https://ffmpeg.org/legal.html",
                        false, true, "TP4/T119C exigen confirmar build, licencia, checksum SHA-256 y avisos antes de redistribuir."),
                component("piper", "Piper TTS", ThirdPartyComponentKind.RUNTIME_TOOL,
                        "tools/piper", "MIT / licencias del proyecto y voces", "https://github.com/rhasspy/piper",
                        false, true, "Binario y voces pueden tener condiciones distintas; no asumir redistribución automática sin ENGINE_ARTIFACTS_MANIFEST."),
                component("coqui-xtts", "Coqui/XTTS wrapper y modelos", ThirdPartyComponentKind.AI_MODEL,
                        "tools/xtts-wrapper y models/tts/xtts", "Pendiente según modelo usado", "",
                        false, true, "Modelos y voces se preparan localmente; T119C exige licencia y checksum antes de RC final."),
                component("python-portable", "Python portable para XTTS", ThirdPartyComponentKind.RUNTIME_TOOL,
                        "tools/xtts-wrapper/.venv", "Python Software Foundation License", "https://docs.python.org/3/license.html",
                        false, true, "Si se empaqueta, debe incluir avisos de Python, dependencias instaladas y checksum del runtime local."),
                component("demo-docx", "Documentos DOCX de ejemplo", ThirdPartyComponentKind.EXAMPLE_ASSET,
                        "src/main/resources/examples", "Propio del proyecto", "",
                        true, false, "Instinto Creativo, Café Luna Azul y El vuelo del Tornillo Dorado son assets de demo internos."),
                component("demo-images", "Imágenes PNG de ejemplos", ThirdPartyComponentKind.EXAMPLE_ASSET,
                        "src/main/resources/examples/**/assets", "Propio del proyecto", "",
                        true, false, "Assets generados para demos internos; se copian al proyecto demo, no fuerzan secuencia visual automática."),
                component("javafx", "JavaFX runtime", ThirdPartyComponentKind.RUNTIME_TOOL,
                        "runtime jpackage", "GPLv2 with Classpath Exception", "https://openjfx.io/",
                        true, false, "jpackage debe conservar avisos de módulos runtime distribuidos."),
                component("maven-dependencies", "Dependencias Maven runtime", ThirdPartyComponentKind.RUNTIME_TOOL,
                        "target/dependency", "Ver dependencias concretas", "",
                        true, false, "La RC debe listar dependencias y sus licencias antes de distribuir."),
                component("tts-scripts", "Scripts TTS/preflight propios", ThirdPartyComponentKind.SCRIPT,
                        "scripts/tts", "Propio del proyecto", "",
                        true, false, "Scripts auditables ASCII-safe para Windows PowerShell 5.1." )
        ));
    }

    private static ThirdPartyComponent component(String id, String name, ThirdPartyComponentKind kind,
                                                 String location, String license, String url,
                                                 boolean redistributed, boolean userProvided, String notes) {
        return new ThirdPartyComponent(id, name, kind, location, license, url, redistributed, userProvided, notes);
    }
}
