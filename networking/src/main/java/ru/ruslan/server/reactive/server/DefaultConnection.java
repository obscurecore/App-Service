package ru.ruslan.server.reactive.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import pl.touk.throwing.ThrowingBiConsumer;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import static pl.touk.throwing.ThrowingConsumer.unchecked;
import static pl.touk.throwing.ThrowingBiConsumer.unchecked;

public class DefaultConnection extends BaseSubscriber<ByteBuffer> implements Connection {
    /*
     Little bit data or information to this connection about outside world
        first of all what verse to mention is that connection will be kind of you will be responsed
        for data reading and data writing, and we don't want to move this responsibility to the main server engine
        because it will just confuse

     We are going to ask main server for some kind of main required field

     And also we are going to ask in order to kind of decouple the serer engine and our kind of source
        of selection from our connection. we want to have some notifications or way to listen to some new
        notifications or actions from main engine and since this is kind of stream of updates. we can use the same
        kind pf programming model using project reactor and basically when we want to listen to for example
        read updates ot to ability read from the connection we want to subscribe to some stream of data(the same for writers
        if you want to listen to some writes updates or for some notification that we can write data to connection to floods of
        another selection keys
    */

    final SocketChannel socketChannel;
    // In order have to listen to readnotify
    final Flux<SelectionKey> readNotifier;
    final Flux<SelectionKey> writeNotifier;
    //region Complex Params
    final Scheduler scheduler;

    volatile SelectionKey currentSelectionKey;

    ByteBuffer current;
    //endregion

    DefaultConnection(
            SocketChannel socketChannel,
            SelectionKey initialSelectionKey,
            Flux<SelectionKey> readNotifier,
            Flux<SelectionKey> writeNotifier
    ) {
        this.socketChannel = socketChannel;
        this.readNotifier = readNotifier;
        this.writeNotifier = writeNotifier;
        this.currentSelectionKey = initialSelectionKey;
        //region Attach to Scheduler
        this.scheduler = Schedulers.single(Schedulers.parallel());
        //endregion
    }

    @Override
    public void close() {
        var key = currentSelectionKey;

        if (key != null) {
            dispose();
            currentSelectionKey = null;
            key.cancel();
        }
    }

    /*
        in order to receive we have to listen *readnotify
        so once we got the notifications that we can read something from the connection
         we should start read from it
     */
    @Override
    public Flux<ByteBuffer> receive() {
        return readNotifier
                /*
                On te other hand to start listening for read event we have register kind of listener
                we have do remember that part (as in nio... define, say i want to listen to read updates)

                but the main question since we have everything reactive and if you going to take a look
                at our business logic(*MAIN) and this business logic could be written in absolutely different
                very mad.
                for example we can delay our subscription, we can do some synchronous action, we can send, create some proxy server
                which will send data to another server and of course we don't know when we get for exapmple when the endpoint
                or consumer logic will be able to start listening for the data. that's why we don't want to start
                listening too early, so we don't want registry too early for incoming read notifications and that's why we
                want a register only and only when the kind of our business logic sent us a subscription or notification
                that it's ready



                The convenient way to listen to that action is of course by adding operator on subscribe
                    and what it gives us - so once someone subscribe to our kind of stream we
                    are absolutely to start doing something
                    and this will be main point and the main kind of start
                    point for our listening to read notifications
                 */
                .doOnSubscribe(unchecked(__ -> {
                    var selector = currentSelectionKey.selector();

                    socketChannel.register(selector, SelectionKey.OP_READ);

                    selector.wakeup();
                }))
                //region Complex Receive Pipe
                .onBackpressureLatest()
                .doOnNext(sk -> currentSelectionKey = sk)
                .publishOn(scheduler)
                .doOnCancel(this::close)
                //endregion
                /*
                Main logic that allows us to read few bytes from the channel
                 */
                .handle(unchecked(new ThrowingBiConsumer<SelectionKey, SynchronousSink<ByteBuffer>, IOException>() {
                    @Override
                    public void accept(SelectionKey sk, SynchronousSink<ByteBuffer> sink) throws IOException {
                        var buffer = ByteBuffer.allocateDirect(1024);
                        var read = socketChannel.read(buffer);

                        if (read > 0) {
                            //try to send
                            sink.next(buffer.flip());
                        }
                    }
                }));
    }

    @Override
    public Mono<Void> send(Publisher<ByteBuffer> dataStream) {
        return Mono
                .<Void>fromRunnable(() ->
                        Flux.from(dataStream)
                                .subscribe(this)
                )
                .doOnCancel(this::close);
    }

    @Override
    protected void hookOnSubscribe(Subscription subscription) {
        subscription.request(1);
        writeNotifier
                //region Complex Write Pipe
                .doOnNext(sk -> {
                    currentSelectionKey = sk;
                    sk.interestOps(SelectionKey.OP_READ);
                })
                .publishOn(scheduler)
                //endregion
                .subscribe(__ -> hookOnNext(current));
    }

    @Override
    protected void hookOnNext(ByteBuffer buffer) {
        int result;

        try {
            result = socketChannel.write(buffer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (result == -1) {
            cancel();
        }

        if (buffer.hasRemaining()) {
            current = buffer;
            var key = currentSelectionKey;

            key.interestOps(SelectionKey.OP_WRITE);
            key.selector().wakeup();

            return;
        }

        request(1);
    }
}
