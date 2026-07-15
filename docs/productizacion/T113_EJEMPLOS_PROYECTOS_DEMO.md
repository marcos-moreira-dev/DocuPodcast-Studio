# T113 — Menú Ejemplos y proyectos demo internos

## Objetivo

T113 agrega una entrada de producto para probar DocuPodcast Studio sin depender de archivos externos del usuario.

El flujo no crea un nuevo workspace. Usa una ventana secundaria simple:

1. El usuario abre **Ejemplos > Abrir ejemplo...** o pulsa **Probar ejemplo** en Inicio.
2. La app muestra una ventana secundaria de selección.
3. El usuario elige un ejemplo.
4. La app muestra un aviso explicando que creará un proyecto demo.
5. Se abre un `FileChooser` para elegir nombre y ubicación del `.docupodcast.json`.
6. Se aplica `ProjectContainerPathPolicy` para crear carpeta contenedora.
7. Se copia el Word demo y, si existen, assets visuales internos.
8. Se importa el Word como fuente documental de solo lectura.
9. Se guarda y abre automáticamente el proyecto generado.

## Ejemplos incluidos

### Instinto Creativo

Documento narrativo con imagen embebida. Sirve para probar lectura cómoda, recuperación de imagen DOCX y la regla de bloque visual fuente.

Recurso:

```text
src/main/resources/examples/instinto-creativo/source.docx
```

### Cafe Luna Azul

Documento contable de negocio hipotético con tabla. Sirve para probar tablas como bloque visual fuente y lectura de documento empresarial.

Recurso:

```text
src/main/resources/examples/caso-contable-cafe-luna/source.docx
```

### El vuelo del Tornillo Dorado

Guion teatral cómico corto sobre dos aviadores en un avión antiguo. Incluye tres imágenes PNG internas para storyboard.

Recursos:

```text
src/main/resources/examples/aviadores-comicos/source.docx
src/main/resources/examples/aviadores-comicos/assets/escena-01-hangar.png
src/main/resources/examples/aviadores-comicos/assets/escena-02-cabina.png
src/main/resources/examples/aviadores-comicos/assets/escena-03-aterrizaje.png
```

Las imágenes se copian al proyecto como assets disponibles. En T113 no se asocian automaticamente al texto: el usuario decide qué fragmento usa cada visual, manteniendo la regla de que visual fuente o asset disponible no equivale a storyboard automático.

## Clases agregadas

```text
application.examples.ExampleProjectDescriptor
application.examples.ExampleAssetDescriptor
application.examples.ExampleProjectCatalog
application.examples.ClasspathExampleProjectCatalog
application.examples.CreateExampleProjectUseCase
application.examples.ExampleProjectMaterialization
application.services.ExampleApplicationServices
presentation.examples.ExampleProjectDialog
```

## Superficies

- `AppCommandId.OPEN_EXAMPLE_PROJECT`
- Menú `Ejemplos`
- Acción `Probar ejemplo` en Inicio
- `ExampleProjectDialog` como ventana secundaria

## Reglas de producto

- Ejemplos no es workspace.
- La ventana solo selecciona el tipo de ejemplo y termina.
- La creación usa el flujo normal de proyecto y carpeta contenedora.
- Los DOCX demo viven dentro del programa.
- Los assets demo se copian al proyecto; no dependen de internet.
- Las imágenes del guion teatral quedan disponibles para storyboard, pero no se atan automáticamente a fragmentos.

## Validación local sugerida

```bat
cd C:\Users\MARCOS MOREIRA\Downloads\mo\scripts
.\99-diagnostico-completo.bat
```

Prueba manual:

1. Abrir Inicio.
2. Pulsar **Probar ejemplo** o usar **Ejemplos > Abrir ejemplo...**.
3. Crear el ejemplo **Instinto Creativo** y verificar que abre Documento.
4. Crear el ejemplo **Cafe Luna Azul** y verificar que la tabla aparece como bloque fuente.
5. Crear el ejemplo **El vuelo del Tornillo Dorado** y verificar que las tres imágenes se copian como assets disponibles.

## Actualización DEMO-TEATRO-RC1

El demo teatral `El vuelo del Tornillo Dorado` ahora puede materializar visuales preasignados: el Word sigue siendo texto limpio y las imagenes se copian dentro del proyecto como assets `media/images`, asociadas a fragmentos mediante capas `IMAGE`.

## Actualizacion Demo Aviadores desde manifiesto MD

Para el demo teatral Aviadores, el flujo vigente usa `DirectoryChooser`: el usuario elige una carpeta contenedora y la app crea dentro una subcarpeta `Aviadores Comicos Demo/`. En esa subcarpeta materializa primero `teatro.md`, `source/source.docx` y `assets/`. Despues valida e importa el manifiesto para generar `Aviadores Comicos Demo.docupodcast.json`.

El Word sigue siendo la fuente documental. `teatro.md` es la autoridad para actos, escenas, rangos `texto_inicio/texto_fin`, personajes, voces, objetos, imagenes, placements y mapa espacial. El fallback fijo solo completa campos ausentes y no reemplaza la estructura definida por el manifiesto.
