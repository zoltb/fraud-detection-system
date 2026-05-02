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
    public void handleIdleEvent(ListenerContainerIdleEvent event){
        if (reportGenerated.compareAndSet(false, true)) {
        log.info("No messages received for 10 seconds. Generating summary...");
        LogFinalStatsics();
    }
    }

    // When every time we stop the whole process to hae result -> @PreDestroy
    public void LogFinalStatsics() {
        String sql = """
                SELECT fraud_type, COUNT(DISTINCT user_id) as affected_users
            FROM transactions
            GROUP BY fraud_type
            """;

        log.info("--------------------------------------------");
        log.info("FINAL FRAUD STATISTICS (BY AFFECTED USERS)");
        log.info("--------------------------------------------");

        jdbcTemplate.query(sql, (rs) -> {
            String type = rs.getString("fraud_type");
            int users = rs.getInt("affected_users");

            // Ha a fraud_type null, akkor az egy tiszta (Clean) tranzakció volt
            String label = (type == null) ? "CLEAN TRANSACTIONS" : type;
            log.info("{}: {} user(s)", label, users);
        });
        log.info("-----------------------------------");
        }
}
