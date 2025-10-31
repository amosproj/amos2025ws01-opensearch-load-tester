# Git Hooks Setup

This directory contains Git hooks and setup scripts for the team.

## 🎯 Purpose

The Git hooks automate:

- **Co-Authored-By**: Automatic addition of co-authors based on `@mentions` in commit messages
- **Signed-off-by**: Automatic signing of commits

## 📦 Installation

### One-time per developer

After cloning the repository, run once:

#### macOS / Linux

```bash
cd scripts
./setup-hooks.sh
```

#### Windows (Git Bash)

```bash
cd scripts
./setup-hooks.sh
```

#### Windows (CMD)

```cmd
cd scripts
setup-hooks.bat
```

## 📝 Usage

### Commit with Co-Authors

Use `@username` in your commit message:

```bash
git commit -m "Add search feature @Carlit0"
```

The hook automatically converts this to:

```
Add search feature

Co-Authored-By: Carlo Strachwitz <carlostrachwitz@googlemail.com>
Signed-off-by: Your Name <your.email@example.com>
```

### Multiple Co-Authors

```bash
git commit -m "Refactor API @engelharddirk @SeboKnt @Leolingio"
```

### Case-Insensitive

The `@mentions` are case-insensitive.

### ⚠️ No Manual Trailers!

**Do NOT add Co-Authored-By manually!**

**Wrong:**

```bash
git commit -m "feat: feature

Co-Authored-By: Person <email@example.com>"
```

**Correct:**

```bash
git commit -m "Add feature @username"
```

This ensures that the correct emails from `.coauthors` are always used.

## ⚙️ Configuration

### Adding/Updating Team Members

Edit the `.coauthors` file in the repository root:

```bash
# Format: github-username=Full Name <email>
newTeamMember=Max Mustermann <max.mustermann@example.com>
```

Usernames in the config file should be lowercase for consistency.

### GitHub Email Format Consistency

**Important:** GitHub uses different email formats across different interfaces:

- **Old format**: `ID+username@users.noreply.github.com`
- **New format**: `ID+username.noreply.github.com` (without `@users`)

This inconsistency can cause the same person to appear with different emails in commits.

**Solution:** Just use the git hook!

### Finding GitHub Email Addresses

If you don't know a team member's GitHub email address:

1. **From Git History** (recommended):

   ```bash
   git log --all --grep="Co-Authored-By.*username" --pretty=format:"%B" | grep "Co-Authored-By:"
   ```

## 🔧 Technical Details

### What Does the Hook Do?

1. **Parse**: Extracts all `@mentions` from the commit message
2. **Lookup**: Looks up email addresses in `.coauthors`
3. **Cleanup**: Removes `@mentions` from the final commit message
4. **Trailers**: Adds `Co-Authored-By` and `Signed-off-by` trailers

### Cross-Platform Compatibility

The hook is compatible with:

- ✅ macOS (bash/zsh)
- ✅ Linux (bash/sh)
- ✅ Windows (Git Bash)

### Performance

- **Fast**: No API calls, only local parsing
- **Offline**: Works without internet connection

## 🐛 Troubleshooting

### Hook Not Executing

```bash
# Check if hook is installed and executable
ls -la .git/hooks/prepare-commit-msg

# If not executable:
chmod +x .git/hooks/prepare-commit-msg
```

### Co-Author Not Found

```bash
# Check if .coauthors file exists
cat .coauthors

# Check if username is in the file (case-insensitive)
grep -i "username" .coauthors
```

### Git Config Missing

If you see these errors:

```
Error: git config user.name is empty
Error: git config user.email is empty
```

Then configure Git:

```bash
git config user.name "Your Name"
git config user.email "your.email@example.com"
```
