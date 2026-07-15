# Mapa de paquetes Java propuesto

```text
com.marcosmoreiradev.docupodcaststudio
├── bootstrap
├── domain
│   ├── project
│   ├── assets
│   ├── document
│   ├── reading
│   ├── script
│   ├── voice
│   ├── storyboard
│   ├── media
│   ├── audio
│   └── playback
├── application
│   ├── services
│   ├── project
│   ├── assets
│   ├── document
│   ├── reading
│   ├── script
│   ├── voice
│   ├── storyboard
│   ├── audio
│   ├── playback
│   ├── export
│   ├── guide
│   └── observability
├── infrastructure
│   ├── json
│   ├── docx
│   ├── markdown
│   ├── media
│   ├── tts
│   ├── audio
│   ├── export
│   └── observability
└── presentation
    ├── shell
    ├── toolbar
    ├── workspace
    ├── sidedock
    ├── welcome
    ├── document
    ├── script
    ├── storyboard
    ├── audio
    ├── voice
    ├── playback
    ├── observability
    ├── guide
    └── dialogs
```

Regla: los paquetes de dominio no conocen JavaFX ni infraestructura.
