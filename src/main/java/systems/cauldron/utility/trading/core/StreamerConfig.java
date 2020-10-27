package systems.cauldron.utility.trading.core;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class StreamerConfig {
    private String socketUrl;
    private String appId;
    private String token;
    private String accountId;
    private String credential;
    private String subscriptionKey;
}
