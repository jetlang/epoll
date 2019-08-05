package org.jetlang.epoll;

import sun.misc.Unsafe;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        DatagramChannel channel = DatagramChannel.open();
        channel.socket().bind(new InetSocketAddress(9999));
        channel.configureBlocking(false);
        EPoll e = new EPoll("epoll", 5, 16, 1024 * 8);
        e.start();
        CountDownLatch latch = new CountDownLatch(2);
        e.register(channel, new DatagramReader() {
            @Override
            public EventResult onRead(Unsafe unsafe, long readBufferAddress) {
                latch.countDown();
                return EventResult.Remove;
            }

            @Override
            public void onRemove() {
                try {
                    System.out.println("closing " + channel);
                    channel.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        e.execute(latch::countDown);
        System.out.println("latch.await(5, TimeUnit.SECONDS) = " + latch.await(5, TimeUnit.SECONDS));
        e.close();
    }
}
