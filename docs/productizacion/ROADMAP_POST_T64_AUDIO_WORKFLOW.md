# Roadmap post T64 — audio, capas y cierre funcional

## T65 — NarrativeLayerCoordinator

Extraer del shell las reglas de capas sobre Documento narrable: voz, audio humano, emoción, imagen, ambiente y notas. Debe validar conflictos, reemplazos y targets reales.

## T66 — Round-trip funcional real

Probar abrir documento, escuchar, generar audio, asignar capas, asociar imágenes, guardar, cerrar, reabrir y reproducir sin pérdida.

## T67 — Configuración operativa

Persistir y probar configuración de TTS, STT, FFmpeg, buffer, rutas, modelos y diagnóstico.

## T68 — Rediseño UI aplicado

Aplicar la cara simplificada: Documento primero, lectura cómoda, menos métricas técnicas visibles y complejidad progresiva.

## T69 — Storyboard/video operativo

Asegurar que la imagen asociada a un texto dure lo que dura el audio de ese texto. Mantener paquete renderizable honesto o conectar MP4 real con FFmpeg.

## T70 — Smoke integral

Ejecutar matriz manual con documentos simples, largos, técnicos, diálogos, errores, recuperación y evidencias.

## T71 — Packaging/RC

Preparar app-image/MSI, hashes, manifiestos, licencias, guía final y limitaciones conocidas.
