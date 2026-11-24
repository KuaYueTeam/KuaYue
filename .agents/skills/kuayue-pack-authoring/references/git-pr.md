# Beginner-safe Git and PR help

Assume the contributor does not understand branches, staging, remotes, or forks. Explain the effect of each action in one plain sentence before running it.

## Preflight

Run:

```powershell
py -3 .agents/skills/kuayue-pack-authoring/scripts/inspect_workspace.py
```

Use the Python command shown in the script's platform report. macOS/Linux will normally use `python3` instead.

Verify:

- the repository has no unexpected dirty files;
- the current branch and remote URLs;
- `gh` exists and is authenticated;
- the contributor's GitHub login;
- `origin` is a fork they can push to;
- `upstream` points to `KuaYueTeam/KuaYue`;
- target branch `dev/new_train` exists upstream.

The inspector checks `gh` at runtime. Do not assume it is installed or authenticated on another contributor's computer.

If `gh` is missing, use the user's package manager:

```bash
# Windows with winget
winget install --id GitHub.cli

# Ubuntu/Debian
sudo apt update && sudo apt install gh

# macOS with Homebrew
brew install gh
```

Then run:

```bash
gh auth login
gh auth status
```

If authentication is missing or expired, stop remote work and ask the user to authenticate.

## Prepare a branch

Fetch the target branch:

```bash
git fetch upstream dev/new_train
```

Create a feature branch before pack edits when the worktree is clean enough to do so:

```bash
git switch -c feat/PACK_ID_SHORT_DESCRIPTION upstream/dev/new_train
```

Use `fix/` for corrective work. Do not reset, force-checkout, stash, or discard unrelated changes without explicit permission.

If the contributor already has local changes, do not switch in a way that risks them. Explain the dirty paths and offer a separate worktree or wait for the user to choose.

Never include these local artifacts in the commit unless the user explicitly asks:

```text
doc/local/
manual_test/
build/
.gradle/
run/
runs/
```

## Review and stage

Show the exact task files and unrelated dirty files. Ask for commit confirmation.

After confirmation:

```bash
git add -- FILE_1 FILE_2
git diff --cached --check
git diff --cached --stat
git diff --cached --name-status
```

Review the staged diff before committing. Do not use `git add .` when unrelated changes may exist.

## Commit

Use a concise Conventional Commit:

```text
feat(packs): add VEHICLE preset content
feat(packs): add VEHICLE parcel to PACK_ID
fix(packs): correct VEHICLE block resources
```

Commit:

```bash
git commit -m "COMMIT_MESSAGE"
git show --stat --oneline HEAD
```

Report the commit hash and explain that it is still local until pushed.

## Push and open the PR

Before the remote write, show:

- branch name;
- fork/remote that will receive the branch;
- target repository `KuaYueTeam/KuaYue`;
- target branch `dev/new_train`;
- PR title;
- summarized PR body with validation evidence.

Ask for explicit confirmation. Then:

```bash
git push -u origin BRANCH_NAME
```

If `origin` is the upstream repository and the contributor lacks write access, ask before creating a fork:

```bash
gh repo fork KuaYueTeam/KuaYue --remote --remote-name fork
git remote -v
git push -u fork BRANCH_NAME
```

Write the PR body to a temporary UTF-8 file so multiline Markdown is preserved. Use the agent's file-writing tool when possible; shell heredocs differ across PowerShell, cmd, and POSIX shells.

Windows PowerShell example:

```powershell
$PrBody = Join-Path $env:TEMP "kuayue-pr-body.md"
"## Summary`n`n- Add/update ...`n`n## Validation`n`n- Schema and pack closure passed`n- Gradle tests passed`n- Client test: passed / not run" |
  Set-Content -Encoding utf8 $PrBody
```

macOS/Linux example:

```bash
cat > /tmp/kuayue-pr-body.md <<'EOF'
## Summary

- Add/update ...

## Validation

- Schema and pack closure passed
- Gradle tests passed
- Client test: passed / not run
EOF
```

Open the PR from the contributor's fork. The agent must substitute real values for the uppercase placeholders:

```powershell
gh pr create --repo KuaYueTeam/KuaYue --base dev/new_train --head GITHUB_LOGIN:BRANCH_NAME --title "PR_TITLE" --body-file "PR_BODY_FILE"
```

Use the remote name that actually owns the pushed branch. Verify the returned URL with:

```bash
gh pr view PR_URL --repo KuaYueTeam/KuaYue
```

Report the PR URL and current checks. Do not merge, modify reviewers, force-push, or delete branches unless the user explicitly asks.
