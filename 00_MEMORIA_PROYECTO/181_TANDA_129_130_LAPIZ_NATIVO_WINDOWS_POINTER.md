# Tanda 129-130 - Lapiz nativo oficial y tinta de baja latencia

## Que se implemento

- Se agrego JNA como base para entrada nativa Windows Pointer / Windows Ink:
  - `net.java.dev.jna:jna:5.19.1`
  - `net.java.dev.jna:jna-platform:5.19.1`
- Se actualizaron los modulos Java para permitir el backend nativo:
  - `requires com.sun.jna;`
  - `requires com.sun.jna.platform;`
- Se creo `WindowsPointerInkInputProvider` detras de `InkInputProvider`.
- El proveedor Windows Pointer captura mensajes Win32:
  - `WM_POINTERDOWN`
  - `WM_POINTERUPDATE`
  - `WM_POINTERUP`
  - `WM_POINTERLEAVE`
  - `WM_POINTERCAPTURECHANGED`
- Se consulta informacion del lapiz con Win32:
  - `GetPointerPenInfo`
  - `GetPointerFramePenInfoHistory`
- Se agrego soporte de batches en `InkInputListener` para entregar varios puntos por evento cuando Windows entrega historial coalescido.
- Se ajusto el diagnostico del modal para que no mienta:
  - solo muestra `Entrada: Windows Pointer` cuando llegan paquetes nativos reales;
  - si no llegan, se mantiene como `Entrada: JavaFX mouse` o fallback con razon.
- Se dejo `WintabInkInputProvider` como compatibilidad posterior y placeholder explicito, sin reportarse como nativo real.
- Se actualizo `InkInputProviderFactory` con orden de prioridad:
  - Windows Pointer;
  - Wintab experimental;
  - LectureStudio diagnostico/fallback;
  - JavaFX mouse.
- Se agregaron pruebas fuente/arquitectura para asegurar que la UI de estudio no importe JNA directamente.

## Que quedo fuera

- No se hizo validacion manual con tableta fisica en esta tanda.
- No se implemento backend Wintab real todavia; quedo como compatibilidad v2.
- No se tocaron OCR, PDF, indice, seleccion rectangular ni persistencia principal del proyecto.
- No se cambio `.docupodcast.json`.
- No se toco Domain Model Studio/UENS.

## Decisiones tecnicas

- Windows Pointer es la tecnologia oficial v1 para lapiz nativo en Windows 11.
- JavaFX mouse queda como fallback honesto y visible.
- El proveedor Windows Pointer instala un `WndProc` sobre el `HWND` del `Stage`; si no puede resolverlo o no llegan paquetes reales, no se declara como entrada nativa.
- Los componentes de estudio documental y teatro no importan JNA ni clases Win32 directamente.
- El flujo de input acepta batches para no perder puntos intermedios cuando Windows coalescea eventos.
- Wintab se reserva para la siguiente tanda solo si la tableta del usuario no entrega paquetes por Windows Ink.

## Archivos tocados

- `pom.xml`
- `src/main/java/module-info.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/input/InkInputCapabilities.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/input/InkInputListener.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/input/InkInputProviderFactory.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/input/WindowsPointerInkInputProvider.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/input/WintabInkInputProvider.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemT124InkArchitectureSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemT129T130NativeInkSourceTest.java`

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=TechnicalProblemT129T130NativeInkSourceTest,TechnicalProblemT124InkArchitectureSourceTest" test`
- `mvn -q test`

La suite completa quedo verde. Durante los tests aparecieron advertencias conocidas de PDFBox sobre streams de fixtures PDF, sin fallo de pruebas.

## Proximos pasos exactos

1. T131: validar con tableta real si el modal cambia de `Entrada: JavaFX mouse` a `Entrada: Windows Pointer`.
2. Activar diagnosticos de input para medir eventos/s, puntos/s, presion, borrador, cola y latencia preview.
3. Si Windows Pointer no entrega paquetes en la tableta del usuario, implementar Wintab real via JNA.
4. Si Windows Pointer si entrega mas de 120 puntos/s pero el trazo sigue lento, ajustar el presupuesto del renderer live y aislar tareas PDF durante trazos activos.
5. Verificar con una clase real: PDF grande abierto, problema tecnico, escritura rapida, cambio de color/grosor, borrador y exportacion.
