package systems.cauldron.utility.trading;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class HttpGateway {

    public static CompletableFuture<List<JsonObject>> doAuthorizedGetForJsonList(String url, String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        return HttpClient.newHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(HttpResponse::body)
                .thenApply(is -> {
                    try (JsonReader reader = Json.createReader(is)) {
                        return reader.readArray().stream()
                                .map(JsonValue::asJsonObject)
                                .collect(Collectors.toList());
                    }
                });
    }

    public static CompletableFuture<String> doAuthorizedJsonPost(String url, String token, JsonObject payload) {
        String requestBody = payload.toString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        return HttpClient.newHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(x -> {
                    String location = x.headers().firstValue("Location").orElseThrow(RuntimeException::new);
                    return location.substring(location.lastIndexOf('/') + 1);
                });
    }

    public static CompletableFuture<Void> doAuthorizedJsonPut(String url, String token, JsonObject payload) {
        String requestBody = payload.toString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        return HttpClient.newHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(HttpResponse::body);
    }

    public static CompletableFuture<Void> doAuthorizedJsonDelete(String url, String token) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build();
        return HttpClient.newHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(HttpResponse::body);
    }

    public static CompletableFuture<JsonObject> doUnauthorizedUrlEncodedPostForJsonObject(String url, Map<String, String> params) {
        String requestBody = HttpGateway.urlEncode(params);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        return HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(HttpResponse::body)
                .thenApply(is -> {
                    try (JsonReader reader = Json.createReader(is)) {
                        return reader.readObject();
                    }
                });
    }

    public static String urlEncode(Map<String, String> pairs) {
        return pairs.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
