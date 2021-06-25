package net.kamradtfamily.blockchainnode;

import lombok.extern.slf4j.Slf4j;
import net.kamradtfamily.blockchain.api.Block;
import net.kamradtfamily.blockchain.api.Blockchain;
import net.kamradtfamily.blockchain.api.Message;
import net.kamradtfamily.blockchain.api.Transaction;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

//@Component
@Slf4j
public class SimpleNodeService {
    private static Node myself;
    final private String host;
    final private int port;
    final private NodeRepository peers;
    final private Blockchain blockchain;
    final private ReactiveKafkaConsumerTemplate<String, Message> emitter;

    public SimpleNodeService(@Value("${server.host}") String host,
                             @Value("${server.port}") String port,
                             Blockchain blockchain,
                             NodeRepository peers,
                             ReactiveKafkaConsumerTemplate<String, Message> emitter) {
        this.host = host;
        this.port = Integer.parseInt(port);
        this.blockchain = blockchain;
        this.emitter = emitter;
        this.peers = peers;
        myself = Node.builder()
                .url("http://" + host + ":" + port)
                .build();
        emitter
                .receiveAutoAck()
                .doOnNext(consumerRecord -> log.info("received key={}, value={} from topic={}, offset={}",
                        consumerRecord.key(),
                        consumerRecord.value(),
                        consumerRecord.topic(),
                        consumerRecord.offset())
                )
                .map(ConsumerRecord::value)
                .subscribe(m -> log.info("received message {}", m), e -> log.error("error receiving Message", e));
    }

    public Flux<Node> getPeers() {
        return peers.findAll();
    }

    public Mono<Node> connectToPeer(Node node) {
        return peers.save(node);
    }
}
