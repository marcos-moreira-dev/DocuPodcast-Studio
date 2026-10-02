# Correcciones de importación de carpeta teatral

Se corrigieron las tres brechas reproducidas en la [auditoría de paridad](auditoria-paridad-manual-carpeta-2026-09-21.md): carpeta sin texto propio, audio aplicado al bloque equivocado y pérdida de `aplicar_plano=false` al exportar.

## Comportamiento implementado

- La carpeta oficial usa la misma proyección teatral que la gramática: devuelve proyecto, documento limpio y guion, y la interfaz los aplica juntos.
- Se respeta el archivo indicado por `grammar`, se conservan los límites de escena y se configura el modo teatral y el título de la obra.
- El audio se enlaza al segmento real. Se preservan sus rangos de texto y las pistas con ancla, recorte, modo de finalización, volumen y fundido.
- Cámara, contexto textual y cambios de fondo de intervención se conservan al exportar la gramática. La política de cámara importada también sirve como valor inicial de la interfaz.
- El manifiesto admite varios bindings por archivo y el exportador conserva todos los usos sin duplicar físicamente el recurso.
- Las muestras `VOICE_SAMPLE` se vinculan a perfiles existentes, con tono y transcripción, y sus archivos permanecen portables.
- La importación repetida reutiliza una copia idéntica e íntegra. La identidad de contenido incorpora gramática y configuración de voces; una modificación del diálogo no colisiona con la copia anterior.
- Una actualización genérica de la fuente no puede convertir de nuevo el documento teatral en párrafos con metadata técnica.
- La importación no se aplica sobre otro proyecto si el usuario cambia de proyecto mientras trabaja la tarea de fondo.

Los datos se configuran sin ejecutar IA, TTS, mezcla o render. Los archivos originales y la sesión abierta del usuario permanecieron intactos.

## Evidencia de integración

La compilación completa de los seis módulos terminó correctamente. La selección final de regresiones registró 43 pruebas: 41 ejecutadas sin fallos y 2 omitidas por depender de fixtures externos opcionales. La ejecución separada con la obra grande también terminó correctamente, incluida su exportación, importación y reapertura.

Las tres sondas de auditoría se incorporaron como pruebas positivas. Se añadieron casos para ruta de gramática distinta de la predeterminada, importación repetida, modificación del texto con los mismos assets, audio compartido entre diálogo y pista, muestra de voz portable y protección frente al refresco documental genérico.

La obra grande se exportó como carpeta oficial y se importó en un proyecto vacío. Se comparó el texto de los 557 parlamentos y las cantidades de personajes/utilería; además se guardó y reabrió el resultado. La prueba no contiene nombres, rutas ni reglas de Patria en el código de producto: recibe el proyecto externo mediante propiedades de ejecución.

La carpeta y el proyecto resultantes de la comprobación están bajo `target/patria-folder-audit/carpeta-13239774096927847496/`: `obra/` es la entrada oficial y `proyecto/obra.docupodcast.json` es su resultado importado. Son copias de validación.

Registros: `target/theatre-folder-fixes.log` (incluye la prueba de estrés) y `target/theatre-folder-full-build.log` (compilación completa y regresiones). La auditoría original conserva sus resultados fallidos como evidencia histórica; las pruebas actuales están en `src/test/java/.../application/theatrepackage/TheatreFolderParityAuditTest.java`.

## Límites explícitos

Estas correcciones no certifican que absolutamente todos los ajustes de todos los modos del producto tengan representación de carpeta. `VIDEO`/`OTHER` siguen sin asignación automática a reproducción; crear o instalar motores/perfiles de voz ausentes requiere un contrato adicional. Las muestras transportadas aquí son recursos gestionados del proyecto asociados a perfiles existentes.

No se implementaron acotaciones silenciosas con reloj propio, cues de iluminación ni interpretación de documentación de montaje humano. Tampoco se migró automáticamente el proyecto antiguo abierto: para obtener la proyección limpia se usa la importación completa o el proyecto validado. La aplicación debe arrancar con la compilación actualizada para disponer del nuevo comportamiento.
