# Tanda 19 — Validación fuerte del proyecto y assets

## Objetivo

Evitar que DocuPodcast Studio abra proyectos incompletos, corruptos o inconsistentes como si fueran válidos.

La Tanda 18 logró rehidratar `document/document.json`, `script/narration-script.json` y `storyboard/storyboard.json`. La Tanda 19 agrega la capa que faltaba: validar que lo declarado por `.docupodcast.json` existe físicamente y puede sostener una sesión de trabajo real.

## Cambios principales

```text
ValidateProjectWorkspaceIntegrityUseCase
ProjectApplicationServices.validateProjectWorkspaceIntegrity()
DocuPodcastShellViewModel.openProject(...) valida integridad después de rehidratar
ValidateProjectWorkspaceIntegrityUseCaseTest
```

## Contrato de producto

Al abrir un proyecto:

```text
1. Se lee .docupodcast.json.
2. Se valida el payload declarado por tipo de proyecto.
3. Se rehidratan artefactos materializados.
4. Se valida integridad física y semántica.
5. Solo entonces se crea la sesión activa.
```

## Validaciones nuevas

```text
Cada asset registrado debe existir físicamente.
Cada asset debe apuntar a un archivo regular.
La ruta resuelta debe permanecer dentro de la carpeta del proyecto.
DOCUMENT_ONLY debe rehidratar documento importado si declara documento.
NARRATION_SCRIPT/FULL_PROJECT deben rehidratar guion narrable.
STORYBOARD/FULL_PROJECT deben rehidratar storyboard si lo declaran.
AUDIO_PROJECT debe tener al menos clip, audio final o manifest de audio.
Storyboard debe referenciar segmentos existentes en el guion.
Storyboard debe referenciar imágenes registradas como assets.
```

## Límites explícitos

Esta tanda no implementa todavía:

```text
hash SHA-256 de assets
validación profunda de codecs/audio
validación de app-image/MSI
concatenación real de podcast final
```

Esos puntos quedan para tandas posteriores de exportación, audio y release candidate.
