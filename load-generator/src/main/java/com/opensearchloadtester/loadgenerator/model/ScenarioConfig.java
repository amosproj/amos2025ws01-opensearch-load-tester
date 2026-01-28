package com.opensearchloadtester.loadgenerator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
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

    @NotBlank(message = "please provide a name for the scenario")
    private String name;
    @NotNull(message = "documentType must be ANO or DUO")
    private DocumentType documentType;
    @NotNull
    private Duration scheduleDuration;
    @NotNull
    private Duration queryResponseTimeout;

    @NotNull
    @Min(1)
    private Integer queriesPerSecond;

    @NotNull
    @JsonProperty("enable_warm_up")
    private Boolean warmUpEnabled;

    @NotNull(message = "please specify query_mix")
    @JsonProperty("query_mix")
    private JsonNode queryMix;
}
