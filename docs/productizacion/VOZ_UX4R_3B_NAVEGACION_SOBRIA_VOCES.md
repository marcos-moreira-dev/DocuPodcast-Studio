# VOZ-UX4R-3B — navegación modular sobria de Voces

## Objetivo

Corregir la navegación de la microaplicación **Voces** después de VOZ-UX4R-3 y fijar el criterio visual definitivo del sidebar interno.

La Vista Voces mantiene tres módulos:

```text
Inicio
Configurar motor
Gestionar voces
```

Los botones laterales muestran únicamente el nombre del módulo. La descripción del módulo se muestra dentro del workspace, no dentro del botón lateral. Esta decisión evita filas cargadas, mantiene la vista sobria y deja el área derecha como lugar de explicación y operación.

## Corrección funcional

Se corrige el caso en el que hacer clic en **Inicio** o **Configurar motor** parecía no cambiar el workspace. La causa era que el refresco de listas de voces podía seleccionar de nuevo una voz en `manageVoiceSelector`; ese listener reenviaba la vista a **Gestionar voces**.

Se introduce una bandera de refresco interno para que actualizar listas y selección no cambie de módulo accidentalmente.

## Criterio visual

El sidebar de módulos queda como una navegación administrativa sobria, cercana al criterio de Microsoft Teams/escritorio:

- fondo azul grisáceo claro;
- filas blancas limpias;
- selección con borde lateral de acento;
- sin degradados;
- sin tarjetas futuristas;
- sin descripciones dentro del botón.

## Contrato de producto

- **Inicio**: lista voces registradas, estados y emociones disponibles.
- **Configurar motor**: motor activo, dispositivo CPU/GPU detectado y prueba corta.
- **Gestionar voces**: crear, editar, eliminar, importar/grabar/reemplazar emociones y exportar muestras.

La descripción de cada módulo vive en el encabezado del workspace seleccionado.

## Validación

Guardarraíl agregado:

```text
VoiceUx4R3BModuleNavigationPolishSourceTest
```
