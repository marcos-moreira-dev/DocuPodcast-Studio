# UX-HF2 — Descarga visible, Voz local normalizada y reproducción por fragmentos

Esta tanda corrige problemas detectados al probar la preparación de motores y la lectura real:

- La descarga de Voz IA avanzada distingue recursos obligatorios y opcionales; un archivo opcional no bloquea el uso si el modelo requerido quedó completo.
- Los errores de descarga muestran el recurso obligatorio pendiente en la ventana de progreso, en vez de ocultarse tras mensajes genéricos.
- La configuración inicial usa saltos de línea y un ancho mayor para que el mensaje pueda leerse completo.
- Voz local simple normaliza tildes, signos iniciales y ñ antes de enviar texto a Piper. El documento original no se modifica.
- La lectura por fragmentos puede empezar con el primer fragmento disponible si el usuario pidió escuchar, sin esperar innecesariamente a todo el documento.
- Si el reproductor interno no puede abrir un WAV generado, el estado visible conserva el error y no lo sobreescribe con un mensaje de éxito.
