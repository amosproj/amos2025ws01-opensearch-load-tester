package com.opensearchloadtester.loadgenerator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.loadgenerator.client.MetricsReporterClient;
import com.opensearchloadtester.loadgenerator.exception.MetricsReporterAccessException;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsReporterClientTests {

    private MetricsReporterClient client;

    @Mock
    private CloseableHttpClient httpClientMock;


    private ArrayList<MetricsDto> getTestMetrics() {
        ArrayList<MetricsDto> metrics = new ArrayList<>();
        metrics.add(new MetricsDto());
        return metrics;
    }

    /**
     * Tests the {@link MetricsReporterClient#sendMetrics(List)} method for the scenario
     * where the HTTP request succeeds with status code 200 (OK)
     *
     * <p>This test does the following:</p>
     * <ol>
     *     <li>Creates a sample {@link MetricsDto} list with test data</li>
     *     <li>Mocks a {@link CloseableHttpClient} to simulate sending the HTTP POST request</li>
     *     <li>Overrides the {@link HttpClients#createDefault()} method to return the mocked client</li>
     *     <li>Mocks the execution of the HTTP request to return a {@link ClassicHttpResponse}
     *         with HTTP status 200 (OK) via a {@link HttpClientResponseHandler}</li>
     *     <li>Calls {@link MetricsReporterClient#sendMetrics(List)} and asserts that
     *         no exceptions are thrown</li>
     *     <li>Manually calls implicit {@code close()} method of {@link MetricsReporterClient}</li>
     *     <li>Verifies that the HTTP client's {@code execute()} and {@code close()} methods
     *         were called exactly once each</li>
     * </ol>
     *
     * <p>Effectively, this test validates that:</p>
     * <ul>
     *     <li>The metrics reporting logic completes successfully when the server responds with 200 OK</li>
     *     <li>The HTTP client is properly closed after execution</li>
     * </ul>
     */
    @Test
    void testSendMetrics_successStatus200() throws Exception {
        ArrayList<MetricsDto> metrics = getTestMetrics();

        when(httpClientMock.execute(
                any(HttpPost.class),
                any(HttpClientResponseHandler.class))
        ).thenAnswer(invocation -> {
            HttpClientResponseHandler<Integer> handler = invocation.getArgument(1);
            ClassicHttpResponse response = Mockito.mock(ClassicHttpResponse.class);
            Mockito.when(response.getCode()).thenReturn(HttpStatus.SC_OK);
            return handler.handleResponse(response);
        });

        try (MockedStatic<HttpClients> mocked = Mockito.mockStatic(HttpClients.class)) {
            mocked.when(HttpClients::createDefault).thenReturn(httpClientMock);
            client = new MetricsReporterClient("http://metrics/");

            assertDoesNotThrow(() -> client.sendMetrics(metrics));

            Method closeMethod = MetricsReporterClient.class.getDeclaredMethod("close");
            closeMethod.setAccessible(true);
            closeMethod.invoke(client);

            verify(httpClientMock, times(1)).execute(any(HttpPost.class), any(HttpClientResponseHandler.class));
            verify(httpClientMock, times(1)).close();
        }
    }

    /**
     * Tests the {@link MetricsReporterClient#sendMetrics(List)} method for the scenario
     * where the HTTP request succeeds with status code 201 (Created)
     *
     * <p>This test performs the following steps:</p>
     * <ol>
     *     <li>Creates a sample {@link MetricsDto} object with test data</li>
     *     <li>Mocks a {@link CloseableHttpClient} to simulate sending the HTTP POST request</li>
     *     <li>Overrides the {@link HttpClients#createDefault()} method to return the mocked client</li>
     *     <li>Mocks the execution of the HTTP request to return a {@link ClassicHttpResponse}
     *         with HTTP status 201 (Created) via a {@link HttpClientResponseHandler}</li>
     *     <li>Calls {@link MetricsReporterClient#sendMetrics(List)} and asserts that
     *         no exceptions are thrown</li>
     *     <li>Verifies that the HTTP client's {@code execute()} method was called exactly once</li>
     * </ol>
     *
     * <p>This test ensures that:</p>
     * <ul>
     *     <li>The metrics reporting logic completes successfully when the server responds with "201 Created"</li>
     *     <li>The HTTP request is executed exactly once</li>
     * </ul>
     */
    @Test
    void testSendMetrics_successStatus201() throws Exception {
        ArrayList<MetricsDto> metrics = getTestMetrics();

        when(httpClientMock.execute(
                any(HttpPost.class),
                any(HttpClientResponseHandler.class))
        ).thenAnswer(invocation -> {
            HttpClientResponseHandler<Integer> handler = invocation.getArgument(1);
            ClassicHttpResponse response = Mockito.mock(ClassicHttpResponse.class);
            Mockito.when(response.getCode()).thenReturn(HttpStatus.SC_CREATED);
            return handler.handleResponse(response);
        });

        try (MockedStatic<HttpClients> mocked = Mockito.mockStatic(HttpClients.class)) {
            mocked.when(HttpClients::createDefault).thenReturn(httpClientMock);
            client = new MetricsReporterClient("http://metrics/");

            assertDoesNotThrow(() -> client.sendMetrics(metrics));

            verify(httpClientMock, times(1)).execute(any(HttpPost.class), any(HttpClientResponseHandler.class));
        }
    }

    /**
     * Tests the {@link MetricsReporterClient#sendMetrics(List)} method for the scenario
     * where JSON serialization of the metrics fails
     *
     * <p>This test performs the following steps:</p>
     * <ol>
     *     <li>Creates a sample {@link MetricsDto} object with test data</li>
     *     <li>Mocks the {@link ObjectMapper} used by {@link MetricsReporterClient}</li>
     *     <li>Configures the mocked {@link ObjectMapper} to throw a {@link JsonProcessingException}
     *         when {@code writeValueAsString} is called</li>
     *     <li>Sets the mocked ObjectMapper into the {@code MetricsReporterClient} using reflection</li>
     *     <li>Calls {@link MetricsReporterClient#sendMetrics(List)} and asserts that a
     *         {@link MetricsReporterAccessException} is thrown</li>
     * </ol>
     *
     * <p>This test ensures that:</p>
     * <ul>
     *     <li>The client properly wraps JSON serialization errors into a {@code MetricsReporterAccessException}</li>
     *     <li>Errors during metrics serialization are correctly propagated to the caller</li>
     * </ul>
     */
    @Test
    void testSendMetrics_jsonSerializationFails() throws Exception {
        ArrayList<MetricsDto> metrics = getTestMetrics();

        ObjectMapper mapperMock = Mockito.mock(ObjectMapper.class);

        when(mapperMock.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("boom") {
                });

        client = new MetricsReporterClient("http://metrics/");
        ReflectionTestUtils.setField(client, "objectMapper", mapperMock);

        assertThrows(MetricsReporterAccessException.class,
                () -> client.sendMetrics(metrics));
    }

    /**
     * Tests the {@link MetricsReporterClient#sendMetrics(List)} method for the scenario
     * where the HTTP request repeatedly fails with a non-success status code (500 Internal Server Error)
     * and triggers the retry mechanism
     *
     * <p>This test performs the following steps:</p>
     * <ol>
     *     <li>Creates a sample {@link MetricsDto} object with test data</li>
     *     <li>Mocks a {@link CloseableHttpClient} to simulate sending HTTP POST requests</li>
     *     <li>Configures the mocked client so that every execution returns a {@link ClassicHttpResponse}
     *         with HTTP status 500 (Internal Server Error) via a {@link HttpClientResponseHandler}</li>
     *     <li>Overrides {@link HttpClients#createDefault()} to return the mocked client</li>
     *     <li>Calls {@link MetricsReporterClient#sendMetrics(List)} and asserts that a
     *         {@link MetricsReporterAccessException} is thrown after exhausting all retries</li>
     *         <li>Manually calls implicit {@code close()} method of {@link MetricsReporterClient}</li>
     *     <li>Verifies that the HTTP client's {@code execute()} method was called exactly 3 times
     *         (one per retry) and {@code close()} was called once</li>
     * </ol>
     *
     * <p>This test ensures that:</p>
     * <ul>
     *     <li>The retry mechanism is invoked for failed HTTP responses</li>
     *     <li>The client properly throws {@code MetricsReporterAccessException} after exhausting retries</li>
     *     <li>The HTTP client is correctly closed even after repeated failures</li>
     * </ul>
     */
    @Test
    void testSendMetrics_failsAfter3Attempts_httpStatusNotOk() throws Exception {
        ArrayList<MetricsDto> metrics = getTestMetrics();

        when(httpClientMock.execute(any(HttpPost.class), any(HttpClientResponseHandler.class)))
                .thenAnswer(invocation -> {
                    HttpClientResponseHandler<Integer> handler = invocation.getArgument(1);
                    ClassicHttpResponse response = Mockito.mock(ClassicHttpResponse.class);
                    Mockito.when(response.getCode()).thenReturn(500);
                    return handler.handleResponse(response);
                });

        try (MockedStatic<HttpClients> mocked = Mockito.mockStatic(HttpClients.class)) {
            mocked.when(HttpClients::createDefault).thenReturn(httpClientMock);
            client = new MetricsReporterClient("http://metrics/");

            assertThrows(MetricsReporterAccessException.class,
                    () -> client.sendMetrics(metrics));

            Method closeMethod = MetricsReporterClient.class.getDeclaredMethod("close");
            closeMethod.setAccessible(true);
            closeMethod.invoke(client);

            verify(httpClientMock, times(3)).execute(any(HttpPost.class), any(HttpClientResponseHandler.class));
            verify(httpClientMock, times(1)).close();
        }
    }

    /**
     * Tests the {@link MetricsReporterClient#sendMetrics(List)} method for the scenario
     * where the HTTP request fails due to an {@link IOException} on every attempt,
     * triggering the retry mechanism
     *
     * <p>This test performs the following steps:</p>
     * <ol>
     *     <li>Creates a sample {@link MetricsDto} object with test data</li>
     *     <li>Mocks a {@link CloseableHttpClient} to simulate sending HTTP POST requests</li>
     *     <li>Configures the mocked client to throw an {@link IOException} for every execution</li>
     *     <li>Overrides {@link HttpClients#createDefault()} to return the mocked client</li>
     *     <li>Calls {@link MetricsReporterClient#sendMetrics(List)} and asserts that a
     *         {@link MetricsReporterAccessException} is thrown after exhausting all retries</li>
     *     <li>Manually calls implicit {@code close()} method of {@link MetricsReporterClient}</li>
     *     <li>Verifies that the HTTP client's {@code execute()} method was called exactly 3 times
     *         (one per retry) and {@code close()} was called once</li>
     * </ol>
     *
     * <p>This test ensures that:</p>
     * <ul>
     *     <li>The retry mechanism is invoked when I/O errors occur during HTTP requests</li>
     *     <li>The client properly throws {@code MetricsReporterAccessException} after all retries fail</li>
     *     <li>The HTTP client is correctly closed even after repeated I/O failures</li>
     * </ul>
     */
    @Test
    void testSendMetrics_failsAfter3Attempts_ioException() throws Exception {
        ArrayList<MetricsDto> metrics = getTestMetrics();

        when(httpClientMock.execute(any(HttpPost.class), any(HttpClientResponseHandler.class)))
                .thenThrow(new IOException("IO boom"));

        try (MockedStatic<HttpClients> mocked = Mockito.mockStatic(HttpClients.class)) {
            mocked.when(HttpClients::createDefault).thenReturn(httpClientMock);
            client = new MetricsReporterClient("http://metrics/");

            assertThrows(MetricsReporterAccessException.class,
                    () -> client.sendMetrics(metrics));

            Method closeMethod = MetricsReporterClient.class.getDeclaredMethod("close");
            closeMethod.setAccessible(true);
            closeMethod.invoke(client);

            verify(httpClientMock, times(3)).execute(any(HttpPost.class), any(HttpClientResponseHandler.class));
            verify(httpClientMock, times(1)).close();
        }
    }

    /**
     * Tests the {@link MetricsReporterClient#sendMetrics(List)} method for the scenario
     * where creating the {@link CloseableHttpClient} fails
     *
     * <p>This test performs the following steps:</p>
     * <ol>
     *     <li>Mocks the static method {@link HttpClients#createDefault()} to throw a {@link RuntimeException}</li>
     *     <li>Instantiates {@link MetricsReporterClient} and asserts that a {@link MetricsReporterAccessException}
     *     is thrown</li>
     * </ol>
     *
     * <p>This test ensures that:</p>
     * <ul>
     *     <li>The client correctly handles exceptions that occur during HTTP client creation</li>
     *     <li>Such errors are wrapped and propagated as {@code MetricsReporterAccessException}</li>
     * </ul>
     */
    @Test
    void testSendMetrics_httpClientCreationFails() {
        try (MockedStatic<HttpClients> mocked = Mockito.mockStatic(HttpClients.class)) {
            mocked.when(HttpClients::createDefault)
                    .thenThrow(new RuntimeException("boom"));
            assertThrows(MetricsReporterAccessException.class,
                    () -> client = new MetricsReporterClient("http://metrics/"));
        }
    }

    /**
     * Tests the {@link MetricsReporterClient#sendMetrics(List)} method for the scenario
     * where closing the {@link CloseableHttpClient} throws an {@link IOException}
     *
     * <p>This test performs the following steps:</p>
     * <ol>
     *     <li>Mocks a {@link CloseableHttpClient} to throw an {@link IOException} when {@code close()} is called</li>
     *     <li>Overrides {@link HttpClients#createDefault()} to return the mocked client</li>
     *     <li>Tracks output stream and scans for warning due to the failure when closing the client</li>
     *     <li>Manually calls implicit {@code close()} method of {@link MetricsReporterClient}</li>
     *     <li>Verifies that the HTTP client's {@code close()} method was called exactly once</li>
     * </ol>
     *
     * <p>This test ensures that:</p>
     * <ul>
     *     <li>The client correctly propagates exceptions thrown during the closing of the HTTP client</li>
     * </ul>
     */
    @Test
    void testSendMetrics_closeClientThrowsIOException() throws Exception {
        Mockito.doThrow(new IOException("close fail")).when(httpClientMock).close();

        try (MockedStatic<HttpClients> mocked = Mockito.mockStatic(HttpClients.class)) {
            mocked.when(HttpClients::createDefault).thenReturn(httpClientMock);
            client = new MetricsReporterClient("http://metrics/");

            // track stdout
            ByteArrayOutputStream errContent = new ByteArrayOutputStream();
            System.setOut(new PrintStream(errContent));

            Method closeMethod = MetricsReporterClient.class.getDeclaredMethod("close");
            closeMethod.setAccessible(true);
            closeMethod.invoke(client);

            String output = errContent.toString();
            assertTrue(output.contains("WARN") && output.contains("close fail"));

            verify(httpClientMock, times(1)).close();
        } finally {
            System.setErr(System.err);
        }
    }
}
