package com.opensearchloadtester.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoadTestStartSyncStatusDto {

    private int expectedLoadGenerators;
    private int readyLoadGenerators;
    private boolean isStartAllowed;
    private Long plannedStartTimeMillis;
}
