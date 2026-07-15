# Tanda 81B — Documento limpio

T81B corrige la exposición visual de configuración en la pantalla de inicio y limpia el workspace Documento. La configuración queda accesible desde el menú superior y la hoja central deja de mostrar metadatos técnicos del importador.

Cambios principales:

- `WelcomeWorkspaceView` ya no recibe `openSettings`.
- Se elimina la tarjeta de configuración de la bienvenida.
- `SettingsDialog` usa “Ajustes operativos persistentes” en lugar de lenguaje metafórico visible.
- `DocumentWorkspaceView` deja de agregar `document-block-metadata` debajo de cada bloque.
- Los metadatos siguen en `DocumentPropertiesPanel`.

Regla de producto: la portada presenta el producto; la hoja central permite leer/escuchar; el diagnóstico vive en paneles o superficies avanzadas.
