package net.kamradtfamily.blockchain;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import express.Express;
import express.utils.Status;
import io.github.rkamradt.possibly.PossiblyFunction;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.time.Duration;

@Slf4j
public class BlockchainServer {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static void main(String [] args) {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        // print logback's internal status
        StatusPrinter.print(lc);
        MongoClient mongoClient = MongoClients.create("mongodb://root:example@localhost:27017");
        Emitter emitter = new Emitter();
        Blockchain blockchain = new Blockchain(mongoClient.getDatabase("blockchain"), emitter);
        Express app = new Express();
        Node node = new Node();
        Miner miner = new Miner(blockchain);
        app.get("/block", (req, res) -> {
            try {
                Tuple2<Status, String> payload = blockchain.getAllBlocks()
                        .collectList()
                        .map(PossiblyFunction.of(block -> objectMapper.writeValueAsString(block)))
                        .map(p -> {
                            if (p.exceptional()) {
                                return Tuples.of(Status._500, "Uncaught exception " + p.getException().get());
                            }
                            return Tuples.of(Status._200, p.getValue().get());
                        })
                        .defaultIfEmpty(Tuples.of(Status._404, "no blocks found"))
                        .block(Duration.ofSeconds(10));
                res.setStatus(payload.getT1());
                res.send(payload.getT2());
            } catch (Exception ex) {
                log.info("uncaught exception " + ex.getMessage());
                ex.printStackTrace(System.out);
                res.sendStatus(Status._500);
            }
        });
        app.get("/block/:blockHash", (req, res) -> {
            try {
                String hash = req.getParam("blockHash");
                Tuple2<Status, String> payload = blockchain.getBlockByHash(hash)
                        .map(PossiblyFunction.of(block -> objectMapper.writeValueAsString(block)))
                        .map(p -> {
                            if (p.exceptional()) {
                                return Tuples.of(Status._500, "Uncaught exception " + p.getException().get());
                            }
                            return Tuples.of(Status._200, p.getValue().get());
                        })
                        .defaultIfEmpty(Tuples.of(Status._404, hash + " not found"))
                        .block(Duration.ofSeconds(10));
                res.setStatus(payload.getT1());
                res.send(payload.getT2());
            } catch (Exception ex) {
                log.info("uncaught exception " + ex.getMessage());
                ex.printStackTrace(System.out);
                res.sendStatus(Status._500);
            }
        });
        app.get("/transaction", (req, res) -> {
            try {
                Tuple2<Status, String> payload = blockchain.getAllTransactions()
                        .collectList()
                        .map(PossiblyFunction.of(t -> objectMapper.writeValueAsString(t)))
                        .map(p -> {
                            if (p.exceptional()) {
                                return Tuples.of(Status._500, "Uncaught exception " + p.getException().get());
                            }
                            return Tuples.of(Status._200, p.getValue().get());
                        })
                        .defaultIfEmpty(Tuples.of(Status._404, "no transactions found"))
                        .block(Duration.ofSeconds(10));
                res.setStatus(payload.getT1());
                res.send(payload.getT2());
            } catch (Exception ex) {
                log.info("uncaught exception " + ex.getMessage());
                ex.printStackTrace(System.out);
                res.sendStatus(Status._500);
            }
        });
        app.get("/transaction/:transactionId", (req, res) -> {
            try {
                String id = req.getParam("transactionId");
                Tuple2<Status, String> payload = blockchain.getTransactionById(Long.valueOf(id))
                        .map(PossiblyFunction.of(block -> objectMapper.writeValueAsString(block)))
                        .map(p -> {
                            if (p.exceptional()) {
                                return Tuples.of(Status._500, "Uncaught exception " + p.getException().get());
                            }
                            return Tuples.of(Status._200, p.getValue().get());
                        })
                        .defaultIfEmpty(Tuples.of(Status._404, id + " not found"))
                        .block(Duration.ofSeconds(10));
                res.setStatus(payload.getT1());
                res.send(payload.getT2());
            } catch (Exception ex) {
                log.info("uncaught exception " + ex.getMessage());
                ex.printStackTrace(System.out);
                res.sendStatus(Status._500);
            }
        });

        app.get("/node/peers", (req, res) -> {
            try {
                res.setStatus(Status._200).send(objectMapper.writeValueAsString(node.peers));
            } catch (JsonProcessingException e) {
                res.setStatus((Status._500));
            }
        });

        app.post("/node/peers", (req, res) -> {
            Node newNode;
            try {
                newNode = objectMapper.readValue(req.getBody(),
                        Node.class);
            } catch (IOException e) {
                res.setStatus((Status._400));
                return;
            }
            Node newPeer = node.connectToPeer(newNode);
            try {
                res.setStatus(Status._201).send(objectMapper.writeValueAsString(newPeer));
            } catch (JsonProcessingException e) {
                res.setStatus((Status._500));
            }
        });

        app.get("/node/transactions/:transactionId([a-zA-Z0-9]{64})/confirmations", (req, res) -> {
            String id = req.getParam("transactionId");
            node.confirm(id) ;
            res.setStatus(Status._200);
        });

        app.post("/miner/mine/:address", (req, res) -> {
            String address = req.getParam("address");
            final Block newBlock = miner.mine(address);
            blockchain.addBlock(newBlock, true)
                    .doOnError(t -> log.error("Error trying to mine block {}", newBlock, t))
                    .block(Duration.ofSeconds(10));
            log.info("added block {}", newBlock);
            try {
                res.setStatus(Status._201).send(objectMapper.writeValueAsString(newBlock));
            } catch (JsonProcessingException e) {
                res.setStatus((Status._500));
            }
        });

        app.listen(8080);
    }
}
