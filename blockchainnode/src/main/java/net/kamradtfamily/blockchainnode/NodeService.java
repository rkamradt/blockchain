package net.kamradtfamily.blockchainnode;

import lombok.extern.slf4j.Slf4j;
import net.kamradtfamily.blockchain.api.Block;
import net.kamradtfamily.blockchain.api.Blockchain;
import net.kamradtfamily.blockchain.api.Message;
import net.kamradtfamily.blockchain.api.Transaction;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
@Slf4j
public class NodeService {
    private static Node myself;
    final private String host;
    final private int port;
    final private NodeRepository peers;
    final private Blockchain blockchain;
    final private ReactiveKafkaConsumerTemplate<String, Message> emitter;
    final private WebClient client = WebClient.create();

    public NodeService(@Value("${server.host}") String host,
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
        connectToPeers(peers.findAll())
                .last()
                .subscribe(ignore -> emitter
                    .receiveAutoAck()
                    .doOnNext(consumerRecord -> log.info("received key={}, value={} from topic={}, offset={}",
                            consumerRecord.key(),
                            consumerRecord.value(),
                            consumerRecord.topic(),
                            consumerRecord.offset())
                    )
                    .map(ConsumerRecord::value)
                    .flatMap(m -> messageHandler(m))
                    .subscribe(cr ->
                            log.info("send message"),
                            e -> log.error("error sending message", e)));
    }

    public Mono<? extends Object> messageHandler(Message m) {
        if("blockAdded".equals(m.getMessage())) {
            return Mono.just(m.getBlock())
                    .flatMapMany(b ->
                        peers.findAll()
                                .flatMap(p -> sendLatestBlock(p, b)))
                    .last();
        } else if("transactionAdded".equals(m.getMessage())) {
            return Mono.just(m.getTransaction())
                    .flatMapMany(t ->
                            peers.findAll()
                                    .flatMap(p -> sendTransaction(p, t)))
                    .last();
        } else if("getBlocks".equals(m.getMessage())) {
            return peers.findAll()
                    .flatMap(p -> getBlocks(p))
                    .last();
        } else {
            log.error("unknown message {}", m);
            return Mono.empty();
        }
   }

    public Mono<Node> connectToPeer(Node newPeer) {
        return connectToPeers(Flux.just(newPeer))
                .last()
                .switchIfEmpty(Mono.just(myself));
    }

    public Flux<Node> connectToPeers(Flux<Node> newPeers) {
        return newPeers.flatMap((peer) ->
            peers.findByUrl(peer.getUrl())
                    .doOnNext(found -> log.info("peer {} already exists", found))
                    .switchIfEmpty(sendAndInit(peer)));
     }

    public Mono<Node> sendAndInit(Node peer) {
        return sendPeer(peer, myself)
                .map(ignore -> peer)
                .flatMap(sent -> peers.save(sent))
                .map(ignore -> peer)
                .flatMapMany(saved -> initConnection(saved))
                .last()
                .flatMapMany(ignore -> peers.findAll())
                .doOnNext(other -> sendPeer(peer, other))
                .last();
    }

    public Flux<Transaction> initConnection(Node peer) {
        return getLatestBlock(peer)
                .flatMapMany(block -> getTransactions(peer));
    }

    public Mono<Node> sendPeer(Node peer, Node peerToSend) {
        String URL = peer.getUrl() + "/node/peers";
        log.info("Sending {} to peer {}.", peerToSend.getUrl(), URL);
        return client
                .post()
                .uri(URL)
                .body(peerToSend, Node.class)
                .retrieve().bodyToMono(Node.class);

    }

    public Mono<Block> getLatestBlock(Node peer) {
        String URL = peer.getUrl() + "/blocks/latest";
        log.info("Getting latest block from: {}", URL);
        return client
                .get()
                .uri(URL)
                .exchange()
                .flatMap(cr -> cr.bodyToMono(Block.class))
                .flatMap(b -> checkReceivedBlock(b));
    }

    public Mono<Block> getBlocks(Node peer) {
        String URL = peer.getUrl() + "/block/blocks/latest";
        log.info("Getting blocks from: {}", URL);
        return client
                .get()
                .uri(URL)
                .exchange()
                .flatMap(cr -> cr.bodyToMono(Block.class))
                .flatMap(b -> checkReceivedBlock(b));

    }

    public Mono<Block> sendLatestBlock(Node peer, Block block) {
        String URL = peer.getUrl() + "/block/blocks/latest";
        log.info("Posting latest block to: {}", URL);
        return client
                .put()
                .uri(URL)
                .body(block, Block.class)
                .exchange()
                .flatMap(cr -> cr.bodyToMono(Block.class));
    }

    public Mono<Transaction> sendTransaction(Node peer, Transaction transaction) {
        String URL = peer.getUrl() + "/transaction";
        log.info("Sending transaction '{}' to: {}", transaction, URL);
        return client
                .post()
                .uri(URL)
                .body(transaction, Transaction.class)
                .exchange()
                .flatMap(cr -> cr.bodyToMono(Transaction.class));
    }

    public Flux<Transaction> getTransactions(Node peer) {
        String URL = peer.getUrl() + "/transactions";
        log.info("Getting transactions from: {}", URL);
        return client
                .get()
                .uri(URL)
                .exchange()
                .flatMapMany(cr -> cr.bodyToFlux(Transaction.class))
                .flatMap(this::syncTransaction);
    }

    public Mono<Block> getConfirmation(Node peer, long transactionId) {
        String URL = peer.getUrl() + "/block/blocks/transactions/" + transactionId;
        log.info("Getting transactions from: {}", URL);
        return client
                .get()
                .uri(URL)
                .retrieve().bodyToMono(Block.class)
                .onErrorContinue((t, o) -> Mono.empty());
    }

    Mono<Long> getConfirmations(long transactionId) {
        // Get count of peers with confirmations that the transaction exists
        return blockchain.findTransactionInChain(transactionId, blockchain.getAllBlocks())
                .zipWith(peers.findAll()
                        .flatMap(peer -> getConfirmation(peer, transactionId)))
                .count();
    }

    private Mono<Transaction> syncTransaction(Transaction transaction) {
        return blockchain.getTransactionById(transaction.getTransactionId())
                .switchIfEmpty(blockchain.addTransaction(transaction, true));
    }

    private Mono<Block> checkReceivedBlock(Block block) {
        return this.checkReceivedBlocks(Flux.just(block))
                .last();
    }
    private Flux<Block> checkReceivedBlocks(Flux<Block> blocks) {
        return blockchain.getLastBlock()
                .flatMapMany(last -> blocks
                        .filter(block -> block.getHash().equals("0")) // eat the genesis block
                        .sort((b1, b2) -> b1.getIndex() - b2.getIndex() > 0 ? 1 :
                                b1.getIndex() - b2.getIndex() == 0 ? 0 : -1)
                        .takeUntil(br -> br.getIndex() >= last.getIndex()
                                && br.getPreviousHash().equals(last.getHash())))
                .flatMap(b -> blockchain.addBlock(b, true))
                .switchIfEmpty(peers.findAll()
                    .flatMap(p -> getBlocks(p)));
    }

    public Flux<Node> getPeers() {
        return peers.findAll();
    }
}
