package net.kamradtfamily.blockchain;

import reactor.core.publisher.Mono;

import java.util.List;

public class Node {
    public List<Node> peers;
    public Mono<Void> confirm(String id) {
        return Mono.empty();
    }

    public Node connectToPeer(Node newNode) {
        return newNode;
    }
}
