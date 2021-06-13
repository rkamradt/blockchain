package net.kamradtfamily.blockchain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Miner {
    final Blockchain blocks;
    final Random random = new Random();
    public Miner(Blockchain blocks) {
        this.blocks = blocks;
    }
    public Block mine(String address) {
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(Transaction.builder()
                .id(random.nextLong())
                .data(Transaction.Data.builder()
                        .inputs(List.of(Transaction.Input.builder()
                                .address(address)
                                .contract("some contract")
                                .signature("signature")
                                .build()))
                        .outputs(List.of(Transaction.Output.builder()
                                .address(address)
                                .contract("some other contract")
                                .build()))
                        .build())
                .build()
                .withHash());
        Block lastBlock = blocks.getLastBlock().block();
        return Block.builder()
                .transactions(transactions)
                .nonce(0)
                .previousHash(lastBlock.getHash())
                .timestamp(Instant.now().getEpochSecond())
                .index(lastBlock.getIndex() + 1)
                .build()
                .withHash();
    }
}
