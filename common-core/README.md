# common-core

Shared core module used by `load-generator` and `metrics-reporter` (DTOs and future shared helpers).

## Local build
```bash
mvn -f common-core/pom.xml clean install
```
This installs the JAR to your local Maven repo so dependent modules can resolve it (use the same JDK version as configured in the POM).
