package net.kamradtfamily.blockchain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class Emitter {
    Map<String, Consumer<Object>> handlers = new HashMap<>();
    public void addHandler(String message, Consumer<Object> handler) {
        handlers.put(message, handler);
    }
    public void emit(String message, Object args) {
        if(!handlers.containsKey(message)) {
            log.error("message {} emitted without handler", message);
            return;
        }
        Mono.just(args)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(a -> log.info("handling message {}", message))
                .subscribe(a -> handlers.get(message).accept(a));
    }
}
