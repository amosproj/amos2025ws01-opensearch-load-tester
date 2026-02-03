package com.opensearchloadtester.loadgenerator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.loadgenerator.client.MetricsReporterClient;
import com.opensearchloadtester.loadgenerator.exception.MetricsReporterAccessException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

@ExtendWith(MockitoExtension.class)
class MetricsReporterClientTests {

  private MetricsReporterClient client;

  @Mock private CloseableHttpClient httpClientMock;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private ArrayList<MetricsDto> getTestMetrics() {
    ArrayList<MetricsDto> metrics = new ArrayList<>();
    metrics.add(new MetricsDto("lg-1", "test-query", 10, 1L, 10, 200));
    return metrics;
  }

  /**
   * Tests the {@link MetricsReporterClient#sendMetrics(List)} method for the scenario where the
   * HTTP request succeeds with status code 200 (OK)
   *
   * <p>This test does the following:
   *
   * <ol>
   *   <li>Creates a sample {@link MetricsDto} list with test data
   *   <li>Mocks a {@link CloseableHttpClient} to simulate sending the HTTP POST request
   *   <li>Overrides the {@link HttpClients#createDefault()} method to return the mocked client
   *   <li>Mocks the execution of the HTTP request to return a {@link ClassicHttpResponse} with HTTP
   *       status 200 (OK) via a {@link HttpClientResponseHandler}
   *   <li>Calls {@link MetricsReporterClient#sendMetrics(List)} and asserts that no exceptions are
   *       thrown
   *   <li>Manually calls implicit {@code close()} method of {@link MetricsReporterClient}
   *   <li>Verifies that the HTTP client's {@code execute()} and {@code close()} methods were called
   *       exactly once each
   * </ol>
   *
   * <p>Effectively, this test validates that:
   *
   * <ul>
   *   <li>The metrics reporting logic completes successfully when the server responds with 200 OK
   *   <li>The HTTP client is properly closed after execution
   * </ul>
   */
  @Test
  void testSendMetrics_successStatus200() throws Exception {
    ArrayList<MetricsDto> metrics = getTestMetrics();

    when(httpClientMock.execute(any(HttpPost.class), any(HttpClientResponseHandler.class)))
        .thenAnswer(
            invocation -> {
              HttpClientResponseHandler<Integer> handler = invocation.getArgument(1);
              ClassicHttpResponse response = Mockito.mock(ClassicHttpResponse.class);
              Mockito.when(response.getCode()).thenReturn(HttpStatus.SC_OK);
              return handler.handleResponse(response);
            });

    try (MockedStatic<HttpClients> mocked = Mockito.mockStatic(HttpClients.class)) {
      mocked.when(HttpClients::createDefault).thenReturn(httpClientMock);
      client = new MetricsReporterClient("http://metrics/", objectMapper, httpClientMock);

      assertDoesNotThrow(() -> client.sendMetrics(metrics));

      verify(httpClientMock, times(1))
          .execute(any(HttpPost.class), any(HttpClientResponseHandler.class));
    }
  }

  /**
   * Tests the {@link MetricsReporterClient#sendMetrics(List)} method for the scenario where the
   * HTTP request succeeds with status code 201 (Created)
   *
   * <p>This test performs the following steps:
   *
   * <ol>
   *   <li>Creates a sample {@link MetricsDto} object with test data
   *   <li>Mocks a {@link CloseableHttpClient} to simulate sending the HTTP POST request
   *   <li>Overrides the {@link HttpClients#createDefault()} method to return the mocked client
   *   <li>Mocks the execution of the HTTP request to return a {@link ClassicHttpResponse} with HTTP
   *       status 201 (Created) via a {@link HttpClientResponseHandler}
   *   <li>Calls {@link MetricsReporterClient#sendMetrics(List)} and asserts that no exceptions are
   *       thrown
   *   <li>Verifies that the HTTP client's {@code execute()} method was called exactly once
   * </ol>
   *
   * <p>This test ensures that:
   *
   * <ul>
   *   <li>The metrics reporting logic completes successfully when the server responds with "201
   *       Created"
   *   <li>The HTTP request is executed exactly once
   * </ul>
   */
  @Test
  void testSendMetrics_successStatus201() throws Exception {
    ArrayList<MetricsDto> metrics = getTestMetrics();

    when(httpClientMock.execute(any(HttpPost.class), any(HttpClientResponseHandler.class)))
        .thenAnswer(
            invocation -> {
              HttpClientResponseHandler<Integer> handler = invocation.getArgument(1);
              ClassicHttpResponse response = Mockito.mock(ClassicHttpResponse.class);
              Mockito.when(response.getCode()).thenReturn(HttpStatus.SC_CREATED);
              return handler.handleResponse(response);
            });

    try (MockedStatic<HttpClients> mocked = Mockito.mockStatic(HttpClients.class)) {
      mocked.when(HttpClients::createDefault).thenReturn(httpClientMock);
      client = new MetricsReporterClient("http://metrics/", objectMapper, httpClientMock);

      assertDoesNotThrow(() -> client.sendMetrics(metrics));

      verify(httpClientMock, times(1))
          .execute(any(HttpPost.class), any(HttpClientResponseHandler.class));
    }
  }

  /**
   * Tests the {@link MetricsReporterClient#sendMetrics(List)} method for the scenario where JSON
   * serialization of the metrics fails
   *
   * <p>This test performs the following steps:
   *
   * <ol>
   *   <li>Creates a sample {@link MetricsDto} object with test data
   *   <li>Mocks the {@link ObjectMapper} used by {@link MetricsReporterClient}
   *   <li>Configures the mocked {@link ObjectMapper} to throw a {@link JsonProcessingException}
   *       when {@code writeValueAsString} is called
   *   <li>Sets the mocked ObjectMapper into the {@code MetricsReporterClient} using reflection
   *   <li>Calls {@link MetricsReporterClient#sendMetrics(List)} and asserts that a {@link
   *       MetricsReporterAccessException} is thrown
   * </ol>
   *
   * <p>This test ensures that:
   *
   * <ul>
   *   <li>The client properly wraps JSON serialization errors into a {@code
   *       MetricsReporterAccessException}
   *   <li>Errors during metrics serialization are correctly propagated to the caller
   * </ul>
   */
  @Test
  void testSendMetrics_jsonSerializationFails() throws Exception {
    ArrayList<MetricsDto> metrics = getTestMetrics();

    ObjectMapper mapperMock = Mockito.mock(ObjectMapper.class);

    when(mapperMock.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});

    client = new MetricsReporterClient("http://metrics/", objectMapper, httpClientMock);
    ReflectionTestUtils.setField(client, "objectMapper", mapperMock);

    assertThrows(MetricsReporterAccessException.class, () -> client.sendMetrics(metrics));
  }

  /**
   * Tests the {@link MetricsReporterClient#sendMetrics(List)} method for the scenario where the
   * HTTP request repeatedly fails with a non-success status code (500 Internal Server Error) and
   * triggers the retry mechanism
   *
   * <p>This test performs the following steps:
   *
   * <ol>
   *   <li>Creates a sample {@link MetricsDto} object with test data
   *   <li>Mocks a {@link CloseableHttpClient} to simulate sending HTTP POST requests
   *   <li>Configures the mocked client so that every execution returns a {@link
   *       ClassicHttpResponse} with HTTP status 500 (Internal Server Error) via a {@link
   *       HttpClientResponseHandler}
   *   <li>Overrides {@link HttpClients#createDefault()} to return the mocked client
   *   <li>Calls {@link MetricsReporterClient#sendMetrics(List)} and asserts that a {@link
   *       MetricsReporterAccessException} is thrown after exhausting all retries
   *   <li>Manually calls implicit {@code close()} method of {@link MetricsReporterClient}
   *   <li>Verifies that the HTTP client's {@code execute()} method was called exactly 3 times (one
   *       per retry) and {@code close()} was called once
   * </ol>
   *
   * <p>This test ensures that:
   *
   * <ul>
   *   <li>The retry mechanism is invoked for failed HTTP responses
   *   <li>The client properly throws {@code MetricsReporterAccessException} after exhausting
   *       retries
   *   <li>The HTTP client is correctly closed even after repeated failures
   * </ul>
   */
  @Test
  void testSendMetrics_failsAfter3Attempts_httpStatusNotOk() throws Exception {
    ArrayList<MetricsDto> metrics = getTestMetrics();

    when(httpClientMock.execute(any(HttpPost.class), any(HttpClientResponseHandler.class)))
        .thenAnswer(
            invocation -> {
              HttpClientResponseHandler<Integer> handler = invocation.getArgument(1);
              ClassicHttpResponse response = Mockito.mock(ClassicHttpResponse.class);
              Mockito.when(response.getCode()).thenReturn(500);
              return handler.handleResponse(response);
            });

    try (MockedStatic<HttpClients> mocked = Mockito.mockStatic(HttpClients.class)) {
      mocked.when(HttpClients::createDefault).thenReturn(httpClientMock);
      client = new MetricsReporterClient("http://metrics/", objectMapper, httpClientMock);

      assertThrows(MetricsReporterAccessException.class, () -> client.sendMetrics(metrics));

      verify(httpClientMock, times(3))
          .execute(any(HttpPost.class), any(HttpClientResponseHandler.class));
    }
  }

  /**
   * Tests the {@link MetricsReporterClient#sendMetrics(List)} method for the scenario where the
   * HTTP request fails due to an {@link IOException} on every attempt, triggering the retry
   * mechanism
   *
   * <p>This test performs the following steps:
   *
   * <ol>
   *   <li>Creates a sample {@link MetricsDto} object with test data
   *   <li>Mocks a {@link CloseableHttpClient} to simulate sending HTTP POST requests
   *   <li>Configures the mocked client to throw an {@link IOException} for every execution
   *   <li>Overrides {@link HttpClients#createDefault()} to return the mocked client
   *   <li>Calls {@link MetricsReporterClient#sendMetrics(List)} and asserts that a {@link
   *       MetricsReporterAccessException} is thrown after exhausting all retries
   *   <li>Manually calls implicit {@code close()} method of {@link MetricsReporterClient}
   *   <li>Verifies that the HTTP client's {@code execute()} method was called exactly 3 times (one
   *       per retry) and {@code close()} was called once
   * </ol>
   *
   * <p>This test ensures that:
   *
   * <ul>
   *   <li>The retry mechanism is invoked when I/O errors occur during HTTP requests
   *   <li>The client properly throws {@code MetricsReporterAccessException} after all retries fail
   *   <li>The HTTP client is correctly closed even after repeated I/O failures
   * </ul>
   */
  @Test
  void testSendMetrics_failsAfter3Attempts_ioException() throws Exception {
    ArrayList<MetricsDto> metrics = getTestMetrics();

    when(httpClientMock.execute(any(HttpPost.class), any(HttpClientResponseHandler.class)))
        .thenThrow(new IOException("IO boom"));

    try (MockedStatic<HttpClients> mocked = Mockito.mockStatic(HttpClients.class)) {
      mocked.when(HttpClients::createDefault).thenReturn(httpClientMock);
      client = new MetricsReporterClient("http://metrics/", objectMapper, httpClientMock);

      assertThrows(MetricsReporterAccessException.class, () -> client.sendMetrics(metrics));

      verify(httpClientMock, times(3))
          .execute(any(HttpPost.class), any(HttpClientResponseHandler.class));
    }
  }

  /**
   * Tests the {@link MetricsReporterClient#sendMetrics(List)} method for the scenario where
   * creating the {@link CloseableHttpClient} fails
   *
   * <p>This test performs the following steps:
   *
   * <ol>
   *   <li>Mocks the static method {@link HttpClients#createDefault()} to throw a {@link
   *       RuntimeException}
   *   <li>Instantiates {@link MetricsReporterClient} and asserts that a {@link
   *       MetricsReporterAccessException} is thrown
   * </ol>
   *
   * <p>This test ensures that:
   *
   * <ul>
   *   <li>The client correctly handles exceptions that occur during HTTP client creation
   *   <li>Such errors are wrapped and propagated as {@code MetricsReporterAccessException}
   * </ul>
   */
  @Test
  void testSendMetrics_httpClientCreationFails() {
    try (MockedStatic<HttpClients> mocked = Mockito.mockStatic(HttpClients.class)) {
      mocked.when(HttpClients::createDefault).thenThrow(new RuntimeException("boom"));
      assertThrows(
          RuntimeException.class,
          () ->
              client =
                  new MetricsReporterClient(
                      "http://metrics/", objectMapper, HttpClients.createDefault()));
    }
  }
}
