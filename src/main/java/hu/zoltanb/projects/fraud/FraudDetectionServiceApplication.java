package hu.zoltanb.projects.fraud;

import hu.zoltanb.projects.fraud.config.FraudAppConfig;
import hu.zoltanb.projects.fraud.service.RedisAndDbCleaner;
import hu.zoltanb.projects.fraud.transactiongenerator.TransactionGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@EnableKafka
public class FraudDetectionServiceApplication {

    @Autowired
    private FraudAppConfig config;

    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }

    @Bean
    public RedisAndDbCleaner.Sleeper sleeper() {
        return new RedisAndDbCleaner.Sleeper();
    }

    public static void main(String[] args) {
        SpringApplication.run(FraudDetectionServiceApplication.class, args);
    }

    @Bean
    @Profile("!test") // it runs only if it is not a test
    public CommandLineRunner testKafka(TransactionGeneratorService generator, ApplicationContext context) {
        return args -> {
            //waiting 3 sec to Kafka Consumer be ready
            long delay = config.getGenerator().getInitialDelay();
            int count = config.getGenerator().getTransactionCount();
            Thread.sleep(delay);
            //Number of trades
            generator.generateData(count);

            System.out.println("All transactions are sent to Kafka!");
            System.out.println("Waiting 15 seconds for Data...");
            Thread.sleep(15000);
            System.out.println("Shutting down...");

            SpringApplication.exit(context, () -> 0);
            System.exit(0);
        };
    }


}
