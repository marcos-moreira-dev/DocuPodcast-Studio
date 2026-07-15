# Estrategia — Tanda 38

DocuPodcast debe verse como un producto de escritorio limpio para usuarios no profesionales. La complejidad interna puede existir, pero la superficie visible debe apoyarse en componentes consistentes.

Reglas estratégicas:

1. Crear o reutilizar componentes GUI antes de duplicar JavaFX en vistas.
2. Mantener la pantalla principal centrada en Documento narrado.
3. Separar CSS por tokens, componentes y workspace.
4. Evitar archivos CSS gigantes; `docupodcast-light.css` debe ser ensamblador.
5. Mantener configuración y diagnósticos como bodega técnica separada.
