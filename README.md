# OpenSearch Load Tester (AMOS WS 2025)

The **OpenSearch Load Tester** is a tool designed to evaluate the performance limits of a single OpenSearch instance. It consists of two separate Spring Boot projects:

- **Test Manager** - Responsible for configuring, orchestrating and managing test scenarios.
- **Load Generator** - Executes the actual load by sending parallel queries against an OpenSearch instance.

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

---

## 🐳 Docker Setup (Recommended)

### Run with Docker Compose

The easiest way to start both services with automatic port management:

```bash
docker-compose up --build
```

This will start:

- **Test Manager** on `http://localhost:8080`
- **Load Generator** on `http://localhost:8081`

Access the Test Manager API documentation via Swagger UI:
👉 **http://localhost:8080/test-manager-swagger-ui.html**

### Stop the Services

```bash
docker-compose down
```

### Scale Load Generators

To run multiple Load Generator instances:

```bash
docker-compose up --scale load-generator=3
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
