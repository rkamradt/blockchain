package net.kamradtfamily.blockchain.miner;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class UserService {
    final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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

    public Mono<User> addUser(String name) {
        return userRepository.save(User.builder()
                .name(name)
                .build());
    }

    public Mono<User> getUserByName(String userName) {
        return userRepository.findByName(userName);
    }
}
