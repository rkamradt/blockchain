package net.kamradtfamily.blockchain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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
    final private Emitter emitter;
    public Node(@Value("${server.host}") String host,
                @Value("${server.port}") String port,
                Blockchain blockchain,
                Emitter emitter) {
        this.host = host;
        this.port = Integer.parseInt(port);
        this.blockchain = blockchain;
        this.emitter = emitter;
        //hookBlockchain();
        //connectToPeers(peers);
    }
/*
    public void hookBlockchain() {
        emitter.addHandler("blockAdded", (block) -> {
                broadcast(this::sendLatestBlock, block);
        });

        emitter.addHandler("transactionAdded", (newTransaction) -> {
                broadcast(this::sendTransaction, newTransaction);
        });

        emitter.addHandler("blockchainReplaced", (blocks) -> {
                broadcast(this::sendLatestBlock, blocks);
        });
    }

    public Mono<Node> connectToPeer(Node newPeer) {
        return connectToPeers(Arrays.asList(newPeer))
                .last();
    }

    public Flux<Node> connectToPeers(List<Node> newPeers) {
        // Connect to every peer
        let me = `http://${this.host}:${this.port}`;
        newPeers.forEach((peer) -> {
        // If it already has that peer, ignore.
        if (!this.peers.find((element) => { return element.url == peer.url; }) && peer.url != me) {
            this.sendPeer(peer, { url: me });
            console.info(`Peer ${peer.url} added to connections.`);
            this.peers.push(peer);
            this.initConnection(peer);
            this.broadcast(this.sendPeer, peer);
        } else {
            log.info(`Peer ${peer.url} not added to connections, because I already have.`);
        }
        }, this);

    }

    public void initConnection(Node peer) {
        getLatestBlock(peer);
        getTransactions(peer);
    }

    public void sendPeer(Node peer, Node peerToSend) {
        const URL = `${peer.url}/node/peers`;
        console.info(`Sending ${peerToSend.url} to peer ${URL}.`);
        return superagent
                .post(URL)
                .send(peerToSend)
                .catch((err) => {
                log.warn(`Unable to send me to peer ${URL}: ${err.message}`);
            });
    }

    public void getLatestBlock(Node peer) {
        const URL = `${peer.url}/blockchain/blocks/latest`;
        let self = this;
        console.info(`Getting latest block from: ${URL}`);
        return superagent
                .get(URL)
                .then((res) => {
                // Check for what to do with the latest block
                self.checkReceivedBlock(Block.fromJson(res.body));
            })
            .catch((err) => {
                log.warn(`Unable to get latest block from ${URL}: ${err.message}`);
            });
    }

    public void sendLatestBlock(Node peer, Block block) {
        const URL = `${peer.url}/blockchain/blocks/latest`;
        console.info(`Posting latest block to: ${URL}`);
        return superagent
                .put(URL)
                .send(block)
                .catch((err) => {
                console.warn(`Unable to post latest block to ${URL}: ${err.message}`);
            });
    }

    public void getBlocks(Node peer) {
        const URL = `${peer.url}/blockchain/blocks`;
        let self = this;
        log.info(`Getting blocks from: ${URL}`);
        return superagent
                .get(URL)
                .then((res) => {
                // Check for what to do with the block list
                self.checkReceivedBlocks(Blocks.fromJson(res.body));
            })
            .catch((err) => {
                log.warn(`Unable to get blocks from ${URL}: ${err.message}`);
            });
    }

    public void sendTransaction(Node peer, Transaction transaction) {
        const URL = `${peer.url}/blockchain/transactions`;
        log.info(`Sending transaction '${transaction.id}' to: '${URL}'`);
        return superagent
                .post(URL)
                .send(transaction)
                .catch((err) => {
                log.warn(`Unable to put transaction to ${URL}: ${err.message}`);
            });
    }

    public Flux<Transaction> getTransactions(Node peer) {
        const URL = `${peer.url}/blockchain/transactions`;
        let self = this;
        log.info(`Getting transactions from: ${URL}`);
        return superagent
                .get(URL)
                .then((res) => {
                self.syncTransactions(Transactions.fromJson(res.body));
            })
            .catch((err) => {
                log.warn(`Unable to get transations from ${URL}: ${err.message}`);
            });
    }

    public Mono<Boolean> getConfirmation(Node peer, long transactionId) {
        // Get if the transaction has been confirmed in that peer
        const URL = `${peer.url}/blockchain/blocks/transactions/${transactionId}`;
        log.info(`Getting transactions from: ${URL}`);
        return superagent
                .get(URL)
                .then(() => {
        return true;
            })
            .catch(() => {
        return false;
            });
    }

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

    public void broadcast(Consumer consumer, Object args) {
        // Call the function for every peer connected
        log.info("Broadcasting");
        peers.forEach(n -> consumer.accept(args));
    }

    public void syncTransactions(List<Transaction> transactions) {
        // For each received transaction check if we have it, if not, add.
        R.forEach((transaction) => {
                let transactionFound = this.blockchain.getTransactionById(transaction.id);

        if (transactionFound == null) {
            console.info(`Syncing transaction '${transaction.id}'`);
            blockchain.addTransaction(transaction).block();
        }
        }, transactions);
    }

    Mono<Boolean> checkReceivedBlock(Block block) {
        return this.checkReceivedBlocks(Arrays.asList(block));
    }

    Mono<Boolean> checkReceivedBlocks(List<Block> blocks) {
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
