package net.kamradtfamily.blockchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.kamradtfamily.blockchain.api.Block;
import net.kamradtfamily.blockchain.api.Blockchain;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Random;

@Slf4j
@RestController
@RequestMapping("/block")
public class BlockController {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Blockchain blockchain;
    final Random random = new Random();

    public BlockController(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    @GetMapping(path = "", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Block> getBlocks() {
        return blockchain.getAllBlocks();

    }

    @GetMapping(path = "last", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<Block> getLastBlocks() {
        return blockchain.getLastBlock();
    }

    @GetMapping(path = "{blockHash}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Block> getBlock(@PathVariable("blockHash") String blockHash) {
        return blockchain.getBlockByHash(blockHash)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Block Not Found")));

    }
    @PostMapping(path = "mine/{address}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(code = HttpStatus.CREATED, reason = "Block created")
    Mono<Block> mine(@PathVariable("address") String address) {
        return blockchain.getAllTransactions().collectList()
                .flatMap(ts -> blockchain.getLastBlock()
                    .map(b -> Block.builder()
                            .transactions(ts)
                            .nonce(0)
                            .previousHash(b.getHash())
                            .timestamp(Instant.now().getEpochSecond())
                            .index(b.getIndex() + 1)
                            .build()
                            .withHash())
                    .flatMap(b -> blockchain.addBlock(b, true)));
    }
}
