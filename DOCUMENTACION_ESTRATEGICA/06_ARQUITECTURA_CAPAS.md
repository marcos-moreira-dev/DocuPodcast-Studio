# Arquitectura por capas

DocuPodcast Studio debe seguir una arquitectura por capas inspirada en DMS y Fractal.

## Capas

```text
bootstrap
presentation
application
domain
infrastructure
```

## Responsabilidades

### bootstrap

Ensambla la aplicación:

- `ApplicationBootstrap`;
- `ApplicationRuntime`;
- `ApplicationServicesFactory`;
- `InfrastructureServicesFactory`;
- configuración de ventana;
- CSS principal;
- close handler.

### presentation

JavaFX y estado visual:

- Shell.
- Tabs.
- Toolbar.
- SideDock.
- Workspaces.
- Dialogs.
- Guía.
- Statusbar.

No debe importar infraestructura directamente.

### application

Casos de uso, fachadas y puertos:

- importar documento;
- crear guion;
- validar guion;
- generar audio;
- reintentar jobs;
- exportar;
- registrar assets.

### domain

Modelo puro:

- proyecto;
- documento;
- guion;
- voces;
- storyboard;
- audio;
- playback;
- assets.

No debe conocer JavaFX, archivos concretos, TTS real ni UI.

### infrastructure

Implementaciones concretas:

- importador DOCX;
- JSON reader/writer;
- repositorios de archivos;
- motor TTS gateway;
- audio merger;
- exportadores;
- thumbnail generator;
- logs.

## Regla de dependencia

```text
presentation → application → domain
infrastructure → application/domain
bootstrap → todas para ensamblar
```

No:

```text
domain → JavaFX
application → JavaFX
presentation → infrastructure directa
```
