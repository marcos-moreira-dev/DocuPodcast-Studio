package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Configuration-owned installation and smoke surface for Qwen3-TTS 1.7B Q8. */
final class Qwen3TtsEngineAdministration extends AbstractLocalEngineAdministration
        implements ManagedDownloadAdministration {
    static final String LLAMA_VERSION = "b10573";
    static final String MODEL_REVISION = "ca27d74bc954b73dadab5b71ca265d87fc861a7c";
    static final String RUNTIME_URL = "https://github.com/ggml-org/llama.cpp/releases/download/"
            + LLAMA_VERSION + "/llama-b10573-bin-win-cuda-12.4-x64.zip";
    static final long RUNTIME_BYTES = 250_972_941L;
    static final String RUNTIME_SHA256 = "be1d7cadff9578442875bb91dace718558c1890bafaac827d00f7b75626778af";
    static final String CUDA_URL = "https://github.com/ggml-org/llama.cpp/releases/download/"
            + LLAMA_VERSION + "/cudart-llama-bin-win-cuda-12.4-x64.zip";
    static final long CUDA_BYTES = 391_443_627L;
    static final String CUDA_SHA256 = "8c79a9b226de4b3cacfd1f83d24f962d0773be79f1e7b75c6af4ded7e32ae1d6";
    static final String MODEL_URL = "https://huggingface.co/ggml-org/"
            + "Qwen3-TTS-12Hz-1.7B-Base-GGUF/resolve/" + MODEL_REVISION
            + "/Qwen3-TTS-12Hz-1.7B-Base-Q8_0.gguf?download=true";
    static final long MODEL_BYTES = 1_847_874_400L;
    static final String MODEL_SHA256 = "ac7931aeb2e7aad1a6ed6602d353a5679c9d096b18ce8204ac730a8408d572e1";
    static final String CODEC_URL = "https://huggingface.co/ggml-org/"
            + "Qwen3-TTS-12Hz-1.7B-Base-GGUF/resolve/" + MODEL_REVISION
            + "/mmproj-Qwen3-TTS-12Hz-1.7B-Base-Q8_0.gguf?download=true";
    static final long CODEC_BYTES = 446_422_912L;
    static final String CODEC_SHA256 = "6fd65188839bcd6ecc91b277ad471e22a0edfada4699a0fe82f1165c18cfcce2";
    static final long TOTAL_DOWNLOAD_BYTES = RUNTIME_BYTES + CUDA_BYTES + MODEL_BYTES + CODEC_BYTES;

    private static final String DOWNLOAD_ROOT = "tools/qwen3-tts/downloads/";
    private static final String RUNTIME_ARCHIVE = DOWNLOAD_ROOT + "llama-b10573-bin-win-cuda-12.4-x64.zip";
    private static final String CUDA_ARCHIVE = DOWNLOAD_ROOT + "cudart-llama-bin-win-cuda-12.4-x64.zip";
    private static final String RUNTIME_DIRECTORY = "tools/qwen3-tts/llama.cpp";
    private static final String MODEL_TARGET = "models/tts/qwen3-tts/Qwen3-TTS-12Hz-1.7B-Base-Q8_0.gguf";
    private static final String CODEC_TARGET = "models/tts/qwen3-tts/mmproj-Qwen3-TTS-12Hz-1.7B-Base-Q8_0.gguf";
    private static final EngineActionId IMPORT_RUNTIME = new EngineActionId("import-runtime");
    private static final EngineActionId IMPORT_Q8 = new EngineActionId("import-q8-model");

    private final VoiceSynthesisEngine voice;
    private final DiskSpaceProbe diskSpace;
    private final ThreadLocal<ManagedDownloadDecision> decision =
            ThreadLocal.withInitial(() -> ManagedDownloadDecision.USE_EXISTING);

    Qwen3TtsEngineAdministration(VoiceSynthesisEngine voice, RuntimeAssetCatalog assets) {
        this(voice, assets, DiskSpaceProbe.system());
    }

    Qwen3TtsEngineAdministration(VoiceSynthesisEngine voice, RuntimeAssetCatalog assets,
                                 DiskSpaceProbe diskSpace) {
        super(voice, assets, List.of(
                new EngineActionDescriptor(EngineActionId.INSTALL,
                        "Preparar Qwen3-TTS local · 1.7B Q8",
                        "Descarga el runtime privado fijado, talker Q8 y codec Q8. No modifica PATH ni instala servicios.",
                        List.of(), true, TOTAL_DOWNLOAD_BYTES,
                        Map.of("license", "MIT; Apache-2.0", "source", RUNTIME_URL + "; " + MODEL_URL,
                                "managedDownload", "true")),
                action(IMPORT_RUNTIME, "Importar runtime Qwen3-TTS",
                        "Importa un directorio llama.cpp CUDA completo al runtime administrado.", true,
                        directory("runtimeDirectory", "Directorio de llama.cpp CUDA", true)),
                action(IMPORT_Q8, "Importar modelo Qwen3-TTS 1.7B Q8",
                        "Importa los dos GGUF oficiales Q8_0 a destinos administrados.", true,
                        file("modelFile", "Talker 1.7B Base Q8_0", true),
                        file("codecFile", "Codec/mmproj Q8_0", true)),
                action(EngineActionId.REPAIR, "Reparar Qwen3-TTS",
                        "Limpia exclusivamente staging incompleto de Qwen3-TTS.", true),
                action(EngineActionId.SMOKE_TEST, "Probar Qwen3-TTS Q8",
                        "Genera un WAV real con el runtime, modelo y referencia neutral administrados.", false)));
        this.voice = voice;
        this.diskSpace = java.util.Objects.requireNonNull(diskSpace, "diskSpace");
    }

    @Override public ManagedDownloadPreflight inspectDownload(EngineActionRequest request) throws IOException {
        Path runtimeDirectory = runtime.target(RUNTIME_DIRECTORY);
        Path model = runtime.target(MODEL_TARGET);
        Path codec = runtime.target(CODEC_TARGET);
        Path speaker = assets.require(Qwen3TtsVoiceEngine.ID, "speaker");
        boolean partial = hasStagingPayload(runtime.staging(engineId().value()));
        boolean runtimeValid = validates(runtimeDirectory, Qwen3TtsEngineAdministration::verifyRuntimeDirectory);
        boolean modelValid = validates(model, path -> verifyFile(path, MODEL_BYTES, MODEL_SHA256, "talker Q8"));
        boolean codecValid = validates(codec, path -> verifyFile(path, CODEC_BYTES, CODEC_SHA256, "codec Q8"));
        boolean speakerValid = Files.isRegularFile(speaker);
        boolean any = Files.exists(runtimeDirectory) || Files.exists(model) || Files.exists(codec)
                || Files.exists(runtime.target(RUNTIME_ARCHIVE)) || Files.exists(runtime.target(CUDA_ARCHIVE));
        ManagedDownloadState state = partial ? ManagedDownloadState.PARTIAL
                : runtimeValid && modelValid && codecValid && speakerValid ? ManagedDownloadState.VALID
                : any ? ManagedDownloadState.INVALID : ManagedDownloadState.MISSING;
        long actual = regularSize(model) + regularSize(codec) + regularSize(runtime.target(RUNTIME_ARCHIVE))
                + regularSize(runtime.target(CUDA_ARCHIVE));
        return new ManagedDownloadPreflight(
                "Qwen3-TTS 1.7B Base Q8_0 · llama.cpp " + LLAMA_VERSION, state, runtimeDirectory,
                TOTAL_DOWNLOAD_BYTES, actual, hashes(RUNTIME_SHA256, CUDA_SHA256, MODEL_SHA256, CODEC_SHA256),
                hashesIfPresent(runtime.target(RUNTIME_ARCHIVE), runtime.target(CUDA_ARCHIVE), model, codec),
                "MIT; Apache-2.0", RUNTIME_URL + " | " + MODEL_URL,
                diagnosis(runtimeValid, modelValid, codecValid, speakerValid, partial));
    }

    @Override public EngineActionResult executeDownload(EngineActionRequest request,
                                                        ManagedDownloadDecision selected,
                                                        ExecutionContext context)
            throws IOException, InterruptedException {
        decision.set(selected == null ? ManagedDownloadDecision.CANCEL : selected);
        try {
            return execute(request, context);
        } finally {
            decision.remove();
        }
    }

    @Override protected List<GenerationArtifact> perform(EngineActionRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        if (EngineActionId.INSTALL.equals(request.actionId())) {
            ManagedDownloadDecision selected = decision.get();
            if (selected == ManagedDownloadDecision.REDOWNLOAD || !inspectDownload(request).valid()) {
                ensureSpace();
            }
            runtime.downloadVerified(RUNTIME_URL, RUNTIME_ARCHIVE, engineId().value(), context, selected,
                    path -> verifyFile(path, RUNTIME_BYTES, RUNTIME_SHA256, "runtime llama.cpp"));
            runtime.downloadVerified(CUDA_URL, CUDA_ARCHIVE, engineId().value(), context, selected,
                    path -> verifyFile(path, CUDA_BYTES, CUDA_SHA256, "runtime CUDA"));
            runtime.downloadVerified(MODEL_URL, MODEL_TARGET, engineId().value(), context, selected,
                    path -> verifyFile(path, MODEL_BYTES, MODEL_SHA256, "talker Q8"));
            runtime.downloadVerified(CODEC_URL, CODEC_TARGET, engineId().value(), context, selected,
                    path -> verifyFile(path, CODEC_BYTES, CODEC_SHA256, "codec Q8"));
            runtime.installZipOverlayDirectoryVerified(
                    List.of(runtime.target(RUNTIME_ARCHIVE), runtime.target(CUDA_ARCHIVE)),
                    RUNTIME_DIRECTORY, engineId().value(), context,
                    Qwen3TtsEngineAdministration::verifyRuntimeDirectory);
        } else if (IMPORT_RUNTIME.equals(request.actionId())) {
            runtime.importDirectoryVerified(inputPath(request, "runtimeDirectory"), RUNTIME_DIRECTORY,
                    context, Qwen3TtsEngineAdministration::verifyPinnedRuntimeDirectory);
        } else if (IMPORT_Q8.equals(request.actionId())) {
            Path model = inputPath(request, "modelFile");
            Path codec = inputPath(request, "codecFile");
            requireQ8(model, "talker");
            requireQ8(codec, "codec/mmproj");
            runtime.importFileVerified(model, MODEL_TARGET, context,
                    path -> verifyFile(path, MODEL_BYTES, MODEL_SHA256, "talker Q8"));
            runtime.importFileVerified(codec, CODEC_TARGET, context,
                    path -> verifyFile(path, CODEC_BYTES, CODEC_SHA256, "codec Q8"));
        } else if (EngineActionId.REPAIR.equals(request.actionId())) {
            runtime.repair(engineId().value(), context);
        } else if (EngineActionId.SMOKE_TEST.equals(request.actionId())) {
            Path output = runtime.target("diagnostics/qwen3-tts/qwen3-tts-q8-smoke.wav");
            Files.createDirectories(output.getParent());
            voice.synthesize(new VoiceSynthesisRequest(
                    "¡Qué alegría! Esta es una prueba local de voz natural.", "es", "",
                    assets.require(Qwen3TtsVoiceEngine.ID, "speaker"), output,
                    Map.of("styleId", "HAPPY")), context);
            return List.of(new GenerationArtifact("audio", output.toUri(),
                    Map.of("purpose", "smoke", "quantization", "Q8_0")));
        }
        return List.of();
    }

    private void ensureSpace() throws IOException {
        long required = Math.addExact(TOTAL_DOWNLOAD_BYTES,
                Math.max(512L * 1024L * 1024L, TOTAL_DOWNLOAD_BYTES * 15L / 100L));
        long available = diskSpace.usableBytes(assets.root());
        if (available < required) {
            throw new EngineExecutionException(EngineDiagnosticCode.NO_SPACE,
                    "No hay espacio suficiente para preparar Qwen3-TTS 1.7B Q8.",
                    Map.of("requiredBytes", Long.toString(required),
                            "availableBytes", Long.toString(available), "margin", "15%"));
        }
    }

    private static void verifyRuntimeDirectory(Path directory) throws IOException {
        List<String> missing = List.of("llama-tts.exe", "llama.dll", "ggml.dll", "ggml-cuda.dll",
                        "cudart64_12.dll", "cublas64_12.dll", "cublasLt64_12.dll").stream()
                .filter(name -> !Files.isRegularFile(directory.resolve(name))).toList();
        if (!missing.isEmpty()) {
            throw new IOException("El runtime llama.cpp/CUDA está incompleto: "
                    + String.join(", ", missing) + ".");
        }
    }

    private static void verifyPinnedRuntimeDirectory(Path directory) throws IOException {
        verifyRuntimeDirectory(directory);
        Process process = new ProcessBuilder(directory.resolve("llama-tts.exe").toString(), "--version")
                .redirectErrorStream(true)
                .start();
        try {
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("El runtime importado no respondió al validar llama.cpp "
                        + LLAMA_VERSION + ".");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0 || !output.contains("build 10573")) {
                throw new IOException("El runtime importado no es llama.cpp " + LLAMA_VERSION
                        + ": " + output.strip());
            }
        } catch (InterruptedException interrupted) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("Se canceló la validación del runtime Qwen3-TTS.", interrupted);
        }
    }

    private static void verifyFile(Path file, long bytes, String sha256, String label) throws IOException {
        if (!Files.isRegularFile(file) || Files.size(file) != bytes) {
            throw new IOException("El " + label + " no coincide con el tamaño fijado.");
        }
        if (!sha256.equalsIgnoreCase(ManagedDownloadPreflightInspector.sha256(file))) {
            throw new IOException("El " + label + " no coincide con el SHA-256 fijado.");
        }
    }

    private static void requireQ8(Path file, String label) throws IOException {
        String name = file.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (!Files.isRegularFile(file) || !name.contains("q8_0")
                || name.contains("bf16") || name.contains("fp32")) {
            throw new IOException("El " + label + " debe ser explícitamente Q8_0; no se admiten BF16/FP32.");
        }
    }

    private static boolean validates(Path path, SafeRuntimeOperations.PathValidator validator) {
        try {
            validator.validate(path);
            return true;
        } catch (IOException | RuntimeException invalid) {
            return false;
        }
    }

    private static boolean hasStagingPayload(Path staging) throws IOException {
        if (!Files.isDirectory(staging)) return false;
        try (var paths = Files.walk(staging)) {
            return paths.anyMatch(path -> !path.equals(staging));
        }
    }

    private static long regularSize(Path path) throws IOException {
        return Files.isRegularFile(path) ? Files.size(path) : 0L;
    }

    private static String hashes(String runtimeHash, String cudaHash, String modelHash, String codecHash) {
        return "runtime=" + runtimeHash + ";cuda=" + cudaHash
                + ";talker=" + modelHash + ";codec=" + codecHash;
    }

    private static String hashesIfPresent(Path runtimeArchive, Path cudaArchive, Path model, Path codec)
            throws IOException {
        LinkedHashMap<String, Path> files = new LinkedHashMap<>();
        files.put("runtime", runtimeArchive);
        files.put("cuda", cudaArchive);
        files.put("talker", model);
        files.put("codec", codec);
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Path> entry : files.entrySet()) {
            if (!Files.isRegularFile(entry.getValue())) continue;
            if (!result.isEmpty()) result.append(';');
            result.append(entry.getKey()).append('=')
                    .append(ManagedDownloadPreflightInspector.sha256(entry.getValue()));
        }
        return result.toString();
    }

    private static String diagnosis(boolean runtimeValid, boolean modelValid, boolean codecValid,
                                    boolean speakerValid, boolean partial) {
        if (partial) return "Hay staging incompleto; usa Reparar o Redescargar.";
        if (!runtimeValid) return "Falta o está incompleto el runtime llama.cpp CUDA fijado.";
        if (!modelValid) return "Falta o no coincide el talker 1.7B Q8_0.";
        if (!codecValid) return "Falta o no coincide el codec/mmproj Q8_0.";
        if (!speakerValid) return "Falta la referencia neutral oficial incluida con la aplicación.";
        return "Runtime, talker Q8, codec Q8 y referencia neutral validados.";
    }
}
