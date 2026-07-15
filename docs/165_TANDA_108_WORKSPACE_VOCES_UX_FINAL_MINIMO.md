# Tanda 108 — Workspace Voces final mínimo con criterio UX/UI

## Objetivo

Convertir la vista Voces en una biblioteca usable y honesta, no en un panel técnico de capacidades. La regla de producto queda:

```text
Voces administra voces.
Documento asigna voces.
Configuración prepara motores.
```

## Cambios principales

- `VoiceLibraryWorkspaceView` reorganiza la experiencia como biblioteca: lista de voces a la izquierda, detalle de voz seleccionada en el centro y acciones de muestra/registro en una zona clara.
- La UI deja de dominar la pantalla con IDs técnicos (`VOC-*`, `STY-*`, `CHR-*`) y prioriza nombres humanos como `Narrador`, `Mi voz` y estilos visibles.
- Se mantiene la sección `Asignar al segmento seleccionado`, pero queda contextualizada como atajo; la asignación principal pertenece al sidebar del Documento.
- Se agrega frase sugerida de grabación para orientar la captura de muestra propia/autorizada.
- Se refuerza el lenguaje honesto: muestra de referencia no implica clonación ni síntesis real si el motor no lo soporta.
- Se agregan estilos CSS específicos para lista de voces, detalle seleccionado y frase de referencia.

## Hotfix incluido

La tanda incorpora el ajuste posterior a T110R-HF1:

- La búsqueda de la guía vuelve a encontrar `reintentar`.
- El issue DOCX de imagen sin descripción conserva un texto compatible con tests existentes.
- El test DOCX sintético incluye una entrada `word/media/image1.png` para validar `embeddedImageBase64` de forma coherente.

## Límites explícitos

Esta tanda no implementa clonación real de voz ni creación avanzada de perfiles personalizados. La biblioteca queda preparada y más usable, pero la síntesis real sigue dependiendo del motor configurado.
