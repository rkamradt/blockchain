package net.kamradtfamily.blockchain.api;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BlockchainTest {
    Blockchain sut;

    @BeforeAll
    void before() {
        BlockRepository blocks = new MockBlockRepository();
        TransactionRepository transactions = new MockTransactionRepository();
//        ReactiveKafkaProducerTemplate<String, Message> = new MockReactiveProducer();
//        sut = new Blockchain(blocks, transactions);
    }

    @Test
    void getAllBlocks() {
    }

    @Test
    void getBlockByHash() {
    }

    @Test
    void getLastBlock() {
    }

    @Test
    void getDifficulty() {
    }

    @Test
    void getAllTransactions() {
    }

    @Test
    void getTransactionById() {
    }

    @Test
    void findTransactionInChain() {
    }

    @Test
    void addBlock() {
    }

    @Test
    void addTransaction() {
    }

    @Test
    void removeBlockTransactionsFromTransactions() {
    }

    @Test
    void checkBlock() {
    }

    @Test
    void checkTransaction() {
    }
}