# Memoria — Tanda 81B

La Tanda 81B nace de dos observaciones de usuario posteriores a T81A:

1. Configuración no debe aparecer como tarjeta dentro de la pantalla de inicio. Debe estar únicamente en el menu bar.
2. El workspace Documento no debe mostrar metadatos como `styleName`, `styleId`, `readingProfile`, `classificationSource` o similares en la hoja.

Se corrigió `WelcomeWorkspaceView` y `DocumentWorkspaceView` sin tocar el cerebro V1. Los metadatos siguen accesibles en `DocumentPropertiesPanel` para diagnóstico.

La dirección visual se mantiene: Documento debe ser la superficie primaria y limpia; los ajustes avanzados quedan fuera del flujo de lectura.
