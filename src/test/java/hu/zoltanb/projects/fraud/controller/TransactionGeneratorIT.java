package hu.zoltanb.projects.fraud.controller;

import hu.zoltanb.projects.fraud.config.FraudAppConfig;
import hu.zoltanb.projects.fraud.model.Transaction;
import hu.zoltanb.projects.fraud.transactiongenerator.TransactionGeneratorService;
import hu.zoltanb.projects.fraud.service.TransactionProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/*@ActiveProfiles("test")
@RestClientTest(value = TransactionGeneratorService.class)
*/
@ActiveProfiles("test")
@TestPropertySource(properties = "app.scheduling.enabled=true")
@RestClientTest(TransactionGeneratorService.class)
class TransactionGeneratorIT {
    @Autowired
    private TransactionGeneratorService generatorService;

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionProducer producer; // Mock it to avoid calling the real Kafka

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    private FraudAppConfig config;

    private FraudAppConfig.Generator generator = new FraudAppConfig.Generator();

    @BeforeEach
    void setUp() {
        when(config.getGenerator()).thenReturn(generator);

        mockServer.reset();
    }

    @Test
    @DisplayName("Check getting data and the enrichment of the data")
    void shouldFetchFromApiAndEnrichData() throws Exception {

        // GIVEN - Starting data
        final Long EXPECTED_USER_ID = 1L;
        final Long EXPECTED_MERCHANT_ID = 5L;
        final BigDecimal EXPECTED_AMOUNT = new BigDecimal("150.00");
        final int TRANSACTION_COUNT = 1;
        final String MOCK_URL = "http://localhost:8080/api/mock/transactions";

        Transaction mockApiTx = Transaction.builder()
                .userId(EXPECTED_USER_ID)
                .merchantId(EXPECTED_MERCHANT_ID)
                .amount(new BigDecimal(String.valueOf(EXPECTED_AMOUNT)))
                .build();

        String jsonResponse = objectMapper.writeValueAsString(mockApiTx);

        mockServer.expect(requestTo(MOCK_URL))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockApiTx), MediaType.APPLICATION_JSON));

        // WHEN - Calling the generator once
        generatorService.generateData(TRANSACTION_COUNT);

        // THEN - Get the data and capture it from the Service
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(producer).sendTestTransaction(captor.capture());

        Transaction finalTx = captor.getValue();

        // Data check
        assertThat(finalTx.getUserId()).isEqualTo(EXPECTED_USER_ID);
        assertThat(finalTx.getTransactionId()).isNotNull(); // Az AtomicLong-ból jön
        assertThat(finalTx.getCreatedAt()).isNotNull();    // A LocalDateTime.now()-ból jön
        assertThat(finalTx.getAmount()).isEqualByComparingTo(EXPECTED_AMOUNT);

        // check every call related to the mock
        mockServer.verify();
    }
}
