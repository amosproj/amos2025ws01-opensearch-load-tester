# Contributing to OpenSearch Load Tester

Thank you for your interest in contributing to this project!

## Getting Started

```bash
# Clone the repository
git clone https://github.com/amosproj/amos2025ws01-opensearch-load-tester.git
cd amos2025ws01-opensearch-load-tester

# Install Git hooks
./scripts/setup-hooks.sh

# Configure Git identity (required for commits)
git config user.name "Your Name"
git config user.email "your.email@example.com"
```

> For detailed setup instructions (Code Style, IDE configuration, Git Hooks), see the [Developer Documentation](https://github.com/amosproj/amos2025ws01-opensearch-load-tester/wiki/Developer-Documentation).

## Commit Guidelines

Follow the [Conventional Commits](https://www.conventionalcommits.org/) standard:

```
<type>: <description>
```

**Types:** `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

**Examples:**

```bash
git commit -m "feat: add rate limiting to API endpoints"
git commit -m "fix: resolve null pointer in TestScenarioController"
```

### Co-Authoring

Mention collaborators with `@username` in your commit message:

```bash
git commit -m "feat: add metrics dashboard @LeaBuchner @SeboKnt"
```

The Git hook automatically converts these to `Co-Authored-By` trailers.

**Team members:** @LeaBuchner, @engelharddirk, @Carlit0, @BeEugen, @SeboKnt, @Leolingio, @Hydraneut, @SaraBelz

## Branching Strategy

```
<type>/<issue-number>-<short-description>

# Examples:
feature/42-add-authentication
fix/38-memory-leak
docs/51-api-documentation
```

## Pull Request Process

**Before creating a PR:**

- [ ] Code compiles without errors
- [ ] All tests pass
- [ ] Commit messages follow conventions

**Review requirements:**

- 2 approvals from team members
- All CI checks must pass
- No merge conflicts

## Issue Reporting

- **Bugs:** Check if already reported, then create an issue with steps to reproduce
- **Features:** Create an issue with label `enhancement`

## Need Help?

- [Developer Documentation](https://github.com/amosproj/amos2025ws01-opensearch-load-tester/wiki/Developer-Documentation) - Setup, Code Style, Git Hooks
- [Project Wiki](https://github.com/amosproj/amos2025ws01-opensearch-load-tester/wiki) - General documentation
- Team chat for questions

## License

This project is licensed under the MIT License - see [LICENSE](LICENSE) for details.
