package systems.cauldron.utility.trading;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;


@Getter
@Setter
public class Configuration {
    private String clientId;
    private String redirectUri;
    private String authorizationCode;
    private String refreshToken;
    private Instant refreshTokenExpiry;

    public long getSecondsBeforeRefreshTokenExpiration() {
        return refreshToken != null && refreshTokenExpiry != null ? Duration.between(Instant.now(), refreshTokenExpiry).toSeconds() : -1;
    }
}
