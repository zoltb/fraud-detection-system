package hu.zoltanb.projects.fraud;

import hu.zoltanb.projects.fraud.transactiongenerator.TransactionGeneratorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class FraudDetectionServiceApplication {

    @Value("${fraud.generator.transaction-count}")
    private int transactionCount;

    @Value("${fraud.generator.initial-delay-ms}")
    private long initialDelay;

    public static void main(String[] args) {
        SpringApplication.run(FraudDetectionServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner testKafka(TransactionGeneratorService generator) {
        return args -> {
            //waiting 3 sec to Kafka Consumer be ready
            System.out.println("waiting for Kafka start");
            Thread.sleep(initialDelay);
            System.out.println("Starting mass data generation...");
            //Number of trades
            generator.generateData(transactionCount);

            System.out.println("All transactions are sent to Kafka!");

            System.out.println("Test trade is done");
        };
    }


}
