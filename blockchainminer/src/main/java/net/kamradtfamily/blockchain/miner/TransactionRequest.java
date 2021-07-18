package net.kamradtfamily.blockchain.miner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionRequest {
    String inputContract;
    String outputContract;
    String outputAddress;
}
