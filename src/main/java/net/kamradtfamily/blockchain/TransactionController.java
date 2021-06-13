package net.kamradtfamily.blockchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/transaction")
public class TransactionController {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Blockchain blockchain;

    public TransactionController(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    @GetMapping(path = "", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Transaction> getTransactions() {
        return blockchain.getAllTransactions();
    }

    @GetMapping(path = "{transactionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Transaction> getTransaction(@PathVariable("transactionId") Long transactionId) {
        return blockchain.getTransactionById(transactionId);
    }

}
