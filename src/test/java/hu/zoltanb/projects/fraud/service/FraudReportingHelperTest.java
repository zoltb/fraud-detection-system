package hu.zoltanb.projects.fraud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

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
        public FraudReportingHelper fraudReportingHelper(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
            return new FraudReportingHelper(jdbcTemplate, objectMapper);
        }
    }

    @Autowired
    private FraudReportingHelper fraudReportingHelper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Check the logs after having transactions")
    void shouldLogCorrectStatistics(CapturedOutput capturedOutput) {
        // GIVEN Generate test data in H2
        jdbcTemplate.execute("INSERT INTO transactions (user_id, fraud_type) VALUES (1, NULL)");
        jdbcTemplate.execute("INSERT INTO transactions (user_id, fraud_type) VALUES (2, 'CARD_TESTING')");
        jdbcTemplate.execute("INSERT INTO transactions (user_id, fraud_type) VALUES (3, 'VELOCITY')");

        // WHEN
        fraudReportingHelper.LogFinalStatistics();

        // THEN
        String out = capturedOutput.getAll();

        assertThat(out).contains("FINAL FRAUD STATISTICS (BY AFFECTED USERS)"); // Pontos fejléc!
        assertThat(out).contains("CLEAN TRANSACTIONS: 1 user(s)");
        assertThat(out).contains("CARD_TESTING: 1 user(s)");
        assertThat(out).contains("VELOCITY: 1 user(s)");
    }
}



