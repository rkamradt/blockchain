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
    final private NodeRepository peers;
    final private Blockchain blockchain;
    final private ReactiveKafkaConsumerTemplate<String, Message> emitter;
    final private WebClient client = WebClient.create();

    public NodeService(@Value("${server.myself}") String myselfUrl,
                       Blockchain blockchain,
                       NodeRepository peers,
                       ReactiveKafkaConsumerTemplate<String, Message> emitter) {
        this.blockchain = blockchain;
        this.emitter = emitter;
        this.peers = peers;
        myself = Node.builder()
                .url(myselfUrl)
                .build();
        connectToPeers(peers.findAll());
        emitter.receiveAutoAck()
                    .doOnNext(consumerRecord -> log.info("received key={}, value={} from topic={}, offset={}",
                            consumerRecord.key(),
                            consumerRecord.value(),
                            consumerRecord.topic(),
                            consumerRecord.offset())
                    )
                    .map(ConsumerRecord::value)
                    .flatMap(m -> messageHandler(m))
                    .subscribe(cr ->
                            log.info("message handled"),
                            e -> log.error("error handling message", e));
    }

    public Mono<? extends Object> messageHandler(Message m) {
        if("addedBlock".equals(m.getMessage())) {
            return peers.findAll()
                    .flatMap(p -> sendLatestBlock(p, m.getBlock()))
                    .switchIfEmpty(Mono.just(m.getBlock()))
                    .last();
        } else if("addedTransaction".equals(m.getMessage())) {
            return peers.findAll()
                    .flatMap(p -> sendTransaction(p, m.getTransaction()))
                    .switchIfEmpty(Mono.just(m.getTransaction()))
                    .last();
        } else if("getBlocks".equals(m.getMessage())) {
            return peers.findAll()
                    .flatMap(p -> getBlocks(p))
                    .switchIfEmpty(blockchain.getLastBlock())
                    .last();
        } else {
            log.error("unknown message {}", m);
            return Mono.empty();
        }
   }

    public Flux<Node> connectToPeers(Flux<Node> newPeers) {
        return newPeers
                .doOnNext(peer -> log.info("connecting to peer {} from {}", peer, myself))
                .filter(peer -> !peer.getUrl().equals(myself.getUrl()))
                .flatMap(peer ->
                    peers.findByUrl(peer.getUrl())
                         .doOnNext(found -> log.info("peer {} already exists", found))
                         .switchIfEmpty(sendAndInit(peer)));
     }

    public Mono<Node> sendAndInit(Node peer) {
        return peers.save(peer)
                .doOnNext(n -> log.info("peer {} saved", n))
                .flatMap(saved -> sendPeer(saved, myself))
                .doOnNext(p -> log.info("connected to peer {}", p))
                .flatMap(saved -> initConnection(saved))
                .flatMapMany(ignore -> peers.findAll())
                .filter(other -> !other.getUrl().equals(peer.getUrl()))
                .doOnNext(other -> log.info("sending {} to {}", other, peer))
                .doOnNext(other -> sendPeer(peer, other))
                .switchIfEmpty(Mono.just(peer))
                .last();
    }

    public Mono<Node> initConnection(Node peer) {
        return getLatestBlock(peer)
                .doOnNext(p -> log.info("getting transactions from peer {}", p))
                .flatMapMany(block -> getTransactions(peer))
                .map(ignore -> peer)
                .switchIfEmpty(Mono.just(peer))
                .last();
    }

    public Mono<Node> sendPeer(Node peer, Node peerToSend) {
        if(peer.getUrl().equals(myself.getUrl())) {
            log.warn("avoiding post node request to self");
            return Mono.just(peer);
        }
        String URL = peer.getUrl() + "/node/peers";
        log.info("Sending {} to peer {}.", peerToSend, URL);
        return client
                .post()
                .uri(URL)
                .body(Mono.just(peerToSend), Node.class)
                .exchange()
                .doOnNext(cr -> log.info("response from peer {}", cr.rawStatusCode()))
                .flatMap(cr -> cr.bodyToMono(Node.class))
                .doOnNext(node -> log.info("sent {} to peer {}", node, URL))
                .map(self -> peer);

    }

    public Mono<Block> getLatestBlock(Node peer) {
        if(peer.getUrl().equals(myself.getUrl())) {
            log.warn("avoiding get latest block request to self");
            return Mono.empty();
        }
        String URL = peer.getUrl() + "/block/last";
        log.info("Getting latest block from: {}", URL);
        return client
                .get()
                .uri(URL)
                .exchange()
                .flatMap(cr -> cr.bodyToMono(Block.class))
                .flatMap(b -> checkReceivedBlock(b, peer));
    }

    public Mono<Block> getBlocks(Node peer) {
        if(peer.getUrl().equals(myself.getUrl())) {
            log.warn("avoiding get blocks request to self");
            return Mono.empty();
        }
        String URL = peer.getUrl() + "/block/last";
        log.info("Getting blocks from: {}", URL);
        return client
                .get()
                .uri(URL)
                .exchange()
                .flatMap(cr -> cr.bodyToMono(Block.class))
                .flatMap(b -> checkReceivedBlock(b, peer));

    }

    public Mono<Block> sendLatestBlock(Node peer, Block block) {
        if(peer.getUrl().equals(myself.getUrl())) {
            log.warn("avoiding put block request to self");
            return Mono.empty();
        }
        String URL = peer.getUrl() + "/block/last";
        log.info("Posting latest block to: {}", URL);
        return client
                .put()
                .uri(URL)
                .body(Mono.just(block), Block.class)
                .exchange()
                .flatMap(cr -> cr.bodyToMono(Block.class));
    }

    public Mono<Transaction> sendTransaction(Node peer, Transaction transaction) {
        if(peer.getUrl().equals(myself.getUrl())) {
            log.warn("avoiding post transaction request to self");
            return Mono.just(transaction);
        }
        String URL = peer.getUrl() + "/transaction/" + transaction.getId();
        log.info("Sending transaction '{}' to: {}", transaction, URL);
        return client
                .post()
                .uri(URL)
                .body(Mono.just(transaction), Transaction.class)
                .exchange()
                .flatMap(cr -> cr.bodyToMono(Transaction.class));
    }

    public Flux<Transaction> getTransactions(Node peer) {
        if(peer.getUrl().equals(myself.getUrl())) {
            log.warn("avoiding get transaction request to self");
            return Flux.empty();
        }
        String URL = peer.getUrl() + "/transaction";
        log.info("Getting transactions from: {}", URL);
        return client
                .get()
                .uri(URL)
                .exchange()
                .flatMapMany(cr -> cr.bodyToFlux(Transaction.class))
                .flatMap(this::syncTransaction);
    }

    public Mono<Block> getConfirmation(Node peer, long transactionId) {
        if(peer.getUrl().equals(myself.getUrl())) {
            log.warn("avoiding confirmation request to self");
            return blockchain.getLastBlock();
        }
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

    private Mono<Block> checkReceivedBlock(Block block, Node peer) {
        return this.checkReceivedBlocks(Flux.just(block), peer)
                .last();
    }
    private Flux<Block> checkReceivedBlocks(Flux<Block> blocks, Node peer) {
        return blockchain.getLastBlock()
                .flatMapMany(last -> blocks
                        .doOnNext(block -> log.info("checking last block {} against {}", last, block))
                        .filter(block -> block.getPreviousHash().equals("0")) // eat the genesis block
                        .sort((b1, b2) -> b1.getIndex() - b2.getIndex() > 0 ? 1 :
                                b1.getIndex() - b2.getIndex() == 0 ? 0 : -1)
                        .filter(block -> block.getIndex() > last.getIndex()
                                && block.getPreviousHash().equals(last.getHash())))
                .doOnNext(b -> log.info("adding block {} from peer {}", b, peer))
                .flatMap(b -> blockchain.addBlock(b, true))
                .switchIfEmpty(peers.findAll()
                        .filter(p -> !p.getUrl().equals(peer.getUrl()))
                        .flatMap(p -> getBlocks(p)))
                .switchIfEmpty(blockchain.getLastBlock());
    }

    public Flux<Node> getPeers() {
        return peers.findAll();
    }
}
