package hu.zoltanb.projects.fraud.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.partitions}")
    private int partitions;

    @Value("${app.kafka.topic}")
    private String topicName;

    @Bean
    public NewTopic transactionsTopic() {
        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(1)
                .build();
    }
}