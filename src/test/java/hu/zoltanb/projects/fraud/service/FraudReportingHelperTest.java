package hu.zoltanb.projects.fraud.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.kafka.event.ListenerContainerIdleEvent;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudReportingHelperTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private FraudReportingHelper fraudReportingHelper;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setup() {
        listAppender = new ListAppender<>();
        listAppender.start();

        // For "REPORT" logger
        Logger reportLogger = (Logger) LoggerFactory.getLogger("REPORT");
        reportLogger.addAppender(listAppender);

        // FraudReportingHelper class related logger for messages
        Logger classLogger = (Logger) LoggerFactory.getLogger(FraudReportingHelper.class);
        classLogger.addAppender(listAppender);
    }

    @Test
    @DisplayName("Checks the header logs and SQL execution")
    void logFinalStatistics_LogsHeadersAndExecutesQuery() {
        // WHEN
        fraudReportingHelper.LogFinalStatistics();

        // THEN check the run of the query
        verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class));

        // Ellenőrizzük a fixen kiírt fejléceket a konzol kimeneten
        assertThat(listAppender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .contains("AGGREGATED USER FRAUD REPORT");
    }

    @Test
    @DisplayName("Checks the ResultSet is OK or not")
    void logFinalStatistics_ProcessesResultSetCorrectly() throws SQLException {
        // GIVEN Simulation of the returning rows (ResultSet)
        ResultSet mockResultSet = mock(ResultSet.class);
        lenient().when(mockResultSet.getString("fraud_combination")).thenReturn("CARD_TESTING, VELOCITY");
        lenient().when(mockResultSet.getInt("total_tx")).thenReturn(5);

        // jdbcTemplate.queryforced to run FraudReportingHelper lambda (RowMapper) logic
        given(jdbcTemplate.query(anyString(), any(RowMapper.class))).willAnswer(invocation -> {
            RowMapper<?> rowMapper = invocation.getArgument(1);
            // RowMapper runs on mocked ResultSet
            rowMapper.mapRow(mockResultSet, 1);
            return null;
        });

        // WHEN
        fraudReportingHelper.LogFinalStatistics();

        // THEN Checking logs
        assertThat(listAppender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .contains("CARD_TESTING, VELOCITY: 5 transactions");
    }

    @Test
    @DisplayName("Kafka Idle Event test: starting report generation")
    void handleIdleEvent_TriggersStatisticsGeneration() throws SQLException {
        // GIVEN - Simulate empty Kafka
        ListenerContainerIdleEvent mockEvent = mock(ListenerContainerIdleEvent.class);
        ResultSet mockResultSet = mock(ResultSet.class);
        given(mockResultSet.getString("fraud_combination")).willReturn("CARD_TESTING");
        given(mockResultSet.getInt("total_tx")).willReturn(50);

        given(jdbcTemplate.query(anyString(), any(RowMapper.class))).willAnswer(invocation -> {
            RowMapper<?> rowMapper = invocation.getArgument(1);
            rowMapper.mapRow(mockResultSet, 1);
            return null;
        });
        // WHEN - Send it to the method in Helper
        fraudReportingHelper.handleIdleEvent(mockEvent);

        // THEN - Checking
        assertThat(listAppender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .contains("No messages received for 10 seconds. Generating summary...");
        verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class));
    }
}


