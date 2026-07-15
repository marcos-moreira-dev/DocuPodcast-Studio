package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.util.List;

/** Builds the authoritative engine artifact contract for T119C and later RC gates. */
public final class BuildEngineArtifactManifestUseCase {
    public EngineArtifactManifest execute() {
        return new EngineArtifactManifest("engine-artifacts-v1", List.of(
                artifact("ffmpeg-exe", "FFmpeg executable", "ffmpeg", EngineArtifactKind.EXECUTABLE,
                        "tools/ffmpeg/bin/ffmpeg.exe", true, true, false,
                        "GPL/LGPL según build usado", "https://ffmpeg.org/legal.html",
                        "Obligatorio para media, normalización y video 720p/1080p/2K/4K."),
                artifact("ffprobe-exe", "FFprobe executable", "ffmpeg", EngineArtifactKind.EXECUTABLE,
                        "tools/ffmpeg/bin/ffprobe.exe", true, true, false,
                        "GPL/LGPL según build usado", "https://ffmpeg.org/legal.html",
                        "Obligatorio para diagnóstico y validación de video final."),
                artifact("xtts-python", "Python local para Coqui/XTTS", "coqui-xtts", EngineArtifactKind.RUNTIME,
                        "tools/xtts-wrapper/.venv/Scripts/python.exe", true, true, false,
                        "Python Software Foundation License", "https://docs.python.org/3/license.html",
                        "Debe ser Python local/portable, nunca Python global ni PATH del sistema."),
                artifact("xtts-wrapper", "Wrapper Python Coqui/XTTS", "coqui-xtts", EngineArtifactKind.SCRIPT,
                        "tools/xtts-wrapper/synthesize_xtts.py", true, true, false,
                        "Propio del proyecto", "",
                        "Puente controlado por Java para síntesis de voz de alta calidad."),
                artifact("xtts-config", "XTTS config.json", "coqui-xtts", EngineArtifactKind.MODEL_CONFIG,
                        "models/tts/xtts/config.json", true, false, true,
                        "Pendiente según modelo usado", "",
                        "Modelo puede ser user-provided o distribuido solo con licencia/checksum confirmados."),
                artifact("xtts-weights", "XTTS model weights", "coqui-xtts", EngineArtifactKind.MODEL_FILE,
                        "models/tts/xtts/*.pth|*.safetensors", true, false, true,
                        "Pendiente según modelo usado", "",
                        "Pesos requeridos para Coqui/XTTS; RC final no debe cerrar sin procedencia clara."),
                artifact("xtts-vocab", "XTTS vocab", "coqui-xtts", EngineArtifactKind.MODEL_FILE,
                        "models/tts/xtts/vocab.json|vocab.txt", true, false, true,
                        "Pendiente según modelo usado", "",
                        "Vocabulario requerido por el modelo XTTS seleccionado."),
                artifact("xtts-neutral-voice", "Voz neutral prediseñada", "coqui-xtts", EngineArtifactKind.VOICE_SAMPLE,
                        "models/tts/xtts/speakers/voz-por-defecto.wav", true, true, false,
                        "Propio/autorizado por el proyecto", "",
                        "Única voz base protegida; no eliminable por el usuario."),
                artifact("piper-exe", "Piper executable", "piper", EngineArtifactKind.EXECUTABLE,
                        "tools/piper/piper.exe", false, true, true,
                        "MIT / licencia del build usado", "https://github.com/rhasspy/piper",
                        "Modo intermedio/liviano. Si se incluye, requiere checksum/licencia."),
                artifact("piper-voice-onnx", "Piper voice .onnx", "piper", EngineArtifactKind.MODEL_FILE,
                        "models/tts/piper/voices/*.onnx", false, false, true,
                        "Pendiente según voz usada", "https://huggingface.co/rhasspy/piper-voices",
                        "Piper usa modelos de voz, no muestra humana para clonación."),
                artifact("piper-voice-config", "Piper voice .onnx.json", "piper", EngineArtifactKind.MODEL_CONFIG,
                        "models/tts/piper/voices/*.onnx.json", false, false, true,
                        "Pendiente según voz usada", "https://huggingface.co/rhasspy/piper-voices",
                        "Configuración obligatoria de la voz Piper correspondiente." )
        ));
    }

    private static EngineArtifactDescriptor artifact(String id, String displayName, String engine, EngineArtifactKind kind,
                                                     String expectedPath, boolean requiredForFinalRc, boolean redistributedByDefault,
                                                     boolean userProvidedAllowed, String licenseName, String sourceUrl, String notes) {
        return new EngineArtifactDescriptor(id, displayName, engine, kind, expectedPath, requiredForFinalRc,
                redistributedByDefault, userProvidedAllowed, licenseName, sourceUrl, EngineArtifactDescriptor.PENDING_SHA256, notes);
    }
}
