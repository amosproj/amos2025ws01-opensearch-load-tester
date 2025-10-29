# OpenSearch Load Tester (AMOS WS 2025)

The **OpenSearch Load Tester** is a tool designed to evaluate the performance limits of a single OpenSearch instance. It consists of two separate Spring Boot projects:

- **Test Manager** - Responsible for configuring, orchestrating and managing test scenarios.
- **Load Generator** - Executes the actual load by sending parallel queries against an OpenSearch instance.

---

## 🚀 Getting Started

### Prerequisites

- Java 25 or later
- Maven
- Docker

### Clone the Repository

```
bash

git clone https://github.com/amosproj/amos2025ws01-opensearch-load-tester.git
Navigate to the repository.
```

### Run the Test Manager

Navigate to the `test-manager` directory:

```
bash

cd test-manager
./mvnw spring-boot:run
```

Once the application has started, the Test Manager API documentation can be accessed via Swagger UI in your browser:
👉 **load **

### Run a Load Generator

Navigate to the `load-generator` directory:

```
bash

cd load-generator
./mvnw spring-boot:run
```

Multiple Load Generators can be started simultaneously to increase the total load.

---

## 📄 License

This project is licensed under the MIT License.
