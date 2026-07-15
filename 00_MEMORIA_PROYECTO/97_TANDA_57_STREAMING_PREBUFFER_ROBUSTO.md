# Memoria — Tanda 57

Decisión: la reproducción de documentos largos debe comportarse como streaming con prebuffer. No se debe obligar al usuario a esperar a que todo el documento esté procesado. El estado de buffer se muestra en Documento, con lenguaje de usuario final.

Regla vigente:

```text
5 fragmentos iniciales antes de iniciar.
10 fragmentos adelantados como objetivo.
Si falta el siguiente fragmento: pausar, preparar y continuar automáticamente.
```
