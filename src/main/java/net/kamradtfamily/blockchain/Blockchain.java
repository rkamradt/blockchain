package net.kamradtfamily.blockchain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.BsonString;
import org.bson.Document;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
public class Blockchain {
    final static ObjectMapper mapper = new ObjectMapper();
    final Emitter emitter;
    final MongoCollection<Document> blocks;
    final MongoCollection<Document> transactions;
    public Blockchain(MongoDatabase database, Emitter emitter) {
        // Some places uses the emitter to act after some data is changed
        this.blocks = database.getCollection("blocks");
        this.transactions = database.getCollection("transactions");
        this.emitter = emitter;
        init();
    }

    public void init() {
        Flux.from(blocks.find())
                .map(this::mapToBlock)
                .flatMap(b -> removeBlockTransactionsFromTransactions(b))
                .switchIfEmpty(Mono.from(blocks.insertOne(mapBlockToDocument(Block.genesis)))
                    .map(result -> Block.genesis))
                .blockLast(Duration.of(10, ChronoUnit.SECONDS));
    }

    public Flux<Block> getAllBlocks() {
        return Flux.from(blocks.find())
                .map(this::mapToBlock);
    }

    private Document mapBlockToDocument(Block block) {
        try {
            return Document.parse(mapper.writeValueAsString(block));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("error parsing block to doc", e);
        }
    }

    private Block mapToBlock(Document d) {
        try {
            return mapper.readValue(d.toJson(), Block.class);
        } catch (IOException e) {
            throw new RuntimeException("error parsing doc to block", e);
        }
    }

    private Document mapTransactionToDocument(Transaction transaction) {
        try {
            return Document.parse(mapper.writeValueAsString(transaction));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("error parsing transaction to doc", e);
        }
    }

    private Transaction mapToTransaction(Document d) {
        try {
            return mapper.readValue(d.toJson(), Transaction.class);
        } catch (IOException e) {
            throw new RuntimeException("error parsing doc to transaction", e);
        }
    }

    public Mono<Block> getBlockByIndex(long index) {
        BsonDocument bson = new BsonDocument();
        bson.put("index", new BsonInt64(index));
        return Mono.from(blocks.find(bson))
                .map(this::mapToBlock);
    }

    public Mono<Block> getBlockByHash(String hash) {
        BsonDocument bson = new BsonDocument();
        bson.put("hash", new BsonString(hash));
        return Mono.from(blocks.find(bson))
                .map(this::mapToBlock);
    }

    public Mono<Block> getLastBlock() {
        return Mono.from(blocks.aggregate(
                    Arrays.asList(
                            Aggregates.group("$index", Accumulators.max("maxindex", 0L))
                    )
                ))
                .flatMap(r -> this.getBlockByIndex(r.get("maxindex", Long.class)))
                .doOnNext(b -> log.info("last block is {}", b));
    }

    public long getDifficulty(long index) {
        return 0L;
    }

    public Flux<Transaction> getAllTransactions() {
        return Flux.from(transactions.find())
                .map(this::mapToTransaction);
    }

    public Mono<Transaction> getTransactionById(long id) {
        BsonDocument bson = new BsonDocument();
        bson.put("id", new BsonInt64(id));
        return Mono.from(blocks.find(bson))
                .map(this::mapToTransaction);
    }

    /**
     *
     * Return a flux of blocks that contain the transaction
     *
     * @param transactionId
     * @return a flux of blocks that contain the transaction
     */
    public Flux<Block> findTransactionInChain(long transactionId, Flux<Block> referenceBlockchain) {
        return referenceBlockchain
            .filter(b -> b.getTransactions()
                .stream()
                .filter(t -> t.getId() == transactionId)
                .findAny()
                .isPresent()
            );
    }

    public Mono<Block> addBlock(Block newBlock, boolean emit) {
        log.info("adding block {}", newBlock);
        return getLastBlock()
                .doOnNext(b -> log.info("in addBlock last block = {}", b))
                .flatMap(l -> checkBlock(newBlock, l, getAllBlocks())
                                .map(t -> newBlock))
                .flatMap(ignore -> Mono.from(blocks.insertOne(mapBlockToDocument(newBlock))))
                .map(ignore -> newBlock)
                .flatMap(b -> removeBlockTransactionsFromTransactions(b))
                .doOnNext(b -> {
                    log.info("Block added: {}", newBlock);
                    if(emit) emitter.emit("blockAdded", b);
                });
    }

    public Mono<Transaction> addTransaction(Transaction newTransaction, boolean emit) throws TransactionAssertionError {
        return checkTransaction(newTransaction, getAllBlocks())
                .map(b -> newTransaction)
                .switchIfEmpty(Mono.from(transactions.insertOne(mapTransactionToDocument(newTransaction)))
                        .map(ignore -> newTransaction)
                        .doOnNext(t -> {
                            log.info("Transaction added: {}", t);
                            if(emit) emitter.emit("transactionAdded", t);
                        }));
    }

    public Mono<Block> removeBlockTransactionsFromTransactions(Block newBlock) {
        if(newBlock.getTransactions().isEmpty()) {
            return Mono.just(newBlock);
        }
        return Mono.from(transactions
                .bulkWrite(newBlock.getTransactions()
                        .stream()
                        .map(t -> new DeleteOneModel<Document>(new Document("id", t.getId())))
                        .collect(Collectors.toList()),new BulkWriteOptions().bypassDocumentValidation(true)))
                .map(ignore -> newBlock);
    }

    public Mono<Block> checkBlock(Block newBlock, Block previousBlock, Flux<Block> referenceBlockchain) {
        log.info("checking block {}", newBlock);
        final String blockHash = newBlock.toHash();

        if (previousBlock.getIndex() + 1 != newBlock.getIndex()) { // Check if the block is the last onelog.error("Invalid index: expected {} got {}", previousBlock.getIndex() + 1, newBlock.getIndex());
            return Mono.error(() -> new BlockAssertionError("Invalid index: expected "
                    + (previousBlock.getIndex() + 1)
                    + " got "
                    + newBlock.getIndex()));
        } else if (!previousBlock.getHash().equals(newBlock.getPreviousHash())) { // Check if the previous block is correct
            return Mono.error(() -> new BlockAssertionError("Invalid previoushash: expected "
                    + previousBlock.getHash()
                    + " got "
                    + newBlock.getPreviousHash()));
        } else if (!blockHash.equals(newBlock.getHash())) { // Check if the hash is correct
            return Mono.error(() -> new BlockAssertionError("Invalid hash: expected "
                    + blockHash
                    + " got "
                    + newBlock.getHash()));
        } else if (newBlock.calculateDifficulty() >= getDifficulty(newBlock.getIndex())) { // If the difficulty level of the proof-of-work challenge is correct
            return Mono.error(() ->  new BlockAssertionError("Invalid proof-of-work difficulty: expected "
                    + newBlock.calculateDifficulty()
                    + " be smaller than "
                    + getDifficulty(newBlock.getIndex())));
        }
        return Flux.fromStream(newBlock.getTransactions().stream())
                .flatMap(t -> checkTransaction(t, referenceBlockchain))
                .last()
                .map(ignore -> newBlock);
    }

    public Mono<Transaction> checkTransaction(Transaction transaction, Flux<Block> referenceBlockchain) {
        transaction.check(transaction);
        return this.findTransactionInChain(transaction.getId(), referenceBlockchain)
                .map(ignore -> transaction)
                .flatMap(ignore -> Flux.<Transaction>error(() ->
                        new TransactionAssertionError("block found with transaction")))
                .switchIfEmpty(Flux.just(transaction))
                .last();
    }

}
