package com.opensearchloadtester.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MetricsDto {

    @NotBlank
    private String loadGeneratorId;
    @NotBlank
    private String queryType;
    @Min(0)
    private long requestDurationMillis;
    @Min(0)
    private Long queryDurationMillis;
    @Min(0)
    private Integer totalHits;
    private int httpStatusCode;
}
