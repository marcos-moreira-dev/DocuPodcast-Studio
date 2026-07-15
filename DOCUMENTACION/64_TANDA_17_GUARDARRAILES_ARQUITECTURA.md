# Tanda 17 — Guardarraíles de arquitectura y promesas visibles

## Motivo

Después de la Tanda 16C el build local reportado quedó en verde. La siguiente prioridad de producto no era agregar funciones nuevas, sino impedir que la app siguiera creciendo con deuda silenciosa:

```text
- domain no debe depender de application;
- JavaFX debe quedar acotado a UI/bootstrap/app;
- presentation no debe importar adaptadores infrastructure;
- las acciones visibles no deben prometer capacidades productivas incompletas;
- las acciones roadmap deben mostrarse como preparación o pendiente, no como botón real.
```

## Cambios aplicados

### 1. Mapeo de audio fuera del dominio

Se retiró de `AudioJobSnapshot` la dependencia hacia `AudioJobStatusDto`.

Antes, el dominio conocía un DTO de aplicación mediante:

```text
AudioJobSnapshot.fromStatus(...)
AudioJobSnapshot.toStatusDto(...)
```

Ahora el mapeo vive en:

```text
src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/AudioJobSnapshotMapper.java
```

Esto mantiene `AudioJobSnapshot` como objeto de dominio/persistencia puro y deja la conversión a DTO en capa de aplicación.

### 2. Usos migrados

Se actualizaron los puntos que necesitaban convertir snapshot/status:

```text
MockAudioGenerationGateway
LocalTtsProcessAudioGenerationGateway
DocuPodcastShellViewModel
MockAudioGenerationGatewayPersistenceTest
```

### 3. Guardarraíles de arquitectura

Se agregó:

```text
src/test/java/com/marcosmoreiradev/docupodcaststudio/architecture/ArchitectureBoundaryTest.java
```

Este test fuente protege:

```text
domain no importa application / infrastructure / presentation / JavaFX
application no importa infrastructure / presentation / JavaFX
presentation no importa infrastructure
JavaFX solo aparece en presentation, bootstrap o DocuPodcastStudioApp
```

### 4. Guardarraíl de acciones visibles

Se agregó:

```text
src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/ux/VisibleActionContractSourceTest.java
```

Este test evita que la bienvenida vuelva a presentar `Importar guion Markdown` como acción productiva mientras no exista importador real. También verifica que `Voz IA`, `Grabar voz` y `Audio a texto` sigan enrutadas a handlers de preparación, no a una ejecución falsa.

### 5. Welcome sin falso botón Markdown

La columna izquierda de bienvenida ya no se presenta como `Acciones iniciales` con labels estilo botón. Ahora se muestra como flujo recomendado y declara explícitamente:

```text
Markdown de guion: pendiente de importador real
```

También se retiró el estilo `.welcome-action` y su hover, sustituyéndolo por:

```text
.welcome-step
.welcome-roadmap-note
```

### 6. Test de mapper

Se agregó:

```text
src/test/java/com/marcosmoreiradev/docupodcaststudio/application/audio/AudioJobSnapshotMapperTest.java
```

Valida ida y vuelta básica entre `AudioJobStatusDto` y `AudioJobSnapshot` sin reintroducir dependencia de aplicación dentro del dominio.

## Validación en este entorno

No se ejecutó Maven completo porque `mvn` no está instalado en este entorno.

Sí se validó:

```text
javac --release 21 sobre domain + application
javac --release 21 de tests nuevos con stubs JUnit
revisión estática de boundaries de import
búsqueda de dependencias domain → application
búsqueda de usos obsoletos AudioJobSnapshot.fromStatus/toStatusDto
```

## Validación local requerida

En Windows:

```bat
scripts\02-ejecutar-tests.bat
scripts\01-ejecutar-app.bat
```

Resultado esperado:

```text
BUILD SUCCESS
Tests OK
```

## Alcance

Esta tanda no implementa rehidratación completa, parser Markdown ni motor TTS robusto. Solo deja guardarraíles iniciales y corrige la fuga de capa más evidente antes de seguir madurando el producto.

## Próxima tanda

```text
Tanda 18 — Rehidratación completa del proyecto
```
