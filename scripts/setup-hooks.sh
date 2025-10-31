#!/bin/bash

# setup-hooks.sh
# Installs Git hooks for the repository
# Cross-platform compatible (macOS, Linux, Windows Git Bash)

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
GIT_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || echo "$SCRIPT_DIR/..")"
HOOKS_SRC="$SCRIPT_DIR/git-hooks"
HOOKS_DEST="$GIT_ROOT/.git/hooks"

echo "🔧 Setting up Git hooks..."

# Check if we're in a git repository
if [ ! -d "$GIT_ROOT/.git" ]; then
    echo "❌ Error: Not in a Git repository"
    exit 1
fi

# Check if hooks source directory exists
if [ ! -d "$HOOKS_SRC" ]; then
    echo "❌ Error: Hooks directory not found: $HOOKS_SRC"
    exit 1
fi

# Create hooks directory if it doesn't exist
mkdir -p "$HOOKS_DEST"

# Install each hook
HOOKS_INSTALLED=0
for hook_file in "$HOOKS_SRC"/*; do
    if [ -f "$hook_file" ]; then
        hook_name=$(basename "$hook_file")
        dest_file="$HOOKS_DEST/$hook_name"
        
        # Backup existing hook if present
        if [ -f "$dest_file" ]; then
            backup_file="$dest_file.backup-$(date +%Y%m%d-%H%M%S)"
            echo "📦 Backing up existing hook: $hook_name → $backup_file"
            mv "$dest_file" "$backup_file"
        fi
        
        # Copy hook and make executable
        cp "$hook_file" "$dest_file"
        chmod +x "$dest_file"
        echo "✅ Installed: $hook_name"
        HOOKS_INSTALLED=$((HOOKS_INSTALLED + 1))
    fi
done

if [ $HOOKS_INSTALLED -eq 0 ]; then
    echo "⚠️  No hooks found to install"
    exit 1
fi

echo ""
echo "🎉 Successfully installed $HOOKS_INSTALLED hook(s)!"
echo ""
echo "📝 Usage:"
echo "   - Use @username in commit messages to add co-authors"
echo "   - Edit .coauthors file to add/update team members"
echo "   - Signed-off-by is automatically added to the commit message"
echo ""
echo "Example commit:"
echo "   git commit -m 'Add new feature @maxmuster'"
echo "Example outcome:"
echo "   Add new feature"
echo "   Co-Authored-By: Max Mustermann <maxmuster@example.com>"
echo "   Signed-off-by: Your Name <your.email@example.com>"
echo ""

