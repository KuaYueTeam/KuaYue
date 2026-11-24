#!/usr/bin/env python3
"""Read-only KuaYue workspace and GitHub CLI inspection."""

from __future__ import annotations

import argparse
import json
import os
import platform
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Any


def run(
    args: list[str],
    *,
    cwd: Path,
    check: bool = False,
) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        args,
        cwd=cwd,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=check,
    )


def git(repo: Path, *args: str) -> str:
    result = run(["git", *args], cwd=repo)
    return result.stdout.strip() if result.returncode == 0 else ""


def find_repo(start: Path) -> Path:
    result = run(["git", "rev-parse", "--show-toplevel"], cwd=start)
    if result.returncode != 0:
        raise SystemExit(f"not inside a Git repository: {start}")
    return Path(result.stdout.strip()).resolve()


def parse_remotes(repo: Path) -> dict[str, str]:
    output = git(repo, "remote", "-v")
    remotes: dict[str, str] = {}
    for line in output.splitlines():
        fields = line.split()
        if len(fields) >= 3 and fields[2] == "(fetch)":
            remotes[fields[0]] = fields[1]
    return remotes


def parse_status(repo: Path) -> list[dict[str, str]]:
    output = git(repo, "status", "--porcelain=v1")
    entries: list[dict[str, str]] = []
    for line in output.splitlines():
        if len(line) < 3:
            continue
        entries.append({"status": line[:2], "path": line[3:]})
    return entries


def inspect_packs(repo: Path) -> list[dict[str, Any]]:
    source = repo / "modules/train-parts/resources/packs"
    packs: list[dict[str, Any]] = []
    if not source.is_dir():
        return packs
    for manifest_path in sorted(source.glob("*/pack.json")):
        try:
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            packs.append(
                {
                    "id": manifest_path.parent.name,
                    "path": str(manifest_path.parent.relative_to(repo)),
                    "error": str(exc),
                    "parcels": [],
                }
            )
            continue
        parcels: list[dict[str, str]] = []
        for parcel_rel in manifest.get("parcels", []):
            parcel_path = manifest_path.parent / parcel_rel
            parcel_id = None
            try:
                parcel = json.loads(parcel_path.read_text(encoding="utf-8"))
                parcel_id = parcel.get("id")
            except (OSError, json.JSONDecodeError):
                pass
            parcels.append(
                {
                    "id": parcel_id or "(invalid)",
                    "path": str(parcel_path.relative_to(repo)),
                }
            )
        packs.append(
            {
                "id": manifest.get("id", manifest_path.parent.name),
                "path": str(manifest_path.parent.relative_to(repo)),
                "required": bool(manifest.get("required", False)),
                "dependencies": manifest.get("dependencies", {}).get("packs", {}),
                "parcels": parcels,
            }
        )
    return packs


def inspect_gh(repo: Path) -> dict[str, Any]:
    executable = shutil.which("gh")
    if not executable:
        return {
            "installed": False,
            "authenticated": False,
            "version": None,
            "login": None,
            "status": "gh is not installed",
        }

    version = run(["gh", "--version"], cwd=repo)
    auth = run(["gh", "auth", "status"], cwd=repo)
    authenticated = auth.returncode == 0
    login = None
    if authenticated:
        user = run(["gh", "api", "user", "--jq", ".login"], cwd=repo)
        if user.returncode == 0:
            login = user.stdout.strip() or None
    output = (auth.stdout + auth.stderr).strip()
    return {
        "installed": True,
        "authenticated": authenticated,
        "version": version.stdout.splitlines()[0] if version.stdout else None,
        "login": login,
        "status": output,
    }


def inspect_platform(repo: Path) -> dict[str, Any]:
    is_windows = os.name == "nt"
    if is_windows and shutil.which("py"):
        python = ["py", "-3"]
    elif shutil.which("python3"):
        python = ["python3"]
    elif shutil.which("python"):
        python = ["python"]
    else:
        python = []
    wrapper_name = "gradlew.bat" if is_windows else "gradlew"
    gradle_wrapper = f".\\{wrapper_name}" if is_windows else f"./{wrapper_name}"
    return {
        "system": platform.system() or os.name,
        "release": platform.release(),
        "machine": platform.machine(),
        "is_windows": is_windows,
        "shell_environment": os.environ.get("SHELL")
        or os.environ.get("ComSpec")
        or "(unknown)",
        "python": python,
        "python_available": bool(python),
        "gradle_wrapper": gradle_wrapper,
        "gradle_wrapper_present": (repo / wrapper_name).is_file(),
        "temp_dir": tempfile.gettempdir(),
    }


def collect(repo: Path) -> dict[str, Any]:
    target_ref = "refs/remotes/upstream/dev/new_train"
    target_ref_result = run(
        ["git", "show-ref", "--verify", "--quiet", target_ref],
        cwd=repo,
    )
    return {
        "repository": str(repo),
        "platform": inspect_platform(repo),
        "branch": git(repo, "branch", "--show-current"),
        "head": git(repo, "rev-parse", "--short", "HEAD"),
        "target_branch": "dev/new_train",
        "target_ref_present": target_ref_result.returncode == 0,
        "remotes": parse_remotes(repo),
        "status": parse_status(repo),
        "ignored_process_paths": [
            path
            for path in ["doc/local", "manual_test", "build", ".gradle", "run", "runs"]
            if (repo / path).exists()
        ],
        "packs": inspect_packs(repo),
        "gh": inspect_gh(repo),
    }


def print_markdown(data: dict[str, Any]) -> None:
    print(f"Repository: `{data['repository']}`")
    host = data["platform"]
    python = " ".join(host["python"]) if host["python"] else "(not found)"
    print(f"Platform: `{host['system']}` / `{host['machine']}`")
    print(f"Python command: `{python}`")
    print(f"Gradle wrapper: `{host['gradle_wrapper']}`")
    print(f"Temp directory: `{host['temp_dir']}`")
    print(f"Branch: `{data['branch']}` at `{data['head']}`")
    print(f"Target: `KuaYueTeam/KuaYue:{data['target_branch']}`")
    print(f"Target ref fetched: {data['target_ref_present']}")
    print()
    print("## Remotes")
    for name, url in data["remotes"].items():
        print(f"- `{name}`: {url}")
    print()
    print("## GitHub CLI")
    gh = data["gh"]
    print(f"- Installed: {gh['installed']}")
    print(f"- Authenticated: {gh['authenticated']}")
    print(f"- Login: {gh['login'] or '(unknown)'}")
    print(f"- Version: {gh['version'] or '(unknown)'}")
    if gh["status"]:
        print("\n```text")
        print(gh["status"])
        print("```")
    print()
    print("## Dirty files")
    if not data["status"]:
        print("- none")
    else:
        for entry in data["status"]:
            print(f"- `{entry['status'].strip()}` {entry['path']}")
    print()
    print("## Packs")
    for pack in data["packs"]:
        required = " required" if pack.get("required") else ""
        print(f"- `{pack['id']}`{required}")
        for parcel in pack["parcels"]:
            print(f"  - `{parcel['id']}`: `{parcel['path']}`")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=Path, default=Path.cwd())
    parser.add_argument("--format", choices=("markdown", "json"), default="markdown")
    args = parser.parse_args()
    repo = find_repo(args.repo.resolve())
    data = collect(repo)
    if args.format == "json":
        print(json.dumps(data, ensure_ascii=False, indent=2))
    else:
        print_markdown(data)
    return 0


if __name__ == "__main__":
    sys.exit(main())
