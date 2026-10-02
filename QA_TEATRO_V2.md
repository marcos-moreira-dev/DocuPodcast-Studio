# QA Teatro v2

Cobertura automatizada:

- carpeta contra ZIP;
- ausencia opcional sin warning;
- asset declarado faltante;
- hash incorrecto;
- IDs estables;
- herencia, reset por escena, eventos y posesión;
- snapshot sin IA;
- export/import round-trip;
- guardar/cerrar/abrir conservando IDs y snapshot.

Última ejecución focalizada: **28 tests, 0 fallos, 0 errores**.

Camino feliz esperado: cero warnings. Los errores de sintaxis, referencia, ruta, hash, tamaño, ciclo o contradicción impiden el commit.

Comando focalizado:

```powershell
mvn -pl studio-desktop -am "-Dtest=TheatreDeterministicStateTest,OfficialTheatrePackageTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```
