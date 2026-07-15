# Smoke visual final — Vista Voces T121-V10

## Propósito

Validar manualmente que la Vista Voces queda cerrada como biblioteca de voces, muestras por tono y pruebas, sin volver a mezclar asignación documental ni nombres técnicos de motores en la interfaz gráfica.

## Checklist manual

1. Abrir la aplicación y entrar por `Vista > Voces`.
2. Confirmar que la pantalla muestra `Biblioteca de voces` y `Voces del proyecto`.
3. Confirmar que la interfaz no muestra nombres técnicos de motores. Debe usar `Voz IA avanzada`, `Voz local simple` y `Modo de prueba`.
4. Confirmar que no aparece asignación a segmento, fragmento seleccionado, personaje, rol ni estilo de interpretación.
5. Confirmar que la voz neutral prediseñada aparece protegida o explicada como base del proyecto.
6. Seleccionar una voz avanzada y revisar la sección `Registro de muestras por tono`.
7. Confirmar que el tono neutral aparece como obligatorio.
8. Seleccionar un tono recomendado y verificar que cambia la frase guía.
9. Importar una muestra para el tono seleccionado.
10. Grabar una muestra para el tono seleccionado, cancelar y confirmar que la muestra previa no se reemplaza.
11. Detener y registrar una muestra; confirmar que queda asociada al tono correcto.
12. Descargar/exportar una muestra a una carpeta elegida por el usuario.
13. Eliminar una muestra gestionada y confirmar que no borra archivos externos originales.
14. Escribir una frase en `Prueba generada` y usar `Generar prueba con esta voz`.
15. Reproducir la última prueba generada.
16. Solicitar un tono faltante y confirmar que se informa `fallback neutral` cuando existe muestra neutral.
17. Cambiar a `Voz local simple` desde Configuración y volver a Voces.
18. Confirmar que en `Voz local simple` no se muestran muestras humanas, tonos de referencia, clonación, catálogo teatral ni estilos expresivos.
19. Probar `Probar lectura simple` y reproducir la última prueba.
20. Cambiar a `Modo de prueba` y confirmar que no promete voz real.

## Criterio de aprobación

La Vista Voces aprueba si todas las acciones anteriores se pueden ejecutar o aparecen bloqueadas con mensaje humano claro, sin nombres técnicos de motores en la interfaz gráfica normal y sin reintroducir Guion, STT/Whisper ni asignación de fragmentos dentro de Voces.
