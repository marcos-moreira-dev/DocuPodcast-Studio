<p align="center">
  <img src="packaging/windows/docupodcast-icon-transparent.png" width="180" alt="Logo de DocuPodcast Studio">
</p>

<h1 align="center">DocuPodcast Studio</h1>

<p align="center">
  <strong>Del documento a una experiencia audiovisual. En tu equipo, bajo tu dirección.</strong>
</p>

<p align="center">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white">
  <img alt="JavaFX 21" src="https://img.shields.io/badge/JavaFX-21-1f6feb">
  <img alt="Windows x86-64" src="https://img.shields.io/badge/Windows-x86--64-0078D4?logo=windows11&logoColor=white">
  <img alt="Local first" src="https://img.shields.io/badge/AI-local--first-6f42c1">
  <img alt="Estado: desarrollo activo" src="https://img.shields.io/badge/estado-desarrollo_activo-f0ad4e">
</p>

---

## Un estudio audiovisual en tu equipo

Transforma documentos y guiones en narraciones, ilustraciones y videos con motores locales. DocuPodcast reúne estudio documental, teatro, tinta digital y exportación multimedia, manteniendo los proyectos y sus recursos bajo tu control.

- **Estudio documental:** importa Word y PDF, prepara la lectura y produce audio y video con fondos, mascotas e ilustraciones de IA opcionales.
- **Teatro:** organiza personajes, voces, intervenciones y recursos escénicos en proyectos editables.
- **Ejercicios técnicos:** dibuja y combina tinta, imágenes, formas y texto en un lienzo compartido por el flujo normal y Express.
- **Flujos Express:** convierte documentos por lotes y accede a herramientas de producción directa.
- **IA local configurable:** integra motores de voz, interpretación e imagen mediante contratos compartidos. Los modelos se instalan por separado.
- **Proyectos portables:** conserva la configuración y los medios junto al archivo `.docupodcast.json`.

El producto está en **desarrollo activo**. Las capacidades y limitaciones vigentes se describen en el [estado del producto](docs/product/current-status.md). La generación real depende del hardware y de los motores instalados; las ilustraciones aprobadas pueden reutilizarse en exportaciones posteriores.

## Qué incluye este repositorio

Código, pruebas, documentación, interfaz, workflows, manifiestos y recursos oficiales de ejemplo de teatro y estudio documental. Los ejemplos y sus medios necesarios forman parte del producto.

Los pesos de IA, instalaciones de motores, entornos Python, cachés, diagnósticos y exportaciones locales se mantienen fuera de Git. Las descargas iniciales pueden necesitar Internet; el procesamiento está diseñado para ejecutarse localmente.

## Empezar

Requisitos: Windows x64, Temurin JDK 21, Maven 3.9+ y Toolchains. La configuración exacta y el recorrido **entender → preparar → comprobar → probar → ejecutar → contribuir** están en la [guía de desarrollo](docs/development/build-test.md).

Desde la raíz, después de preparar el entorno:

```powershell
scripts\00-diagnosticar.bat
scripts\02-ejecutar-tests.bat
scripts\01-ejecutar-app.bat --smoke
scripts\01-ejecutar-app.bat
```

El diagnóstico predeterminado no compila ni descarga. Los modelos locales no son requisito del recorrido determinista. Las generaciones reales son optativas y pueden tardar horas.

## Mapa

- Cinco módulos Maven: contratos (`studio-media-api`), tinta (`studio-ink`), escritorio (`studio-desktop`), adaptadores locales (`studio-local-media-adapters`) y entrada productiva (`studio-launcher`).
- `src/`: fuentes, recursos y pruebas compilados por **studio-desktop**, no un sexto módulo.
- `samples/`: ejemplos oficiales y voces; forman parte del proyecto.
- `scripts/`: [siete accesos públicos](scripts/README.md) e implementación compartida.
- `docs/`: [arquitectura, decisiones, desarrollo y operaciones](docs/README.md).
- `models/`, `tools/`, `runtime/`: contratos y configuración junto a recursos locales instalados; no son basura.
- `target/`: salidas regenerables de compilación y experimentos delimitados. `dist/app-image/`: distribución empaquetada.

Consulta [contribución](CONTRIBUTING.md) antes de modificar componentes compartidos. No ejecutes limpiezas generales sobre recursos, ejemplos o configuraciones personales.
