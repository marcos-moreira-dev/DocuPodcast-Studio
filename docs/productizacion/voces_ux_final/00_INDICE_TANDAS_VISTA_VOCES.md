# VOZ-TTS5A — Documento con voces/tonos reales

Documento ya no muestra el catálogo global de tonos: filtra voces avanzadas por muestra Neutral y tonos por `registeredTones()` de la voz seleccionada. Documento técnico: `docs/productizacion/VOZ_TTS5A_DOCUMENTO_VOCES_TONOS_REALES.md`.

# Planificación de implementación — Vista Voces UX/UI final

## Propósito

Este paquete documental aterriza la planificación específica de la **Vista Voces** de DocuPodcast Studio. La vista actual funciona como prototipo parcial, pero mezcla gestión de voces, asignación al documento, personajes, estilos y capacidades del motor. La decisión de producto es rehacerla con una UX moderna, limpia, sobria y coherente con el look and feel de la aplicación.

## Decisiones base

- No mostrar la palabra `Coqui` en la UX/UI. En la interfaz se usará **Voz IA avanzada**.
- La voz local simple queda como modo intermedio mínimo, con máxima simplicidad.
- Mock queda como **Modo de prueba**, prácticamente sin opciones.
- La vista Voces no asigna fragmentos. La asignación de voces a fragmentos/oraciones vive en Documento.
- La vista Voces gestiona voces, muestras y pruebas.
- Solo existe una voz neutral prediseñada protegida.
- Todas las demás voces son creadas/importadas/grabadas por el usuario.
- Las emociones/tonos son **muestras de referencia**, no comandos de texto tipo “dilo enojado”.
- Si falta una muestra de tono, se usa el tono neutral de la misma voz y se avisa al usuario.
- Todas las muestras deben poder grabarse, importarse, reproducirse, reemplazarse, descargarse/exportarse y eliminarse.
- Descargar muestra usa `DirectoryChooser` para que el usuario elija carpeta.
- La vista debe usar componentes GUI transversales, no JavaFX estilizado ad hoc.

## Tandas de implementación planificadas

1. `T121-V01 — Deshuesamiento de la vista Voces actual`
2. `T121-V02 — Modelo de dominio para voces, muestras y tonos teatrales`
3. `T121-V03 — Servicios de almacenamiento seguro y descarga de muestras`
4. `T121-V04 — Wizard de registro de voz avanzada`
5. `T121-V05 — Prueba generada con frase editable`
6. `T121-V06 — Voz local simple mínima`
7. `T121-V07 — Fallback de tonos faltantes en Documento`
8. `T121-V08 — Rediseño visual final de VoiceLibraryWorkspaceView`
9. `T121-V09 — CSS/componentes transversales de Voces`
10. `T121-V10 — Tests, documentación y smoke visual de Voces`

## Archivos incluidos

- `T121_VISTA_VOCES_PLAN_MAESTRO.md`
- `T121_V01_DESHUESAMIENTO_VISTA_VOCES_ACTUAL.md`
- `T121_V02_MODELO_VOCES_MUESTRAS_TONOS.md`
- `T121_V03_ALMACENAMIENTO_DESCARGA_MUESTRAS.md`
- `T121_V04_WIZARD_REGISTRO_VOZ_AVANZADA.md`
- `T121_V05_PRUEBA_GENERADA_FRASE_EDITABLE.md`
- `T121_V06_PIPER_MODO_MINIMO.md` (documento histórico de planificación; UX visible usa Voz local simple)
- `T121_V07_FALLBACK_TONOS_FALTANTES_DOCUMENTO.md`
- `T121_V08_REDISHENO_VISUAL_FINAL_VOCES.md`
- `T121_V09_COMPONENTES_CSS_VOCES.md`
- `T121_V10_TESTS_DOCUMENTACION_SMOKE_VOCES.md`


## Ajuste agregado — Frase guía por tono

Cada tono del catálogo teatral extendido debe tener una frase guía por defecto para grabación. Al grabar, la aplicación muestra esa frase, el usuario la lee, y luego puede cancelar, detener o guardar la muestra. Esto aplica a Neutral y a todos los tonos del catálogo.


## Estado actualizado

- T121-V05 — implementada: prueba generada con frase editable, fallback neutral y cache de prueba en proyecto.

- T121-V10 — implementada: tests, documentación y smoke visual final de Voces.

## Alineación VOZ-UX4R-DOC1 — simplificación final

La planificación histórica T121 se mantiene como antecedente, pero el contrato operativo vigente para las próximas tandas se simplifica a tres módulos principales:

1. **Inicio** — lista sobria de voces registradas, estado y emociones disponibles. Acciones rápidas: editar y eliminar.
2. **Configurar motor** — selector de motor, selector CPU/GPU real para todos los motores, estado del motor y textbox de prueba.
3. **Gestionar voces** — crear/editar/eliminar voces, importar/grabar/reemplazar muestras por emoción, reproducir y exportar muestras.

Reglas añadidas por decisión de producto:

- No usar tarjetas futuristas ni diseño web decorativo; la vista debe ser administrativa y sobria.
- Eliminar voz exige confirmación indicando que también se eliminarán los archivos/muestras asociados.
- En Gestionar voces debe poder seleccionarse voz + emoción para reemplazar esa emoción importando o grabando de nuevo.
- El selector de dispositivo aplica a Voz IA avanzada y Voz local simple, con detección real de CPU/GPU.
- El catálogo debe soportar muchas emociones; Neutral es obligatoria y las demás son muestras de referencia.
- Documento solo muestra voces con Neutral y emociones registradas para esa voz.

Documento rector nuevo: `docs/productizacion/VOZ_UX4R_CONTRATO_FINAL_MODULOS.md`.

## Estado VOZ-UX4R-2A

Implementada la primera tanda de deshuesamiento visible: `VoiceLibraryWorkspaceView` pasa a un shell modular de tres módulos (**Inicio**, **Configurar motor**, **Gestionar voces**) con navegación lateral sobria y sin `SplitPane`. La lógica profunda de motor/dispositivo y gestión completa de voces queda para VOZ-UX4R-2B y VOZ-UX4R-3.

Documento técnico: `docs/productizacion/VOZ_UX4R_2A_SHELL_MODULAR_VOCES.md`.

## VOZ-UX4R-2B — Configurar motor real dentro de Voces

Se completa el módulo Configurar motor de la microaplicación Voces con selector de motor activo, selector de dispositivo de renderizado CPU/GPU detectado, estado honesto y prueba por textbox. Las selecciones se persisten en `OperationalSettings`, la misma configuración interna usada por Configuración. No se resuelve todavía la descarga de Voz IA avanzada/Coqui; queda para `MOTOR-SMOKE4R / COQUI-DL1`.

## VOZ-UX4R-3B — navegación modular sobria de Voces

Corrige la navegación interna de Vista Voces: los módulos Inicio, Configurar motor y Gestionar voces cambian correctamente sin que el refresco de selección devuelva la vista a Gestionar voces. El sidebar ahora muestra solo el nombre de cada módulo, con estilo administrativo sobrio tipo escritorio/Teams, sin degradados ni descripciones dentro del botón.

## VOZ-UX4R-3C — sidebar oscuro y anti-dashboard

Implementada sobre VOZ-UX4R-3B: Inicio de Voces ya no usa métricas/tarjetas de dashboard; Configurar motor usa filas sobrias con `InfoBadge`; el sidebar queda oscuro, full-height y sin gradientes. Documento técnico: `docs/productizacion/VOZ_UX4R_3C_SIDEBAR_OSCURO_ANTI_DASHBOARD.md`.

