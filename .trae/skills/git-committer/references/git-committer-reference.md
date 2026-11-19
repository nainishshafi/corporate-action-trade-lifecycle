# Git Committer Reference

This reference provides detailed guidance for using the `git-committer` skill.

## Conventional Commit Types

| Type | Description |
| :--- | :--- |
| `feat` | A new feature |
| `fix` | A bug fix |
| `docs` | Documentation only changes |
| `style` | Changes that do not affect the meaning of the code |
| `refactor` | A code change that neither fixes a bug nor adds a feature |
| `perf` | A code change that improves performance |
| `test` | Adding missing tests or correcting existing tests |
| `chore` | Changes to the build process or auxiliary tools and libraries |

## Commit Message Template

```text
<type>(<scope>): <short summary>

- <detailed bullet point 1>
- <detailed bullet point 2>
```

## Troubleshooting Identity Issues

If you see:
`fatal: unable to auto-detect email address`

Run:
```bash
git config user.email "nainishshafi@users.noreply.github.com"
git config user.name "nainishshafi"
```
