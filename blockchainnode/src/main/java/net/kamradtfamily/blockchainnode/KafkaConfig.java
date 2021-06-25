package net.kamradtfamily.blockchainnode;

import net.kamradtfamily.blockchain.api.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.SenderOptions;

import java.util.Collections;
import java.util.Map;

@Configuration
public class KafkaConfig {
    public static final String TOPIC = "blockchain";
    @Bean
    public ReactiveKafkaProducerTemplate<String, Message> reactiveKafkaProducerTemplate(
            KafkaProperties properties) {
        Map<String, Object> props = properties.buildProducerProperties();
        return new ReactiveKafkaProducerTemplate(SenderOptions.create(props));
    }

    @Bean
    public ReceiverOptions<String, Message> kafkaReceiverOptions(KafkaProperties kafkaProperties) {
        ReceiverOptions<String, Message> basicReceiverOptions = ReceiverOptions.create(kafkaProperties.buildConsumerProperties());
        return basicReceiverOptions.subscription(Collections.singletonList(TOPIC));
    }

    @Bean
    public ReactiveKafkaConsumerTemplate<String, Message> reactiveKafkaConsumerTemplate(ReceiverOptions<String, Message> kafkaReceiverOptions) {
        return new ReactiveKafkaConsumerTemplate<String, Message>(kafkaReceiverOptions);
    }
}
