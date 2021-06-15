package net.kamradtfamily.blockchain;

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
    String inputAddress;
    String outputContract;
    String outputAddress;
    String signature;
}
