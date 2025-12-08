package com.opensearchloadtester.loadgenerator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
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

    private String name;
    private DocumentType documentType;
    private Duration duration;
    @Min(1)
    private int queriesPerSecond;
    @JsonProperty("enable_warm_up")
    private boolean warmUpEnabled;
    private QueryConfig query;
}
