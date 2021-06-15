package net.kamradtfamily.blockchain.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * A transaction is associated with the block in a block chain and represents a transfer
 * of some unit of value. In our case the unit of value is just a string called a contract
 * that users can exchange. Both Input and Output data have a contract that should be
 * compliments, an address associated with the data, and the Input is signed by the
 * private key of the initiating user.
 */
@lombok.Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Document
public class Transaction {
    static ObjectMapper mapper = new ObjectMapper();
    @Id
    private String id;
    private long transactionId;
    private String hash;
    public Data data;

    /**
     *
     * Add a 'wither' for hash since it must be calculate after the transaction is built
     *
     * @return the transaction with a hash added
     */
    public Transaction withHash() {
        hash = toHash();
        return this;
    }

    /**
     *
     * create a hash value from the transactionId and data
     *
     * @return the hash value
     */
    public String toHash() {
        try {
            return CryptoUtil.hash(transactionId + mapper.writeValueAsString(this.data));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("json error", e);
        }
    }
    public void check(Transaction transaction) {

    }
    @lombok.Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Data {
        private Input input;
        private List<Output> outputs;
    }
    @lombok.Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Input {
        private String contract;
        private String address;
        private String signature;
    }
    @lombok.Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Output {
        private String contract;
        private String address;
    }

}
