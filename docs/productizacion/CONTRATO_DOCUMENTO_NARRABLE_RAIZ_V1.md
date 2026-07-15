# Contrato V1 — Documento narrable como objeto raíz

## Decisión

Para DocuPodcast Studio V1, el objeto padre de experiencia no es el guion, ni el audio, ni el storyboard. El objeto padre es el **Documento narrable**.

```text
Documento fuente solo lectura
→ Documento narrable del proyecto
→ capas opcionales de narración, voz, audio, imagen, storyboard, transcripción y exportación
```

El usuario abre un Word/DOCX, PDF, Markdown/MD o TXT para **leerlo y escucharlo**. La app puede preparar internamente proyecciones técnicas para audio, segmentación, sincronización y exportación, pero esas proyecciones no deben dominar el lenguaje principal del producto.

## Qué significa Documento narrable

El Documento narrable es la representación de trabajo que DocuPodcast crea a partir de una fuente inmutable. Es el centro funcional del proyecto.

Debe contener o relacionar:

- bloques y rangos legibles;
- selección por bloque, oración o rango cuando esté disponible;
- estado de lectura y reproducción;
- capas de voz, audio, emoción/intención, imagen, ambiente y notas;
- referencias a audio generado;
- referencias a imágenes/storyboard;
- diagnóstico de importación separado de la lectura normal;
- metadatos necesarios para guardar, reabrir y exportar.

## Papel del guion

En V1, el guion no debe presentarse como un segundo producto padre obligatorio. Debe entenderse como una **proyección interna o avanzada de narración** derivada del Documento narrable.

Puede existir porque sirve para:

- segmentar texto para TTS/audio;
- conservar compatibilidad con `NarrationScriptDocument` y `docupodcast-script-v1`;
- importar/exportar un formato avanzado cuando el usuario lo necesite;
- depurar o editar narración en modo avanzado.

Pero para el usuario normal, el flujo no debe ser “Word y después guion”. Debe ser:

```text
Abrir documento → leer/escuchar → ajustar capas opcionales → exportar
```

## Papel del storyboard

El storyboard es opcional. No reemplaza al Documento narrable.

Su regla V1 es:

```text
Una imagen puede asociarse a uno o varios rangos/segmentos del Documento narrable.
Al renderizar video, la imagen dura lo que dure la lectura hablada del texto asociado.
```

Si una misma imagen se usa en varios textos, el asset debe reutilizarse; no debe duplicarse físicamente por cada uso.

## Tipos de fuente preparados

La experiencia de apertura debe prepararse para distinguir perfiles de documento:

| Fuente | Estado V1 | Observación |
|---|---|---|
| Word/DOCX | Prioritario | Lectura principal, estilos y estructura cuando sea posible. |
| PDF | Preparado como fuente solo lectura | Puede requerir extracción futura por páginas/bloques. |
| Markdown/MD | Fuente solo lectura o importación avanzada separada | Abrir MD como documento no equivale a importar `docupodcast-script-v1`. |
| TXT | Fuente simple solo lectura | Estructura mínima por párrafos/líneas. |

Puede existir una ventana o asistente “Abrir documento” que pregunte el tipo de fuente o detecte automáticamente el perfil. En ambos casos, el destino sigue siendo Documento narrable.

## Regla anti-cabina

La app no debe mostrar al usuario normal conceptos como manifest, gateway, chunk, job, offsets o render package como camino principal.

Esos conceptos pueden existir en Diagnóstico, Configuración, Audio avanzado o Reportes, pero la pantalla principal debe hablar en términos de:

- abrir documento;
- escuchar;
- pausar/reanudar;
- seleccionar texto;
- asociar voz, audio o imagen;
- exportar.

## Impacto sobre arquitectura

La auditoría del cerebro debe revisar que los flujos internos apunten al Documento narrable como raíz. Las clases existentes con nombre `Script` pueden conservarse temporalmente por compatibilidad, pero deben quedar clasificadas como proyecciones internas/avanzadas, no como raíz conceptual del producto.
