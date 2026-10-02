package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in audible smoke using the exact production Qwen and Piper adapters. */
@EnabledIfSystemProperty(named = "docupodcast.fastListen.physical",
        matches = "true")
final class FastListenPhysicalSmokeTest {
    private static final String MODEL = "qwen3-vl:4b-instruct-q8_0";
    private static final String BLOCK_GRAMMAR = """
            Lee visualmente toda la pagina como una persona. Devuelve TODOS los bloques
            semanticos utiles en orden fisico humano. Conserva literalmente texto, cifras,
            simbolos, formulas, codigo y rotulos dentro de diagramas. No omitas titulos,
            teoremas, demostraciones ni parrafos por contener notacion matematica. Omite solo
            decoracion, encabezados, pies y numeros de pagina repetitivos.

            Una tabla completa es una sola region TABLE y SOURCE contiene todas sus celdas
            visibles, con filas separadas por saltos y celdas por " ; ". SOURCE puede contener
            libremente JSON, codigo, Unicode, formulas, llaves y corchetes. SPEECH se omite para
            texto ordinario. Para IMAGE, SOURCE conserva todos los rotulos y SPEECH describe
            relaciones visuales. Para MATH usa SPEECH solo si agrega comprension. No inventes,
            resumas ni devuelvas una pagina vacia cuando existe contenido visible.

            pageRole es CONTENT para una pagina normal de explicacion. Usa INDEX solo para un
            indice, BIBLIOGRAPHY para referencias, CATALOG para un catalogo y VISUAL_REFERENCE
            solo cuando la pagina sea principalmente una referencia visual.

            Responde exclusivamente con esta gramatica, sin Markdown, JSON ni razonamiento:
            PAGE|V1|idioma|rol
            BEGIN|tipo|xMin|yMin|xMax|yMax|narratabilidad|confianza
            SOURCE
            contenido visible literal
            SPEECH
            lectura opcional solo para MATH o IMAGE
            END
            DONE
            Emite BEGIN/SOURCE/[SPEECH]/END por cada region visible. tipo usa TITLE, HEADING,
            SUBHEADING, PARAGRAPH, LIST, SIDEBAR, TABLE, MATH, IMAGE, CAPTION, CODE o UNKNOWN;
            narratabilidad usa N, X o U; bbox y confianza son numeros. DONE es la ultima linea.
            Nunca emitas literalmente tipo, xMin, yMin, xMax, yMax, narratabilidad o confianza.
            Sustituyelos por valores reales. Cada BEGIN debe ir seguido inmediatamente por
            SOURCE, incluso para texto ordinario. Ejemplo de respuesta completa valida:
            PAGE|V1|es|CONTENT
            BEGIN|PARAGRAPH|80|120|920|260|N|95
            SOURCE
            Texto visible de ejemplo.
            END
            DONE
            OBLIGATORIO: la primera linea debe ser exactamente PAGE|V1|es|CONTENT.
            No empieces con BEGIN. Java descartara toda la respuesta si falta PAGE.
            """;

    @Test
    void qwenPageTwoOverlapsPhysicalPiperPageOneAndProducesPlayableWav()
            throws Exception {
        Path root = Path.of(System.getProperty("docupodcast.fastListen.root", "."))
                .toAbsolutePath().normalize();
        Path page1 = Path.of(System.getProperty("docupodcast.fastListen.page1"));
        Path page2 = Path.of(System.getProperty("docupodcast.fastListen.page2"));
        assertTrue(Files.isRegularFile(page1), "Falta la pagina 1: " + page1);
        assertTrue(Files.isRegularFile(page2), "Falta la pagina 2: " + page2);
        Path evidence = root.resolve("target/fast-listen-physical");
        Files.createDirectories(evidence);
        Path wav = evidence.resolve("page-01-piper.wav");
        Files.deleteIfExists(wav);
        Files.deleteIfExists(evidence.resolve("events.log"));
        Files.deleteIfExists(evidence.resolve("qwen-p1.raw.txt"));
        Files.deleteIfExists(evidence.resolve("qwen-p2.raw.txt"));
        Files.deleteIfExists(evidence.resolve("FAST_LISTEN_PHYSICAL_REPORT.md"));

        PriorityResourceScheduler scheduler = new PriorityResourceScheduler(
                ComputeResourceBudget.incrementalReaderDefaults());
        LinkedHashMap<String, Long> marks = new LinkedHashMap<>();
        long base = System.nanoTime();
        marks.put("T0_FAST_LISTEN", base);
        try (MediaEnginePlatform platform = LocalMediaAdapters.create(
                LocalMediaLayout.development(root))) {
            ContentAnalysisEngine qwen = platform.contentAnalysisEngines()
                    .supporting(ContentAnalysisOperation.PAGE_SEMANTIC_READING)
                    .getFirst();
            VoiceSynthesisEngine piper = platform.voiceEngines()
                    .require(new EngineId("piper"));

            ContentAnalysisResult first;
            try (ResourceLease lease = scheduler.acquire(qwenAdmission("qwen-p1"))) {
                first = qwen.analyze(semanticRequest(page1, 1),
                        context("qwen-p1", evidence, lease));
            }
            Files.writeString(evidence.resolve("qwen-p1.raw.txt"),
                    first.structuredJson(), StandardCharsets.UTF_8);
            assertValidBlock(first.structuredJson());
            marks.put("T1_PAGE_ACCEPTED", System.nanoTime());
            String speech = firstNarratableSource(first.structuredJson());
            String finalSpeech = speech.substring(0, Math.min(500, speech.length()));
            marks.put("T2_NARRATION_AVAILABLE", System.nanoTime());

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                long qwenP2Started = System.nanoTime();
                var qwenP2 = executor.submit(() -> {
                    try (ResourceLease lease = scheduler.acquire(qwenAdmission("qwen-p2"))) {
                        return qwen.analyze(semanticRequest(page2, 2),
                                context("qwen-p2", evidence, lease));
                    }
                });
                waitForActive(scheduler, "qwen-p2", 30);
                long ttsStarted = System.nanoTime();
                VoiceSynthesisBatchResult voice;
                try (ResourceLease lease = scheduler.acquire(piperAdmission())) {
                    voice = piper.synthesizeBatch(
                            new VoiceSynthesisBatchRequest(List.of(
                                    new VoiceSynthesisUnit("PDFSEG-P1-SMOKE", finalSpeech,
                                            "es_ES-default-medium", "", null, wav,
                                            Map.of("sourcePage", "1"))),
                                    "es", Map.of()),
                            context("piper-p1", evidence, lease));
                }
                assertFalse(voice.units().isEmpty());
                assertTrue(Files.size(wav) > 44L);
                marks.put("T3_FIRST_WAV", System.nanoTime());
                marks.put("T4_PLAYBACK_STARTED", physicalPlaybackStart(wav));

                ContentAnalysisResult second = qwenP2.get(20, TimeUnit.MINUTES);
                long qwenP2Ended = System.nanoTime();
                Files.writeString(evidence.resolve("qwen-p2.raw.txt"),
                        second.structuredJson(), StandardCharsets.UTF_8);
                assertValidBlock(second.structuredJson());
                assertTrue(qwenP2Started < marks.get("T3_FIRST_WAV")
                                && qwenP2Ended > ttsStarted,
                        "Qwen P2 y Piper P1 no se solaparon fisicamente");
                writeReport(evidence, marks, qwenP2Started, qwenP2Ended,
                        ttsStarted, first, second);
            }
        }
    }

    private static ContentAnalysisRequest semanticRequest(Path image, int page) {
        return new ContentAnalysisRequest(
                ContentAnalysisOperation.PAGE_SEMANTIC_READING,
                List.of(new AnalysisVisualInput(image, "complete-page", "image/png")),
                BLOCK_GRAMMAR, "Pagina solicitada: " + page + ".", "es", "",
                Map.of("model", MODEL, "maxOutputTokens", "1800",
                        "contextWindowTokens", "8192", "batchSize", "512",
                        "responseProtocol", "page-semantic-block-v1",
                        "pageNumber", Integer.toString(page),
                        "computePriority", "INTERACTIVE_ANALYSIS"));
    }

    private static ExecutionContext context(String id, Path evidence,
                                            ResourceLease lease) {
        return new ExecutionContext(id, CancellationToken.NONE,
                (stage, progress, message) -> appendEvent(
                        evidence, id, stage, progress, message),
                ExecutionPolicy.defaults(), lease,
                GenerationArtifactStaging.NONE,
                ComputePreference.specificDevice("gpu-nvidia-0", true));
    }

    private static ComputeAdmissionRequest qwenAdmission(String id) {
        long mib = 1024L * 1024L;
        ComputeDeviceId gpu = ComputeDeviceId.parse("gpu-nvidia-0");
        ModelResidencyKey key = new ModelResidencyKey(
                "qwen3-vl-local", MODEL, "q8_0-ctx8192-batch512-kvq8-flash", gpu);
        ComputeResourceDemand demand = new ComputeResourceDemand(
                Map.of(ResourceId.QWEN_INFERENCE, 1, ResourceId.CPU_HEAVY, 1),
                512L * mib, 384L * mib, true, gpu, 1,
                new ModelResidencyDemand(key, 4_000L * mib, 1_800L * mib),
                EncoderResourceDemand.none());
        return new ComputeAdmissionRequest(id, id,
                ComputeJobPriority.INTERACTIVE_ANALYSIS,
                ComputeWorkloadKind.CONTENT_ANALYSIS, demand,
                CancellationToken.NONE);
    }

    private static ComputeAdmissionRequest piperAdmission() {
        ComputeResourceDemand demand = new ComputeResourceDemand(
                Map.of(ResourceId.CPU_HEAVY, 1),
                192L * 1024L * 1024L, 0L, true, ComputeDeviceId.CPU_0,
                0, null, EncoderResourceDemand.none());
        return new ComputeAdmissionRequest("piper-p1", "piper-p1",
                ComputeJobPriority.USER_AUDIO,
                ComputeWorkloadKind.VOICE_SYNTHESIS, demand,
                CancellationToken.NONE);
    }

    private static void assertValidBlock(String output) {
        String normalized = output == null ? ""
                : output.replace("\r\n", "\n").replace('\r', '\n').strip();
        assertTrue(normalized.startsWith("PAGE|V1|"));
        assertTrue(normalized.endsWith("DONE"));
        String[] lines = normalized.split("\n", -1);
        int regions = 0;
        int index = 1;
        while (index < lines.length - 1) {
            while (index < lines.length - 1 && lines[index].isBlank()) index++;
            if (index >= lines.length - 1) break;
            String line = lines[index++];
            assertTrue(line.startsWith("BEGIN|"),
                    "Se esperaba BEGIN y se obtuvo: " + line);
            regions++;
            String[] fields = line.split("\\|", -1);
            assertEquals(8, fields.length, "BEGIN requiere ocho campos");
            assertTrue(List.of("TITLE", "HEADING", "SUBHEADING", "PARAGRAPH",
                            "LIST", "SIDEBAR", "BOX", "TABLE", "MATH", "IMAGE",
                            "CAPTION", "CODE", "UNKNOWN").contains(fields[1]),
                    "tipo invalido: " + fields[1]);
            double xMin = Double.parseDouble(fields[2]);
            double yMin = Double.parseDouble(fields[3]);
            double xMax = Double.parseDouble(fields[4]);
            double yMax = Double.parseDouble(fields[5]);
            assertTrue(xMin >= 0 && yMin >= 0 && xMax <= 1000 && yMax <= 1000
                    && xMax > xMin && yMax > yMin, "bbox invalido: " + line);
            assertTrue(List.of("N", "X", "U").contains(fields[6]));
            double confidence = Double.parseDouble(fields[7]);
            assertTrue(confidence >= 0 && confidence <= 100);
            assertTrue(index < lines.length - 1 && "SOURCE".equals(lines[index]),
                    "Cada BEGIN debe ir seguido inmediatamente por SOURCE");
            index++;
            boolean content = false;
            boolean ended = false;
            while (index < lines.length - 1) {
                String payload = lines[index++];
                if ("END".equals(payload)) {
                    ended = true;
                    break;
                }
                assertFalse(payload.startsWith("BEGIN|"),
                        "Falta END antes de la siguiente region");
                if (!payload.isBlank() && !"SPEECH".equals(payload)) content = true;
            }
            assertTrue(ended, "Cada region debe terminar en END");
            assertTrue(content, "Cada region debe conservar SOURCE o SPEECH");
        }
        assertTrue(regions > 0);
    }

    private static String firstNarratableSource(String output) {
        String[] lines = output.replace("\r\n", "\n").split("\n");
        StringBuilder text = new StringBuilder();
        boolean source = false;
        for (String line : lines) {
            if ("SOURCE".equals(line)) { source = true; continue; }
            if (source && ("SPEECH".equals(line) || "END".equals(line))) break;
            if (source && !line.isBlank()) {
                if (!text.isEmpty()) text.append(' ');
                text.append(line.strip());
            }
        }
        assertFalse(text.isEmpty(), "La primera region no contiene SOURCE");
        return text.toString();
    }

    private static long physicalPlaybackStart(Path wav) throws Exception {
        try (var stream = AudioSystem.getAudioInputStream(wav.toFile());
             Clip clip = AudioSystem.getClip()) {
            clip.open(stream);
            clip.start();
            long started = System.nanoTime();
            long deadline = started + TimeUnit.SECONDS.toNanos(2);
            while (!clip.isRunning() && System.nanoTime() < deadline) {
                Thread.sleep(10L);
            }
            assertTrue(clip.isRunning(), "El dispositivo de audio no inicio el WAV");
            Thread.sleep(150L);
            clip.stop();
            return started;
        }
    }

    private static void waitForActive(PriorityResourceScheduler scheduler,
                                      String operation, int seconds) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (System.nanoTime() < deadline) {
            if (scheduler.snapshot().active().stream()
                    .anyMatch(entry -> entry.operationId().equals(operation))) return;
            Thread.sleep(25L);
        }
        fail("Qwen P2 no obtuvo admision fisica");
    }

    private static void appendEvent(Path evidence, String id, String stage,
                                    double progress, String message) {
        try {
            Files.writeString(evidence.resolve("events.log"),
                    System.nanoTime() + "|" + id + "|" + stage + "|"
                            + progress + "|" + message + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) { }
    }

    private static void writeReport(Path evidence, Map<String, Long> marks,
                                    long qwenStart, long qwenEnd, long ttsStart,
                                    ContentAnalysisResult first,
                                    ContentAnalysisResult second) throws Exception {
        long t0 = marks.get("T0_FAST_LISTEN");
        String report = """
                # FAST_LISTEN physical smoke

                - Qwen: `%s`, contexto 8192, Q8, serial
                - TTS: Piper fisico
                - T0->T1: %d ms
                - T1->T2: %d ms
                - T2->T3: %d ms
                - T3->T4: %d ms
                - T0->T4: %d ms
                - Qwen P2: %d..%d ms
                - Piper P1: %d..%d ms
                - overlap: true
                - P1/P2: protocolo V1 terminado en DONE
                - P1 tok/s: %s
                - P2 tok/s: %s
                """.formatted(MODEL,
                ms(t0, marks.get("T1_PAGE_ACCEPTED")),
                ms(marks.get("T1_PAGE_ACCEPTED"), marks.get("T2_NARRATION_AVAILABLE")),
                ms(marks.get("T2_NARRATION_AVAILABLE"), marks.get("T3_FIRST_WAV")),
                ms(marks.get("T3_FIRST_WAV"), marks.get("T4_PLAYBACK_STARTED")),
                ms(t0, marks.get("T4_PLAYBACK_STARTED")),
                ms(t0, qwenStart), ms(t0, qwenEnd), ms(t0, ttsStart),
                ms(t0, marks.get("T3_FIRST_WAV")),
                first.diagnostics().getOrDefault("generationTokensPerSecond", ""),
                second.diagnostics().getOrDefault("generationTokensPerSecond", ""));
        Files.writeString(evidence.resolve("FAST_LISTEN_PHYSICAL_REPORT.md"),
                report, StandardCharsets.UTF_8);
    }

    private static long ms(long start, long end) {
        return Duration.ofNanos(Math.max(0L, end - start)).toMillis();
    }
}
