package systems.cauldron.utility.trading;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BasicTest {
    @Test
    public void ensureSavedRefreshTokenWorks() {
        AuthenticationService authenticator = new AuthenticationService();

        Supplier<String> tokenSource = authenticator.start();

        String initialToken = tokenSource.get();
        assertNotNull(initialToken);
        assertFalse(initialToken.isEmpty());

        authenticator.stop();
    }

    @Test
    public void ensureTokenRefreshWorks() throws InterruptedException {
        AuthenticationService authenticator = new AuthenticationService();

        Supplier<String> tokenSource = authenticator.start();

        String initialToken = tokenSource.get();
        assertNotNull(initialToken);
        assertFalse(initialToken.isEmpty());

        Thread.sleep(1000 * 60 * 35);

        String refreshedToken = tokenSource.get();
        assertNotNull(refreshedToken);
        assertFalse(refreshedToken.isEmpty());

        assertNotEquals(initialToken, refreshedToken);

        authenticator.stop();

        String refreshedTokenAfterAuthenticatorStopped = tokenSource.get();
        assertNotNull(refreshedTokenAfterAuthenticatorStopped);
        assertFalse(refreshedTokenAfterAuthenticatorStopped.isEmpty());

        assertEquals(refreshedToken, refreshedTokenAfterAuthenticatorStopped);
    }


    @Test
    public void ensureAccountDataWorks() {
        AuthenticationService authenticator = new AuthenticationService();

        Supplier<String> tokenSource = authenticator.start();

        AccountService accountData = new AccountService(tokenSource);

        accountData.printRawResponse();

        Map<String, BigDecimal> availableCashBalances = accountData.getAvailableCashBalances().join();
        assertFalse(availableCashBalances.isEmpty());
        System.out.println(availableCashBalances);

        authenticator.stop();
    }

    @Test
    public void ensureSlingerWorks() {
        AuthenticationService authenticator = new AuthenticationService();

        Supplier<String> tokenSource = authenticator.start();

        AccountService accountService = new AccountService(tokenSource);
        OrderService orderService = new OrderService(tokenSource);

        Slinger slinger = new Slinger(accountService, orderService);

        slinger.execute("QQQ", BigDecimal.valueOf(280)).thenAccept(CompletableFuture::allOf).join();

        Map<String, BigDecimal> availableCashBalances = accountService.getAvailableCashBalances().join();
        assertFalse(availableCashBalances.isEmpty());
        System.out.println(availableCashBalances);

        authenticator.stop();
    }
}