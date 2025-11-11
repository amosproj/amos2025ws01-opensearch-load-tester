# OpenSearch Load Tester (AMOS WS 2025)

The **OpenSearch Load Tester** is a tool designed to evaluate the performance limits of a single OpenSearch instance. It consists of three separate Spring Boot projects:

- **Test Data Manager** - Responsible for indexing test data into OpenSearch.
- **Load Generator** - Executes the actual load by sending parallel queries to an OpenSearch instance.
- **Metrics Reporter** - Responsible for collecting and exporting the test results.

---

## 🚀 Getting Started

### Prerequisites

- Java 25 or later
- Maven
- Docker & Docker Compose

### Clone the Repository

```bash
git clone https://github.com/amosproj/amos2025ws01-opensearch-load-tester.git
cd amos2025ws01-opensearch-load-tester
```

### Setup Git Hooks (once only)

Install the Git hooks for automatic co-author management:

```bash
cd scripts
./setup-ooks.sh //little tipfeheler ./setup-hooks.sh
```

See [scripts/README.md](scripts/README.md) for details on using `@mentions` in commits.


---

## 🐳 Docker Setup (Recommended)

### Run with Docker Compose

The easiest way to start these three services with automatic port management:

```bash
docker-compose up --build -d
```

This will start:

1. The import of test data via the **Test Data Manager**.
2. The **Metrics Reporter** and one instance of the **Load Generator** after the test data has been imported.

### Stop the Services

```bash
docker-compose down
```

### Scale Load Generators

To run multiple Load Generator instances:

```bash
REPLICAS=3 docker-compose up --build -d
```

### Run the Whole Stack with Integrated OpenSearch

```bash
REPLICAS=3 docker-compose --profile opensearch up --build -d
```

Example Usage: `curl -X PUT "http://localhost:9200/test-index"`

### Remove all Docker Resources

```bash
docker-compose --profile opensearch down --volumes --rmi local --remove-orphans
```

---

## 💻 Manual Setup (Without Docker)

### Run the Test Manager

Navigate to the `test-manager` directory:

```bash
cd test-manager
./mvnw spring-boot:run
```

Once the application has started, the Test Manager API documentation can be accessed via Swagger UI in your browser:
👉 **http://localhost:8080/test-manager-swagger-ui.html**

### Run a Load Generator

Navigate to the `load-generator` directory:

```bash
cd load-generator
./mvnw spring-boot:run
```

Multiple Load Generators can be started simultaneously to increase the total load.

---

## 📄 License

This project is licensed under the MIT License.
