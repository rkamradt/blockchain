package net.kamradtfamily.blockchainnode;

import net.kamradtfamily.blockchain.api.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import reactor.kafka.receiver.ReceiverOptions;

import java.util.Collections;

@Configuration
public class KafkaConsumerConfig {
    @Bean
    public ReceiverOptions<String, Message> kafkaReceiverOptions(@Value(value = "${FAKE_CONSUMER_DTO_TOPIC}") String topic, KafkaProperties kafkaProperties) {
        ReceiverOptions<String, Message> basicReceiverOptions = ReceiverOptions.create(kafkaProperties.buildConsumerProperties());
        return basicReceiverOptions.subscription(Collections.singletonList(topic));
    }

    @Bean
    public ReactiveKafkaConsumerTemplate<String, Message> reactiveKafkaConsumerTemplate(ReceiverOptions<String, Message> kafkaReceiverOptions) {
        return new ReactiveKafkaConsumerTemplate<String, Message>(kafkaReceiverOptions);
    }
}
