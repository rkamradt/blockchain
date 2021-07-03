package net.kamradtfamily.blockchainnode;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.kamradtfamily.blockchain.api.Blockchain;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/node")
public class NodeController {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Blockchain blockchain;
    private final NodeService nodeService;

    public NodeController(Blockchain blockchain, NodeService nodeService) {
        this.blockchain = blockchain;
        this.nodeService = nodeService;
    }

    @GetMapping(path = "peers", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Node> getNodes() {
        return nodeService.getPeers();
    }

    @PostMapping(path = "peers", produces = MediaType.APPLICATION_JSON_VALUE)
    Mono<Node> addNode(@RequestBody Node node) {
        return nodeService.connectToPeers(Flux.just(node))
                .switchIfEmpty(Mono.just(node))
                .last();
    }

    @GetMapping(path = "transactions/:transactionId/confirmations", produces = MediaType.ALL_VALUE)
    Mono<String> getTransactionFromNode(@RequestParam("transactionId") String transactionId) {
        return nodeService.getConfirmations(Long.valueOf(transactionId))
                .map(b -> b.toString());
    }
}
