package com.opensearchloadtester.loadgenerator.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QueryType {
    YEAR_RANGE("/queries/q1_ano_payroll_range.json");

    private final String templatePath;
}
