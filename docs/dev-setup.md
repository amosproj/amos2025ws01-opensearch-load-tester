# Development Setup Guide

**Quick setup for new developers** - Ready in 3 minutes!

## Quick Start

### 1. Enable Code Style

1. Open IntelliJ IDEA and load the project
2. **Settings** → **Editor** → **Code Style**
3. Select **"Project"** from the Scheme dropdown
   - Automatically loaded from `.idea/codeStyles/Project.xml`
     - if not import it via the the little gear icon first
4. Enable **"Use per-project settings"** (if available)
5. ✅ Enable **"Enable EditorConfig support"**
6. Click **Apply** → **OK**

> 💡 If "Project" doesn't appear: Restart IntelliJ IDEA

### 2. Enable Auto-Format (Required)

1. **Settings** → **Tools** → **Actions on Save**
2. ✅ Enable **"Reformat code"**
3. ✅ Enable **"Optimize imports"**
4. Click **Apply** → **OK**

**Done!** 🎉 Code will now auto-format on save.

---

## Code Style Overview

Follows **[Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)**:

- ✅ **Egyptian Braces**: `{` on same line
- ✅ **Import Order**: java → javax → org → com → static
- ✅ **Spaces**: `if (condition)` not `if(condition)`
- ✅ **Indentation**: 4 spaces
- ✅ **Line Length**: Max 120 characters

---

## Test

Create a test file and format with `Ctrl+Alt+L` (Windows/Linux) or `Cmd+Option+L` (macOS):

**Before:**

```java
public void test()
{
    if(condition)
    {
        System.out.println("test");
    }
}
```

**After (correct):**

```java
public void test() {
    if (condition) {
        System.out.println("test");
    }
}
```

---

## CLI Formatting (Optional)

If your IDE formatting is off or you want a quick check in CI, use Spotless:

```bash
mvn -f common-core/pom.xml spotless:apply
mvn -f load-generator/pom.xml spotless:apply
mvn -f metrics-reporter/pom.xml spotless:apply
mvn -f testdata-generator/pom.xml spotless:apply
```

Check formatting without changing files:

```bash
mvn -f common-core/pom.xml -P lint validate
mvn -f load-generator/pom.xml -P lint validate
mvn -f metrics-reporter/pom.xml -P lint validate
mvn -f testdata-generator/pom.xml -P lint validate
```

---

## Troubleshooting

**Code style not applied?**

- Restart IntelliJ IDEA
- Verify **"Project"** scheme is selected

**Auto-format not working?**

- Check **Settings** → **Tools** → **Actions on Save**
- Both options must be enabled

**Imports not sorted?**

- Use **Code** → **Optimize Imports** (`Ctrl+Alt+O` / `Cmd+Option+O`)

---

## Code Style Examples

### Egyptian Braces (Google Style Guide)

**✅ Correct:**

```java
public void method() {
    if (condition) {
        // code
    } else {
        // code
    }
}
```

**❌ Incorrect:**

```java
public void method()
{
    if (condition)
    {
        // code
    }
    else
    {
        // code
    }
}
```

### Spaces (Google Style Guide)

**✅ Correct:**

```java
if (condition) {          // Space between if and (
    int result = a + b;    // Spaces around operators
    String x = a != null ? a : "default";
    Runnable r = () -> {};
}
```

**❌ Incorrect:**

```java
if(condition) {
    int result = a+b;
    String x = a!=null?a:"default";
    Runnable r = ()->{};
}
```

---

## Need Help?

- **Issues?** → Restart IntelliJ IDEA
- **Details?** → [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)

---

**Last Updated**: 2025-11-11
