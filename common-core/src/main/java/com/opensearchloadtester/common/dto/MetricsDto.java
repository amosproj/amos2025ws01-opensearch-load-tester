package com.opensearchloadtester.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MetricsDto {

    private String loadGeneratorId;
    private String queryType;
    private Long requestDurationMillis;
    private Long queryDurationMillis;
    private Integer totalHits;
    private int httpStatusCode;
}
