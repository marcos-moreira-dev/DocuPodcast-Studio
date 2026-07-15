# Voces, personajes y estilos

DocuPodcast Studio debe permitir lectura simple para textos académicos y dramatización controlada para teatro o guiones.

## Familias de voces

```text
PREDEFINED   voces incluidas para empezar rápido
OWN          voz propia del usuario
AUTHORIZED   voces de amigos/colaboradores con permiso
IMPORTED     voces importadas desde paquete/proyecto
```

## Voz vs personaje

Una voz es un perfil técnico/acústico.

Un personaje es una entidad narrativa que puede usar una voz y un estilo por defecto.

Ejemplo:

```text
VoiceProfile: VOC-001, Narrador neutro
CharacterProfile: CHR-001, Narrador, voice=VOC-001
```

## Estilos

Los estilos son intención de interpretación:

- neutro;
- serio;
- alegre;
- triste;
- enojado;
- sorprendido;
- dramático;
- suave.

No deben prometer resultado si el motor TTS no soporta control emocional.

## Política ética

La app debe apoyar:

- voz propia;
- voces autorizadas;
- voces prediseñadas.

No debe fomentar clonar voces de personas sin permiso.

## UI mínima

- Biblioteca de voces.
- Probar voz.
- Crear personaje.
- Asignar voz a segmento/personaje.
- Mostrar advertencia si el motor no soporta un estilo.
