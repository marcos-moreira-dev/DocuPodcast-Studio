# Tanda 48 — Documentación maestra de continuidad, producto y pendientes

## Propósito

Esta tanda no introduce funcionalidad nueva. Su objetivo es dejar registrada la brújula del producto, el estado acumulado de las tandas, las decisiones visuales y las características pendientes para que el proyecto pueda continuar aunque se pierda el contexto de chat.

## Brújula vigente del producto

DocuPodcast Studio debe sentirse como un **lector narrado de documentos Word/DOCX**, no como una cabina técnica. El usuario principal no es un profesional de audio, video, IA o desarrollo; es una persona que sabe usar ofimática básica, abre documentos y quiere leerlos/escucharlos con menos esfuerzo.

La experiencia principal debe ser:

```text
Abrir Word/DOCX
→ ver el documento como una página legible
→ pulsar Escuchar documento
→ escuchar por oraciones o fragmentos
→ ver resaltada la oración activa
→ ajustar voz/audio/imagen solo si lo necesita
→ exportar podcast, evidencia o video simple cuando corresponda
```

La infraestructura interna puede ser compleja, pero debe permanecer detrás de una operación simple. La pantalla principal es la zona pública del supermercado; configuración, motores, modelos, jobs, diagnósticos y exportaciones técnicas son la bodega.

## Principios que no se deben romper

1. **Todo importable se trata primero como documento de trabajo.** Puede ser una obra de teatro, un informe técnico, un ensayo, apuntes de estudio o texto creativo. No se debe forzar al usuario a elegir “guion” vs “documento técnico” al inicio.
2. **El documento fuente es inmutable en V1.** Word/DOCX, PDF, Markdown/MD y TXT se abren en modo solo lectura. Las voces, audios, emociones, imágenes, notas, proyección de narración y asociaciones viven como artefactos del proyecto `.docupodcast.json`, no se escriben dentro del archivo fuente.
3. **Documento es la pantalla operativa.** Guion, Audio, Voces, Storyboard y Configuración pueden existir como vistas avanzadas, pero no deben dominar el flujo normal.
4. **Acción principal contextual.** Un solo botón puede decir `Escuchar documento`, `Reproducir desde aquí` o una variante similar según selección y estado. No llenar la pantalla con botones redundantes.
5. **Mini rail plegable.** Imágenes, audios, voces y asignaciones deben vivir en un rail lateral compacto, plegable y retraíble, estilo PowerPoint/editor de video, no en un panel gigante fijo.
6. **Configuración separada.** Los motores TTS/STT, modelos, buffers, rendimiento, rutas, logs y diagnóstico viven en Configuración.
7. **Arquitectura visual transversal.** Usar JavaFX nativo está permitido, pero las piezas repetibles deben ser componentes GUI propios y CSS modular.
8. **No paneles por paneles.** No anidar panel dentro de panel dentro de panel sin necesidad visual o funcional. Si un contenedor no aporta estructura o semántica, no debe existir.
9. **Estética formal moderna.** Abandonar el estilo Windows XP/JavaFX crudo. Inspirarse en aplicaciones sobrias como Teams, MuseScore, Subtitle Edit, Domain Model Studio y Fractal, sin copiar ni convertir la UI en un circo visual.
10. **Usuario no técnico.** Evitar términos como job, manifest, checksum, gateway, TTS command o pipeline en la pantalla principal. Esos términos pueden aparecer en Configuración o Diagnóstico avanzado.

## Estado implementado relevante

La base actual ya contiene:

```text
namespace com.marcosmoreiradev.docupodcaststudio
proyecto .docupodcast.json
importación Word/DOCX
ReadableDocument y DocumentBlock
selección exacta por oración mediante DocumentTextRange / DocumentSentenceSpan
capas narrativas persistibles en narrativeLayers.assignments
VoiceLibrary inicial
motor mock
TTS por proceso configurable
STT/Whisper por proceso configurable
jobs persistidos/reanudables
playback sincronizado
seguimiento visual del bloque activo
prebuffer conceptual por chunks
mini rail plegable
mini storyboard integrado con miniaturas
configuración guiada de motores/modelos
exportación de podcast WAV final
exportación de paquete auditable
exportación de video simple como paquete/plan, no MP4 final
componentes GUI transversales iniciales en presentation.components
CSS modular inicial con docupodcast-light.css como ensamblador
```

## Lo que falta para que sea producto usable

El producto todavía no está terminado. Las piezas internas están avanzadas, pero falta convertirlas en una experiencia coherente y visible. Las tandas pendientes deben centrarse en:

```text
modernización visual real
pantalla Documento realmente operativa
flujo Word → escuchar de punta a punta
selección/asignación desde UI sin fricción
mini rail multimedia realmente usable
motor de voz guiado y verificable
streaming/prebuffer robusto en documentos largos
exportación MP4 opcional con FFmpeg o paquete preparado
round-trip completo validado desde uso real
refactor de coordinadores para no hacer crecer el ViewModel
release candidate instalable
```

## Regla para nuevas tandas

Cada tanda nueva debe declarar explícitamente:

```text
qué mejora ve el usuario normal
qué queda en Configuración/Diagnóstico
qué componentes GUI reutiliza o crea
qué CSS modular toca
qué no se debe romper
qué tests o guardarraíles agrega
```

Si una tanda no mejora la experiencia principal o no reduce deuda estructural, debe justificarse antes de implementarse.
