#!/usr/bin/env python3
import importlib.util
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WORKLOAD = ROOT / "workloads" / "amos-load-tester" / "workload.py"


def load_workload_module():
    spec = importlib.util.spec_from_file_location("amos_workload", WORKLOAD)
    if spec is None or spec.loader is None:
        raise RuntimeError("Failed to load workload module")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def render_template(template_text: str, params: dict) -> str:
    text = template_text
    for key, value in params.items():
        text = text.replace("{{" + key + "}}", str(value))
    return text


def extract_placeholders(template_text: str) -> set:
    return set(re.findall(r"{{\\s*([^}]+?)\\s*}}", template_text))


def main() -> int:
    module = load_workload_module()
    failures = []

    for query_type, template_path in module.TEMPLATE_MAP.items():
        params = module._query_params_for(query_type)
        template_text = module._load_template(template_path)
        placeholders = extract_placeholders(template_text)
        missing = placeholders - set(params.keys())
        if missing:
            failures.append((query_type, f"missing params: {sorted(missing)}"))
            continue

        rendered = render_template(template_text, params)

        if "{{" in rendered or "}}" in rendered:
            failures.append((query_type, "unreplaced placeholder"))
            continue

        try:
            json.loads(rendered)
        except json.JSONDecodeError as exc:
            failures.append((query_type, f"invalid json: {exc}"))

    if failures:
        print("Query validation failed:")
        for query_type, reason in failures:
            print(f"- {query_type}: {reason}")
        return 1

    print("All query templates rendered without placeholders and JSON is valid.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
