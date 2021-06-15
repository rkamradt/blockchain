package net.kamradtfamily.blockchain.miner;

import net.kamradtfamily.blockchain.api.Block;
import net.kamradtfamily.blockchain.api.Blockchain;
import net.kamradtfamily.blockchain.api.Transaction;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class Miner {
    final Blockchain blocks;
    final Random random = new Random();
    public Miner(Blockchain blocks) {
        this.blocks = blocks;
    }
    public Mono<Block> mine(String address) {
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(Transaction.builder()
                .transactionId(random.nextLong())
                .data(Transaction.Data.builder()
                        .input(Transaction.Input.builder()
                                .address(address)
                                .contract("some contract")
                                .signature("signature")
                                .build())
                        .outputs(List.of(Transaction.Output.builder()
                                .address(address)
                                .contract("some other contract")
                                .build()))
                        .build())
                .build()
                .withHash());
        return blocks.getLastBlock()
                .map(b -> Block.builder()
                        .transactions(transactions)
                        .nonce(0)
                        .previousHash(b.getHash())
                        .timestamp(Instant.now().getEpochSecond())
                        .index(b.getIndex() + 1)
                        .build()
                        .withHash());
    }
}
