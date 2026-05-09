package hu.zoltanb.projects.fraud.transactiongenerator;

import hu.zoltanb.projects.fraud.config.FraudAppConfig;
import hu.zoltanb.projects.fraud.model.Transaction;
import hu.zoltanb.projects.fraud.service.TransactionProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TransactionGeneratorServiceTest {

    @Mock
    private TransactionProducer producer;
    @Mock
    private RestClient.Builder restClientBuilder;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) //goes forward in the "call chain" if there is null value
    private RestClient restClient;
    @Mock
    private FraudAppConfig config;

    private TransactionGeneratorService service;

    @BeforeEach
    void setUp() {
        // It gives back itself in case of call of baseUrl()
        when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        // At the end gives back the mock
        when(restClientBuilder.build()).thenReturn(restClient);

        var mockGen = mock(FraudAppConfig.Generator.class);
        when(config.getGenerator()).thenReturn(mockGen);
        when(mockGen.getSleepMs()).thenReturn(0);
        when(mockGen.getApiUrl()).thenReturn("http://localhost");
        // the created mockbuilder will run
        service = new TransactionGeneratorService(producer, restClientBuilder, config);
    }

    @Nested
    @DisplayName("Successful process")
    class PositiveFlows {

        @Test
        @DisplayName("Testing data generation and incrementation")
        void shouldProcessExactCount() throws InterruptedException {
            // GIVEN successful receiving data
            Transaction mockApiTx = Transaction.builder().amount(BigDecimal.valueOf(10.0)).build();
            when(restClient.get().uri(anyString()).retrieve().body(Transaction.class)).thenReturn(mockApiTx);

            // WHEN
            service.generateData(5);

            // THEN
            verify(producer, times(5)).sendTestTransaction(any());
        }

        @Test
        @DisplayName("ID incrementation and date check")
        void shouldEnrichDataCorrectly() throws InterruptedException {
            // GIVEN
            Transaction mockApiTx = Transaction.builder().amount(BigDecimal.valueOf(10.0)).build();
            when(restClient.get().uri(anyString()).retrieve().body(Transaction.class)).thenReturn(mockApiTx);
            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

            // WHEN
            service.generateData(2);

            // THEN
            verify(producer, times(2)).sendTestTransaction(captor.capture());
            List<Transaction> sentTxs = captor.getAllValues();

            assertThat(sentTxs.get(0).getTransactionId()).isEqualTo(1L);
            assertThat(sentTxs.get(1).getTransactionId()).isEqualTo(2L);
            assertThat(sentTxs.get(0).getCreatedAt()).isNotNull();
        }

        @Nested
        @DisplayName("Error and edge cases")
        class ErrorAndEdgeCases {

            @Test
            @DisplayName("If API returns null the loop will continue but won't send data")
            void shouldHandleNullApiResponse() throws InterruptedException {
                // GIVEN
                when(restClient.get().uri(anyString()).retrieve().body(Transaction.class)).thenReturn(null);

                // WHEN
                service.generateData(1);

                // THEN
                verify(producer, never()).sendTestTransaction(any());
            }

            @Test
            @DisplayName("Exception doesn't ruin the loop")
            void shouldContinueAfterException() throws InterruptedException {
                // GIVEN
                // 1st get will fail and 2nd will be ok
                when(restClient.get().uri(anyString()).retrieve().body(Transaction.class))
                        .thenThrow(new RuntimeException("API error"))
                        .thenReturn(Transaction.builder().amount(BigDecimal.valueOf(50.0)).build());

                // WHEN
                service.generateData(2);

                // THEN
                // Producer will be noticed only in case of successful loop.
                verify(producer, times(1)).sendTestTransaction(any());
            }

            @Test
            @DisplayName("If count = 0 the code won't do anything")
            void shouldDoNothingOnZeroCount() throws InterruptedException {
                // WHEN
                service.generateData(0);

                // THEN
                verifyNoInteractions(restClient);
                verifyNoInteractions(producer);
            }
        }
    }


}
