# Desarrollo: preparar, comprobar, probar y ejecutar

## Entender

Lee la [arquitectura](../architecture/overview.md). Theatre, Documentary y Narrative consumen componentes transversales: interfaz y vistas especializadas, IA local, multimedia, tinta, persistencia y coordinación. Un cambio compartido debe considerar todos sus consumidores.

El reactor raíz contiene cinco módulos:

| Módulo | Responsabilidad |
| --- | --- |
| studio-media-api | Contratos multimedia compartidos |
| studio-ink | Integración de tinta |
| studio-desktop | Aplicación, vistas y semánticas; compila fuentes y pruebas de **src de la raíz** |
| studio-local-media-adapters | Implementaciones de motores locales |
| studio-launcher | Único punto de entrada productivo y ensamblaje |

No muevas fuentes ni cambies versiones por el nombre histórico de la versión Maven.

## Preparar

1. Instala Temurin **JDK 21** y Maven **3.9 o posterior**. Comprueba `java -version` y `mvn -version`; JAVA_HOME debe apuntar al JDK, no a su carpeta bin.
2. Integra la entrada de [.mvn/toolchains.xml.example](../../.mvn/toolchains.xml.example) en `%USERPROFILE%\.m2\toolchains.xml`: versión 21, vendor temurin y jdkHome real. **No sobrescribas otras entradas ni configuraciones personales.**
3. Abre PowerShell en la raíz del repositorio, incluso si su ruta contiene espacios.

La primera compilación Maven puede descargar dependencias y el auxiliar nativo de tinta. Esto no instala modelos ni motores de IA.

Código, tests, fixtures, ejemplos, licencias, manifiestos y workflows son parte del proyecto. Modelos descargados, entornos instalados y estado local no forman parte de una compilación y no deben borrarse para recompilar.

## Comprobar

```powershell
scripts\00-diagnosticar.bat
scripts\00-diagnosticar.bat --help
```

Sin opciones inspecciona versiones, Toolchains y layout: no compila ni descarga. Un auxiliar nativo todavía no resuelto se indica como no instalado. Para resolverlo explícitamente:

```powershell
scripts\00-diagnosticar.bat --resolve-native
```

Los errores reales devuelven código 1. Una comprobación optativa no solicitada aparece como no ejecutada.

## Probar

```powershell
scripts\02-ejecutar-tests.bat
scripts\03-verificar-completo.bat
```

La primera ejecuta `mvn test`. La segunda ejecuta `mvn verify -Pgui-e2e` (TestFX/Monocle, Failsafe), instala dependencias del launcher y valida su arranque. No requiere copia hermana ni modelos locales.

La paridad histórica solo se ejecuta con `--reference-root=<ruta>` explícito sobre una referencia autorizada. No utilices copias de emergencia como dependencia habitual.

Las pruebas con motores reales están **fuera del recorrido mínimo**: consulta `scripts\04-smoke-capacidades-reales.bat --help` antes de ejecutarlas; pueden generar medios durante horas. Instalar un archivo no certifica que su motor funcione.

## Ejecutar

```powershell
scripts\01-ejecutar-app.bat --smoke
scripts\01-ejecutar-app.bat
```

Ambas compilan e instalan el reactor necesario; smoke abre el launcher y termina. Los scripts respetan los overrides existentes `DOCUPODCAST_APP_ROOT` y `DOCUPODCAST_RUNTIME_ROOT`; sin ellos usan la raíz del repositorio. Las propiedades JVM `docupodcast.app.root` y `docupodcast.runtime.root` tienen precedencia. Los scripts no reescriben el PATH del usuario.

Para empaquetar usa `scripts\05-generar-app-image.bat`. Mantiene `dist/app-image`, incluye recursos y muestras oficiales y no incorpora modelos ni entornos Python.

## Diagnosticar fallos y contribuir

`scripts\06-exportar-diagnosticos.bat` guarda registros y ZIP bajo `target/diagnostics` incluso si una comprobación falla; conserva el código de fallo. Revisa el contenido antes de compartirlo. Admite las comprobaciones optativas `--resolve-native` y `--reference-root=<ruta>`.

Todos los accesos admiten `--help` y rechazan argumentos desconocidos. No se generan medios reales desde el diagnóstico ordinario.

Para comprobar los contratos de los accesos sin ejecutar generaciones: `powershell -NoProfile -File scripts/maintenance/test-onboarding.ps1`. Crea una maqueta de checkout con espacios bajo `target/onboarding-tests`, sin modelos ni copia hermana; comprueba ayuda, argumentos, diagnóstico sin compilación y ZIP ante fallos. No sustituye a compilar un checkout completo ni a las pruebas Java.

Sigue [CONTRIBUTING](../../CONTRIBUTING.md). Las nuevas salidas experimentales deben estar delimitadas bajo `target/experiments/<prueba>`; no cambies destinos productivos para cumplir esa regla. No borres carpetas mixtas como tmp, output, runtime o samples. Revisa el dry-run del auxiliar de mantenimiento y referencias/uso activo antes de autorizar cualquier borrado.
