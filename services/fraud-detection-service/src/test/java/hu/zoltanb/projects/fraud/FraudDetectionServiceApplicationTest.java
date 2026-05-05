package hu.zoltanb.projects.fraud;

import hu.zoltanb.projects.fraud.service.TransactionConsumer;
import hu.zoltanb.projects.fraud.service.TransactionProducer;
import hu.zoltanb.projects.fraud.transactiongenerator.TransactionGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FraudDetectionServiceApplicationTest {

    @MockBean
    private TransactionProducer transactionProducer;
    @MockBean
    private TransactionConsumer transactionConsumer;
    @MockBean
    private TransactionGeneratorService transactionGeneratorService;

    //Smoke test -> checking only the Spring context
    @Test
    void contextLoads() {
    }

}
