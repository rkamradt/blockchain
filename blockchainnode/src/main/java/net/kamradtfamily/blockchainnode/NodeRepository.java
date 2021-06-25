package net.kamradtfamily.blockchainnode;


import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface NodeRepository extends ReactiveMongoRepository<Node, String> {
    Mono<Node> findByUrl(String url);
}
