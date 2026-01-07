package com.opensearchloadtester.loadgenerator;

import com.opensearchloadtester.loadgenerator.client.LoadTestStartSyncClient;
import com.opensearchloadtester.loadgenerator.exception.LoadTestStartSyncException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class LoadTestStartSyncClientTests {

    @Test
    void happyPath_registerReady_and_awaitStartPermission() throws IOException {
        CloseableHttpClient http = mock(CloseableHttpClient.class);

        try (MockedStatic<HttpClients> mocked = mockStatic(HttpClients.class)) {
            mocked.when(HttpClients::createDefault).thenReturn(http);

            // registerReady(): return 200
            when(http.execute(any(HttpPost.class), any(HttpClientResponseHandler.class))).thenReturn(200);

            // awaitStartPermission(): return JSON with startAllowed=true and future plannedStart
            when(http.execute(any(HttpGet.class), any(HttpClientResponseHandler.class)))
                    .thenAnswer(inv -> {
                        HttpClientResponseHandler<Object> h = inv.getArgument(1);

                        long future = System.currentTimeMillis() + 2_000;
                        String json = "{\"startAllowed\":true,\"plannedStartTimeMillis\":" + future +
                                ",\"readyLoadGenerators\":2,\"expectedLoadGenerators\":2}";

                        ClassicHttpResponse resp = mock(ClassicHttpResponse.class);
                        when(resp.getCode()).thenReturn(200);
                        when(resp.getEntity()).thenReturn(new StringEntity(json));
                        return h.handleResponse(resp);
                    });

            LoadTestStartSyncClient client = new LoadTestStartSyncClient("http://metrics-reporter");

            assertDoesNotThrow(() -> client.registerReady("lg-1"));
            assertDoesNotThrow(client::awaitStartPermission);
        }
    }

    @Test
    void errorPath_registerReady_httpError_and_awaitStartPermission_parseError() throws IOException {
        CloseableHttpClient http = mock(CloseableHttpClient.class);

        try (MockedStatic<HttpClients> mocked = mockStatic(HttpClients.class)) {
            mocked.when(HttpClients::createDefault).thenReturn(http);

            LoadTestStartSyncClient client = new LoadTestStartSyncClient("http://metrics-reporter");

            // registerReady(): return 500
            when(http.execute(any(HttpPost.class), any(HttpClientResponseHandler.class))).thenReturn(500);
            assertThrows(LoadTestStartSyncException.class, () -> client.registerReady("lg-1"));

            // awaitStartPermission(): invalid JSON -> parse error path
            reset(http);
            when(http.execute(any(HttpGet.class), any(HttpClientResponseHandler.class)))
                    .thenAnswer(inv -> {
                        HttpClientResponseHandler<Object> h = inv.getArgument(1);

                        ClassicHttpResponse resp = mock(ClassicHttpResponse.class);
                        when(resp.getCode()).thenReturn(200);
                        when(resp.getEntity()).thenReturn(new StringEntity("invalid-json"));
                        return h.handleResponse(resp);
                    });

            assertThrows(LoadTestStartSyncException.class, client::awaitStartPermission);
        }
    }
}
