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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Component
@Slf4j
public class Node {
    final private String host;
    final private int port;
    final private List<Node> peers = new ArrayList<>();
    final private Blockchain blockchain;
    final private ReactiveKafkaConsumerTemplate<String, Message> emitter;
    final private String url;
    final private WebClient client = WebClient.create();

    public Node(@Value("${server.host}") String host,
                @Value("${server.port}") String port,
                Blockchain blockchain,
                ReactiveKafkaConsumerTemplate<String, Message> emitter) {
        this.host = host;
        this.port = Integer.parseInt(port);
        this.blockchain = blockchain;
        this.emitter = emitter;
        this.url = "http://" + host + ":" + port;
        connectToPeers(peers);
        emitter
                .receiveAutoAck()
                // .delayElements(Duration.ofSeconds(2L)) // BACKPRESSURE
                .doOnNext(consumerRecord -> log.info("received key={}, value={} from topic={}, offset={}",
                        consumerRecord.key(),
                        consumerRecord.value(),
                        consumerRecord.topic(),
                        consumerRecord.offset())
                )
                .map(ConsumerRecord::value)
                .subscribe(m -> messageHandler(m), e -> log.error("error receiving Message"));
    }

    public void messageHandler(Message m) {
        if("blockAdded".equals(m.getMessage())) {
            Flux.just(m.getBlock())
                    .flatMap(b ->
                        Flux.just(peers.toArray(new Node [0]))
                                .flatMap(p -> sendLatestBlock(p, b)));
        }
        if("transactionAdded".equals(m.getMessage())) {
            Flux.just(m.getTransaction())
                    .flatMap(t ->
                            Flux.just(peers.toArray(new Node [0]))
                                    .flatMap(p -> sendTransaction(p, t)));
        }
   }

    public Mono<Node> connectToPeer(Node newPeer) {
        return connectToPeers(Arrays.asList(newPeer))
                .last();
    }

    public Flux<Node> connectToPeers(List<Node> newPeers) {
        newPeers.forEach((peer) -> {
            if (peers.stream().noneMatch(other -> other.url == peer.url) && peer.url != this.url) {
                this.sendPeer(peer, this );
                log.info("Peer {} added to connections.", peer.url);
                this.peers.add(peer);
                this.initConnection(peer);
//                this.broadcast(this::sendPeer, peer);
            } else {
                log.info("Peer {} not added to connections, because I already have it.", peer.url);
            }
        });
        return Flux.just(newPeers.toArray(new Node[0]));

    }

    public void initConnection(Node peer) {
        getLatestBlock(peer);
        getTransactions(peer);
    }

    public Mono<ClientResponse> sendPeer(Node peer, Node peerToSend) {
        String URL = peer.url + "/node/peers";
        log.info("Sending {} to peer {}.", peerToSend.url, URL);
        return client
                .post()
                .uri(URL)
                .body(peerToSend, Node.class)
                .exchange();
    }

    public Mono<Block> getLatestBlock(Node peer) {
        String URL = peer.url + "/blocks/latest";
        log.info("Getting latest block from: {}", URL);
        return client
                .get()
                .uri(URL)
                .exchange()
                .flatMap(cr -> cr.bodyToMono(Block.class));
//                .flatMap(b -> checkReceivedBlock(b));
    }

    public Mono<ClientResponse> sendLatestBlock(Node peer, Block block) {
        String URL = peer.url + "/blocks/latest";
        log.info("Posting latest block to: {}", URL);
        return client
                .put()
                .uri(URL)
                .body(block, Block.class)
                .exchange();
    }

    public Flux<Block> getBlocks(Node peer) {
        String URL = peer.url + "/blocks";
        log.info("Getting blocks from: {}", URL);
        return client
                .get()
                .uri(URL)
                .exchange()
                .flatMapMany(cr -> cr.bodyToFlux(Block.class));
    }

    public Mono<ClientResponse> sendTransaction(Node peer, Transaction transaction) {
        String URL = peer.url + "/transactions";
        log.info("Sending transactin '{}' to: {}", transaction, URL);
        return client
                .post()
                .uri(URL)
                .body(transaction, Transaction.class)
                .exchange();
    }

    public Flux<Transaction> getTransactions(Node peer) {
        String URL = peer.url + "/transactions";
        log.info("Getting transactions from: {}", URL);
        return client
                .get()
                .uri(URL)
                .exchange()
                .flatMapMany(cr -> cr.bodyToFlux(Transaction.class));
    }

    public Mono<Boolean> getConfirmation(Node peer, long transactionId) {
        String URL = peer.url + "/transactions/blocks/transactions/" + transactionId;
        log.info("Getting transactions from: {}", URL);
        return client
                .get()
                .uri(URL)
                .exchange()
                .map(cr -> cr.rawStatusCode() == 200);
    }
/*
    Mono<Boolean> getConfirmations(long transactionId) {
        // Get from all peers if the transaction has been confirmed
        let foundLocally = this.blockchain.getTransactionFromBlocks(transactionId) != null ? true : false;
        return Promise.all(R.map((peer) => {
        return this.getConfirmation(peer, transactionId);
        }, this.peers))
            .then((values) => {
        return R.sum([foundLocally, ...values]);
            });
    }
*/
    public void broadcast(Consumer consumer, Object args) {
        // Call the function for every peer connected
        log.info("Broadcasting");
        peers.forEach(n -> consumer.accept(args));
    }
/*
    public void syncTransactions(List<Transaction> transactions) {
        // For each received transaction check if we have it, if not, add.
        transactions.forEach(transaction -> {
                let transactionFound = this.blockchain.getTransactionById(transaction.getTransactionId());

        if (transactionFound == null) {
            console.info(`Syncing transaction '${transaction.id}'`);
            blockchain.addTransaction(transaction).block();
        }
        });
    }
    Mono<Block> checkReceivedBlock(Block block) {
        return this.checkReceivedBlocks(Arrays.asList(block))
                .last();
    }
    Flux<Block> checkReceivedBlocks(List<Block> blocks) {
        const receivedBlocks = blocks.sort((b1, b2) => (b1.index - b2.index));
        const latestBlockReceived = receivedBlocks[receivedBlocks.length - 1];
        const latestBlockHeld = this.blockchain.getLastBlock();

        // If the received blockchain is not longer than blockchain. Do nothing.
        if (latestBlockReceived.index <= latestBlockHeld.index) {
            console.info('Received blockchain is not longer than blockchain. Do nothing');
            return false;
        }

        console.info(`Blockchain possibly behind. We got: ${latestBlockHeld.index}, Peer got: ${latestBlockReceived.index}`);
        if (latestBlockHeld.hash === latestBlockReceived.previousHash) { // We can append the received block to our chain
            console.info('Appending received block to our chain');
            this.blockchain.addBlock(latestBlockReceived);
            return true;
        } else if (receivedBlocks.length === 1) { // We have to query the chain from our peer
            console.info('Querying chain from our peers');
            this.broadcast(this.getBlocks);
            return null;
        } else { // Received blockchain is longer than current blockchain
            console.info('Received blockchain is longer than current blockchain');
            this.blockchain.replaceChain(receivedBlocks);
            return true;
        }
    }
*/
    public List<Node> getPeers() {
        return peers;
    }
}
