# 02 — Historia del chat y decisiones acumuladas

El proyecto comenzó con un código Python de TTS/Whisper y una idea inicial: convertir documentos largos en audio local con una voz agradable, evitando voces robóticas que cansen al estudiar. Luego se descartó llamar al producto “mini Word”, porque eso lo empujaría a competir con un editor de documentos. El nombre que quedó fue **DocuPodcast Studio**.

## Decisiones tempranas

- La app debe estar hecha en **JavaFX**.
- Debe sentirse autocontenida.
- No debe exigir al usuario instalar Python manualmente.
- Si Python es necesario para TTS de calidad, debe ir empaquetado o invocado como componente interno.
- La app no necesita generar audio en tiempo real; puede tardar minutos.
- Debe mostrar progreso, ETA y segmento actual.
- Debe guardar progreso para reintentos y reanudación.

## Evolución del concepto

La idea original de “documento a podcast” evolucionó a:

```text
Documento → Guion narrable → Performance → Audio → Storyboard vivo
```

Esto permite:

- lectura educativa neutral;
- lectura dramatizada para teatro;
- voces por personaje;
- estilos o emociones cuando el motor lo soporte;
- imágenes asociadas por segmento;
- reproducción sincronizada de texto, audio e imagen.

## Word como fuente prioritaria

El usuario aclaró que sus notas están en Word. Por tanto:

- Word/DOCX no es una feature secundaria.
- DOCX debe estar en el MVP.
- Markdown es importante, pero como intercambio humano/IA.
- PDF simple puede venir después porque es menos semántico que Word.

## Referencias leídas

Se leyó estratégicamente:

- **Fractal Render Studio** en 10 tandas.
- **Domain Model Studio/UENS** en 15 tandas.

Conclusión:

```text
DMS = mejor base para shell/UI/scaffolding/documentación.
Fractal = mejor base para jobs largos/progreso/batch.
DocuPodcast = dominio propio.
```
