# Contrato frontal T81F — Toolbar principal con iconos y grupos

## Contrato visual

La toolbar principal de DocuPodcast Studio debe comportarse como una barra de accesos frecuentes, no como un índice de todos los módulos internos. T81F establece que la barra principal solo contiene acciones globales y repetidas:

```text
Documento → Inicio / Abrir documento
Lectura → Escuchar documento
Salida → Exportar
Vista → Pantalla completa
```

## Por qué no se muestran vistas técnicas

El usuario normal no debe decidir entre `Narración avanzada`, `Audio`, `Voces`, `Storyboard` o `Jobs` para escuchar un documento. Esas superficies pueden existir como herramientas avanzadas, pero no deben aparecer como botones de primer nivel.

La regla de producto es:

```text
Lo frecuente va cerca de la hoja.
Lo contextual va en el inspector izquierdo.
Lo visual va en el rail derecho.
Lo técnico va en Configuración o Herramientas avanzadas.
```

## Relación con la barra flotante

La toolbar superior abre/activa acciones globales. La barra flotante dentro de Documento mantiene el control inmediato de lectura:

```text
Escuchar / Pausar / Reanudar / Detener / Refrescar contenido
```

Por tanto, T81F no elimina la barra flotante. La complementa.

## Relación con el menu bar

El menu bar conserva las acciones completas y clásicas:

- Archivo;
- Editar;
- Ver;
- Documento;
- Lectura;
- Herramientas;
- Exportar;
- Configuración;
- Ayuda.

La toolbar no debe duplicar todo el menú. Solo debe anticipar las acciones más usadas.

## Componente transversal

`ToolbarActionButton` concentra la construcción del botón de toolbar con icono, texto y tooltip. Las tandas posteriores no deben crear botones sueltos para toolbar dentro de workspaces.

