# Contributing to OpenSearch Load Tester

Thank you for your interest in contributing to this project! 🎉

## 🚀 Getting Started

### 1. Repository Setup

```bash
# Clone the repository
git clone https://github.com/amosproj/amos2025ws01-opensearch-load-tester.git
cd amos2025ws01-opensearch-load-tester

# Install Git hooks (important!)
cd scripts
./setup-hooks.sh
cd ..
```

### 2. Git Configuration

Make sure your Git configuration is correct:

```bash
git config user.name "Your Name"
git config user.email "your.email@example.com"
```

**Important:** Both `user.name` and `user.email` must be configured, otherwise the Git hook will fail and prevent commits. The hook automatically adds a `Signed-off-by` trailer to every commit, which requires your Git identity to be set up.

## 📝 Commit Guidelines

### Signed-off-by Requirement

**All commits must be signed off.** The Git hook automatically adds a `Signed-off-by` trailer to every commit message. This is a requirement for contributing to this project.

The `Signed-off-by` line indicates that you certify:

- You have the right to submit the work under the project's license
- You understand and agree to the Developer Certificate of Origin (DCO)

**If the Git hook fails** with an error about missing `user.name` or `user.email`, you must configure Git:

```bash
git config user.name "Your Name"
git config user.email "your.email@example.com"
```

### Co-Authoring with @mentions

When collaborating with others (pair programming, joint debugging, etc.), mention them in your commit message:

```bash
# Single co-author
git commit -m "feat: feature @LeaBuchner"

# Multiple co-authors
git commit -m "fix: fix @engelharddirk @SeboKnt"

# With detailed description
git commit -m "fix:  memory leak in load generator

Found and fixed critical memory leak that occurred during
high-load scenarios.

@Hydraneut @Leolingio"
```

The Git hook automatically adds the Co-Authored-By trailers:

```
Fix memory leak in load generator

Found and fixed critical memory leak that occurred during
high-load scenarios.

Co-Authored-By: Alexander Lorenz <166607070+Hydraneut@users.noreply.github.com>
Co-Authored-By: Leo Hofmann <67865068+Leolingio@users.noreply.github.com>
Signed-off-by: Your Name <your.email@example.com>
```

**Note:** The `@mentions` are removed from the final commit message body, but the co-authors are properly credited via the `Co-Authored-By` trailers.

### Manual Trailers Are Not Allowed

**Do NOT add Co-Authored-By trailers manually!**

If you try to add them manually, the Git hook will **abort the commit** and show an error message

**Example of what NOT to do:**

```bash
# ❌ Wrong - manual trailers
git commit -m "Add feature

Co-Authored-By: Some Person <someone@example.com>"
```

**Instead, use @mentions:**

```bash
# ✅ Correct - use @mentions
git commit -m "Add feature @someUsername"
```

The hook ensures consistency and prevents incorrect or mismatched email formats from the different built-in `co-authored-by` functionalities vscode, GitHub Desktop, command line etc..

### Commit Message Format

Follow the [Conventional Commits](https://www.conventionalcommits.org/) standard:

```
<type>: <description>

[optional body]

[optional footer]
```

**Types:**

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, whitespace)
- `refactor`: Code refactoring
- `test`: Adding or modifying tests
- `chore`: Build, dependencies, configuration changes

**Examples:**

```bash
git commit -m "feat: add rate limiting to API endpoints @BeEugen"
git commit -m "fix: resolve null pointer in TestScenarioController"
git commit -m "docs: update API documentation in README"
git commit -m "refactor: simplify query builder logic @SaraBelz"
```

## 🌿 Branching Strategy

### Branch Naming

```
<type>/<issue-number>-<short-description>

# Examples:
feature/42-add-authentication
fix/38-memory-leak
docs/51-api-documentation
```

### Workflow

```bash
# 1. Create a new branch
git checkout -b feature/123-my-feature

# 2. Make changes and commit
git commit -m "feat: add my feature @LeaBuchner"

# 3. Push the branch
git push origin feature/123-my-feature

# 4. Create a Pull Request on GitHub
```

## 🧪 Code Quality

### Before Committing

Make sure you sign off your commits - the Git hook will do this automatically, but only if your Git configuration is set up correctly.

**Pre-commit checklist:**

- [ ] Git `user.name` and `user.email` are configured
- [ ] Code compiles without errors
- [ ] All tests pass
- [ ] Commit message follows conventions

```bash
# Test Java/Maven projects
cd test-manager
./mvnw clean test

cd load-generator
./mvnw clean test
```

### Code Style

- **Java**: Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- **Indentation**: 4 spaces (no tabs)
- **Line Length**: Maximum 120 characters
- **Comments**: Javadoc for public methods and classes

## 👥 Team Members

Current team members (for `@mentions`):

@LeaBuchner, @engelharddirk, @Carlit0, @BeEugen, @SeboKnt, @Leolingio, @Hydraneut, @SaraBelz

This list is maintained in `.coauthors` and can be updated there.

## 🐛 Issue Reporting

### Bug Reports

If you find a bug:

1. Check if the bug is already reported as an issue
2. If not, create a new issue with:
   - **Title**: Brief description
   - **Description**: What happened? What was expected?
   - **Steps to Reproduce**: How can the bug be reproduced?
   - **Environment**: Java version, OS, etc.
   - **Screenshots**: If relevant

### Feature Requests

For new features:

1. Create an issue with the label `enhancement`
2. Describe:
   - **Problem**: What problem does the feature solve?
   - **Solution**: How should the feature work?
   - **Alternatives**: Are there other approaches?

## 📖 Documentation

- Document new features in code (Javadoc)
- Update `README.md` for API changes
- Add examples for new features
- Update `Documentation/` folder when necessary

## 🔍 Pull Request Process

### Checklist

Before creating a Pull Request:

- [ ] Code compiles without errors
- [ ] All tests pass successfully
- [ ] New features have tests
- [ ] Documentation is updated
- [ ] Commit messages follow conventions
- [ ] Co-authors are mentioned (if applicable)
- [ ] **All commits are signed off** (automatically done by Git hook)

### PR Description

```markdown
## Description

Brief description of changes

## Related Issues

Fixes #123

## Changes

- Added feature A
- Fixed bug B
- Updated documentation

## Testing

- [ ] Unit tests added
- [ ] Integration tests performed
- [ ] Manual testing performed

## Screenshots

(if UI changes)
```

### Review Process

1. At least **2 approval** from a team member
2. All **CI checks** must pass
3. No **merge conflicts**
4. Code review feedback has been addressed

## 🎯 Best Practices

### Do's ✅

- ✅ Small, focused commits
- ✅ Meaningful commit messages
- ✅ Mention co-authors when collaborating
- ✅ Write tests for new code
- ✅ Document your code
- ✅ Pull regularly from `main`
- ✅ Configure Git identity before first commit

### Don'ts ❌

- ❌ Large commits with many unrelated changes
- ❌ Push directly to `main` (forbidden)
- ❌ Commits without descriptions
- ❌ Code without tests
- ❌ Commit debugging code
- ❌ Commit API keys or secrets
- ❌ Skip Git hook setup

## 🆘 Troubleshooting

### Git Hook Errors

**Error: "git config user.name is empty"**

```bash
# Solution: Configure Git identity
git config user.name "Your Name"
git config user.email "your.email@example.com"

# Verify configuration
git config user.name
git config user.email
```

**Error: "git config user.email is empty"**

Same solution as above - make sure both `user.name` and `user.email` are configured.

**Hook not executing**

```bash
# Check if hook is installed and executable
ls -la .git/hooks/prepare-commit-msg

# If not executable, make it executable
chmod +x .git/hooks/prepare-commit-msg

# If hook doesn't exist, reinstall
cd scripts
./setup-hooks.sh
```

**Co-author not found**

```bash
# Check if .coauthors file exists
cat .coauthors

# Check if username is in the file (case-insensitive)
grep -i "username" .coauthors
```

### Getting Help

If you have questions or problems:

1. **Git Hooks**: See [scripts/README.md](scripts/README.md)
2. **Team**: Ask in the team chat
3. **Issues**: Create an issue on GitHub
4. **Documentation**: Check [Documentation/README.md](Documentation/README.md)

## 📜 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) for details.

---

**Happy Coding! 🚀**
