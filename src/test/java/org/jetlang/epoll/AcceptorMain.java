package org.jetlang.epoll;

import sun.misc.Unsafe;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class AcceptorMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        ServerSocketChannel rcv = ServerSocketChannel.open();
        rcv.configureBlocking(false);
        rcv.bind(new InetSocketAddress(8080));

        EPoll e = new EPoll("epoll", 5, 16, 1024 * 8, new PollStrategy.Wait(), EventBatch.NO_OP);
        e.start();
        e.register(rcv, EPoll.EventTypes.EPOLLIN.value, new EventConsumer.Factory() {
            @Override
            public EventConsumer create(int fd, Unsafe unsafe, EPoll.Controls controls, EPoll.Packet[] pkts) {
                return new EventConsumer() {
                    @Override
                    public EventResult onEvent() {
                        try {
                            SocketChannel accept = rcv.accept();
                            System.out.println("accept = " + accept);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            return EventResult.Remove;
                        }
                        return EventResult.Continue;
                    }

                    @Override
                    public void onRemove() {
                        try {
                            rcv.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                };
            }
        });
        Thread.sleep(60 * 1000);
        e.close();
    }
}
