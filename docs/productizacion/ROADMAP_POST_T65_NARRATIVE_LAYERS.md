# Roadmap posterior a T65 — capas narrativas

## Estado

T65 extrae `NarrativeLayerCoordinator` y baja deuda del shell. Las capas siguen siendo artefactos del proyecto, ancladas al Documento narrable y a la proyección interna de narración.

## Tandas pendientes

### T66 — round-trip funcional real

Probar y endurecer el flujo completo:

```text
abrir documento
seleccionar texto
asignar voz/emoción/audio/imagen
guardar
cerrar
reabrir
confirmar capas/rail/audio/storyboard
```

Debe detectar placeholders y no tratarlos como assets reales.

### T67 — configuración operativa

Persistir y aplicar configuración real de TTS, STT, FFmpeg, buffer, rutas, modelos y diagnóstico.

### T68 — rediseño UI aplicado

Simplificar Documento e Inicio bajo criterios de lector Word narrado, usando el catálogo de componentes transversales sin convertir la app en cabina técnica.

### T69 — storyboard/video operativo

Asegurar que la imagen asociada dure lo que dura el texto hablado y que el contrato de video sea honesto: paquete renderizable o MP4 real según disponibilidad de FFmpeg.

### T70 — smoke integral

Ejecutar matriz manual con documentos simples, largos, técnicos, diálogos, errores, cancelaciones y reaperturas.

### T71 — packaging/RC

App-image/MSI, hashes, manifiestos, licencias, guía de instalación, limitaciones conocidas y checklist final.
