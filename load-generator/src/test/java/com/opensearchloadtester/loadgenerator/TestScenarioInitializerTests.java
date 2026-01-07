package com.opensearchloadtester.loadgenerator;

import com.opensearchloadtester.loadgenerator.client.LoadTestStartSyncClient;
import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import com.opensearchloadtester.loadgenerator.service.LoadRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TestScenarioInitializerTests {

    @Mock
    private ScenarioConfig scenarioConfig;
    @Mock
    private LoadRunner loadRunner;
    @Mock
    private OpenSearchGenericClient openSearchClient;
    @Mock
    private LoadTestStartSyncClient loadTestStartSyncClient;

    @Test
    void run_warmupDisabled_singleReplica_executesScenario_withoutSync() {
        when(scenarioConfig.getName()).thenReturn("test-scenario");
        when(scenarioConfig.isWarmUpEnabled()).thenReturn(false);

        TestScenarioInitializer initializer = new TestScenarioInitializer(
                "lg-1",
                1,
                scenarioConfig,
                loadRunner,
                openSearchClient,
                loadTestStartSyncClient
        );

        initializer.run();

        verify(loadRunner, times(1)).executeScenario(scenarioConfig);
        verifyNoInteractions(loadTestStartSyncClient);
    }

    @Test
    void run_warmupDisabled_multipleReplicas_syncsThenExecutesScenario_inOrder() {
        when(scenarioConfig.getName()).thenReturn("test-scenario");
        when(scenarioConfig.isWarmUpEnabled()).thenReturn(false);

        TestScenarioInitializer initializer = new TestScenarioInitializer(
                "lg-1",
                3,
                scenarioConfig,
                loadRunner,
                openSearchClient,
                loadTestStartSyncClient
        );

        initializer.run();

        InOrder inOrder = inOrder(loadTestStartSyncClient, loadRunner);
        inOrder.verify(loadTestStartSyncClient).registerReady("lg-1");
        inOrder.verify(loadTestStartSyncClient).awaitStartPermission();
        inOrder.verify(loadRunner).executeScenario(scenarioConfig);
    }
}
