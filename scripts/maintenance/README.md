# Workspace maintenance

Preview recoverable workspace output without changing files:

```powershell
.\scripts\maintenance\clean-workspace.ps1
```

Apply the explicit cleanup allowlist:

```powershell
.\scripts\maintenance\clean-workspace.ps1 -Apply
```

Verify the ignored engines and model files required by the local installation:

```powershell
.\scripts\maintenance\audit-local-runtime.ps1
```

Do not replace the cleanup script with `git clean -xfd`. Local engines and model weights are intentionally ignored by Git and would be at risk.

