package net.kamradtfamily.blockchain;

public class BlockchainAssertionError extends Throwable {
    public BlockchainAssertionError(String s, BlockAssertionError error) {
        super(s, error);
    }

    public BlockchainAssertionError(String s) {
        super(s);
    }
}
