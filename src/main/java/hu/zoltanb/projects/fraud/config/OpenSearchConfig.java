package hu.zoltanb.projects.fraud.config;

import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import java.net.URI;

@Configuration
@org.springframework.context.annotation.Profile("!test")
public class OpenSearchConfig {
    @Value("${app.opensearch.uris:http://localhost:9200}")
    private String osUri;

    @Bean
    public OpenSearchClient openSearchClient() {
        RestClient restClient = RestClient.builder(org.apache.http.HttpHost.create(osUri)).build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new OpenSearchClient(transport);
    }
}
