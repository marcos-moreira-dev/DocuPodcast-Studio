# Diagnostico input tableta y tinta

## Situacion verificada
- El modal muestra `Entrada: JavaFX mouse (sin tableta nativa)` cuando la app no tiene proveedor nativo real.
- `LectureStudioStylusInputProvider` solo detecta clases por reflexion y delega a `JavaFxMouseInputProvider`; no captura paquetes nativos de tableta.
- Si JavaFX coalesce eventos, el motor recibe pocos puntos por segundo. El suavizado posterior no puede reconstruir detalle que nunca entro.

## Cambio aplicado
- Los flags de diagnostico de tinta ahora son activables por propiedades JVM:
  - `-Ddocupodcast.ink.inputDiagnostics=true`
  - `-Ddocupodcast.ink.renderDiagnostics=true`
  - `-Ddocupodcast.ink.perfDiagnostics=true`
  - `-Ddocupodcast.ink.fastDebug=true`
- El log de tinta reporta `pointsPerSecond`, cola pendiente, pico de cola, drops, puntos drenados y tiempo de frame.
- El texto del modal diferencia explicitamente JavaFX mouse de entrada nativa de tableta.

## Como medir
Arrancar la app con:

```powershell
mvn -q javafx:run -Ddocupodcast.ink.inputDiagnostics=true -Ddocupodcast.ink.renderDiagnostics=true
```

Dibujar 10 segundos en el modal de problema tecnico y revisar `pointsPerSecond`.

## Proximo paso tecnico real
Implementar un proveedor nativo detras de `InkInputProvider`:
1. Intentar backend Windows Pointer/JNA o Wintab si se confirma disponibilidad.
2. Capturar pointer id, timestamp, presion, borrador y coordenadas a alta frecuencia.
3. Mantener fallback JavaFX, pero nunca reportarlo como stylus nativo.
4. Criterio minimo: el modal debe mostrar `Entrada: Stylus nativo` solo si hay eventos nativos reales, y los puntos por segundo deben superar claramente el fallback JavaFX.

## Fuera de alcance de este diagnostico
- OCR.
- Cambios de `.docupodcast.json`.
- Domain Model Studio/UENS.
- Nueva UI de teatro para bocetos.
