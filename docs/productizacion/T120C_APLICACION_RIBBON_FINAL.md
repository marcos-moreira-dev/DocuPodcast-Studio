# T120C — Aplicación del Ribbon final

## Objetivo

Aplicar el contrato final del Ribbon definido en T120B y cerrar detalles de lenguaje, superficies y disponibilidad para que el Ribbon quede organizado por intención de usuario, no por módulos técnicos heredados.

## Reglas de producto

- Inicio concentra arranque rápido: abrir fuente, escuchar documento, reproducir selección y proyecto.
- Lectura concentra preparación/generación/cancelación de audio.
- Vista concentra navegación principal: Documento y Voces, más panel visual, ventana y soporte.
- Exportar concentra audio, paquete, video simple, estado y carpeta de exportaciones.
- Storyboard no es pestaña principal ni módulo visible. La capacidad visual vive como panel/rail del Documento.
- Voces aparece como vista principal secundaria y se etiqueta como biblioteca/gestión de voces, no como “personajes”.
- Preparar no debe decir “Preparar audio” si realmente prepara la lectura del documento antes de generar audio.
- Cancelar debe decir “Cancelar generación” para referirse al job activo.

## Cambios realizados

- `AppCommandRegistry` cambia `PREPARE_DOCUMENT_READING` a “Preparar lectura”.
- La descripción del comando deja de decir “narración interna necesaria” y pasa a “Preparar la lectura del documento antes de generar audio”.
- `OPEN_VOICE_LIBRARY` pasa de “Voces y personajes” a “Biblioteca de voces”.
- `CANCEL_AUDIO_JOB` pasa de “Cancelar audio” a “Cancelar generación”.
- `CREATE_STORYBOARD` deja de permitirse en Ribbon; queda como comando legacy/toolbar/menu para compatibilidad técnica.
- `OPEN_STORYBOARD` deja de declarar Ribbon como superficie permitida.
- `WorkspaceToolbarActionProvider` usa “Preparar lectura” en vez de “Preparar audio”.
- Se agrega `RibbonFinalApplicationT120CSourceTest` para proteger el contrato.

## Criterios de aceptación

- `RibbonView` no contiene botón ni pestaña Storyboard.
- El Ribbon mantiene solo Inicio, Lectura, Vista y Exportar.
- Vista incluye Documento y Voces.
- `CREATE_STORYBOARD` no está permitido en Ribbon.
- Los labels finales no usan “Preparar audio” ni “Voces y personajes”.
- Las superficies visuales siguen invocando `AppCommandDispatcher`.

## Próxima tanda

T121 — Auditoría UX/UI Voces contigo.
