# Memoria — Hotfix tests Windows v2

El usuario ejecutó los tests localmente en Windows y Maven reportó cinco fallos. El entorno base estaba correcto: Java en PATH era Temurin 21, Maven corría con JDK 23, y el build estaba preparado para usar Maven Toolchain Java 21.

Correcciones:

- Normalización de separadores de ruta en `LocalTtsProcessConfigurationTest`.
- Mensaje de advertencia del importador DOCX para imagen sin descripción.
- Rebaseline de tests fuente para el nuevo lenguaje de producto de toolbar/audio/voz.
- Limpieza de jerga visible: `Whisper/STT` queda como referencia técnica futura, no como etiqueta principal de usuario.

Este hotfix no cambia el roadmap de Tanda 15.
