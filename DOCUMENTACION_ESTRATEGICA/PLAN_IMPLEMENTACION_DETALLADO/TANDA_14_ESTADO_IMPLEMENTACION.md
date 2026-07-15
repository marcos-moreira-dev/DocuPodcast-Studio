# Tanda 14 — Estado de implementación

Estado: IMPLEMENTADA.

## Alcance entregado

- Guía integrada con contenidos y búsqueda.
- Tema Word-first.
- Exportación de recursos IA.
- Índice `00_indice_recursos_ia.md` generado por exportador.
- Recursos oficiales: gramática, plantilla, prompt, ejemplos y referencia ética de voces.
- Acciones UI en menú Ayuda y toolbar.
- Tests fuente y unitarios de catálogo/exportación.

## Límites conscientes

- La guía muestra Markdown como texto plano, no como renderer enriquecido completo.
- Los ejemplos importables todavía no tienen parser Markdown completo de guion conectado en esta tanda.
- La ayuda operativa por SideDock queda parcial; los temas oficiales ya cubren el contenido base.

## Validación esperada

Ejecutar:

```bat
scripts\00-verificar-entorno.bat
scripts\03-verificar-toolchain.bat
scripts\02-ejecutar-tests.bat
scripts\01-ejecutar-app.bat
```

Smoke manual:

1. Abrir Ayuda > Guía.
2. Buscar “Word”.
3. Abrir tema “Importar notas desde Word”.
4. Exportar recursos IA a una carpeta temporal.
5. Verificar `00_indice_recursos_ia.md` y ejemplos exportados.
