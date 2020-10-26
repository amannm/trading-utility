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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class HttpGateway {

    public static CompletableFuture<JsonObject> doAuthorizedGetForJsonObject(String url, Supplier<String> accessTokenSource, Map<String, List<String>> queryParams) {
        HttpRequest request = buildAuthorizedGet(url, accessTokenSource, queryParams);
        return doJsonResponseRequest(request, HttpGateway::readJsonObject);
    }

    public static CompletableFuture<List<JsonObject>> doAuthorizedGetForJsonList(String url, Supplier<String> accessTokenSource, Map<String, List<String>> queryParams) {
        HttpRequest request = buildAuthorizedGet(url, accessTokenSource, queryParams);
        return doJsonResponseRequest(request, HttpGateway::readJsonObjectList);
    }

    public static CompletableFuture<String> doAuthorizedJsonPost(String url, Supplier<String> accessTokenSource, JsonObject payload) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessTokenSource.get())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
        return HttpClient.newHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(x -> {
                    String location = x.headers().firstValue("Location").orElseThrow(RuntimeException::new);
                    return location.substring(location.lastIndexOf('/') + 1);
                });
    }

    public static CompletableFuture<Void> doAuthorizedJsonPut(String url, Supplier<String> accessTokenSource, JsonObject payload) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessTokenSource.get())
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
        return doEmptyResponseRequest(request);
    }

    public static CompletableFuture<Void> doAuthorizedDelete(String url, Supplier<String> accessTokenSource) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessTokenSource.get())
                .DELETE()
                .build();
        return doEmptyResponseRequest(request);
    }

    public static CompletableFuture<JsonObject> doUnauthorizedUrlEncodedPostForJsonObject(String url, Map<String, List<String>> params) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(HttpGateway.urlEncode(params)))
                .build();
        return doJsonResponseRequest(request, HttpGateway::readJsonObject);
    }

    private static HttpRequest buildAuthorizedGet(String url, Supplier<String> accessTokenSource, Map<String, List<String>> queryParams) {
        return HttpRequest.newBuilder()
                .uri(URI.create(queryParams.isEmpty() ? url : url + "?" + urlEncode(queryParams)))
                .header("Authorization", "Bearer " + accessTokenSource.get())
                .GET()
                .build();
    }

    private static CompletableFuture<Void> doEmptyResponseRequest(HttpRequest request) {
        return HttpClient.newHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(HttpResponse::body);
    }

    private static <T> CompletableFuture<T> doJsonResponseRequest(HttpRequest request, Function<JsonReader, T> mapper) {
        return HttpClient.newHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(HttpResponse::body)
                .thenApply(is -> {
                    try (JsonReader reader = Json.createReader(is)) {
                        return mapper.apply(reader);
                    }
                });
    }

    private static JsonObject readJsonObject(JsonReader reader) {
        return reader.readObject();
    }

    private static List<JsonObject> readJsonObjectList(JsonReader reader) {
        return reader.readArray().stream()
                .map(JsonValue::asJsonObject)
                .collect(Collectors.toList());
    }

    public static String urlEncode(Map<String, List<String>> pairs) {
        return pairs.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(String.join(",", e.getValue()), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
