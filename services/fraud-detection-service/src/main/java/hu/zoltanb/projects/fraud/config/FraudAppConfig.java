package hu.zoltanb.projects.fraud.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
@Configuration
@ConfigurationProperties(prefix = "fraud")
@Data
public class FraudAppConfig {

    private Detection detection = new Detection();
    private Generator generator = new Generator();

    @Data
    public static class Detection {
        private CardTest cardTest = new CardTest();
        private Velocity velocity = new Velocity();

        @Data
        public static class CardTest {
            private BigDecimal amountLimit;
            private int countLimit;
            private int durationMinutes;
        }

        @Data
        public static class Velocity {
            private int countLimit;
            private int durationMinutes;
        }
    }

    @Data
    public static class Generator {
        // Getterek és Setterek (vagy @Data annotáció a Lomboktól)
        private int transactionCount;
        private long initialDelay; // A YAML-ben: initial-delay

        private long minUserId;
        private long maxUserId;
        private long minMerchantId;
        private long maxMerchantId;
        private double minAmount;
        private double maxAmount;
        private int sleepMs;
    }
}