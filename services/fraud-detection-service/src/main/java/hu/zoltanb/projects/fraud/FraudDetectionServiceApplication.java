package hu.zoltanb.projects.fraud;

import hu.zoltanb.projects.fraud.service.TransactionProducer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;

import java.sql.SQLOutput;

@SpringBootApplication
@EnableKafka
public class FraudDetectionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudDetectionServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner testKafka(TransactionProducer producer) {
        return args -> {
            //waiting 3 sec to Kafka Consumer be ready
            System.out.println("waiting for Kafka start");
            Thread.sleep(3000);

            producer.sendTestTransaction();

            System.out.println("Test trade is done");
        };
    }

}
