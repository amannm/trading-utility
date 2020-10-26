package systems.cauldron.utility.trading;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class StreamerService {

    private final static System.Logger LOG = System.getLogger(StreamerService.class.getName());

    private final StreamerConfig config;

    private final AtomicReference<WebSocket> socket = new AtomicReference<>();
    private final AtomicLong requestIdSource = new AtomicLong();
    private final Map<String, Consumer<JsonObject>> requestHandlers = new ConcurrentHashMap<>();

    public StreamerService(StreamerConfig config) {
        this.config = config;
    }

    // TODO: do this properly
    public void start() {
        CountDownLatch openLatch = new CountDownLatch(1);
        HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI.create("wss://" + config.getSocketUrl() + "/ws"), new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                LOG.log(System.Logger.Level.INFO, "message received: " + data.toString());
                JsonObject jsonObject;
                try (JsonReader reader = Json.createReader(new StringReader(data.toString()))) {
                    jsonObject = reader.readObject();
                }
                JsonArray responses = jsonObject.getJsonArray("response");
                if (responses != null) {
                    JsonObject response = responses.get(0).asJsonObject();
                    Consumer<JsonObject> handler = requestHandlers.remove(response.getString("requestid"));
                    if (handler != null) {
                        handler.accept(response);
                    }
                }
                return WebSocket.Listener.super.onText(webSocket, data, last);
            }

            @Override
            public void onOpen(WebSocket webSocket) {
                LOG.log(System.Logger.Level.INFO, "socket opened");
                doLoginRequest(webSocket, openLatch);
                WebSocket.Listener.super.onOpen(webSocket);
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                LOG.log(System.Logger.Level.ERROR, "socket closed with statusCode: " + statusCode + " and reason: " + reason);
                return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
            }
        }).join();
        try {
            openLatch.await(5L, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        LOG.log(System.Logger.Level.INFO, "streamer started successfully");
    }

    // TODO: do this properly
    public void stop() {
        Optional.ofNullable(socket.getAndSet(null)).ifPresent(s -> {
            if (!s.isOutputClosed()) {
                if (!s.isInputClosed()) {
                    // attempt clean disconnect
                    CountDownLatch latch = new CountDownLatch(1);
                    String requestId = String.valueOf(requestIdSource.getAndIncrement());
                    JsonObject logoutRequest = Json.createObjectBuilder()
                            .add("service", "ADMIN")
                            .add("command", "LOGOUT")
                            .add("requestid", requestId)
                            .add("account", config.getAccountId())
                            .add("source", config.getAppId())
                            .build();
                    requestHandlers.put(requestId, response -> {
                        JsonNumber code = response.getJsonObject("content").getJsonNumber("code");
                        if (code != null) {
                            switch (code.intValue()) {
                                case 0:
                                    latch.countDown();
                                    break;
                                case 3:
                                default:
                                    throw new RuntimeException(response.getString("msg"));
                            }
                        }
                    });
                    s.sendText(logoutRequest.toString(), true)
                            .thenCompose(x -> {
                                try {
                                    latch.await(5L, TimeUnit.SECONDS);
                                } catch (InterruptedException ex) {
                                    throw new RuntimeException(ex);
                                }
                                if (!x.isOutputClosed()) {
                                    return x.sendClose(WebSocket.NORMAL_CLOSURE, "client shutting down");
                                } else {
                                    return CompletableFuture.completedFuture(null);
                                }
                            }).join();
                } else {
                    // abrupt disconnect
                    s.abort();
                }
            }
        });
        LOG.log(System.Logger.Level.INFO, "streamer stopped successfully");
    }

    private void doLoginRequest(WebSocket webSocket, CountDownLatch openLatch) {
        String requestId = String.valueOf(requestIdSource.getAndIncrement());
        JsonObject loginRequest = Json.createObjectBuilder()
                .add("service", "ADMIN")
                .add("command", "LOGIN")
                .add("requestid", requestId)
                .add("account", config.getAccountId())
                .add("source", config.getAppId())
                .add("parameters", Json.createObjectBuilder()
                        .add("credential", config.getCredential())
                        .add("token", config.getToken())
                        .add("version", "1.0"))
                .build();
        requestHandlers.put(requestId, response -> handleLoginResponse(webSocket, response, openLatch));
        webSocket.sendText(loginRequest.toString(), true);
    }

    private void doAccountActivitySubscriptionRequest(WebSocket webSocket, CountDownLatch openLatch) {
        String requestId = String.valueOf(requestIdSource.getAndIncrement());
        JsonObject loginRequest = Json.createObjectBuilder()
                .add("service", "ACCT_ACTIVITY")
                .add("command", "SUBS")
                .add("requestid", requestId)
                .add("account", config.getAccountId())
                .add("source", config.getAppId())
                .add("parameters", Json.createObjectBuilder()
                        .add("keys", config.getSubscriptionKey())
                        .add("fields", "0,1,2,3"))
                .build();
        requestHandlers.put(requestId, response -> handleLoginResponse(webSocket, response, openLatch));
        webSocket.sendText(loginRequest.toString(), true);
    }

    private void handleLoginResponse(WebSocket webSocket, JsonObject response, CountDownLatch openLatch) {
        JsonNumber code = response.getJsonObject("content")
                .getJsonNumber("code");
        if (code != null) {
            switch (code.intValue()) {
                case 0:
                    socket.set(webSocket);
                    openLatch.countDown();
                    break;
                case 3:
                default:
                    throw new RuntimeException(response.getString("msg"));
            }
        }
    }
}
