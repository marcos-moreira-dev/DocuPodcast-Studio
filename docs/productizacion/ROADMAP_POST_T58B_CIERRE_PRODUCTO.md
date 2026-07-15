# Roadmap post T58B — cierre seguro de producto

## Contexto

Tanda 58B dejó la base verde según validación local del usuario. El siguiente paso no debe ser un refactor grande inmediato: primero hay que ejecutar un smoke exploratorio mínimo para saber si las capacidades básicas funcionan como usuario.

## Orden recomendado

| Orden | Tanda | Propósito |
|---:|---|---|
| 1 | T58C — Smoke exploratorio mínimo | Probar abrir Word, escuchar, guardar y reabrir antes de rediseñar. |
| 2 | T59 — Criterios de diseño/scaffolding | Definir qué ve el usuario normal y qué va a Avanzado/Diagnóstico. |
| 3 | T59A — Componentes GUI transversales | Evitar botones, cards, headers, rails y paneles hardcodeados por workspace. |
| 4 | T59B — Limpieza UX Documento | Hacer que Documento se sienta como lector narrado, no cabina técnica. |
| 5 | T60 — Refactor coordinadores | Reducir `DocuPodcastShellViewModel` con coordinadores por flujo. |
| 6 | T61 — Round-trip real | Guardar/cerrar/reabrir con voces, capas, imágenes, storyboard, audio y jobs. |
| 7 | T62 — Configuración operativa | Persistir y probar motores, modelos, buffer, STT, FFmpeg y diagnóstico. |
| 8 | T63 — Smoke real completo | Probar documentos cortos, largos, técnicos, teatro, audio, STT y video. |
| 9 | T64 — Packaging / RC | App-image/MSI, hashes, manifiestos, FFmpeg, guías y limitaciones. |

## Regla de decisión después de T58C

```text
Si falla lo básico: corregir funcionalidad.
Si funciona pero confunde: rediseñar scaffolding/UX.
Si funciona y se entiende: avanzar a auditoría GUI y refactor.
```
