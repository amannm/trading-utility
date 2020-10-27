package systems.cauldron.utility.trading.core;

import javax.json.Json;
import javax.json.JsonObject;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class OrderService {

    private static final String ACCOUNT_ORDERS_ENDPOINT = "https://api.tdameritrade.com/v1/accounts/%s/orders";
    private static final String ACCOUNT_ORDER_ENDPOINT = "https://api.tdameritrade.com/v1/accounts/%s/orders/%s";

    private final Supplier<String> accessTokenSource;

    public OrderService(Supplier<String> accessTokenSource) {
        this.accessTokenSource = accessTokenSource;
    }

    public CompletableFuture<String> createEquityBuyLimitOrder(String accountId, BigDecimal price, int quantity, String symbol) {
        JsonObject requestBody = Json.createObjectBuilder()
                .add("orderType", "LIMIT")
                .add("session", "NORMAL")
                .add("price", price.toString())
                .add("duration", "DAY")
                .add("orderStrategyType", "SINGLE")
                .add("orderLegCollection", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("instruction", "BUY")
                                .add("quantity", quantity)
                                .add("instrument", Json.createObjectBuilder()
                                        .add("symbol", symbol)
                                        .add("assetType", "EQUITY")
                                )
                        )
                )
                .build();
        return doCreateOrder(accountId, requestBody);
    }

    public CompletableFuture<Void> deleteOrder(String accountId, String orderId) {
        return doDeleteOrder(accountId, orderId);
    }

    private CompletableFuture<String> doCreateOrder(String accountId, JsonObject payload) {
        return HttpGateway.doAuthorizedJsonPost(String.format(ACCOUNT_ORDERS_ENDPOINT, accountId), accessTokenSource, payload);
    }

    private CompletableFuture<Void> doUpdateOrder(String accountId, String orderId, JsonObject payload) {
        return HttpGateway.doAuthorizedJsonPut(String.format(ACCOUNT_ORDER_ENDPOINT, accountId, orderId), accessTokenSource, payload);
    }

    private CompletableFuture<Void> doDeleteOrder(String accountId, String orderId) {
        return HttpGateway.doAuthorizedDelete(String.format(ACCOUNT_ORDER_ENDPOINT, accountId, orderId), accessTokenSource);
    }
}
