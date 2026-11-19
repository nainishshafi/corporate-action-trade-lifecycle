---
name: git-committer
description: Automates git commit operations with consistent identity and conventional commit messages. Use when asked to "git commit", "commit changes", or "create a git commit skill".
version: 1.0.0
---

# Git Committer

Automates git staging and commits using a pre-configured identity (`nainishshafi <nainishshafi@users.noreply.github.com>`) and following Conventional Commits standards.

## When to Use This Skill

- When a user asks to commit changes to the repository.
- When git identity is missing in a new environment.
- When you need to ensure consistent, professional commit messages.

## Core Directives

1. **Verify Identity**: Always ensure `user.name` and `user.email` are set before committing.
2. **Conventional Commits**: Use `feat:`, `fix:`, `refactor:`, `docs:`, `test:`, or `chore:` prefixes.
3. **Atomic Commits**: Stage related changes together; avoid giant "everything" commits.
4. **Summary and Body**: Include a concise summary line and a bulleted body for complex changes.

## Workflow

### 1. Check Staged Files
Verify what is currently staged using `git status`.

### 2. Configure Identity (If Needed)
If `git commit` fails with "Author identity unknown", run:
```bash
git config user.email "nainishshafi@users.noreply.github.com"
git config user.name "nainishshafi"
```

### 3. Stage and Commit
```bash
git add <paths>
git commit -m "<type>(<scope>): <subject>

- <body>"
```

## Resources

- [Conventional Commits](https://www.conventionalcommits.org/)
- [Git Committer Reference](references/git-committer-reference.md)
