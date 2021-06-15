package net.kamradtfamily.blockchain;

import net.kamradtfamily.blockchain.api.Message;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.sender.SenderOptions;

import java.util.Map;

@Configuration
public class KafkaProducerConfig {
    @Bean
    public ReactiveKafkaProducerTemplate<String, Message> reactiveKafkaProducerTemplate(
            KafkaProperties properties) {
        Map<String, Object> props = properties.buildProducerProperties();
        return new ReactiveKafkaProducerTemplate(SenderOptions.create(props));
    }
}
