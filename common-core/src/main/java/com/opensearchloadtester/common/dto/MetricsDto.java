package com.opensearchloadtester.common.dto;

import jakarta.validation.constraints.Max;
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
    private Long requestDurationMillis;

    @Min(0)
    private Long queryDurationMillis;

    private Integer totalHits;

    @Min(100)
    @Max(599)
    private int httpStatusCode;
}
