# Política de no promesas falsas

DocuPodcast debe ser honesto con el usuario.

## Ejemplos prohibidos

- Mostrar “Exportar MP3” si no hay encoder.
- Mostrar “Leer llorando” si el motor no soporta estilo emocional.
- Mostrar “Generar película” si solo hay storyboard estático.
- Marcar plantilla IA como importable si tiene placeholders.
- Mostrar “Importar Word” si no hay importador DOCX real.
- Mostrar “Podcast completo” si faltan segmentos.

## Solución

- Capabilities por motor/workspace.
- Toolbar filtrada por capacidades.
- Tooltips claros.
- Estados deshabilitados con explicación.
- Tests fuente anti-fachada.

## Regla

Si no existe cadena UI → caso de uso → implementación → test, no debe mostrarse como función disponible.
