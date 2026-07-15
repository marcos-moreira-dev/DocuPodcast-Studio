# PF5A — Auditoría local de artefactos de motores

## Objetivo

Iniciar PF5 sin convertirlo en una tanda gigante: antes de descargar o empaquetar binarios/modelos, la app debe saber auditar qué artefactos reales existen, qué falta y qué checksums locales tienen.

## Cambios

- Nuevo `AuditEngineArtifactsUseCase`.
- Nuevos DTO de auditoría:
  - `EngineArtifactFileStatus`.
  - `EngineArtifactAuditReport`.
- Configuración → Motores de voz agrega `Inventario local de motores` con botón `Auditar artefactos locales`.
- La auditoría guarda `target/legal/ENGINE_ARTIFACTS_AUDIT.md`.
- Nuevo `scripts/35-auditar-artefactos-motores.bat` para diagnóstico técnico equivalente desde consola.
- `scripts/16-release-candidate.bat` incluye la auditoría PF5A.

## Alcance real

La auditoría no descarga ni instala nada. Revisa `tools/`, `models/` y `scripts/`, calcula SHA-256 cuando hay archivo concreto y deja acciones recomendadas por artefacto.

## Próximo paso PF5B

Conectar desde Configuración un flujo guiado de preparación por bloque: FFmpeg, Voz IA avanzada, Voz local simple y manifiesto legal, con decisiones explícitas del usuario antes de descargar o importar.
