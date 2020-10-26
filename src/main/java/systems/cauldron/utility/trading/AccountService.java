package systems.cauldron.utility.trading;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AccountService {


    private static final String ACCOUNTS_ENDPOINT = "https://api.tdameritrade.com/v1/accounts";

    private final Supplier<String> accessTokenSource;

    public AccountService(Supplier<String> accessTokenSource) {
        this.accessTokenSource = accessTokenSource;
    }

    private static final JsonWriterFactory WRITER_FACTORY = Json.createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, true));

    public void printRawResponse() {
        doAccountsCall().thenAccept(jsonObjects -> jsonObjects.forEach(o -> {
            try (JsonWriter jsonWriter = WRITER_FACTORY.createWriter(System.out)) {
                jsonWriter.write(o);
            }
        })).join();
    }

    public CompletableFuture<Map<String, BigDecimal>> getAvailableCashBalances() {
        return doAccountsCall().thenApply(accounts -> accounts.stream()
                .map(o -> o.getJsonObject("securitiesAccount"))
                .collect(Collectors.toMap(
                        o -> o.getString("accountId"),
                        o -> o.getJsonObject("initialBalances")
                                .getJsonNumber("totalCash").bigDecimalValue()
                )));
    }

    private CompletableFuture<List<JsonObject>> doAccountsCall() {
        return HttpGateway.doAuthorizedGetForJsonList(ACCOUNTS_ENDPOINT, accessTokenSource, Map.of(
                "fields", Collections.singletonList("positions")
        ));
    }

}
