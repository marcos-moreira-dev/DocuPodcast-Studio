# T120B — Contrato final del Ribbon y navegación principal

## Objetivo

Dejar fijado en código y documentación el contrato del Ribbon por intención del usuario, no por módulos técnicos heredados. La navegación principal de DocuPodcast Studio queda centrada en Inicio, Lectura, Vista y Exportar.

## Decisiones aplicadas

- Inicio concentra arranque rápido: abrir documento, escuchar documento/selección y proyecto.
- Lectura concentra preparación y generación de audio: preparar lectura, generar audio y cancelar generación.
- Vista concentra navegación principal y paneles: Documento/Lector, Voces, panel visual, pantalla completa, configuración y guía.
- Exportar concentra salidas: audio, paquete, video simple, estado y carpeta de exportaciones.
- Storyboard deja de ser pestaña principal del Ribbon.
- El rail visual vive como panel contextual del Documento, no como módulo separado.
- Voces pasa a estar en Vista como superficie principal secundaria.

## Cambios implementados

- Agregado `AppCommandId.OPEN_DOCUMENT_READER`.
- Registrado comando `OPEN_DOCUMENT_READER` en `AppCommandRegistry`.
- Registrado handler en `DocuPodcastShellView` para volver al lector principal.
- `RibbonView` elimina la pestaña `Storyboard`.
- `RibbonView` agrega en Vista el grupo `Vistas principales` con Documento y Voces.
- `RibbonView` mueve el rail derecho a `Panel visual`.
- `RibbonView` renombra el grupo de exportación de video a `Video`.
- Inicio agrega reproducción de selección como acción secundaria de escucha rápida.
- Lectura queda enfocada en preparación/generación/cancelación.
- Welcome mantiene “Preparar voz” sin vender Configuración como tarjeta operativa.

## Guardarraíles

- No debe volver `tab("storyboard", "Storyboard")`.
- No debe aparecer `Storyboard` como pestaña principal.
- Vista debe contener Documento y Voces.
- Panel visual debe ser el camino para rail/visuales.
- Configuración puede estar en Vista/Soporte, pero Inicio no debe vender Configuración como tarjeta operativa.

## Validación focal

Se validaron de forma focal los tests de Ribbon, Welcome, Configuración como superficie y el nuevo `RibbonFinalContractT120BSourceTest`.
