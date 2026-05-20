package hu.zoltanb.projects.fraud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.kafka.event.ListenerContainerIdleEvent;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class) // Tiszta és gyors egységteszt Spring kontextus nélkül
@ExtendWith(OutputCaptureExtension.class) // Biztosítja a logok elkapását (CapturedOutput)
class FraudReportingHelperTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private FraudReportingHelper fraudReportingHelper;

    @Test
    @DisplayName("Checks the header logs and SQL execution")
    void logFinalStatistics_LogsHeadersAndExecutesQuery(CapturedOutput output) {
        // WHEN
        fraudReportingHelper.LogFinalStatistics();

        // THEN check the run of the query
        verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class));

        // Ellenőrizzük a fixen kiírt fejléceket a konzol kimeneten
        assertThat(output.getOut()).contains("AGGREGATED USER FRAUD REPORT");
        assertThat(output.getOut()).contains("------------------------------------------------");
    }

    @Test
    @DisplayName("Checks the ResultSet is OK or not")
    void logFinalStatistics_ProcessesResultSetCorrectly(CapturedOutput output) throws SQLException {
        // GIVEN Simulation of the returning rows (ResultSet)
        ResultSet mockResultSet = mock(ResultSet.class);
        given(mockResultSet.getString("fraud_combination")).willReturn("CARD_TESTING, VELOCITY");
        given(mockResultSet.getInt("total_users")).willReturn(5);

        // jdbcTemplate.queryforced to run FraudReportingHelper lambda (RowMapper) logic
        given(jdbcTemplate.query(anyString(), any(RowMapper.class))).willAnswer(invocation -> {
            RowMapper<?> rowMapper = invocation.getArgument(1);
            // Lefuttatjuk a helper belső RowMapper-ét a mockolt ResultSet-ünkön
            rowMapper.mapRow(mockResultSet, 1);
            return null;
        });

        // WHEN
        fraudReportingHelper.LogFinalStatistics();

        // THEN Checking logs
        assertThat(output.getOut()).contains("CARD_TESTING, VELOCITY: 5 user(s)");
    }

    @Test
    @DisplayName("Kafka Idle Event test: starting report generation")
    void handleIdleEvent_TriggersStatisticsGeneration(CapturedOutput output) {
        // GIVEN - Simulate empty Kafka
        ListenerContainerIdleEvent mockEvent = mock(ListenerContainerIdleEvent.class);

        // WHEN - Send it to the method in Helper
        fraudReportingHelper.handleIdleEvent(mockEvent);

        // THEN - Checking
        assertThat(output.getOut()).contains("No messages received for 10 seconds. Generating summary...");
        verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class));
    }
}


