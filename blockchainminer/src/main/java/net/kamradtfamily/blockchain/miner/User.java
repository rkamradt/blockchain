package net.kamradtfamily.blockchain.miner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
    String id;
    String name;
    String address;
    String publicKey;
    String privateKey;
}
