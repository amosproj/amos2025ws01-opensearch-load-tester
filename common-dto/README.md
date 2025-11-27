# common-dto

Shared DTO module used by `load-generator` and `metrics-reporter`.

## Local build

```bash
mvn -f common-dto/pom.xml clean install
```

This installs the JAR to your local Maven repo so dependent modules can resolve it (use the same JDK version as configured in the POM).
