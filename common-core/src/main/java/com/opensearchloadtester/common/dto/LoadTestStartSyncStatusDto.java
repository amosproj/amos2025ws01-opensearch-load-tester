package com.opensearchloadtester.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoadTestStartSyncStatusDto {

    private int expectedLoadGenerators;
    private int readyLoadGenerators;
    private boolean isStartAllowed;
    private Long plannedStartTimeMillis;
}
