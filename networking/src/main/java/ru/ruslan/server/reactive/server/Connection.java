package ru.ruslan.server.reactive.server;

import java.nio.ByteBuffer;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface Connection extends AutoCloseable {
    // Allow listen incoming byte buffers
    Flux<ByteBuffer> receive();
    // Second will simply allows to send those data back to the stream
    Mono<Void> send(Publisher<ByteBuffer> dataStream);
}
