package hu.zoltanb.projects.fraud.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.event.ListenerContainerIdleEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudReportingHelper {
    private final JdbcTemplate jdbcTemplate;


    private final AtomicBoolean reportGenerated = new AtomicBoolean(false);

    // Value from 21th line is 'false', after event it will be 'true'
    @EventListener
    public void handleIdleEvent(ListenerContainerIdleEvent event) {
        if (reportGenerated.compareAndSet(false, true)) {
            log.info("No messages received for 10 seconds. Generating summary...");
            LogFinalStatsics();
        }
    }

    // When every time we stop the whole process to hae result -> @PreDestroy
    public void LogFinalStatsics() {
        String sql = """
                    SELECT
                    CASE WHEN max_fraud IS NULL THEN 'CLEAN TRANSACTIONS' ELSE max_fraud END as label,
                    COUNT(*) as users
                FROM (
                    SELECT user_id, MAX(fraud_type) as max_fraud
                    FROM transactions
                    GROUP BY user_id
                ) user_summary
                GROUP BY label;
                """;

        log.info("--------------------------------------------");
        log.info("FINAL FRAUD STATISTICS (BY AFFECTED USERS)");
        log.info("--------------------------------------------");

        jdbcTemplate.query(sql, (rs) -> {
            String type = rs.getString("label");
            int users = rs.getInt("users");

            // Ha a fraud_type null, akkor az egy tiszta (Clean) tranzakció volt
            String label = (type == null) ? "CLEAN TRANSACTIONS" : type;
            log.info("{}: {} user(s)", label, users);
        });
        log.info("-----------------------------------");
    }
}
