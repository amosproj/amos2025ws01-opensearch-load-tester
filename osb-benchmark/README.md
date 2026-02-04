# OpenSearch Benchmark Integration

Dieses Verzeichnis integriert **OpenSearch Benchmark (OSB)** als Alternative zum bestehenden Load Tester. Die Workload spiegelt die Query-Typen und Szenarien aus `load-generator/src/main/resources/scenarios` nach.

## Schnellstart (mit bestehendem Stack)

1. Basis-Stack starten (OpenSearch + ggf. weitere Services):

```bash
docker compose -f docker-compose.yaml up -d
```

2. OSB ausführen (separater Compose-Override):

```bash
docker compose -f docker-compose.yaml -f osb-benchmark/docker-compose.osb.yaml run --rm osb-benchmark
```

## Szenarien (Test Procedures)

Die Workload definiert folgende Test-Procedures:

- `default`
- `custom`
- `ano-low`, `ano-medium`, `ano-high`, `ano-multi-regex`, `ano-long`, `ano-full`
- `duo-low`, `duo-medium`, `duo-high`, `duo-long`, `duo-full`

Beispiel für ein anderes Szenario:

```bash
OSB_TEST_PROCEDURE=duo-medium \
  docker compose -f docker-compose.yaml -f osb-benchmark/docker-compose.osb.yaml run --rm osb-benchmark
```

## Durchsatz und Clients anpassen

`target-throughput` wird in OSB **als Gesamt-Throughput über alle Clients** verstanden. Wenn du mehrere Clients verwendest, bleibt der Gesamtwert gleich und wird intern aufgeteilt.

Beispiel mit 4 Clients und angepasstem Throughput:

```bash
OSB_WORKLOAD_PARAMS=clients:4,duo_medium_target_throughput:200 \
  OSB_TEST_PROCEDURE=duo-medium \
  docker compose -f docker-compose.yaml -f osb-benchmark/docker-compose.osb.yaml run --rm osb-benchmark
```

## Ergebnisse

OSB schreibt die Summary im CSV-Format nach `/reports/osb_summary.csv`.

Hinweis: Der OSB-Container läuft als `root`, damit er in das Volume `/reports` schreiben kann.

## Sampling für bessere Trefferquote

Damit Term-Queries echte Werte treffen, sampeln wir optional Feldwerte aus OpenSearch und speichern sie in einer Seed-Datei.
Das passiert standardmäßig vor dem OSB-Run.

Um das Sampling zu deaktivieren:

```bash
OSB_SAMPLE_VALUES=0 \\
  docker compose -f docker-compose.yaml -f osb-benchmark/docker-compose.osb.yaml run --rm osb-benchmark
```

Optional kannst du die OpenSearch-URL überschreiben:

```bash
OPENSEARCH_URL=http://deine-opensearch:9200 \\
  docker compose -f docker-compose.yaml -f osb-benchmark/docker-compose.osb.yaml run --rm osb-benchmark
```

Falls du den Speicherort der Seed-Datei anpassen möchtest:

```bash
OSB_SEED_VALUES_PATH=/reports/osb_seed_values.json \\
  docker compose -f docker-compose.yaml -f osb-benchmark/docker-compose.osb.yaml run --rm osb-benchmark
```

## Query-Validierung

Zum Kontrollieren, ob alle Templates vollständig ersetzt werden (keine `{{...}}` Reste), gibt es einen Validator:

```bash
python3 osb-benchmark/scripts/validate_queries.py
```

## Hinweise

- Der Docker-Workflow von OSB unterstützt nur den `benchmark-only`-Pipeline-Modus. Es wird also eine **bereits existierende OpenSearch-Instanz** mit Daten benötigt.
- Die Query-Templates sind aus dem Load Generator kopiert und werden in `workload.py` parametrisiert (ähnliche Randomisierung wie im Java-Loadtester).

## Struktur

```
osb-benchmark/
  docker-compose.osb.yaml
  scripts/
    run-osb.sh
    validate_queries.py
  workloads/
    amos-load-tester/
      workload.json
      workload.py
      templates/
        query-templates/
        queries/
```
