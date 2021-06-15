package net.kamradtfamily.blockchain.api;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface BlockRepository  extends ReactiveMongoRepository<Block, String> {
    Mono<Block> findByIndex(Long index);
    Mono<Block> findByHash(String hash);
}
