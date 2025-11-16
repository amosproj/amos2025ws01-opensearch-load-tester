.PHONY: run stop logs clean loadtest setup-loadtest status help

help:
	@echo "Verfügbare Befehle:"
	@echo "  make run          - Startet alle Container"
	@echo "  make stop         - Stoppt alle Container"
	@echo "  make logs         - Zeigt Container Logs"
	@echo "  make loadtest     - Interaktiver Loadtest Setup"
	@echo "  make status       - Zeigt Container-Status"
	@echo "  make clean        - Stoppt Container und löscht Volumes"

loadtest:
	@echo "Loadtest Konfiguration"
	@echo "=========================="
	@read -p "Anzahl Load-Generator Replicas (default: 3): " replicas; \
	replicas=$${replicas:-3}; \
	read -p "Anzahl zu generierende Datensätze (default: 1000): " records; \
	records=$${records:-1000}; \
	echo ""; \
	echo "Konfiguriere Loadtest mit:"; \
	echo "  - Load-Generator Replicas: $$replicas"; \
	echo "  - Datensätze: $$records"; \
	echo ""; \
	sed -i.bak "s/^LOAD_GENERATOR_REPLICAS=.*/LOAD_GENERATOR_REPLICAS=$$replicas/" .env; \
	sed -i.bak "s/^TEST_DATA_GENERATION_COUNT=.*/TEST_DATA_GENERATION_COUNT=$$records/" .env; \
	rm -f .env.bak; \
	echo ".env wurde aktualisiert"; \
	echo ""; \
	read -p "Container neu starten? (Y/n): " restart; \
	if [ -z "$$restart" ] || [ "$$restart" = "y" ] || [ "$$restart" = "Y" ]; then \
		make clean; \
		make run; \
		echo ""; \
		echo "🎯 Loadtest läuft!"; \
		make logs;  \
	fi


run:
	@echo "Container werden gestartet..."
	@docker-compose --profile opensearch up --build -d > /dev/null 2>&1
	@echo ""
	@echo "Container erfolgreich gestartet!"
	@echo ""
	@echo " Container IP-Adressen:"
	@for c in $$(docker-compose ps -q); do \
		name=$$(docker inspect --format='{{.Name}}' $$c | sed 's#/##'); \
		ip=$$(docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $$c); \
		ports=$$(docker inspect --format='{{range $$p, $$conf := .NetworkSettings.Ports}}{{if $$conf}}{{(index $$conf 0).HostPort}}->{{$$p}} {{end}}{{end}}' $$c); \
		if [ -n "$$ports" ]; then \
			echo "   $$name"; \
			echo "     └─ IP: $$ip"; \
			echo "     └─ Ports: $$ports"; \
		else \
			echo "   $$name → $$ip"; \
		fi; \
	done

stop:
	@echo "Stoppe Container..."
	@docker-compose down
	@echo "Container gestoppt"

logs:
	@echo "Container-Logs (Ctrl+C zum Beenden)..."
	@docker-compose logs -f

status:
	@echo "Ressourcen-Nutzung:"
	@docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"

clean:
	@echo "Cleanup"
	@docker-compose --profile opensearch down --volumes --rmi local --remove-orphans
	@echo "Alle Container, Volumes und Images entfernt"
