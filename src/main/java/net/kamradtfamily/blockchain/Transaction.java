package net.kamradtfamily.blockchain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.List;

@lombok.Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transaction {
    static ObjectMapper mapper = new ObjectMapper();
    private long id;
    private String hash;
    public Data data;

    public Transaction withHash() {
        hash = toHash();
        return this;
    }
    public String toHash() {
        try {
            return CryptoUtil.hash(id + mapper.writeValueAsString(this.data));
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
