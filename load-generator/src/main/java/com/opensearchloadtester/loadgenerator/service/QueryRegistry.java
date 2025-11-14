package com.opensearchloadtester.loadgenerator.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
/**
 * Central registry that maps logical query IDs (q1, q2, ...) to
 * concrete JSON template file names under resources/queries/.
 *
 * This decouples the external API (queryId) from the internal file names,
 * so the frontend / client only has to know about q1..q11 instead of file paths.
 */
@Component
public class QueryRegistry {

    // Mapping from public queryId (used by the REST API) to template JSON file
    // The files are expected to live under: src/main/resources/queries/

    private final Map<String, String> queries = Map.ofEntries(
            Map.entry("q1", "q1_ano_payroll_range.json"),
            Map.entry("q2", "q2_duo_invoice_category.json"),
            Map.entry("q3", "q3_duo_state_location.json"),
            Map.entry("q4", "q4_duo_booking_by_client_and_state.json"),
            Map.entry("q5", "q5_ano_clients_aggregation.json"),
            Map.entry("q6", "q6_ano_client_by_year.json"),
            Map.entry("q7", "q7_duo_client_by_customer_number.json"),
            Map.entry("q8", "q8_duo_client_by_name_and_state.json"),
            Map.entry("q9", "q9_ano_payroll_type_language.json"),
            Map.entry("q10", "q10_duo_booking_by_costcenter_and_date.json"),
            Map.entry("q11", "q11_duo_booking_by_amount_range.json")

    );

    public String getTemplateFile(String queryId) {
        String file = queries.get(queryId);
        if (file == null) {
            throw new IllegalArgumentException("Unknown queryId: " + queryId);
        }
        return file;
    }

    public Set<String> listQueryIds() {
        return queries.keySet();
    }
}
