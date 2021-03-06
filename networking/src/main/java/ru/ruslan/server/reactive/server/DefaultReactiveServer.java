package ru.ruslan.server.reactive.server;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.function.Function;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import reactor.core.scheduler.Schedulers;
import reactor.util.concurrent.Queues;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import static pl.touk.throwing.ThrowingConsumer.unchecked;
import static pl.touk.throwing.ThrowingRunnable.unchecked;

public class DefaultReactiveServer implements ReactiveServer {

    private Function<Connection, Mono<Void>> connectionsHandler;

    private final InetSocketAddress address;

    DefaultReactiveServer(String host, int port) {
        this.address = InetSocketAddress.createUnresolved(host, port);
    }

    @Override
    public ReactiveServer handle(Function<Connection, Mono<Void>> connectionsHandler) {
        this.connectionsHandler = connectionsHandler;
        return this;
    }
/*
 In order to since everything reactive and we have to return our kind of server
       we have to make a startup of the server async we have to wrtie
 */
    @Override
    public Mono<Void> start() {
        /*
       1*
       Convenient way yo listen some subscribers.
            So we want to create some source of data in
            that case which will push new connections to downstream
         */
        return Flux.push(unchecked(sink -> {
                var connections = new HashMap<SocketChannel, Tuple2<FluxSink<SelectionKey>, FluxSink<SelectionKey>>>();

                //startup socket channel
                var server = ServerSocketChannel
                    .open()
                    .bind(new InetSocketAddress(address.getHostName(), address.getPort()));

                server.configureBlocking(false);

                var selector = Selector.open();

                server.register(selector, SelectionKey.OP_ACCEPT);

                sink.onDispose(unchecked(server::close)::run);
                /*
                simplementing some plain nio interaction

                */
                //Listening cancellation
                while (!sink.isCancelled()) {
                    selector.select(unchecked(key -> {
                        if (key.isValid()) {
                            if (key.isAcceptable()) {
                                /*Socket Accepting
                                // Return socket channel which represent connection( and it's some
                                     wrapper around socket channel
                                */
                                var sc = server.accept();

                                sc.configureBlocking(false);

                                //region Complex Fluxes Creation
                                var readsProcessor = UnicastProcessor.create(Queues.<SelectionKey>one().get());
                                var writesProcessor = UnicastProcessor.create(Queues.<SelectionKey>one().get());
                                //endregion

                                connections.put(
                                    sc,
                                    //region Value
                                    Tuples.of(readsProcessor.sink(), writesProcessor.sink())
                                    //endregion
                                );

                                sink.next(connectionsHandler.apply(new DefaultConnection( //region Connection Params
                                    sc,
                                    key,
                                    readsProcessor,
                                    writesProcessor
                                    //endregion
                                )).subscribe());
                                //endregion
                            }
                            else if (key.isReadable()) {
                                //region Read Notification
                                connections.get(key.channel())
                                           .getT1()
                                           .next(key);
                                //endregion
                            }
                            else if (key.isWritable()) {
                                //region Write Notification
                                connections.get(key.channel())
                                           .getT2()
                                           .next(key);
                                //endregion
                            }
                        }
                    }));
                }
            }))
                /*
                we want to run our kind of main business logic hidden inside out push
                    or inside our kind of source on different thread for do that we create
                    another scheduler. so scheduler is a part of project reactor which
                    allows us to create a thread pool. the thread pool on which this part (1*) will be ran


                 */
            .subscribeOn(Schedulers.newSingle(DefaultReactiveServer.class.getSimpleName()))
            .collectList()
            .doOnDiscard(Disposable.class, Disposable::dispose)
            /*
            then - we just ignore everything and convert our flux to mono and that's it
             */
                .then();
    }
}
