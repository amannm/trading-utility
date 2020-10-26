package systems.cauldron.utility.trading;

import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AuthenticationTest {
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
}