package net.kamradtfamily.blockchain.miner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(
            UserService userService
    ) {
        this.userService = userService;
    }
    @GetMapping(path = "",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<User> getUsers(
            @RequestHeader("Authorization") String authorization) {
        checkForUser("admin", authorization);
        return userService.getAllUsers()
                .doOnNext(u -> u.setPrivateKey(null));
    }

    @GetMapping(path = "{userId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<User> getUser(
            @PathVariable("userId") String userId,
            @RequestHeader("Authorization") String authorization) {
        return userService.getUser(userId)
                .switchIfEmpty(Mono.error(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User Not Found")))
                .doOnNext(u -> checkForUser(u.getName(), authorization))
                .onErrorResume((e) -> Mono.error(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Unauthorized")))
                .doOnNext(u -> u.setPrivateKey(null));
    }
    @PostMapping(path = "",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(code = HttpStatus.CREATED, reason = "User created")
    Mono<User> addUser(@RequestBody AddUserRequest addUserRequest) throws NoSuchAlgorithmException {
        return userService.getUserByName(addUserRequest.getName())
                .flatMap(u -> Mono.<User>error(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "User already exists")
                 ))
                .switchIfEmpty(userService.addUser(addUserRequest.getName()))
                .doOnNext(u -> u.setPrivateKey(null));
    }

    @PostMapping(path = "transaction/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(code = HttpStatus.CREATED, reason = "Transaction created")
    Mono<Long> addTransaction(@RequestHeader("Authorization") String authorization,
                              @PathVariable("userId") String userId,
                              @RequestBody TransactionRequest transactionRequest) {
        return userService.getUser(userId)
                .switchIfEmpty(Mono.error(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User Not Found")))
                .doOnNext(u -> checkForUser(u.getName(), authorization))
                .onErrorResume((e) -> Mono.error(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Unauthorized")))
                .flatMap(user -> userService.addTransaction(
                        user,
                        transactionRequest.getInputContract(),
                        transactionRequest.getOutputContract(),
                        transactionRequest.getOutputAddress()));

    }

    @PostMapping(path = "mine/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(code = HttpStatus.CREATED, reason = "Transaction created")
    Mono<String> createBlock(@RequestHeader("Authorization") String authorization,
                              @PathVariable("userId") String userId,
                              @RequestBody TransactionRequest transactionRequest) {
        return userService.getUser(userId)
                .switchIfEmpty(Mono.error(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User Not Found")))
                .doOnNext(u -> checkForUser(u.getName(), authorization))
                .onErrorResume((e) -> Mono.error(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Unauthorized")))
                .flatMap(user -> userService.createBlock(
                        user));

    }

    private void checkForUser(String userName, String authorization) {
        if(!authorization.startsWith("Basic ")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Must use Basic authentication"
            );
        }
        String [] usernamePassword = new String(Base64.getDecoder()
                .decode(authorization.split(" ")[1]))
                .split(":");
        if(!userName.equals(usernamePassword[0]) ||
            !userName.equals(usernamePassword[1])) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Must use Basic authentication"
            );
        }
    }

}
