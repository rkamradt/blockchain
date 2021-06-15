package net.kamradtfamily.blockchain.api;

public class BlockchainAssertionError extends Throwable {
    public BlockchainAssertionError(String s, BlockAssertionError error) {
        super(s, error);
    }

    public BlockchainAssertionError(String s) {
        super(s);
    }
}
