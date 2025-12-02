.PHONY: run stop logs clean loadtest status help build curl

help:
	@echo "Verfügbare Befehle:"
	@echo "  make build        - Baut alle Docker-Images"
	@echo "  make run          - Startet alle Container"
	@echo "  make stop         - Stoppt alle Container"
	@echo "  make logs         - Zeigt Container-Logs"
	@echo "  make curl         - Startet eine Shell im Curl-Debug Container"
	@echo "  make loadtest     - Interaktiver Loadtest Setup"
	@echo "  make status       - Zeigt Ressourcen-Nutzung der Container"
	@echo "  make clean        - Stoppt Container und löscht Volumes/Images"
	@echo "  make help         - Zeigt diese Hilfe an"

loadtest:
	@echo "Loadtest Konfiguration"
	@echo "=========================="
	@read -p "Anzahl Load-Generator Replicas (default: 3): " replicas; \
	replicas=$${replicas:-3}; \
	read -p "Anzahl zu generierende Datensätze (default: 1000): " records; \
	records=$${records:-1000}; \
	read -p "Art der Testdata (default: ANO): " type; \
	type=$${type:-ANO}; \
	echo ""; \
	echo "Konfiguriere Loadtest mit:"; \
	echo "  - Load-Generator Replicas: $$replicas"; \
	echo "  - Datensätze: $$records"; \
	echo ""; \
	sed -i.bak "s/^LOAD_GENERATOR_REPLICAS=.*/LOAD_GENERATOR_REPLICAS=$$replicas/" .env; \
	sed -i.bak "s/^TEST_DATA_GENERATION_COUNT=.*/TEST_DATA_GENERATION_COUNT=$$records/" .env; \
	sed -i.bak "s/^TEST_DATA_GENERATION_DOCUMENT_TYPE=.*/TEST_DATA_GENERATION_DOCUMENT_TYPE=$$type/" .env; \
	rm -f .env.bak; \
	echo ".env wurde aktualisiert"; \
	echo ""; \
	read -p "Container (neu) starten? (Y/n): " restart; \
	echo ""; \
	if [ -z "$$restart" ] || [ "$$restart" = "y" ] || [ "$$restart" = "Y" ]; then \
		make clean; \
		make build; \
		make run; \
		make curl; \
	fi

build:
	@echo "Docker Images werden gebaut..."
	@docker-compose build testdata-generator
	@docker-compose build load-generator
	@docker-compose build metrics-reporter
	@echo "Alle Images wurden erfolgreich gebaut!"

run:
	@echo "Container werden gestartet..."
	@docker-compose --profile curl up -d
	@echo ""
	@echo "Container erfolgreich gestartet!"
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
	@echo "Container werden gestoppt..."
	@docker-compose down
	@echo "Container gestoppt"

logs:
	@echo "Container-Logs (Ctrl+C zum Beenden)..."
	@docker-compose logs -f

status:
	@echo "Ressourcen-Nutzung:"
	@docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"

curl:
	@echo "Attach Curl Container..."
	@docker exec -it curl sh

clean:
	@echo "Falls noch alte Container laufen werden die gelöscht"
	@docker-compose --profile curl down --volumes --rmi local --remove-orphans
	@echo "Alle Container, Volumes und Images entfernt"
