<p align="center">
    <img src="https://github.com/user-attachments/assets/bca47974-651c-4e81-bd67-6e6cc4c3cf6b" alt="LoadTester Logo" width="300">
</p>

# OpenSearch Load Tester (AMOS WS 2025) 🔎⚡

The **OpenSearch Load Tester** is a tool designed to evaluate the performance limits of a single OpenSearch instance. It consists of three separate Spring Boot projects and one JavaFX project:

- **Test Data Generator (Spring-Boot)** - Responsible for generating testdata and indexing into OpenSearch.
- **Load Generator (Spring-Boot)** - Executes the actual load by sending parallel queries to an OpenSearch instance.
- **Metrics Reporter (Spring-Boot)** - Responsible for collecting and exporting the test results.
- **UI (JavaFX)** - Graphical user interface to configure and start load tests.

In addition, Docker Compose configurations are provided to facilitate easy deployment and management of the entire stack,
including an optional integrated OpenSearch instance for testing purposes, as well as Grafana Stack (Alloy + Loki + Grafana) to visualize the results.

---

## 🚀 Getting Started

### Prerequisites

- Java 25 or later ☕
- Maven 🛠️
- Docker & Docker Compose 🐳
- Make 🔧

### Clone the Repository

```bash
git clone https://github.com/amosproj/amos2025ws01-opensearch-load-tester.git
cd amos2025ws01-opensearch-load-tester
```

### Start the Load Testing via GUI 🖱️🖥️

To start load testing, you can use the UI by running the following command:

```bash
bash ./start-ui.sh         # Normal start
bash ./start-ui.sh rebuild # With full rebuild
```

The `rebuild` flag triggers a complete rebuild of all components.

Once launched, you can configure and initiate load tests using the dashboard.

**Execution and Results**

Clicking the "Start Load Test" button triggers the build process for all required Docker images and launches the complete OpenSearch load-tester stack.
You can view the results in Grafana:

> URL: http://localhost:3000
>
> Path: Dashboards → Load Testing → OpenSearch Load Test

### Start the Load Testing via CLI

For convenient operation, Makefile targets are provided to deploy the load-generator on systems without GUI.

#### Interactive Loadtest Setup via CLI

- `make loadtest`  
  Interactive setup for load tests. Prompts for:

  - `LOAD_GENERATOR_REPLICAS` (default: `3`)
  - `TEST_DATA_GENERATION_COUNT` (default: `1000`)
  - `TEST_DATA_GENERATION_DOCUMENT_TYPE` (default: `ANO`)

  updates the value in `.env` and then runs `make clean`, `make build`, `make run` and `make curl` to open shell to start loadtest 🚀.

---

## 🐳 Docker Setup

### Run with Docker Compose ▶️

The easiest way to start the whole stack with automatic port management:

```bash
docker-compose up --build -d
```

This will start:

1. The generation of testdata via the **Test Data Generator**
2. The **Metrics Reporter**
3. The **Grafana Stack** (Alloy + Loki + Grafana) to visualize the results
4. All **Load Generator** replicas after the testdata has been indexed.

### Stop the Services ⏹️

```bash
docker-compose down
```

### Remove all Docker Resources

```bash
docker-compose down --volumes --rmi local --remove-orphans
```

---

## Further Documentation

- [User documentation](https://github.com/amosproj/amos2025ws01-opensearch-load-tester/wiki/User-Documentation)
- [Build documentation](https://github.com/amosproj/amos2025ws01-opensearch-load-tester/wiki/Build-Documentation)
- [Design documentation](https://github.com/amosproj/amos2025ws01-opensearch-load-tester/wiki/Design-Documentation)
- [Developer documentation](https://github.com/amosproj/amos2025ws01-opensearch-load-tester/wiki/Developer-Documentation)

---

## 📄 License

This project is licensed under the MIT License.
