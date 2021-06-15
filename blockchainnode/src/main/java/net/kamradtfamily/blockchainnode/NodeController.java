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
    private final Node node;

    public NodeController(Blockchain blockchain, Node node) {
        this.blockchain = blockchain;
        this.node = node;
    }

    @GetMapping(path = "peers", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Node> getNodes() {
        return Flux.just(node.getPeers().toArray(new Node[0]));
    }

    @PostMapping(path = "peers", produces = MediaType.APPLICATION_JSON_VALUE)
    Mono<Node> addNode(Node node) {
        return Mono.just(node); //node.connectToPeer(node);
    }

    @GetMapping(path = "transactions/:transactionId", produces = MediaType.ALL_VALUE)
    Mono<String> getTransactionFromNode(@RequestParam("transactionId") String transactionId) {
        return Mono.just(Boolean.TRUE) //node.getConfirmations(Long.valueOf(transactionId))
                .map(b -> b.toString());
    }
}
