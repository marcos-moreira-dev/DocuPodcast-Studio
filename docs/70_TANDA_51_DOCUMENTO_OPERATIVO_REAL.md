# Tanda 51 — Documento como pantalla operativa real

## Propósito

Esta tanda consolida el workspace Documento como la pantalla principal de operación de DocuPodcast Studio.

La experiencia visible debe estar pensada para una persona no técnica: abrir un Word/DOCX, leerlo, escucharlo, seleccionar fragmentos y usar capas de voz/audio/imagen solo cuando haga falta. La estructura, propiedades, diagnóstico y ajustes internos siguen existiendo, pero quedan plegados para no dominar la pantalla.

## Decisiones

- El SideDock del Documento inicia plegado por defecto.
- El usuario ve primero el documento y la acción principal, no una cabina técnica.
- La barra lateral compacta sigue disponible para estructura, propiedades, acciones, perfil, diagnóstico y ayuda.
- La página de lectura conserva texto cómodo y superficie tipo documento.
- Las reglas CSS modernas viven en archivos modulares, especialmente `css/document/document-page.css`.

## Cambios principales

- `WorkspaceSideDock` acepta estado inicial colapsado.
- `DocumentWorkspaceView` usa el SideDock colapsado al entrar a Documento.
- El divisor inicial favorece el documento sobre los paneles técnicos.
- Se agrega una nota de pantalla operativa para reforzar el flujo lector.
- Se corrigen tests fuente de CSS después de la modularización de Tanda 50B.

## Guardarraíles

Se agrega `DocumentOperationalWorkspaceSourceTest` para evitar que el workspace Documento vuelva a abrir con un panel técnico dominante por defecto.

## No alcance

No implementa todavía generación de audio real por oración, asignaciones completas desde UI ni renderizado MP4. Esta tanda ordena la pantalla principal para que esas funciones entren sin aumentar la sensación de scaffolding.
