# Escuchar documento end-to-end — T72

## Brújula

DocuPodcast Studio V1 debe sentirse como un lector narrado. La acción principal no es “crear guion”, “generar job” ni “abrir manifest”; la acción principal es **Escuchar documento**.

Cadena oficial:

```text
Documento narrable → narración interna → audio/buffer → playback
```

## Estados de decisión

`PrepareDocumentListeningUseCase` define un plan explícito para el botón principal:

| Fase | Significado | Respuesta de producto |
|---|---|---|
| `NO_DOCUMENT` | No hay fuente cargada | Pedir abrir documento. |
| `NO_NARRATABLE_TEXT` | Hay documento pero no texto narrable | Pedir revisar perfil/clasificación. |
| `BUILD_NARRATION_PROJECTION` | Falta proyección interna | Crear narración interna desde el Documento narrable. |
| `PLAY_EXISTING_AUDIO` | Ya existe manifest/audio | Reproducir desde Documento. |
| `WAIT_FOR_AUDIO_BUFFER` | Audio en generación | Mantener lectura y esperar buffer. |
| `SAVE_PROJECT_REQUIRED` | Falta ruta de proyecto para jobs/assets | Pedir guardar proyecto. |
| `GENERATE_AUDIO` | Todo listo para generar | Lanzar audio por segmentos. |

## Qué NO cambia

- No se edita la fuente DOCX/PDF/MD/TXT.
- No se obliga al usuario normal a abrir Narración avanzada.
- No se promete OCR para PDF escaneado.
- No se presenta el buffer como concepto principal de producto.

## Próxima deuda

T73 debe robustecer audio jobs: reusar segmentos existentes, reanudar incompletos, limpiar fallidos, marcar obsolescencia cuando cambia la fuente y reportar diagnósticos de motor.
