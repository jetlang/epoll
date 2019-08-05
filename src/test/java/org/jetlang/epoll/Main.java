package org.jetlang.epoll;

import sun.misc.Unsafe;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        DatagramChannel rcv = DatagramChannel.open();
        rcv.socket().bind(new InetSocketAddress(9999));
        rcv.configureBlocking(false);

        DatagramChannel sender = DatagramChannel.open();
        EPoll e = new EPoll("epoll", 5, 16, 1024 * 8);
        e.start();
        CountDownLatch latch = new CountDownLatch(2);
        e.register(rcv, new DatagramReader() {
            @Override
            public EventResult onRead(Unsafe unsafe, long readBufferAddress) {
                System.out.println("unsafe.getLong(readBufferAddress) = " + unsafe.getLong(readBufferAddress));
                latch.countDown();
                return EventResult.Remove;
            }

            @Override
            public void onRemove() {
                try {
                    System.out.println("closing " + rcv);
                    rcv.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        e.execute(latch::countDown);
        InetSocketAddress target = new InetSocketAddress("wud-mrettig02", 9999);
        ByteBuffer buf = ByteBuffer.allocate(8);
        for(int i = 0; i < 20; i++){
            buf.clear();
            buf.put((byte)String.valueOf(i).charAt(0));
            buf.flip();
            sender.send(buf, target);
        }

        System.out.println("latch.await(5, TimeUnit.SECONDS) = " + latch.await(30, TimeUnit.SECONDS));
        e.close();
    }
}
