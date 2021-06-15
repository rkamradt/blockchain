package net.kamradtfamily.blockchain.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * A Block in the block chain. Blocks have a hash calculated from the values in the
 * block, a previous hash that is the prior block in the chain, a timestamp that
 * gives the time of creation, a nonce that indicates how many iterations the block
 * went through before proof of work was complete, and some data associated with the
 * block. The first block in the block chain is the genesis block with a hash of "0"
 * and a previous hash of "0". Block are created by mining.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Document
public class Block {
    static ObjectMapper mapper = new ObjectMapper();
    public static Block genesis = Block.builder()
            .index(0)
            .previousHash("0")
            .timestamp(0)
            .nonce(0)
            .transactions(Collections.emptyList())
            .build()
            .withHash();
    @Id
    private String id;
    private long index;
    private String previousHash;
    private long timestamp;
    private long nonce;
    private String hash;
    private List<Transaction> transactions;

    /**
     *
     * Add a 'wither' for hash since it must be calculate after the block is built
     *
     * @return the block with a hash added
     */
    public Block withHash() {
        hash = toHash();
        return this;
    }

    /**
     *
     * create a hash value from the index, previousHash, timestamp, nonce, and transactions
     *
     * @return the hash value
     */
    public String toHash() {
        try {
            return CryptoUtil.hash(this.index + this.previousHash + this.timestamp + mapper.writeValueAsString(this.transactions) + this.nonce);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Json decoding error", e);
        }
    }

    /**
     *
     * The difficulty measure, always return -1 to make it easy to mine
     *
     * @return the value must be less than the
     */
    public long calculateDifficulty() {
        return -1l;
    }
}
