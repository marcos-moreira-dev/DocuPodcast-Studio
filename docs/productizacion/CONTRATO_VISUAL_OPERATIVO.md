# Contrato visual operativo — DocuPodcast Studio

## Objetivo visual

DocuPodcast Studio debe sentirse como una aplicación de escritorio moderna, formal y tranquila. La interfaz debe estar optimizada para leer y escuchar documentos, no para mostrar toda la fábrica interna.

## Layout principal esperado

```text
┌───────────────────────────────────────────────────────────────┐
│ Menú superior: Archivo, Documento, Reproducción, Configuración │
├───────────────────────────────────────────────────────────────┤
│ Barra compacta: Abrir Word | Escuchar/Reproducir desde aquí    │
├───────────────────────────────┬───────────────────────────────┤
│ Documento renderizado          │ Mini rail plegable             │
│ como página legible            │ imágenes / audio / voces        │
│                                │ asignadas o sin asignar         │
├───────────────────────────────┴───────────────────────────────┤
│ Estado simple: listo, generando, reproduciendo, cargando        │
└───────────────────────────────────────────────────────────────┘
```

El usuario debe poder entender la aplicación sin saber qué es un job, un manifest, un gateway o un modelo TTS.

## Pantalla Documento

Debe mostrar el contenido como documento:

```text
título grande
subtítulos claros
párrafos cómodos
listas legibles
tablas/imagenes representadas sin romper la lectura
oración activa resaltada/subrayada
scroll estable hacia una franja cómoda
```

La letra base del documento debe ser cómoda, no pequeña:

```text
texto normal 18 px aproximado
interlineado amplio
márgenes de lectura
ancho de página controlado
zoom/configuración de lectura en Configuración
```

## Mini rail multimedia

El rail debe ser plegable y retraíble. Debe servir para ver y operar capas sin invadir la lectura.

Debe mostrar:

```text
imágenes asignadas
imágenes sin texto asignado
audios asignados
audios sin texto asignado
voces/personajes asignados
estado del fragmento: listo, pendiente, generando, falló
```

Al hacer clic en un item asignado:

```text
resaltar texto asociado
hacer scroll al texto asociado
mostrar información breve
```

Al hacer clic en un item sin asignar:

```text
mostrar “Sin texto asignado”
ofrecer asociar a selección actual si existe
```

## Componentes GUI transversales

Los componentes reutilizables deben crecer con criterio, no de forma excesiva. Crear componente cuando se repite estructura real, no por cada etiqueta o botón.

Componentes válidos o esperados:

```text
PrimaryActionStrip
EmptyStateView
SectionHeader
SettingsPageView
CollapsibleMediaRail
DocumentPageContainer
DocumentSentenceView
MediaRailItem
StatusChip
EngineSetupCard
ModelInstallStepView
```

Evitar:

```text
paneles anidados sin necesidad
cards dentro de cards solo por estilo
botones con exceso de radius/sombra/color
cada workspace inventando su propia botonera
setStyle() inline
CSS gigante sin modularidad
```

## CSS

`docupodcast-light.css` debe ser ensamblador. Las reglas reales deben vivir por área:

```text
css/tokens.css
css/base.css
css/components/actions.css
css/components/cards.css
css/components/chips.css
css/components/media-rail.css
css/components/settings-shell.css
css/document/document-page.css
css/document/document-sentences.css
css/playback/playback.css
css/workspace/workspace-shell.css
css/compat-legacy.css temporal
```

`compat-legacy.css` debe reducirse progresivamente. No debe convertirse en basurero permanente.

## Look and feel

Inspiración aceptable:

```text
Teams: estructura moderna, sidebar claro, espacios calmados
MuseScore: documento central, seguimiento del elemento activo
Subtitle Edit: herramientas técnicas separadas y claras
Domain Model Studio: toolbar/SideDock ordenados
Fractal Render Studio: separación entre operación, inspector y observabilidad
```

No copiar literalmente. Usar como referencia de claridad, no de identidad visual.
