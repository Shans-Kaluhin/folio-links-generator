package org.folio.util;

import static org.folio.FolioLinksGeneratorApp.exitWithError;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Setter;
import lombok.SneakyThrows;
import org.folio.model.Configuration;

@Setter
public class HttpWorker {
  private final Configuration configuration;
  private String okapiToken;

  public HttpWorker(Configuration configuration) {
    this.configuration = configuration;
  }

  public HttpRequest constructGETRequest(String uri) {
    return constructRequest(uri).GET().build();
  }

  @SneakyThrows
  public HttpRequest constructPUTRequest(String uri, String body) {
    return constructRequest(uri)
      .header("Content-Type", "application/json")
      .PUT(HttpRequest.BodyPublishers.ofString(body))
      .build();
  }

  @SneakyThrows
  public HttpRequest constructPOSTRequest(String uri, String body) {
    return constructRequest(uri)
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(body))
      .build();
  }

  @SneakyThrows
  public HttpRequest constructPOSTRequest(String uri, Path filePath) {
    return constructRequest(uri)
      .header("Content-Type", "application/octet-stream")
      .POST(HttpRequest.BodyPublishers.ofByteArray(Files.readAllBytes(filePath)))
      .build();
  }

  public HttpRequest.Builder constructRequest(String uri) {
    var builder = HttpRequest.newBuilder()
      .uri(URI.create(configuration.getOkapiUrl() + uri))
      .header("x-okapi-tenant", configuration.getTenant());

    if (okapiToken != null) {
      builder.header("x-okapi-token", okapiToken);
    }

    return builder;
  }

  @SneakyThrows
  public HttpResponse<String> sendRequest(HttpRequest request) {
    return HttpClient.newHttpClient()
      .send(request, HttpResponse.BodyHandlers.ofString());
  }

  public void verifyStatus(HttpResponse<String> response, int expectedStatus, String errorMessage) {
    if (response.statusCode() != expectedStatus) {
      exitWithError(errorMessage);
    }
  }
}
