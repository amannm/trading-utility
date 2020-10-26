package systems.cauldron.utility.trading;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.json.JsonObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class UserPrincipalsService {

    private final static Logger LOG = LogManager.getLogger(UserPrincipalsService.class);

    private static final String USER_PRINCIPALS_ENDPOINT = "https://api.tdameritrade.com/v1/userprincipals";
    private static final String STREAMER_SUBSCRIPTION_KEYS_ENDPOINT = "https://api.tdameritrade.com/v1/userprincipals/streamersubscriptionkeys";

    private static final DateTimeFormatter TIMESTAMP_FORMAT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .parseLenient()
            .appendOffset("+HHMM", "+0000")
            .parseStrict()
            .toFormatter();

    private final Supplier<String> accessTokenSource;

    public UserPrincipalsService(Supplier<String> accessTokenSource) {
        this.accessTokenSource = accessTokenSource;
    }

    public CompletableFuture<StreamerConfig> getStreamerConfig() {
        return doUserPrincipalsCall().thenApply(response -> {

                    JsonObject streamerInfo = response.getJsonObject("streamerInfo");
                    String socketUrl = streamerInfo.getString("streamerSocketUrl");
                    String appId = streamerInfo.getString("appId");
                    String token = streamerInfo.getString("token");
                    String tokenTimestamp = streamerInfo.getString("tokenTimestamp");
                    String userGroup = streamerInfo.getString("userGroup");
                    String accessLevel = streamerInfo.getString("accessLevel");
                    String acl = streamerInfo.getString("acl");

                    String subscriptionKey = response.getJsonObject("streamerSubscriptionKeys")
                            .getJsonArray("keys").get(0).asJsonObject()
                            .getString("key");

                    JsonObject account = response.getJsonArray("accounts").get(0).asJsonObject();
                    String accountId = account.getString("accountId");
                    String company = account.getString("company");
                    String segment = account.getString("segment");
                    String accountCdDomainId = account.getString("accountCdDomainId");

                    String credential = "userid" + "=" + URLEncoder.encode(accountId, StandardCharsets.UTF_8) + "&" +
                            "token" + "=" + URLEncoder.encode(token, StandardCharsets.UTF_8) + "&" +
                            "company" + "=" + URLEncoder.encode(company, StandardCharsets.UTF_8) + "&" +
                            "segment" + "=" + URLEncoder.encode(segment, StandardCharsets.UTF_8) + "&" +
                            "cddomain" + "=" + URLEncoder.encode(accountCdDomainId, StandardCharsets.UTF_8) + "&" +
                            "usergroup" + "=" + URLEncoder.encode(userGroup, StandardCharsets.UTF_8) + "&" +
                            "accesslevel" + "=" + URLEncoder.encode(accessLevel, StandardCharsets.UTF_8) + "&" +
                            "authorized" + "=" + "Y" + "&" +
                            "timestamp" + "=" + TIMESTAMP_FORMAT.parse(tokenTimestamp, Instant::from).toEpochMilli() + "&" +
                            "appid" + "=" + URLEncoder.encode(appId, StandardCharsets.UTF_8) + "&" +
                            "acl" + "=" + URLEncoder.encode(acl, StandardCharsets.UTF_8);

                    return StreamerConfig.builder()
                            .accountId(accountId)
                            .socketUrl(socketUrl)
                            .appId(appId)
                            .credential(credential)
                            .token(token)
                            .subscriptionKey(subscriptionKey)
                            .build();
                }
        );
    }

    private CompletableFuture<JsonObject> doUserPrincipalsCall() {
        return HttpGateway.doAuthorizedGetForJsonObject(USER_PRINCIPALS_ENDPOINT, accessTokenSource, Map.of(
                "fields", Arrays.asList("streamerConnectionInfo", "streamerSubscriptionKeys")
        ));
    }

    private CompletableFuture<JsonObject> doStreamerSubscriptionKeyCall(String accountId) {
        return HttpGateway.doAuthorizedGetForJsonObject(USER_PRINCIPALS_ENDPOINT, accessTokenSource, Map.of(
                "accountIds", Collections.singletonList(accountId)
        ));
    }
}
