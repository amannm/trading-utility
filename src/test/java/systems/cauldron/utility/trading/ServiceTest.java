package systems.cauldron.utility.trading;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ServiceTest {

    static AuthenticationService authenticator;
    static Supplier<String> tokenSource;

    @BeforeAll
    public static void setup() {
        authenticator = new AuthenticationService();
        tokenSource = authenticator.start();
    }

    @AfterAll
    public static void cleanup() {
        authenticator.stop();
    }


    @Test
    public void ensureAccountDataWorks() {
        AccountService accountData = new AccountService(tokenSource);
        accountData.printRawResponse();
        Map<String, BigDecimal> availableCashBalances = accountData.getAvailableCashBalances().join();
        assertFalse(availableCashBalances.isEmpty());
        System.out.println(availableCashBalances);
    }

    @Test
    public void ensureSlingerWorks() {

        AccountService accountService = new AccountService(tokenSource);
        OrderService orderService = new OrderService(tokenSource);

        Slinger slinger = new Slinger(accountService, orderService);

        slinger.execute("QQQ", BigDecimal.valueOf(280)).thenAccept(CompletableFuture::allOf).join();

        Map<String, BigDecimal> availableCashBalances = accountService.getAvailableCashBalances().join();
        assertFalse(availableCashBalances.isEmpty());
        System.out.println(availableCashBalances);
    }

    @Test
    public void ensureUserPrincipalsWorks() {
        UserPrincipalsService service = new UserPrincipalsService(tokenSource);
        service.getStreamerConfig().thenAccept(config -> {
            assertNotNull(config.getAccountId());
            assertNotNull(config.getAppId());
            assertNotNull(config.getCredential());
            assertNotNull(config.getSocketUrl());
            assertNotNull(config.getSubscriptionKey());
            assertNotNull(config.getToken());
        }).join();
    }


    @Test
    public void ensureStreamerWorks() {
        UserPrincipalsService service = new UserPrincipalsService(tokenSource);
        service.getStreamerConfig().thenAccept(config -> {
            StreamerService streamerService = new StreamerService(config);
            streamerService.start();
            streamerService.stop();
        }).join();
    }
}