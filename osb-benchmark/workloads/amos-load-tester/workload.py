import json
import os
import random
import re
from datetime import date, timedelta

TEMPLATE_ROOT = os.path.join(os.path.dirname(__file__), "templates")

TEMPLATE_MAP = {
    "ANO_PAYROLL_RANGE": "query-templates/q1_ano_payroll_range.json",
    "DUO_INVOICE_CATEGORY": "query-templates/q2_duo_invoice_category.json",
    "DUO_STATE_LOCATION": "query-templates/q3_duo_state_location.json",
    "DUO_BOOKING_BY_CLIENT_AND_STATE": "query-templates/q4_duo_booking_by_client_and_state.json",
    "ANO_CLIENTS_AGGREGATION": "query-templates/q5_ano_clients_aggregation.json",
    "ANO_CLIENT_BY_YEAR": "query-templates/q6_ano_client_by_year.json",
    "DUO_CLIENT_BY_CUSTOMER_NUMBER": "query-templates/q7_duo_client_by_customer_number.json",
    "DUO_CLIENT_BY_NAME_AND_STATE": "query-templates/q8_duo_client_by_name_and_state.json",
    "ANO_PAYROLL_TYPE_LANGUAGE": "query-templates/q9_ano_payroll_type_language.json",
    "DUO_BOOKING_BY_COSTCENTER_AND_DATE": "query-templates/q10_duo_booking_by_costcenter_and_date.json",
    "DUO_BOOKING_BY_AMOUNT_RANGE": "query-templates/q11_duo_booking_by_amount_range.json",
    "DOCNAME_REGEX": "query-templates/leaf/q1_docName_regex.json",
    "ANO_MULTI_REGEX": "query-templates/leaf/q2_ano_multi_regex.json",
    "DUO_MULTI_REGEX": "query-templates/leaf/q3_duo_multi_regex.json",
    "ANO_DIS_MAX": "queries/compound/q3_ano_dis_max.json",
    "ANO_DIS_MAX_EXPENSIVE": "queries/compound/q4_ano_dis_max.json",
    "DUO_INVOICE_DIS_MAX": "queries/compound/q1_duo_dis_max.json",
    "DUO_INVOICE_DIS_MAX_EXPENSIVE": "queries/compound/q2_duo_dis_max.json",
    "DUO_COMPLEX": "query-templates/complex/q1_duo_complex.json",
    "ANO_PREFIX_RANGE": "query-templates/complex/q2_ano_prefix_range.json",
    "DUO_MULTI_PREFIX_SORT": "query-templates/complex/q3_duo_multi_prefix_sort.json",
    "ANO_PREFIX_MATCH": "query-templates/complex/q4_ano_prefix.json",
    "DUO_DATE_RANGE": "query-templates/complex/q5_duo_range.json",
    "ANO_SPAN_NEAR": "query-templates/span/ano_span_near.json",
    "DUO_SPAN_NEAR": "query-templates/span/duo_span_near.json",
    "ANO_MORE_LIKE_THIS": "query-templates/specialized/ano_more_like_this.json",
    "DUO_MORE_LIKE_THIS": "query-templates/specialized/duo_more_like_this.json",
}

TEMPLATE_CACHE = {}
SEED_VALUES = {}

FIRST_NAMES = [
    "Anna",
    "Ben",
    "Clara",
    "David",
    "Eva",
    "Felix",
    "Greta",
    "Henrik",
    "Isabel",
    "Jonas",
]

LAST_NAMES = [
    "Schmidt",
    "Mueller",
    "Schneider",
    "Fischer",
    "Weber",
    "Meyer",
    "Wagner",
    "Becker",
    "Hoffmann",
    "Schulz",
]

COMPANY_BASE = [
    "Muster",
    "Beispiel",
    "Nord",
    "Sueds",
    "Alpen",
    "Rhein",
    "Elbe",
    "Weser",
    "Main",
    "Hansa",
]

COMPANY_SUFFIX = ["GmbH", "AG", "KG", "OHG", "GbR", "UG", "e.K."]

MONTH_NAMES = {
    1: "Januar",
    2: "Februar",
    3: "März",
    4: "April",
    5: "Mai",
    6: "Juni",
    7: "Juli",
    8: "August",
    9: "September",
    10: "Oktober",
    11: "November",
    12: "Dezember",
}

MONTH_NAME_TO_NUM = {
    "Januar": 1,
    "Februar": 2,
    "März": 3,
    "April": 4,
    "Mai": 5,
    "Juni": 6,
    "Juli": 7,
    "August": 8,
    "September": 9,
    "Oktober": 10,
    "November": 11,
    "Dezember": 12,
}

PAYROLL_TYPES = ["Monthly", "Yearly", "Quarterly"]
LANGUAGES = ["German", "English", "Spanish", "French"]

SEARCH_TERMS_ANO = [
    "Brutto-Netto-Abrechnung",
    "Abrechnung",
    "Brutto",
    "Netto",
    "Lohn",
]

SEARCH_TERMS_DUO_SIMPLE = [
    "Rechnung",
    "Nettosumme",
    "Gesamtbetrag",
    "USt-IdNr.",
    "Bankverbindung",
]

SEARCH_TERMS_DUO_EXPENSIVE = [
    "Wolle",
    "Leder",
    "Stahl",
    "Kupfer",
    "Marmor",
    "Gummi",
    "Papier",
    "Service",
]

DOC_TOKENS = ["Brutto", "Netto", "Abrechnung", "Januar", "März", "Juli", "Oktober", "2018", "2020", "2025"]

APPROVAL_STATES = ["APPROVED", "NOT_RELEVANT", "UNDISPATCHED"]
LOCATIONS = ["BELEGE", "BELEGFREIGABE"]

INVOICE_CATEGORIES = ["SUPPLIER_INVOICE", "OTHER", "SALES_INVOICE"]
PAID_STATUSES = ["FULLY_PAID", "NOT_PAID"]

PRODUCT_PARTS = [
    ("Stahl", "Rohr"),
    ("Kupfer", "Draht"),
    ("Holz", "Platte"),
    ("Papier", "Rolle"),
    ("Glas", "Scheibe"),
    ("Alu", "Profil"),
    ("Kunststoff", "Box"),
    ("Textil", "Rolle"),
    ("Gummi", "Dichtung"),
    ("Keramik", "Fliese"),
]


def _load_template(path):
    if path not in TEMPLATE_CACHE:
        full_path = os.path.join(TEMPLATE_ROOT, path)
        with open(full_path, "r", encoding="utf-8") as handle:
            TEMPLATE_CACHE[path] = handle.read()
    return TEMPLATE_CACHE[path]


def _render_template(path, params):
    text = _load_template(path)
    for key, value in params.items():
        text = text.replace("{{" + key + "}}", str(value))
    return json.loads(text)


def _random_year():
    current_year = date.today().year
    return str(random.randint(current_year - 10, current_year))


def _random_year_after(from_year):
    current_year = date.today().year
    return str(random.randint(int(from_year), current_year))


def _random_full_name():
    return f"{random.choice(FIRST_NAMES)} {random.choice(LAST_NAMES)}"


def _random_company_name():
    base = random.choice(COMPANY_BASE)
    suffix = random.choice(COMPANY_SUFFIX)
    return f"{base} {suffix}"


def _weighted_choice(mix):
    choices = []
    total = 0
    for entry in mix:
        if isinstance(entry, str):
            weight = 1
            qtype = entry
        else:
            qtype = entry.get("type")
            weight = entry.get("weight") or entry.get("percent") or 1
        total += weight
        choices.append((qtype, total))
    r = random.uniform(0, total)
    for qtype, cumulative in choices:
        if r <= cumulative:
            return qtype
    return choices[-1][0]


def _load_seed_values():
    global SEED_VALUES
    seed_path = os.getenv("OSB_SEED_VALUES_PATH")
    if not seed_path:
        seed_path = os.path.join(os.path.dirname(__file__), "seed-values.json")
    if os.path.exists(seed_path):
        try:
            with open(seed_path, "r", encoding="utf-8") as handle:
                SEED_VALUES = json.load(handle)
        except Exception:
            SEED_VALUES = {}


def _seed_list(section, key):
    return SEED_VALUES.get(section, {}).get(key) or []


def _pick_seed(section, key):
    values = _seed_list(section, key)
    if values:
        return random.choice(values)
    return None


def _iso_date(value):
    return value.strftime("%Y-%m-%d")


def _iso_datetime_range_for_year(year):
    return f"{year}-01-01T00:00:00Z", f"{year}-12-31T23:59:59Z"


def _invoice_number():
    return f"{random.randint(10000, 99999)}/{random.randint(1, 9999)}"


def _duo_complex_params():
    search_terms = "Rechnung" if random.random() < 0.8 else random.choice(
        ["Nettosumme", "Gesamtbetrag", "USt-IdNr.", "Datum"]
    )
    suffix = "GmbH" if random.random() < 0.6 else random.choice(["AG", "KG", "OHG", "GbR", "UG", "e.K."])
    business_partner_wildcard = f"*{suffix}*"
    invoice_number_fragment = str(random.randint(1, 99999))

    start_date = date.today() - timedelta(days=random.randint(365, 3650))
    end_date = start_date + timedelta(days=random.randint(30, 365))
    if end_date > date.today():
        end_date = date.today()

    categories = random.sample(INVOICE_CATEGORIES, k=3)

    return {
        "search_terms": search_terms,
        "invoice_date_from": _iso_date(start_date),
        "invoice_date_to": _iso_date(end_date),
        "business_partner_wildcard": business_partner_wildcard,
        "invoice_number_fragment": invoice_number_fragment,
        "category_1": categories[0],
        "category_2": categories[1],
        "category_3": categories[2],
    }


def _ano_dismax_expensive_params():
    payroll_type = random.choice(PAYROLL_TYPES)
    language = random.choice(LANGUAGES)
    year_from = random.randint(2016, 2026)
    year_to = random.randint(year_from, 2026)
    month = random.randint(1, 12)
    month_name = MONTH_NAMES.get(month, "Januar")
    filename = f"Brutto-Netto-Abrechnung {month_name} {year_to}.pdf"
    creator_name = _random_full_name()
    doc_phrase = f"Brutto-Netto-Abrechnung {month_name} {year_to}"
    search_term = random.choice(
        [
            "Brutto-Netto-Abrechnung",
            f"Abrechnung {month_name}",
            f"{month_name} {year_to}",
        ]
    )
    return {
        "payroll_type": payroll_type,
        "language": language,
        "year_from": str(year_from),
        "year_to": str(year_to),
        "month": str(month),
        "filename": filename,
        "filename_fragment": month_name,
        "creator_name": creator_name,
        "doc_phrase": doc_phrase,
        "search_terms": search_term,
    }


def _ano_dismax_expensive_params_from_filename(filename):
    match = re.match(r"Brutto-Netto-Abrechnung\\s+(?P<month>.+)\\s+(?P<year>\\d{4})\\.pdf", filename)
    if not match:
        return None
    month_name = match.group("month")
    year_to = int(match.group("year"))
    month_num = MONTH_NAME_TO_NUM.get(month_name, 1)
    doc_phrase = f"Brutto-Netto-Abrechnung {month_name} {year_to}"
    search_term = random.choice(
        [
            "Brutto-Netto-Abrechnung",
            f"Abrechnung {month_name}",
            f"{month_name} {year_to}",
        ]
    )
    return {
        "payroll_type": random.choice(PAYROLL_TYPES),
        "language": random.choice(LANGUAGES),
        "year_from": str(year_to),
        "year_to": str(year_to),
        "month": str(month_num),
        "filename": filename,
        "filename_fragment": month_name,
        "creator_name": _pick_seed("ano", "creation_user_display_name") or _random_full_name(),
        "doc_phrase": doc_phrase,
        "search_terms": search_term,
    }


def _duo_invoice_expensive_params():
    start_date = date.today() - timedelta(days=random.randint(30, 3650))
    end_date = start_date + timedelta(days=random.randint(30, 180))
    if end_date > date.today():
        end_date = date.today()

    return {
        "invoice_number": _invoice_number(),
        "business_partner": _random_company_name(),
        "search_terms": random.choice(SEARCH_TERMS_DUO_EXPENSIVE),
        "invoice_date_from": _iso_date(start_date),
        "invoice_date_to": _iso_date(end_date),
        "category": random.choice(INVOICE_CATEGORIES),
        "paid_status": random.choice(PAID_STATUSES),
    }


def _duo_span_params():
    material, product = random.choice(PRODUCT_PARTS)
    return {"material": material, "product": product}


def _ano_span_params():
    current_year = date.today().year
    year = random.randint(current_year - 10, current_year + 1)
    return {"year": str(year)}


def _ano_more_like_this_params():
    return _ano_span_params()


def _duo_more_like_this_params():
    return _duo_span_params()


def _ano_prefix_range_params():
    year = 2025 if random.randint(1, 100) <= 70 else 2026
    gte, lte = _iso_datetime_range_for_year(year)
    return {"prefix": "B", "gte": gte, "lte": lte}


def _duo_date_range_params():
    year = 2025 if random.randint(1, 100) <= 70 else 2026
    gte, lte = _iso_datetime_range_for_year(year)
    return {"gte": gte, "lte": lte}


def _query_params_for(query_type):
    if query_type == "ANO_PAYROLL_RANGE":
        from_year = _random_year()
        return {"from_year": from_year, "to_year": _random_year_after(from_year)}
    if query_type == "ANO_CLIENT_BY_YEAR":
        return {
            "client_name": _pick_seed("ano", "creation_user_display_name") or _random_full_name(),
            "year": _pick_seed("ano", "accounting_year") or _random_year(),
        }
    if query_type == "ANO_CLIENTS_AGGREGATION":
        return {}
    if query_type == "ANO_DIS_MAX":
        return {
            "payroll_type": random.choice(PAYROLL_TYPES),
            "language": random.choice(LANGUAGES),
            "search_terms": random.choice(SEARCH_TERMS_ANO),
        }
    if query_type == "ANO_DIS_MAX_EXPENSIVE":
        filename_seed = _pick_seed("ano", "original_filename")
        if filename_seed:
            params = _ano_dismax_expensive_params_from_filename(filename_seed)
            if params:
                return params
        return _ano_dismax_expensive_params()
    if query_type == "ANO_MULTI_REGEX":
        return {"year": _random_year()}
    if query_type == "ANO_PAYROLL_TYPE_LANGUAGE":
        year_from = _random_year()
        return {
            "payroll_type": random.choice(PAYROLL_TYPES),
            "language": random.choice(LANGUAGES),
            "year_from": year_from,
            "year_to": _random_year_after(year_from),
        }
    if query_type == "ANO_PREFIX_MATCH":
        return {"prefix": "B", "match_query": random.choice(DOC_TOKENS)}
    if query_type == "ANO_PREFIX_RANGE":
        return _ano_prefix_range_params()
    if query_type == "ANO_SPAN_NEAR":
        return _ano_span_params()
    if query_type == "ANO_MORE_LIKE_THIS":
        return _ano_more_like_this_params()
    if query_type == "DOCNAME_REGEX":
        return {"year": _random_year()}
    if query_type == "DUO_INVOICE_CATEGORY":
        return {"category": random.choice(INVOICE_CATEGORIES)}
    if query_type == "DUO_STATE_LOCATION":
        return {"approval_state": random.choice(APPROVAL_STATES), "location": random.choice(LOCATIONS)}
    if query_type == "DUO_BOOKING_BY_CLIENT_AND_STATE":
        return {
            "client_name": _pick_seed("duo", "invoice_business_partner") or _random_company_name(),
            "booking_state": "TO_BOOK",
        }
    if query_type == "DUO_CLIENT_BY_CUSTOMER_NUMBER":
        value = _pick_seed("duo", "customer_number")
        if value is None:
            value = str(random.randint(1000, 1000000)) if random.random() < 0.7 else "null"
        return {"customer_number": value}
    if query_type == "DUO_CLIENT_BY_NAME_AND_STATE":
        return {
            "client_name": _pick_seed("duo", "invoice_business_partner") or _random_company_name(),
            "approval_state": random.choice(APPROVAL_STATES),
        }
    if query_type == "DUO_BOOKING_BY_COSTCENTER_AND_DATE":
        from_year = _random_year()
        cost_center = _pick_seed("duo", "cost_center_1")
        return {
            "date_from": from_year,
            "date_to": _random_year_after(from_year),
            "cost_center_1": cost_center or f"cc-{random.randint(100, 9999)}",
        }
    if query_type == "DUO_BOOKING_BY_AMOUNT_RANGE":
        start = random.uniform(0, 9_999_999)
        end = start + random.uniform(0, 9_999_999)
        return {"amount_min": f"{start:.2f}", "amount_max": f"{end:.2f}"}
    if query_type == "DUO_COMPLEX":
        return _duo_complex_params()
    if query_type == "DUO_MULTI_REGEX":
        return {}
    if query_type == "DUO_INVOICE_DIS_MAX":
        search_term = "Rechnung" if random.random() < 0.8 else random.choice(SEARCH_TERMS_DUO_SIMPLE)
        return {
            "invoice_number": _pick_seed("duo", "invoice_number") or _invoice_number(),
            "business_partner": _pick_seed("duo", "invoice_business_partner") or _random_company_name(),
            "search_terms": search_term,
        }
    if query_type == "DUO_INVOICE_DIS_MAX_EXPENSIVE":
        params = _duo_invoice_expensive_params()
        params["invoice_number"] = _pick_seed("duo", "invoice_number") or params["invoice_number"]
        params["business_partner"] = _pick_seed("duo", "invoice_business_partner") or params["business_partner"]
        return params
    if query_type == "DUO_MULTI_PREFIX_SORT":
        invoice_number = _pick_seed("duo", "invoice_number") or _invoice_number()
        customer_number = _pick_seed("duo", "customer_number") or str(random.randint(1000, 1000000))
        partner = _pick_seed("duo", "invoice_business_partner") or _random_company_name()
        inv_prefix_1 = invoice_number[:2]
        inv_prefix_2 = invoice_number.split("/")[0][:1] if "/" in invoice_number else invoice_number[:1]
        partner_prefix = partner[:1].lower() if partner else chr(random.randint(97, 122))
        customer_prefix = str(customer_number)[:3]
        return {
            "inv_prefix_1": inv_prefix_1,
            "inv_prefix_2": inv_prefix_2,
            "partner_prefix": partner_prefix,
            "customer_prefix": customer_prefix,
        }
    if query_type == "DUO_DATE_RANGE":
        return _duo_date_range_params()
    if query_type == "DUO_SPAN_NEAR":
        return _duo_span_params()
    if query_type == "DUO_MORE_LIKE_THIS":
        return _duo_more_like_this_params()
    raise ValueError(f"Unknown query type: {query_type}")


def _build_query_body(query_type):
    template = TEMPLATE_MAP[query_type]
    params = _query_params_for(query_type)
    return _render_template(template, params)


def mixed_search(track, params, **kwargs):
    mix = params.get("mix") or []
    query_type = _weighted_choice(mix)
    body = _build_query_body(query_type)
    return {"body": body, "index": params.get("index")}


def register(registry):
    registry.register_param_source("mixed-search", mixed_search)


_load_seed_values()
