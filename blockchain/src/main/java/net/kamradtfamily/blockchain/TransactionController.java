package net.kamradtfamily.blockchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.kamradtfamily.blockchain.api.Block;
import net.kamradtfamily.blockchain.api.Blockchain;
import net.kamradtfamily.blockchain.api.Transaction;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Random;

@Slf4j
@RestController
@RequestMapping("/transaction")
public class TransactionController {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Blockchain blockchain;
    final Random random = new Random();

    public TransactionController(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    @GetMapping(path = "", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Transaction> getTransactions() {
        return blockchain.getAllTransactions();
    }

    @GetMapping(path = "{transactionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Transaction> getTransaction(@PathVariable("transactionId") Long transactionId) {
        return blockchain.getTransactionById(transactionId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Transaction Not Found")));
    }

    @PostMapping(path = "{transactionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(code = HttpStatus.CREATED, reason = "Transaction added")
    Mono<Transaction> addTransaction(@RequestBody Transaction transaction) {
        return blockchain.addTransaction(transaction, false);

    }

}
