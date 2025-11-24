package com.opensearchloadtester.loadgenerator.model;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConcurrencyConfig {

    @Min(1)
    private int clientSize;
    @Min(1)
    private int threadPoolSize;
}
