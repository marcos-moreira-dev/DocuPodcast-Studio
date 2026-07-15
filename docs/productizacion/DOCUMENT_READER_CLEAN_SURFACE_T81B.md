# Contrato de superficie limpia del Documento — T81B

## Principio

El workspace Documento es la superficie principal del producto. Su función es que una persona pueda abrir un DOCX/PDF/TXT/Markdown, leerlo, escucharlo y seleccionar fragmentos para asignar capas. No es el lugar para mostrar metadatos de importación, nombres de estilos internos, IDs de Word, nombres de perfiles o detalles de clasificación.

## Qué se permite en la hoja central

La hoja central puede mostrar:

- título del documento;
- texto importado;
- tipo humano del bloque: título, subtítulo, párrafo, imagen detectada, tabla detectada;
- selección visual de bloque u oración;
- resaltado del fragmento leído;
- mensajes humanos de estado de lectura.

## Qué no se permite en la hoja central

No deben aparecer como texto visible bajo párrafos:

- `styleId`;
- `styleName`;
- `readingProfile`;
- `classificationSource`;
- `FirstParagraph`;
- IDs internos;
- términos como manifest, job, chunk, gateway o checksum.

Estos datos pueden existir y son útiles para diagnosticar, pero pertenecen a Detalles/Diagnóstico.

## Destino correcto de los metadatos

`DocumentPropertiesPanel` conserva los metadatos del bloque seleccionado. Esta decisión mantiene el valor técnico sin distraer al usuario que está leyendo.

La distribución queda así:

```text
Documento central = lectura y selección
Inspector/Detalles = propiedades y metadatos
Diagnóstico = advertencias de importación
Configuración = ajustes operativos persistentes
```

## Configuración como superficie de menú

T81B también corrige la navegación: Configuración no se anuncia como tarjeta en la bienvenida ni como botón en la toolbar. El acceso oficial es el menú superior.

Esto evita una mala interpretación: la app no debe sugerir que antes de usar el producto hay que pasar por ajustes. La experiencia inicial debe ser abrir documento y escuchar.

## Riesgo evitado

Antes de T81B, el usuario podía ver una hoja con párrafos acompañados por metadatos de clasificación. Eso hacía que DocuPodcast pareciera una herramienta de depuración del importador, no un lector narrado. T81B corta esa fuga visual.
