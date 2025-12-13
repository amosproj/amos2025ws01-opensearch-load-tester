package com.opensearchloadtester.metricsreporter.controller;

import com.opensearchloadtester.common.dto.LoadTestSyncStatusDto;
import com.opensearchloadtester.metricsreporter.service.LoadTestStartSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/load-test")
@RequiredArgsConstructor
public class LoadTestStartSyncController {

    private final LoadTestStartSyncService syncService;

    @PostMapping("/ready/{loadGeneratorId}")
    public ResponseEntity<Void> markReady(@PathVariable String loadGeneratorId) {
        syncService.markReady(loadGeneratorId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/status")
    public ResponseEntity<LoadTestSyncStatusDto> status() {
        LoadTestSyncStatusDto status = syncService.getStatus();
        return ResponseEntity.ok(status);
    }
}
