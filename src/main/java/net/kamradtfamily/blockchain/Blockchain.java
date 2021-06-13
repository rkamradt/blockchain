package net.kamradtfamily.blockchain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;

/**
 * This class holds the methods associated with blocks and transaction
 * including verifying correctness, and adding to the distributed ledger
 */
@Slf4j
@Component
public class Blockchain {
    final Emitter emitter;
    final BlockRepository blocks;
    final TransactionRepository transactions;

    /**
     *
     * Create a new Blockchain object. When first created, it will inspect the local
     * database, create a genesis block if it is empty of blocks, or if it has blocks
     * ensure that the transaction database does not have any transactions that are
     * already associated with existing blocks.
     *
     * @param blocks The database of blocks
     * @param transactionRepository The database of 'loose' transactions
     * @param emitter An emitter of signals to ensure building consensus.
     */
    public Blockchain(BlockRepository blocks,
                      TransactionRepository transactionRepository,
                      Emitter emitter) {
        // Some places uses the emitter to act after some data is changed
        this.blocks = blocks;
        this.transactions = transactionRepository;
        this.emitter = emitter;
        blocks.findAll()
                .flatMap(this::removeBlockTransactionsFromTransactions)
                .switchIfEmpty(Mono.from(blocks.save(Block.genesis))
                        .map(result -> Block.genesis))
                .subscribe(b -> log.info("inspecting block {} on startup", b),
                e -> log.error("error inspecting blocks on startup", e));
    }

    /**
     *
     * Return all the blocks currently stored in the database
     *
     * @return a Flux of Blocks
     */
    public Flux<Block> getAllBlocks() {
        return blocks.findAll();
    }

    /**
     *
     * get a block by the hash value
     *
     * @param hash the hash value to search for
     * @return A Mono of Block
     */
    public Mono<Block> getBlockByHash(String hash) {
        return blocks.findByHash(hash);
    }

    /**
     *
     * Get the last block in the blockchain. Finds the max index and
     * then returns that value.
     *
     * @return A Mono of Block
     */
    public Mono<Block> getLastBlock() {
        // todo figure out a better way to get max from mongo
        AtomicLong max = new AtomicLong(-1L);
        return blocks.findAll()
                .doOnNext(b -> max.getAndUpdate(i -> Math.max(i, b.getIndex())))
                .last()
                .flatMap(b -> blocks.findByIndex(max.get()));
    }

    /**
     *
     * Get the difficulty level. Always returns 0 because our
     * transactions have no value and so mining should be easy
     *
     * @param index
     * @return
     */
    public long getDifficulty(long index) {
        return 0L;
    }

    /**
     *
     * Get all 'loose' transaction, that is transactions
     * that have been created by the user, but are not part
     * af a block. Loose transactions will eventually be
     * put into a block by a mining operation
     *
     * @return A Flux of Transactions
     */
    public Flux<Transaction> getAllTransactions() {
        return transactions.findAll();
    }

    /**
     *
     * Get a Transaction by id.
     *
     * @param id the id to look for
     * @return A Mono of Transaction
     */
    public Mono<Transaction> getTransactionById(long id) {
        return transactions.findByTransactionId(id);
    }

    /**
     *
     * Return a flux of blocks that contain the transaction. Used
     * mainly to see if a transaction is already part of the block
     * chain. Since a transaction should only exist once, there
     * should ever only be 0 or 1 blocks returned
     *
     * @param transactionId the transaction id we're looking for
     * @return a flux of blocks that contain the transaction
     */
    public Flux<Block> findTransactionInChain(long transactionId, Flux<Block> referenceBlockchain) {
        return referenceBlockchain
            .filter(b -> b.getTransactions()
                .stream()
                .anyMatch(t -> t.getTransactionId() == transactionId)
            );
    }

    /**
     *
     * Add a new block to the block chain. This happens as a result
     * of a mining operation. The block is verified for correctness
     * and all of the transactions contained are removed from the 'loose'
     * transaction database.
     *
     * @param newBlock the block to add
     * @param emit true to emit (set to false when replacing the block chain)
     * @return A Mono of Block, which is always the newBlock, but can be used to chain async operations
     */
    public Mono<Block> addBlock(Block newBlock, boolean emit) {
        log.info("adding block {}", newBlock);
        return getLastBlock()
                .doOnNext(b -> log.info("in addBlock last block = {}", b))
                .flatMap(l -> checkBlock(newBlock, l, getAllBlocks())
                                .map(t -> newBlock))
                .flatMap(ignore -> blocks.save(newBlock))
                .map(ignore -> newBlock)
                .flatMap(this::removeBlockTransactionsFromTransactions)
                .doOnNext(b -> {
                    log.info("Block added: {}", newBlock);
                    if(emit) emitter.emit("blockAdded", b);
                });
    }

    /**
     *
     * Add a loose transaction. The transaction is verified for correctness and
     * that it doesn't already exist in our database. If it is good, the it will
     * be stored for later inclusion in a block
     *
     * @param newTransaction the new transaction
     * @param emit emit to build consensus
     * @return a Mono of Transaction which is always the new transaction if it was added.
     */
    public Mono<Transaction> addTransaction(Transaction newTransaction, boolean emit) {
        return checkTransaction(newTransaction, getAllBlocks())
                .map(b -> newTransaction)
                .switchIfEmpty(Mono.from(transactions.save(newTransaction))
                        .map(ignore -> newTransaction)
                        .doOnNext(t -> {
                            log.info("Transaction added: {}", t);
                            if(emit) emitter.emit("transactionAdded", t);
                        }));
    }

    /**
     *
     * Remove transaction from the database of loose transactions
     *
     * @param newBlock the new block with the transaction to remove
     * @return a Mono of Block which is always the newBlock
     */
    public Mono<Block> removeBlockTransactionsFromTransactions(Block newBlock) {
        if(newBlock.getTransactions().isEmpty()) {
            return Mono.just(newBlock);
        }
        return Flux.fromStream(newBlock.getTransactions().stream())
                .flatMap(t -> transactions.findByTransactionId(t.getTransactionId()))
                .onErrorReturn(Transaction.builder().transactionId(0).build())
                .filter(t -> t.getTransactionId() != 0)
                .flatMap(t -> transactions.delete(t))
                .map(ignore -> newBlock)
                .switchIfEmpty(Flux.just(newBlock))
                .last();
    }

    /**
     *
     * The a block to be added for correctness.
     *
     * @param newBlock the block to add
     * @param previousBlock the last block (always add to the end)
     * @param referenceBlockchain The full blockchain
     * @return A Mono of Block which is always newBlock
     */
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

    /**
     *
     * Check a transaction for correctness
     *
     * @param transaction the transaction to check
     * @param referenceBlockchain the block chain it will be part of
     * @return A Mono of Transaction which is always the transaction
     */
    public Mono<Transaction> checkTransaction(Transaction transaction, Flux<Block> referenceBlockchain) {
        transaction.check(transaction);
        return this.findTransactionInChain(transaction.getTransactionId(), referenceBlockchain)
                .map(ignore -> transaction)
                .flatMap(ignore -> Flux.<Transaction>error(() ->
                        new TransactionAssertionError("block found with transaction")))
                .switchIfEmpty(Flux.just(transaction))
                .last();
    }

}
