package systems.cauldron.utility.trading.core;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public class Slinger {

    private final AccountService accountService;
    private final OrderService orderService;

    public Slinger(AccountService accountService, OrderService orderService) {
        this.accountService = accountService;
        this.orderService = orderService;
    }

    public CompletableFuture<CompletableFuture<?>[]> execute(String symbol, BigDecimal price) {
        return accountService.getAvailableCashBalances().thenApply(x -> x.entrySet().stream().map(e -> {
            int quantity = e.getValue().divideToIntegralValue(price).intValue();
            return orderService.createEquityBuyLimitOrder(e.getKey(), price, quantity, symbol);
        }).toArray(CompletableFuture<?>[]::new));
    }
}
