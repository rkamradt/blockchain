package net.kamradtfamily.blockchain.api;

import org.reactivestreams.Publisher;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MockBlockRepository implements BlockRepository {
    @Override
    public Mono<Block> findByIndex(Long index) {
        return null;
    }

    @Override
    public Mono<Block> findByHash(String hash) {
        return null;
    }

    @Override
    public <S extends Block> Mono<S> insert(S s) {
        return null;
    }

    @Override
    public <S extends Block> Flux<S> insert(Iterable<S> iterable) {
        return null;
    }

    @Override
    public <S extends Block> Flux<S> insert(Publisher<S> publisher) {
        return null;
    }

    @Override
    public <S extends Block> Mono<S> findOne(Example<S> example) {
        return null;
    }

    @Override
    public <S extends Block> Flux<S> findAll(Example<S> example) {
        return null;
    }

    @Override
    public <S extends Block> Flux<S> findAll(Example<S> example, Sort sort) {
        return null;
    }

    @Override
    public <S extends Block> Mono<Long> count(Example<S> example) {
        return null;
    }

    @Override
    public <S extends Block> Mono<Boolean> exists(Example<S> example) {
        return null;
    }

    @Override
    public Flux<Block> findAll(Sort sort) {
        return null;
    }

    @Override
    public <S extends Block> Mono<S> save(S s) {
        return null;
    }

    @Override
    public <S extends Block> Flux<S> saveAll(Iterable<S> iterable) {
        return null;
    }

    @Override
    public <S extends Block> Flux<S> saveAll(Publisher<S> publisher) {
        return null;
    }

    @Override
    public Mono<Block> findById(String s) {
        return null;
    }

    @Override
    public Mono<Block> findById(Publisher<String> publisher) {
        return null;
    }

    @Override
    public Mono<Boolean> existsById(String s) {
        return null;
    }

    @Override
    public Mono<Boolean> existsById(Publisher<String> publisher) {
        return null;
    }

    @Override
    public Flux<Block> findAll() {
        return null;
    }

    @Override
    public Flux<Block> findAllById(Iterable<String> iterable) {
        return null;
    }

    @Override
    public Flux<Block> findAllById(Publisher<String> publisher) {
        return null;
    }

    @Override
    public Mono<Long> count() {
        return null;
    }

    @Override
    public Mono<Void> deleteById(String s) {
        return null;
    }

    @Override
    public Mono<Void> deleteById(Publisher<String> publisher) {
        return null;
    }

    @Override
    public Mono<Void> delete(Block block) {
        return null;
    }

    @Override
    public Mono<Void> deleteAll(Iterable<? extends Block> iterable) {
        return null;
    }

    @Override
    public Mono<Void> deleteAll(Publisher<? extends Block> publisher) {
        return null;
    }

    @Override
    public Mono<Void> deleteAll() {
        return null;
    }
}
