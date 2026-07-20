# Build and test

## Toolchain

- Temurin JDK 21.
- Maven configured by `pom.xml` and `.mvn/toolchains.xml.example`.
- JavaFX 21.

## Commands

```powershell
mvn -q test
mvn clean package
mvn -pl studio-launcher -am -DskipTests install
mvn -f studio-launcher/pom.xml javafx:run
```

`mvn clean package` resolves the LectureStudio Stylus native classifier and copies it to `target/native`; no user-specific Maven path is required.

## Test policy

- Prefer unit and integration tests over source-text assertions.
- Use architecture tests for package dependency rules.
- UI source tests must not assert implementation comments, method ordering or historical documentation.
- Every refactor batch runs targeted tests and then the complete suite.
- Packaging and an application smoke test are required before merging changes that affect runtime wiring.

Generated output belongs in `target`, `generated` or project-specific media folders, never in source directories.
