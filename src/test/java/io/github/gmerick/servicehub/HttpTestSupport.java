package io.github.gmerick.servicehub;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

abstract class HttpTestSupport {
  @LocalServerPort int port;
  final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  final JsonMapper json = new JsonMapper();

  record Result(int status, JsonNode body, HttpHeaders headers) {}

  Result call(String method, String path, String payload) throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .method(
                method,
                payload == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(payload))
            .build();
    var response = client.send(request, HttpResponse.BodyHandlers.ofString());
    return new Result(response.statusCode(), json.readTree(response.body()), response.headers());
  }
}
