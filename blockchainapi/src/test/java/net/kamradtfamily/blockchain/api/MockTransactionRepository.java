package net.kamradtfamily.blockchain.api;

import org.reactivestreams.Publisher;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MockTransactionRepository implements TransactionRepository {
    @Override
    public Mono<Transaction> findByTransactionId(long transactionId) {
        return null;
    }

    @Override
    public <S extends Transaction> Mono<S> insert(S s) {
        return null;
    }

    @Override
    public <S extends Transaction> Flux<S> insert(Iterable<S> iterable) {
        return null;
    }

    @Override
    public <S extends Transaction> Flux<S> insert(Publisher<S> publisher) {
        return null;
    }

    @Override
    public <S extends Transaction> Mono<S> findOne(Example<S> example) {
        return null;
    }

    @Override
    public <S extends Transaction> Flux<S> findAll(Example<S> example) {
        return null;
    }

    @Override
    public <S extends Transaction> Flux<S> findAll(Example<S> example, Sort sort) {
        return null;
    }

    @Override
    public <S extends Transaction> Mono<Long> count(Example<S> example) {
        return null;
    }

    @Override
    public <S extends Transaction> Mono<Boolean> exists(Example<S> example) {
        return null;
    }

    @Override
    public Flux<Transaction> findAll(Sort sort) {
        return null;
    }

    @Override
    public <S extends Transaction> Mono<S> save(S s) {
        return null;
    }

    @Override
    public <S extends Transaction> Flux<S> saveAll(Iterable<S> iterable) {
        return null;
    }

    @Override
    public <S extends Transaction> Flux<S> saveAll(Publisher<S> publisher) {
        return null;
    }

    @Override
    public Mono<Transaction> findById(String s) {
        return null;
    }

    @Override
    public Mono<Transaction> findById(Publisher<String> publisher) {
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
    public Flux<Transaction> findAll() {
        return null;
    }

    @Override
    public Flux<Transaction> findAllById(Iterable<String> iterable) {
        return null;
    }

    @Override
    public Flux<Transaction> findAllById(Publisher<String> publisher) {
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
    public Mono<Void> delete(Transaction transaction) {
        return null;
    }

    @Override
    public Mono<Void> deleteAll(Iterable<? extends Transaction> iterable) {
        return null;
    }

    @Override
    public Mono<Void> deleteAll(Publisher<? extends Transaction> publisher) {
        return null;
    }

    @Override
    public Mono<Void> deleteAll() {
        return null;
    }
}
