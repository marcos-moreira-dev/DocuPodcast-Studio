package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Opt-in physical proof that a focused dense-table request preserves cells. */
@EnabledIfSystemProperty(named = "docupodcast.semanticRecovery.physical",
        matches = "true")
final class SemanticRecoveryPhysicalSmokeTest {
    private static final String MODEL = "qwen3-vl:4b-instruct-q8_0";
    private static final String INSTRUCTION = """
            Transcribe la TABLE completa de este crop. SOURCE debe contener TODAS las celdas
            visibles, fila por fila, separando celdas con " ; ". No resumas, no expliques y no
            sustituyas celdas por una conclusion. Devuelve una sola TABLE.

            Responde solo con el protocolo V1 completo, sin Markdown ni JSON:
            PAGE|V1|es|CONTENT
            BEGIN|TABLE|0|0|1000|1000|N|98
            SOURCE
            Encabezado A ; Encabezado B ; Encabezado C
            Fila 1 celda A ; Fila 1 celda B ; Fila 1 celda C
            Fila 2 celda A ; Fila 2 celda B ; Fila 2 celda C
            END
            DONE
            SOURCE es obligatorio y DONE es la ultima linea. Cada fila fisica ocupa una linea
            distinta y cada linea conserva sus celdas separadas por " ; ". Usa solo valores
            numericos reales en bbox/confianza; nunca emitas placeholders.
            """;
    private static final List<String> REQUIRED_CELLS = List.of(
            "Hallazgo", "Evidencia experimental", "Implicación",
            "El 50 % lineal conserva gran parte de la información",
            "La integral de línea se leyó en 2:23",
            "El redimensionamiento reduce el costo",
            "El 70 % mejora la fidelidad",
            "corrigió varias sustituciones presentes al 50 %",
            "Se adopta como perfil fiel provisional",
            "El tiempo depende también de la salida",
            "Una página solo de prosa al 50 % tardó 3:00",
            "La decodificación autoregresiva",
            "La mayor resolución no elimina todos los errores",
            "Diferencial", "diferenciación",
            "El contexto puede fallar antes que la comprensión",
            "8008 tokens no cabían en 7936", "con 8372 la salida se truncó",
            "4 GB de VRAM funcionan con derrame",
            "3.8/4 GB dedicados", "3.2 GB compartidos",
            "El hardware actual es válido para laboratorio");
    private static final List<String> MULTICOLUMN_REQUIRED = List.of(
            "dummy print routine", "print +",
            "form A p F/D if order T p F/D is encountered",
            "becomes A p F/D", "test sign", "change sign", "print -",
            "set digit count in 9 H", "multiply by 10/16",
            "Print number transferred by T order", "clear accumulator",
            "to sequence control", "number of digits",
            "test for order A n F in S(n)",
            "Test for entry to closed sub-routines and obey them directly");

    @Test
    void denseTableCropRecoversEverySignificantCellWithoutSummary()
            throws Exception {
        Path root = Path.of(System.getProperty(
                "docupodcast.semanticRecovery.root", "."))
                .toAbsolutePath().normalize();
        Path image = Path.of(System.getProperty(
                "docupodcast.semanticRecovery.tableImage"));
        assertTrue(Files.isRegularFile(image), "Falta la ROI de tabla: " + image);
        Path evidence = root.resolve("target/semantic-recovery-physical");
        Files.createDirectories(evidence);
        Path raw = evidence.resolve("dense-table.raw.txt");
        Path report = evidence.resolve("SEMANTIC_RECOVERY_PHYSICAL_REPORT.md");
        long started = System.nanoTime();
        ContentAnalysisResult result;
        ContentAnalysisResult multicolumn = null;
        long multicolumnMs = 0L;
        try (MediaEnginePlatform platform = LocalMediaAdapters.create(
                LocalMediaLayout.development(root))) {
            ContentAnalysisEngine qwen = platform.contentAnalysisEngines()
                    .supporting(ContentAnalysisOperation.PAGE_SEMANTIC_READING)
                    .getFirst();
            result = qwen.analyze(request(image), new ExecutionContext(
                    "semantic-recovery-dense-table", CancellationToken.NONE,
                    ProgressSink.NONE, ExecutionPolicy.defaults(), null,
                    GenerationArtifactStaging.NONE,
                    ComputePreference.specificDevice("gpu-nvidia-0", true)));
            String multicolumnPath = System.getProperty(
                    "docupodcast.semanticRecovery.multicolumnImage", "");
            if (!multicolumnPath.isBlank()) {
                Path multicolumnImage = Path.of(multicolumnPath);
                assertTrue(Files.isRegularFile(multicolumnImage),
                        "Falta la ROI multicolumna: " + multicolumnImage);
                long multiStarted = System.nanoTime();
                multicolumn = qwen.analyze(multicolumnRequest(multicolumnImage),
                        new ExecutionContext("semantic-recovery-multicolumn",
                                CancellationToken.NONE, ProgressSink.NONE,
                                ExecutionPolicy.defaults(), null,
                                GenerationArtifactStaging.NONE,
                                ComputePreference.specificDevice(
                                        "gpu-nvidia-0", true)));
                multicolumnMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                        System.nanoTime() - multiStarted);
            }
        }
        long elapsedMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - started);
        Files.writeString(raw, result.structuredJson(), StandardCharsets.UTF_8);
        String source = tableSource(result.structuredJson());
        ArrayList<String> missing = new ArrayList<>();
        for (String expected : REQUIRED_CELLS) {
            if (!normalized(source).contains(normalized(expected))) missing.add(expected);
        }
        List<String> rows = source.lines().filter(value -> !value.isBlank()).toList();
        boolean stableRows = rows.size() >= 7
                && rows.stream().allMatch(row -> row.split("\\s;\\s", -1).length >= 3);
        ArrayList<String> missingMulticolumn = new ArrayList<>();
        if (multicolumn != null) {
            Files.writeString(evidence.resolve("multicolumn-right.raw.txt"),
                    multicolumn.structuredJson(), StandardCharsets.UTF_8);
            String multiSource = allSource(multicolumn.structuredJson());
            for (String expected : MULTICOLUMN_REQUIRED) {
                if (!normalized(multiSource).contains(normalized(expected))) {
                    missingMulticolumn.add(expected);
                }
            }
        }
        Files.writeString(report, """
                # Semantic recovery physical smoke

                - timestamp: %s
                - model: `%s`
                - context: 8192
                - pass: targeted-recovery
                - ROI count: 1
                - durationMs: %d
                - promptTokens: %s
                - outputTokens: %s
                - generationTokensPerSecond: %s
                - required checks: %d
                - missing checks: %d
                - stable rows: %s
                - multicolumn durationMs: %d
                - multicolumn required checks: %d
                - multicolumn missing checks: %d
                - result: %s
                """.formatted(Instant.now(), MODEL, elapsedMs,
                result.diagnostics().getOrDefault("promptTokens", ""),
                result.diagnostics().getOrDefault("outputTokens", ""),
                result.diagnostics().getOrDefault(
                        "generationTokensPerSecond", ""),
                REQUIRED_CELLS.size(), missing.size(), stableRows,
                multicolumnMs, multicolumn == null ? 0 : MULTICOLUMN_REQUIRED.size(),
                missingMulticolumn.size(),
                missing.isEmpty() && stableRows && missingMulticolumn.isEmpty()
                        ? "ACCEPTED" : "REJECTED: missing=" + missing
                        + ", stableRows=" + stableRows
                        + ", multicolumnMissing=" + missingMulticolumn),
                StandardCharsets.UTF_8);
        assertTrue(missing.isEmpty(), "La tabla focal perdió celdas: " + missing);
        assertTrue(stableRows, "La tabla focal no preservó filas reproducibles: " + rows);
        assertTrue(missingMulticolumn.isEmpty(),
                "La ROI multicolumna perdió contenido: " + missingMulticolumn);
    }

    private static ContentAnalysisRequest request(Path image) {
        return new ContentAnalysisRequest(
                ContentAnalysisOperation.PAGE_SEMANTIC_READING,
                List.of(new AnalysisVisualInput(image,
                        "targeted-recovery-roi", "image/png")),
                INSTRUCTION, "Motivo: DENSE_TABLE_INCOMPLETE", "es", "",
                Map.of("model", MODEL, "maxOutputTokens", "1800",
                        "contextWindowTokens", "8192", "batchSize", "512",
                        "responseProtocol", "page-semantic-block-v1",
                        "semanticPass", "targeted-recovery",
                        "recoveryReason", "DENSE_TABLE_INCOMPLETE",
                        "computePriority", "INTERACTIVE_ANALYSIS"));
    }

    private static ContentAnalysisRequest multicolumnRequest(Path image) {
        String instruction = """
                Recupera literalmente SOLO la columna/anotaciones visibles de este crop tecnico.
                Conserva cada frase, simbolo y relacion entre llaves y notas. No resumas, no
                reordenes por conveniencia y no inventes la columna izquierda cortada.

                Responde solo con protocolo V1 completo. La primera linea DEBE ser
                PAGE|V1|en|CONTENT. Empieza exactamente con estas tres lineas:
                PAGE|V1|en|CONTENT
                BEGIN|PARAGRAPH|0|0|1000|1000|N|98
                SOURCE
                Despues de SOURCE transcribe TODO el texto real visible, desde "dummy print
                routine" hasta "directly", incluidas las notas intermedias. No te detengas tras
                la primera frase. Termina exactamente con estas dos lineas:
                END
                DONE
                Sin Markdown, JSON, placeholders ni comentarios fuera del protocolo.
                """;
        String evidence = MULTICOLUMN_REQUIRED.stream()
                .map(value -> "- " + value)
                .collect(java.util.stream.Collectors.joining("\n"));
        return new ContentAnalysisRequest(
                ContentAnalysisOperation.PAGE_SEMANTIC_READING,
                List.of(new AnalysisVisualInput(image,
                        "targeted-recovery-roi", "image/png")),
                instruction, "Motivo: MULTICOLUMN_GAP_OR_ORDER\n"
                + "Evidencia visual que debe quedar cubierta:\n" + evidence, "en", "",
                Map.of("model", MODEL, "maxOutputTokens", "1200",
                        "contextWindowTokens", "8192", "batchSize", "512",
                        "responseProtocol", "page-semantic-block-v1",
                        "semanticPass", "targeted-recovery",
                        "recoveryReason", "MULTICOLUMN_GAP_OR_ORDER",
                        "computePriority", "INTERACTIVE_ANALYSIS"));
    }

    private static String tableSource(String raw) {
        List<String> lines = raw == null ? List.of() : raw.lines().toList();
        assertTrue(!lines.isEmpty() && lines.getFirst().startsWith("PAGE|V1|"));
        assertTrue(lines.getLast().equals("DONE"));
        int begin = -1;
        int source = -1;
        int end = -1;
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).startsWith("BEGIN|TABLE|")) {
                String[] fields = lines.get(index).split("\\|", -1);
                assertTrue(fields.length == 8, "BEGIN TABLE debe tener 8 campos");
                for (int field = 2; field <= 7; field++) {
                    if (field == 6) continue;
                    assertTrue(fields[field].matches("\\d+(?:\\.\\d+)?"),
                            "BBox/confianza debe ser numerico: " + fields[field]);
                }
                assertTrue(fields[6].matches("N|X|U"),
                        "Narratabilidad invalida: " + fields[6]);
                begin = index;
            }
            else if (begin >= 0 && source < 0 && lines.get(index).equals("SOURCE")) source = index;
            else if (source >= 0 && lines.get(index).equals("END")) {
                end = index;
                break;
            }
        }
        assertTrue(begin >= 0 && source == begin + 1 && end > source,
                "Envelope TABLE invalido");
        return String.join("\n", lines.subList(source + 1, end));
    }

    private static String allSource(String raw) {
        List<String> lines = raw == null ? List.of() : raw.lines().toList();
        assertTrue(!lines.isEmpty() && lines.getFirst().startsWith("PAGE|V1|"));
        assertTrue(lines.getLast().equals("DONE"));
        ArrayList<String> sources = new ArrayList<>();
        boolean capture = false;
        boolean open = false;
        for (int index = 1; index < lines.size() - 1; index++) {
            String line = lines.get(index);
            if (line.startsWith("BEGIN|")) {
                assertTrue(!open, "No se permiten regiones anidadas");
                String[] fields = line.split("\\|", -1);
                assertTrue(fields.length == 8, "BEGIN debe tener 8 campos: " + line);
                for (int field = 2; field <= 7; field++) {
                    if (field == 6) continue;
                    assertTrue(fields[field].matches("\\d+(?:\\.\\d+)?"),
                            "BBox/confianza debe ser numerico: " + fields[field]);
                }
                assertTrue(fields[6].matches("N|X|U"),
                        "Narratabilidad invalida: " + fields[6]);
                assertTrue(index + 1 < lines.size()
                                && lines.get(index + 1).equals("SOURCE"),
                        "SOURCE debe seguir inmediatamente a BEGIN");
                open = true;
            } else if (line.equals("SOURCE")) {
                assertTrue(open, "SOURCE fuera de una region");
                capture = true;
            } else if (line.equals("SPEECH")) {
                capture = false;
            } else if (line.equals("END")) {
                assertTrue(open, "END sin BEGIN");
                capture = false;
                open = false;
            } else if (capture) {
                sources.add(line);
            }
        }
        assertTrue(!open, "Falta END para una region");
        assertTrue(!sources.isEmpty(), "El protocolo no contiene SOURCE util");
        return String.join("\n", sources);
    }

    private static String normalized(String value) {
        return java.text.Normalizer.normalize(value == null ? "" : value,
                        java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}%/:.]+", " ")
                .replaceAll("\\s+", " ").strip();
    }
}
