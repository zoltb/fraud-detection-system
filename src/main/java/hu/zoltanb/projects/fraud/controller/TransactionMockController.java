package hu.zoltanb.projects.fraud.controller;

import hu.zoltanb.projects.fraud.config.FraudAppConfig;
import hu.zoltanb.projects.fraud.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/mock")
@RequiredArgsConstructor
public class TransactionMockController {
    private final FraudAppConfig config;


    @GetMapping("/transactions")
    public Transaction getRandomTransaction() {
        var gen = config.getGenerator();

        // Random data generation
        long randomUserId = ThreadLocalRandom.current().nextLong(
                gen.getMinUserId(), gen.getMaxUserId() + 1);

        long randomMerchantId = ThreadLocalRandom.current().nextLong(
                gen.getMinMerchantId(), gen.getMaxMerchantId() + 1);

        double randomAmount = ThreadLocalRandom.current().nextDouble(
                gen.getMinAmount(), gen.getMaxAmount());

        //ID and Date will be added later by Service)
        return Transaction.builder()
                .userId(randomUserId)
                .merchantId(randomMerchantId)
                .amount(BigDecimal.valueOf(randomAmount).setScale(2, RoundingMode.HALF_UP))
                .build();
    }
}