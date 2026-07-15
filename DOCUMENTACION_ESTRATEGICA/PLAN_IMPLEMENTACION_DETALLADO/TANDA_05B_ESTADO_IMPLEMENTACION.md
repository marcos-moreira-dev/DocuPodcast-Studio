# Tanda 5B — Estado de implementación

## Resultado

Implementada.

## Alcance cerrado

- Reading Profile persistible en `.docupodcast.json`.
- Editor visible en el SideDock del Documento.
- Previsualización de impacto no destructiva.
- Guardar perfil en proyecto.
- Guardar y aplicar al documento.
- Snapshot `document/document.json` incluye perfil activo.

## Validación

- Compilación parcial `javac --release 21`: domain/application/infrastructure.
- Smokes manuales de JSON root + document snapshot.
- Tests fuente/unitarios agregados para preview, persistencia y panel.

## Limitaciones

- La UI JavaFX completa debe validarse localmente con Maven Toolchain.
- Las reglas aún son texto/keywords; no hay UI avanzada por tamaño de fuente porque el importador DOCX aún no expone tamaño.
- El perfil no tiene todavía presets múltiples por proyecto; solo perfil activo.

## Siguiente tanda

Tanda 6 — Guion narrable.
