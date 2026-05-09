package hu.zoltanb.projects.fraud.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FraudReportingHelperTest.Config.class)
@ActiveProfiles("test") // Activates the application-test.yaml
@Transactional // To have automatic clean up
@ExtendWith(OutputCaptureExtension.class)
public class FraudReportingHelperTest {

    @Configuration
    @EnableAutoConfiguration // Creates H2 Datasource and JDBCTemplate
    static class Config {
        // Just for limiting the Spring context
        @Bean
        public FraudReportingHelper fraudReportingHelper(JdbcTemplate jdbcTemplate) {
            return new FraudReportingHelper(jdbcTemplate);
        }
    }

    @Autowired
    private FraudReportingHelper fraudReportingHelper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Check the logs after having transactions")
    void shouldLogCorrectStatistics(CapturedOutput capturedOutput) {
        // 1. Generate test data in H2
        // User 1: fraud_type = null
        jdbcTemplate.execute("INSERT INTO transactions (user_id, fraud_type) VALUES (1, NULL)");
        // User 2: fraud_type = CARD_TESTING
        jdbcTemplate.execute("INSERT INTO transactions (user_id, fraud_type) VALUES (2, 'CARD_TESTING')");
        // User 1: fraud_type = VELOCITY
        jdbcTemplate.execute("INSERT INTO transactions (user_id, fraud_type) VALUES (3, 'VELOCITY')");

// 2. Metódus hívása (közvetlenül a logolót hívjuk, hogy az AtomicBoolean ne zavarjon)
        fraudReportingHelper.LogFinalStatistics();

        // 3. Logok ellenőrzése
        String out = capturedOutput.getAll();

        assertThat(out).contains("FINAL FRAUD STATISTICS (BY AFFECTED USERS)"); // Pontos fejléc!
        assertThat(out).contains("CLEAN TRANSACTIONS: 1 user(s)");
        assertThat(out).contains("CARD_TESTING: 1 user(s)");
        assertThat(out).contains("VELOCITY: 1 user(s)");
    }
}



