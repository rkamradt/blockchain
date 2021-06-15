package net.kamradtfamily.blockchain.miner;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.kamradtfamily.blockchain.api.Block;
import net.kamradtfamily.blockchain.api.Blockchain;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/miner")
public class MinerController {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Blockchain blockchain;
    private final Miner miner;

    public MinerController(Blockchain blockchain, Miner miner) {
        this.blockchain = blockchain;
        this.miner = miner;
    }

    @PostMapping(path = "mine/{address}", produces = MediaType.APPLICATION_JSON_VALUE)
    Mono<Block> mine(@PathVariable("address") String address) {
        return miner.mine(address)
                .flatMap(b -> blockchain.addBlock(b, true));
    }
}
