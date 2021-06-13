package net.kamradtfamily.blockchain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

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
public class Transaction {
    static ObjectMapper mapper = new ObjectMapper();
    private long transactionId;
    private String hash;
    public Data data;

    public Transaction withHash() {
        hash = toHash();
        return this;
    }
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
    static class Data {
        private List<Input> inputs;
        private List<Output> outputs;
    }
    @lombok.Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    static class Input {
        private String contract;
        private String address;
        private String signature;
    }
    @lombok.Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    static class Output {
        private String contract;
        private String address;
    }

}
