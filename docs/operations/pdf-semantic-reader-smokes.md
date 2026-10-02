# Smokes del lector PDF semántico

Son opt-in: `mvn test` no requiere modelos, runtimes ni archivos externos. Ejecute desde la raíz con Java 21 y assets locales de Ollama/Qwen; FAST_LISTEN requiere además Piper.

## Corpus final

Coloque los ocho PDF con sus nombres originales en una carpeta. El harness selecciona 12 páginas y escribe en `target/pdf-final-acceptance`.

```powershell
mvn -pl :docupodcast-studio -am `
  -Ddocupodcast.pdf.finalAcceptance=true `
  -Ddocupodcast.pdf.finalAcceptance.root=D:\Proyectos\g `
  -Ddocupodcast.pdf.finalAcceptance.corpusDir=D:\Corpus\pdf-final `
  -Dtest=PdfFinalAcceptancePhysicalTest `
  -Dsurefire.failIfNoSpecifiedTests=false test
```

`-Ddocupodcast.pdf.finalAcceptance.case=demo-p1` limita un caso; `-Ddocupodcast.pdf.finalAcceptance.inventoryOnly=true` captura evidencia nativa sin Qwen. Criterio: cero omisiones materiales silenciosas en aceptadas y preservación de la página previa en rechazadas. Baseline: 12 casos, 4 aceptados seguros, 8 rechazados seguros; 3 primarios y 1 tras verificador.

## Recuperación ROI

```powershell
mvn -pl :studio-local-media-adapters -am `
  -Ddocupodcast.semanticRecovery.physical=true `
  -Ddocupodcast.semanticRecovery.root=D:\Proyectos\g `
  -Ddocupodcast.semanticRecovery.tableImage=D:\Corpus\roi\tabla-densa.png `
  -Ddocupodcast.semanticRecovery.multicolumnImage=D:\Corpus\roi\multicolumna.png `
  -Dtest=SemanticRecoveryPhysicalSmokeTest `
  -Dsurefire.failIfNoSpecifiedTests=false test
```

La multicolumna es opcional. Evidencia: `target/semantic-recovery-physical`. Debe conservar celdas/columnas en `SOURCE`, sin resumen sustitutivo, y terminar en `DONE`.

## FAST_LISTEN

```powershell
mvn -pl :studio-local-media-adapters -am `
  -Ddocupodcast.fastListen.physical=true `
  -Ddocupodcast.fastListen.root=D:\Proyectos\g `
  -Ddocupodcast.fastListen.page1=D:\Corpus\pages\page-01.png `
  -Ddocupodcast.fastListen.page2=D:\Corpus\pages\page-02.png `
  -Dtest=FastListenPhysicalSmokeTest `
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Evidencia: `target/fast-listen-physical`. Debe haber Block V1 válido, WAV Piper reproducible y solapamiento permitido entre audio y preparación siguiente.

`DONE` ausente, protocolo inválido o cobertura insuficiente implican rechazo, nunca aceptación parcial. Backend, timeout, OOM y cancelación son fallos diferenciados. No repita una truncación con parámetros idénticos. Conserve raw, reportes, métricas y logs de cualquier regresión.

Los experimentos de concurrencia=2 y JSON fueron retirados y no son procedimientos de release. Consulte el [handoff histórico](docupodcast-qwen-reader-handoff-2026-08-09.md).
