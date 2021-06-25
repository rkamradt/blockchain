package net.kamradtfamily.blockchainnode;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.kamradtfamily.blockchain.api.Blockchain;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
//@RestController
//@RequestMapping("/node")
public class SimpleNodeController {
    private final Blockchain blockchain;
    private final SimpleNodeService simpleNodeService;

    public SimpleNodeController(Blockchain blockchain, SimpleNodeService simpleNodeService) {
        this.simpleNodeService = simpleNodeService;
        this.blockchain = blockchain;
    }

    @GetMapping(path = "peers", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Node> getNodes() {
        return simpleNodeService.getPeers();
    }

    @PostMapping(path = "peers", produces = MediaType.APPLICATION_JSON_VALUE)
    Mono<Node> addNode(@RequestBody Node node) {
        return simpleNodeService.connectToPeer(node);
    }

}
