# USER-DECISIONS-HF1 — Decisiones defensivas visibles y excepciones tipadas

## Propósito

La aplicación no debe cambiar silenciosamente la intención explícita del usuario. Si DocuPodcast decide continuar con un fallback defensivo —por ejemplo GPU solicitada pero CPU efectiva, Voz IA avanzada no usable, tono solicitado reemplazado por Neutral o exportación parcial— esa decisión debe poder llegar a la UI como aviso operativo.

## Cambios implementados

- Se agregó `application.decisions.UserVisibleDecision` para modelar decisiones que pueden requerir message box.
- Se agregó `application.decisions.OperationResult<T>` para transportar resultados con decisiones visibles sin depender de JavaFX.
- Se agregó `application.decisions.DefensiveDecisionPolicy` como regla transversal para fallbacks que cambian intención del usuario.
- Se agregó una familia de excepciones tipadas:
  - `ApplicationPreconditionException`
  - `InfrastructureOperationException`
  - `ExternalProcessFailedException`
  - `EngineUnavailableException`
- `UserNotification` ahora puede convertir `UserVisibleDecision` y excepciones `UserFacingApplicationException` a mensajes de producto.
- `ExceptionAlertPresenter` ahora puede mostrar decisiones visibles filtrando las que requieren diálogo.
- El gateway defensivo de Voz IA avanzada ahora lanza `EngineUnavailableException` cuando bloquea generación por readiness real fallido, en lugar de `IllegalStateException` genérica.

## Regla de producto

Si la app cambia una intención explícita del usuario, debe mostrarse un aviso operativo. El status bar no basta para decisiones que alteran motor, GPU, tono, exportación o integridad de proyecto.

## Fuera de alcance

Esta tanda crea el contrato transversal y una primera integración en Voz IA avanzada. Las integraciones específicas de GPU→CPU, tono→Neutral, exportación parcial y proyecto con advertencias deben conectarse en tandas posteriores usando este contrato.
