package com.opensearchloadtester.loadgenerator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioConfig {

    private String name;
    private DocumentType documentType;
    private Duration scheduleDuration;
    private Duration queryResponseTimeout;

    @Min(1)
    private int queriesPerSecond;

    @JsonProperty("enable_warm_up")
    private boolean warmUpEnabled;

    @NotEmpty
    private List<QueryType> queryTypes;
}
