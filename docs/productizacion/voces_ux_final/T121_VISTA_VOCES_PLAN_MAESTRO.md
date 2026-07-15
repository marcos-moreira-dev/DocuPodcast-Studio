# T121 — Plan maestro de implementación: Vista Voces UX/UI final

## Diagnóstico de la vista actual

La vista actual de Voces tiene base técnica, pero no es producto final. En las capturas revisadas se observan problemas concretos:

- Usa el título **Biblioteca de voces**, pero debajo mezcla asignación desde Documento, personajes, estilos, muestra de voz y capacidades del motor.
- Aparece `Uso desde Documento · Asignar al segmento seleccionado`, lo cual contradice la regla: Documento asigna voces, Voces gestiona voces.
- Sigue usando lenguaje de `segmento`, `personajes/roles` y `estilos de interpretación`, heredado de una concepción anterior.
- La pantalla no distingue bien entre motor avanzado, motor local simple y modo de prueba.
- La vista actual se siente funcionalmente parcial y visualmente placeholder.
- El panel derecho muestra tarjetas de estilos, pero eso debe reemplazarse por muestras de referencia por tono.
- La aplicación debe adaptarse por motor, pero sin mostrar jerga técnica al usuario.

## Decisión central de producto

La experiencia avanzada gira alrededor de la **Voz IA avanzada**. Internamente puede existir Coqui/XTTS, pero en UI no debe aparecer el nombre `Coqui`. Piper se trata con máxima simplicidad.

### Nombres visibles

| Interno/técnico | Visible en UX |
|---|---|
| Coqui/XTTS | Voz IA avanzada |
| Piper | Voz local simple |
| Mock | Modo de prueba |
| Emotion/style | Tono de referencia |
| speaker_wav | Muestra de referencia |

## Qué es la vista Voces

La vista Voces es un módulo especializado para:

- crear una voz;
- importar muestras;
- grabar muestras;
- reproducir muestras;
- reemplazar muestras;
- descargar/exportar muestras;
- eliminar muestras;
- renombrar voces;
- eliminar voces importadas;
- generar una prueba con una frase editable usando la voz seleccionada.

## Qué NO es la vista Voces

La vista Voces no debe:

- asignar voces a fragmentos;
- mostrar `Asignar al segmento seleccionado`;
- gestionar el documento;
- mostrar personajes/roles como núcleo del producto;
- mostrar estilos si no son muestras de referencia reales;
- mostrar `Coqui` en la UX;
- prometer emociones garantizadas;
- mostrar opciones avanzadas si Piper está activo;
- mostrar opciones reales si Mock está activo.

## Contrato de la voz neutral prediseñada

Debe existir una única voz neutral prediseñada, protegida, no eliminable. Como solución provisional, el mismo audio base puede duplicarse/reutilizarse para tonos por defecto:

- neutral;
- feliz;
- enojada;
- triste;
- seria;
- etc.

La UI debe aclarar que son referencias prediseñadas compartidas y que pueden reemplazarse más adelante.

## Catálogo teatral extendido

Se incorporará un catálogo amplio de tonos teatrales. El usuario normal puede usar solo `Neutral`; un profesor de teatro o elenco puede registrar muchos tonos si lo desea.

### Catálogo base

- Neutral
- Feliz
- Triste
- Enojada
- Seria
- Calmada
- Preocupada
- Entusiasmada
- Aburrida

### Catálogo teatral extendido

- Alegre
- Eufórica
- Melancólica
- Nerviosa
- Asustada
- Sorprendida
- Dudosa
- Cansada
- Suplicante
- Autoritaria
- Irónica
- Sarcástica
- Misteriosa
- Solemne
- Heroica
- Dramática
- Tensa
- Apurada
- Confundida
- Tierna
- Fría
- Burlona
- Desconfiada
- Arrepentida
- Vulnerable
- Esperanzada
- Resignada
- Amenazante
- Desafiante
- Insinuante no explícita

Nota de seguridad/UX: evitar etiquetas o textos explícitos. Para teatro puede usarse una etiqueta no explícita como `Seductora` o `Insinuante`, sin contenido gráfico ni erótico. El producto debe mantenerse apto para uso educativo.

## Fallback de tonos faltantes

Si un documento o fragmento solicita un tono que no está grabado para la voz seleccionada:

1. La app muestra un aviso.
2. Informa que esa muestra no existe.
3. Usa el tono neutral de la misma voz.
4. No bloquea al usuario.

Texto sugerido:

```text
La voz “María” no tiene grabada la muestra “Heroica”.
DocuPodcast usará la muestra neutral de María para este fragmento.
```

Puede existir opción `No volver a mostrar para esta voz/tono`.

## Layout visual final

### Columna izquierda

- Selector de motor visible:
  - Voz IA avanzada
  - Voz local simple
  - Modo de prueba
- Lista de voces:
  - Voz neutral prediseñada
  - voces importadas/grabadas
- Acciones:
  - Crear voz
  - Importar muestra
  - Grabar muestra

### Panel derecho

- Detalle de voz seleccionada
- Estado
- Tonos/muestras disponibles
- Acciones por muestra:
  - Grabar
  - Importar
  - Reproducir
  - Reemplazar
  - Descargar
  - Eliminar
- Prueba generada:
  - TextBox con frase editable
  - selector de tono
  - Generar prueba con esta voz
  - Reproducir última prueba

## Orden de implementación

1. Deshuesar la vista actual.
2. Crear modelo de voces, muestras y tonos.
3. Implementar almacenamiento seguro y descarga.
4. Implementar wizard de registro.
5. Implementar prueba generada.
6. Simplificar Piper.
7. Implementar fallback de tonos faltantes.
8. Rediseñar `VoiceLibraryWorkspaceView`.
9. Crear CSS/componentes.
10. Agregar tests, documentación y smoke visual.


## Ajuste agregado — Frase guía por tono

Cada tono del catálogo teatral extendido debe tener una frase guía por defecto para grabación. Al grabar, la aplicación muestra esa frase, el usuario la lee, y luego puede cancelar, detener o guardar la muestra. Esto aplica a Neutral y a todos los tonos del catálogo.
