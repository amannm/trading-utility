package systems.cauldron.utility.trading;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class AuthenticationService {

    private static final String AUTHENTICATION_ENDPOINT = "https://api.tdameritrade.com/v1/oauth2/token";
    private final static System.Logger LOG = System.getLogger(AuthenticationService.class.getName());

    private final Configuration configuration;

    private ScheduledExecutorService reinitializer = null;
    private final AtomicReference<ScheduledExecutorService> currentRefresher = new AtomicReference<>();
    private final AtomicReference<String> currentAccessToken = new AtomicReference<>();

    public AuthenticationService() {
        this.configuration = ConfigurationFactory.load();
    }

    /**
     * Blocks until the initial access token is available via the returned supplier.
     */
    public synchronized Supplier<String> start() {
        stop();
        reinitializer = Executors.newSingleThreadScheduledExecutor();
        long secondsUntilRefreshTokenExpiry = configuration.getSecondsBeforeRefreshTokenExpiration();
        if (secondsUntilRefreshTokenExpiry > 0) {
            ScheduledExecutorService refresher = Executors.newSingleThreadScheduledExecutor();
            refresh(configuration.getRefreshToken(), refresher).join();
            reinitializer.schedule(() -> initialize().join(), secondsUntilRefreshTokenExpiry, TimeUnit.SECONDS);
        } else {
            initialize().join();
        }
        return currentAccessToken::get;
    }

    public synchronized void stop() {
        safeShutdown(reinitializer);
        reinitializer = null;
        safeShutdown(currentRefresher.getAndSet(null));
        ConfigurationFactory.save(configuration);
    }

    private CompletableFuture<Void> initialize() {
        return HttpGateway.doUnauthorizedUrlEncodedPostForJsonObject(AUTHENTICATION_ENDPOINT, Map.of(
                "grant_type", Collections.singletonList("authorization_code"),
                "code", Collections.singletonList(configuration.getAuthorizationCode()),
                "client_id", Collections.singletonList(configuration.getClientId()),
                "redirect_uri", Collections.singletonList(configuration.getRedirectUri()),
                "access_type", Collections.singletonList("offline")
        )).thenAccept(response -> {
            String initialAccessToken = response.getString("access_token");
            int initialTokenExpiresIn = response.getInt("expires_in");
            String refreshToken = response.getString("refresh_token");
            int refreshTokenExpiresIn = response.getInt("refresh_token_expires_in");
            ScheduledExecutorService refresher = Executors.newSingleThreadScheduledExecutor();
            refresher.schedule(() -> refresh(refreshToken, refresher).join(), initialTokenExpiresIn, TimeUnit.SECONDS);
            safeShutdown(currentRefresher.getAndSet(refresher));
            reinitializer.schedule(() -> initialize().join(), refreshTokenExpiresIn, TimeUnit.SECONDS);
            currentAccessToken.set(initialAccessToken);
            LOG.log(System.Logger.Level.INFO, "successfully initialized access token");
            LOG.log(System.Logger.Level.INFO, "next access token refresh: " + Instant.now().plus(initialTokenExpiresIn, ChronoUnit.SECONDS).toString());
            LOG.log(System.Logger.Level.INFO, "next refresh token initialization: " + Instant.now().plus(refreshTokenExpiresIn, ChronoUnit.SECONDS).toString());
            configuration.setRefreshToken(refreshToken);
            configuration.setRefreshTokenExpiry(Instant.now().plus(refreshTokenExpiresIn, ChronoUnit.SECONDS));
            ConfigurationFactory.save(configuration);
        });
    }

    private CompletableFuture<Void> refresh(String refreshToken, ScheduledExecutorService refresher) {
        return HttpGateway.doUnauthorizedUrlEncodedPostForJsonObject(AUTHENTICATION_ENDPOINT, Map.of(
                "grant_type", Collections.singletonList("refresh_token"),
                "refresh_token", Collections.singletonList(refreshToken),
                "client_id", Collections.singletonList(configuration.getClientId())
        )).thenAccept(response -> {
            String accessToken = response.getString("access_token");
            int nextTokenExpiresIn = response.getInt("expires_in");
            refresher.schedule(() -> refresh(refreshToken, refresher).join(), nextTokenExpiresIn, TimeUnit.SECONDS);
            currentAccessToken.set(accessToken);
            LOG.log(System.Logger.Level.INFO, "successfully refreshed access token: " + accessToken);
            LOG.log(System.Logger.Level.INFO, "next access token refresh: " + Instant.now().plus(nextTokenExpiresIn, ChronoUnit.SECONDS).toString());
        });
    }

    private static void safeShutdown(ScheduledExecutorService service) {
        if (service != null && !service.isTerminated()) {
            service.shutdown();
            try {
                service.awaitTermination(5L, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                LOG.log(System.Logger.Level.ERROR, "failed to safely terminate process", ex);
                service.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

}
