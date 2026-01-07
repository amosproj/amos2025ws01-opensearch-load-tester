.PHONY: run stop logs clean loadtest status help build

help:
	@echo "Available make commands:"
	@echo "  make build        - Builds all Docker images"
	@echo "  make run          - Starts all containers"
	@echo "  make stop         - Stops all containers"
	@echo "  make logs         - Shows all container logs"
	@echo "    make logs testdata-generator	-> logs for testdata-generator"
	@echo "    make logs load-generator		-> logs for load-generator"
	@echo "    make logs metrics-reporter	-> logs for metrics-reporter"
	@echo "    make logs loki				-> logs for loki"
	@echo "    make logs alloy    			-> logs for alloy"
	@echo "    make logs grafana    		-> logs for grafana"
	@echo "  make loadtest     - Interactive load test setup"
	@echo "  make status       - Shows resource usage of containers"
	@echo "  make clean        - Stops containers and removes volumes/images"
	@echo "  make help         - Shows this help message"
loadtest:
	@echo "Loadtest Configuration"
	@echo "=========================="
	@read -p "Number of Load-Generator Replicas (default: 3): " replicas; \
	replicas=$${replicas:-3}; \
	read -p "Number of records to generate (default: 1000): " records; \
	records=$${records:-1000}; \
	read -p "Type of test data (default: ANO): " type; \
	type=$${type:-ANO}; \
	echo ""; \
	echo "Configuring load test with:"; \
	echo "  - Load-Generator Replicas: $$replicas"; \
	echo "  - Records: $$records"; \
	echo ""; \
	sed -i.bak "s/^LOAD_GENERATOR_REPLICAS=.*/LOAD_GENERATOR_REPLICAS=$$replicas/" .env; \
	sed -i.bak "s/^TEST_DATA_GENERATION_COUNT=.*/TEST_DATA_GENERATION_COUNT=$$records/" .env; \
	sed -i.bak "s/^TEST_DATA_GENERATION_DOCUMENT_TYPE=.*/TEST_DATA_GENERATION_DOCUMENT_TYPE=$$type/" .env; \
	rm -f .env.bak; \
	echo ".env has been updated"; \
	echo ""; \
	read -p "Restart containers? (Y/n): " restart; \
	echo ""; \
	if [ -z "$$restart" ] || [ "$$restart" = "y" ] || [ "$$restart" = "Y" ]; then \
		make clean; \
		make build; \
		make run; \
	fi

build:
	@echo "Docker Images are building..."
	@docker-compose build testdata-generator
	@docker-compose build load-generator
	@docker-compose build metrics-reporter
	@echo "All images have been successfully built!"
run:
	@echo "Containers are starting..."
	@docker-compose up -d
	@echo ""
	@echo "Containers successfully started!"
	@echo ""
	@echo "Load-Generator Container:"
	@for c in $$(docker-compose ps -q); do \
		name=$$(docker inspect --format='{{.Name}}' $$c | sed 's#/##'); \
		if echo "$$name" | grep -q "load-generator"; then \
			ip=$$(docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $$c); \
			echo "  $$name"; \
			echo "    └─ IP: $$ip:8081"; \
		fi; \
	done

stop:
	@echo "Containers are stopping..."
	@docker-compose down
	@echo "Containers stopped"

logs:
	@echo "Container Logs (Ctrl+C to stop)..."; \
	services="$(filter-out $@,$(MAKECMDGOALS))"; \
	if [ -n "$$services" ]; then \
		echo "Showing logs for: $$services"; \
		docker-compose logs -f $$services; \
	else \
		echo "Showing logs for all services"; \
		docker-compose logs -f; \
	fi

status:
	@echo "Resource Usage:"
	@docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"

clean:
	@echo "If any old containers are still running, they will be removed"
	@docker-compose down --volumes --rmi local --remove-orphans
	@echo "All Containers, Volumes and Images removed"
