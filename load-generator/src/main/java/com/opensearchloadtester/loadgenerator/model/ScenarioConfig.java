package com.opensearchloadtester.loadgenerator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioConfig {

    @NotBlank(message = "scenario name must not be blank")
    private String name;
    @NotNull(message = "documentType must be ANO or DUO")
    private DocumentType documentType;
    @NotNull(message = "scheduleDuration must not be null")
    private Duration scheduleDuration;
    @NotNull(message = "queryResponseTimeout must not be null")
    private Duration queryResponseTimeout;

    @Min(1)
    private int queriesPerSecond;

    // TODO: Add validation to ensure queryMix is not null/empty
    @JsonProperty("enable_warm_up")
    private boolean warmUpEnabled;

    // TODO: Add validation to ensure queryMix is not null/empty
    @Valid
    @JsonProperty("query_mix")
    private JsonNode queryMix;
}
