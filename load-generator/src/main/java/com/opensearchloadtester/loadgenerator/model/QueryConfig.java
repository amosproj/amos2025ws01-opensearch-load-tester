package com.opensearchloadtester.loadgenerator.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QueryConfig {

    private QueryType type;
    private Map<String, String> parameters;
}
