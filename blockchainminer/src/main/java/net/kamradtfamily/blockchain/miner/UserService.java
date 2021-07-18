package net.kamradtfamily.blockchain.miner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
public class UserService {
    final private UserRepository userRepository;
    final private WebClient client = WebClient.create();
    final private String transactionUrl;
    final Random random = new Random();


    public UserService(@Value("${transaction.url}") String transactionUrl,
                       UserRepository userRepository) {
        this.userRepository = userRepository;
        this.transactionUrl = transactionUrl;
        userRepository.findByName("admin")
                .switchIfEmpty(userRepository.save(User.builder()
                        .name("admin")
                        .build()))
                .subscribe();
    }

    public Flux<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Mono<User> getUser(String userId) {
        return userRepository.findById(userId);
    }

    public Mono<User> addUser(String name) throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        keyGen.initialize(1024, random);
        KeyPair pair = keyGen.generateKeyPair();
        PrivateKey privateKey = pair.getPrivate();
        PublicKey publicKey = pair.getPublic();
        return userRepository.save(User.builder()
                .name(name)
                .address(UUID.randomUUID().toString())
                .privateKey(Base64.getEncoder().encodeToString(privateKey.getEncoded()))
                .publicKey(Base64.getEncoder().encodeToString(publicKey.getEncoded()))
                .build());
    }

    public Mono<User> getUserByName(String userName) {
        return userRepository.findByName(userName);
    }

    public Mono<Long> addTransaction(User user,
                                       String inputContract,
                                       String outputContract,
                                       String outputAddress)
    {
        PrivateKey privateKey;
        try {
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
                    Base64.getDecoder().decode(user.getPrivateKey())
            );
            KeyFactory keyFactory = KeyFactory.getInstance("DSA");
            privateKey = keyFactory.generatePrivate(privateKeySpec);
        } catch (Exception e) {
            throw new RuntimeException("error reading key", e);
        }
        Transaction transaction = Transaction.builder()
                .transactionId(random.nextLong())
                .data(Transaction.Data.builder()
                        .input(Transaction.Input.builder()
                                .address(user.getAddress())
                                .contract(inputContract)
                                .build()
                                .withSignature(privateKey))
                        .outputs(List.of(Transaction.Output.builder()
                                .address(outputAddress)
                                .contract(outputContract)
                                .build()))
                        .build())
                .build()
                .withHash();
        return client
                .post()
                .uri(transactionUrl + "/" + transaction.getTransactionId())
                .body(Mono.just(transaction), Transaction.class)
                .exchange()
                .flatMap(cr -> cr.bodyToMono(Transaction.class))
                .doOnNext(t -> log.info("transaction {} added", t))
                .map(t -> t.getTransactionId());

    }
}
