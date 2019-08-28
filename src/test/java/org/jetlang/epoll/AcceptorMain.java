package org.jetlang.epoll;

import sun.misc.Unsafe;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AcceptorMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.bind(new InetSocketAddress(8080));

        EPoll e = new EPoll("epoll", 5, 16, 1024 * 8, new PollStrategy.Wait(), EventBatch.NO_OP);
        e.start();
        CountDownLatch latch = new CountDownLatch(1);
        e.register(server, EPoll.EventTypes.EPOLLIN.value, (fd, unsafe, controls, pkts) -> new EventConsumer() {
            @Override
            public EventResult onEvent() {
                try {
                    SocketChannel client = server.accept();
                    client.configureBlocking(false);
                    System.out.println("accept = " + client);
                    e.register(client, EPoll.EventTypes.EPOLLIN.value, (fd1, unsafe1, controls1, pkts1) -> new EventConsumer() {
                        ByteBuffer buffer = ByteBuffer.allocate(1024 * 8);
                        @Override
                        public EventResult onEvent() {
                            try {
                                int read = client.read(buffer);
                                if( read < 0){
                                    return EventResult.Remove;
                                }
                                if(read > 0){
                                    buffer.flip();
                                    String s = new String(buffer.array(), 0, read).trim();
                                    System.out.println("msg = " + s);
                                    if(s.equals("close")){
                                        return EventResult.Remove;
                                    }
                                    if(s.equals("echo")){
                                        client.write(buffer);
                                    }
                                    buffer.clear();
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                                return EventResult.Remove;
                            }
                            return EventResult.Continue;
                        }

                        @Override
                        public void onRemove() {
                            System.out.println("onRemove");
                            try {
                                client.close();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            latch.countDown();
                        }
                    });
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return EventResult.Remove;
                }
                return EventResult.Continue;
            }

            @Override
            public void onRemove() {
                try {
                    server.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        latch.await(1, TimeUnit.HOURS);
        e.close();
        System.out.println("Finished");
    }
}
