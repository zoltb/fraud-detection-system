package hu.zoltanb.projects.fraud.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.event.ListenerContainerIdleEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class FraudReportingHelper {
    private final JdbcTemplate jdbcTemplate;
    ObjectMapper objectMapper = new ObjectMapper();

    public FraudReportingHelper(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    private final AtomicBoolean reportGenerated = new AtomicBoolean(false);

    // Value from 19th line is 'false', after event it will be 'true'
    @EventListener
    public void handleIdleEvent(ListenerContainerIdleEvent event) {
        if (reportGenerated.compareAndSet(false, true)) {
            log.info("No messages received for 10 seconds. Generating summary...");
            LogFinalStatistics();
        }
    }

    // When every time we stop the whole process to hae result -> @PreDestroy
    public void LogFinalStatistics() {
        // Egyszerű lekérdezés: lekérjük a felhasználót és a hozzá tartozó JSONB listát
        String sql = """
           SELECT
               CASE
                   WHEN fraud_labels IS NULL OR fraud_labels = '' THEN 'CLEAN TRANSACTIONS'
                   ELSE '[' || fraud_labels || ']'
               END as fraud_combination,
               COUNT(DISTINCT user_id) as total_users
           FROM (
               SELECT
                    user_id,
                    array_to_string(array_agg(DISTINCT fraud_item ORDER BY fraud_item), ', ') as fraud_labels
               FROM transactions
               LEFT JOIN LATERAL unnest(fraud_types) as fraud_item ON true
               GROUP BY user_id
           ) user_summary
           GROUP BY fraud_combination
           ORDER BY total_users DESC
        """;

        log.info("------------------------------------------------");
        log.info("AGGREGATED USER FRAUD REPORT");
        log.info("------------------------------------------------");
// Mivel az SQL már teljesen kész statisztikát ad, az ObjectMapperre sincs szükség itt!
        jdbcTemplate.query(sql, (rs, rowNum) -> {
            String combination = rs.getString("fraud_combination");
            int users = rs.getInt("total_users");

            log.info("{}: {} user(s)", combination, users);
            return null;
        });

        log.info("------------------------------------------------");
    }
    }