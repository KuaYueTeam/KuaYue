# Cross-platform command guide

Never assume macOS. Windows is the primary contributor environment; macOS and Linux must remain supported.

Run `inspect_workspace.py` first and use its reported `platform`, Python command, Gradle wrapper, and temp directory. Do not persist a command choice from a previous session.

## Python

Use the first available command:

Windows PowerShell:

```powershell
py -3 .agents/skills/kuayue-pack-authoring/scripts/inspect_workspace.py
```

If `py` is unavailable:

```powershell
python .agents/skills/kuayue-pack-authoring/scripts/inspect_workspace.py
```

macOS/Linux:

```bash
python3 .agents/skills/kuayue-pack-authoring/scripts/inspect_workspace.py
```

The contributor-facing scripts use only portable Python APIs. Run them with either command.

Additional model tools:

```text
scripts/analyze_models.py
scripts/normalize_model_json.py
scripts/validate_pack.py
```

Run model/package checks with:

```powershell
py -3 .agents/skills/kuayue-pack-authoring/scripts/analyze_models.py MODEL --format markdown
py -3 .agents/skills/kuayue-pack-authoring/scripts/normalize_model_json.py SOURCE.json OUTPUT.json --namespace NAMESPACE
py -3 .agents/skills/kuayue-pack-authoring/scripts/validate_pack.py PACK_ROOT --require-schema
py -3 .agents/skills/kuayue-pack-authoring/scripts/validate_pack.py PACK_ROOT --require-schema --require-durable-source
```

Use the durable-source check before a commit. It fails for packs under `run/`, generated build output, or compiled output.

## Gradle

Windows PowerShell:

```powershell
.\gradlew.bat :modules:train-parts:test --console=plain
```

Windows `cmd.exe`:

```bat
gradlew.bat :modules:train-parts:test --console=plain
```

macOS/Linux:

```bash
./gradlew :modules:train-parts:test --console=plain
```

Use the same wrapper choice for:

```text
:modules:train-parts:generateKuaYuePresetPacks
:modules:train-parts:runClient
```

Prefer one command per invocation. PowerShell uses a backtick for line continuation; POSIX shells use a backslash. Avoid multiline commands when a single line is readable.

## Temporary files

Windows PowerShell:

```powershell
$KuayueTemp = Join-Path $env:TEMP "kuayue-pack-authoring"
New-Item -ItemType Directory -Force $KuayueTemp | Out-Null
```

Windows `cmd.exe`:

```bat
set "KUAYUE_TEMP=%TEMP%\kuayue-pack-authoring"
if not exist "%KUAYUE_TEMP%" mkdir "%KUAYUE_TEMP%"
```

macOS/Linux:

```bash
KUAYUE_TEMP=/tmp/kuayue-pack-authoring
mkdir -p "$KUAYUE_TEMP"
```

Use the agent's file-writing tool when available instead of shell heredocs. This avoids PowerShell, cmd, Bash, and quote differences.

## Schema virtual environment

Windows PowerShell:

```powershell
$SchemaVenv = Join-Path $env:TEMP "kuayue-pack-schema"
py -3 -m venv $SchemaVenv
& "$SchemaVenv\Scripts\python.exe" -m pip install jsonschema referencing
& "$SchemaVenv\Scripts\python.exe" .agents/skills/kuayue-pack-authoring/scripts/validate_pack.py PACK_ROOT --require-schema
```

macOS/Linux:

```bash
python3 -m venv /tmp/kuayue-pack-schema
/tmp/kuayue-pack-schema/bin/python -m pip install jsonschema referencing
/tmp/kuayue-pack-schema/bin/python .agents/skills/kuayue-pack-authoring/scripts/validate_pack.py PACK_ROOT --require-schema
```

Do not create `.venv`, `venv`, dependency lockfiles, or cache directories inside the repository for this one-off validation.

## Paths

- Use repository-relative paths in JSON, manifests, documentation, and PR summaries.
- Use forward slashes for protocol paths such as `data/<namespace>/kasuga_lib/data_driven/`.
- Quote filesystem paths containing spaces.
- Do not write scripts that depend on `/usr/bin/env`, `/tmp`, `rm`, `cp`, `cat`, `chmod`, Homebrew, or Bash.
- Use Python's `pathlib`, `tempfile`, and `shutil` for portable file operations in generated helpers.

## Shell independence

Git commands and `gh` commands are cross-platform. The surrounding redirection and quoting are not:

- PowerShell does not treat `<` and `>` as command placeholders;
- `cmd.exe` uses `%TEMP%`, PowerShell uses `$env:TEMP`, and POSIX shells commonly use `/tmp`;
- Windows may resolve `python` to an app alias that does nothing useful, so prefer `py -3` when present;
- Windows paths from `Path` may use `\`, while logical resource paths must use `/`;
- do not require executable permission on helper `.py` files; invoke them through Python.
