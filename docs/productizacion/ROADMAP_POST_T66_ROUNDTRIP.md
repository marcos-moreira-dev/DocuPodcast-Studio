# Roadmap posterior a T66 — Cerebro, configuración y cierre funcional

## Base actual

T66 agrega un round-trip funcional ejecutable. Esto reduce el riesgo de seguir refactorizando a ciegas: ya existe una prueba de cerebro para guardar, cerrar y reabrir los artefactos principales.

## Próximas tandas recomendadas

1. **T67 — Configuración operativa real**  
   Persistir y probar TTS, STT, FFmpeg, buffer, rutas, modelos y diagnóstico.

2. **T68 — Rediseño UI aplicado sobre Documento**  
   Reducir scaffolding visible y acercar Documento a un lector Word narrado sin convertirlo en procesador de texto completo.

3. **T69 — Storyboard/video operativo**  
   Asegurar que imagen asociada a texto dure lo que dura el texto hablado. Conectar paquete renderizable o MP4 real según disponibilidad de FFmpeg.

4. **T70 — Smoke integral de usuario**  
   Ejecutar matriz manual con DOCX simple, documento largo, tablas/imágenes, diálogo/teatro, errores, cancelación y reapertura.

5. **T71 — Packaging/RC**  
   App-image/MSI, hashes, manifiestos, licencias, guías y limitaciones conocidas.

## Guardarraíl

No avanzar a RC sin round-trip verde, configuración mínima operativa y smoke integral documentado.
