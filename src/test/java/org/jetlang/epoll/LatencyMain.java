package org.jetlang.epoll;

import sun.misc.Unsafe;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LatencyMain {

    interface Factory {
        Runnable create(DatagramChannel rcv, int msgCount, CountDownLatch latch);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        run(LatencyMain::createNio, 5_000_000, 10_000, 10);
        run(LatencyMain::createEpoll, 5_000_000, 10_000, 10);

        for (int i = 0; i < 10; i++) {
            run(LatencyMain::createNio, 1000, 1, 1);
            run(LatencyMain::createEpoll, 1000, 1, 1);
        }
    }

    private static void run(Factory f, int msgCount, int sleepInterval, int sleepMs) throws IOException, InterruptedException {
        DatagramChannel rcv = DatagramChannel.open();
        rcv.socket().bind(new InetSocketAddress(9999));
        rcv.socket().setReceiveBufferSize(1024 * 1024);
        rcv.configureBlocking(false);

        CountDownLatch latch = new CountDownLatch(1);

        var e = f.create(rcv, msgCount, latch);
        Thread.sleep(1_000);

        DatagramChannel sender = DatagramChannel.open();
        InetSocketAddress target = new InetSocketAddress("127.0.0.1", 9999);
        ByteBuffer buf = ByteBuffer.allocateDirect(8).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < msgCount; i++) {
            buf.clear();
            buf.putLong(System.nanoTime());
            buf.flip();
            sender.send(buf, target);
            if (i % sleepInterval == 0) {
                Thread.sleep(sleepMs);
            }
        }

        latch.await(5, TimeUnit.MINUTES);
        e.run();

        try {
            rcv.close();
        } catch (IOException closeFailed) {
            closeFailed.printStackTrace();
        }
    }

    private static Runnable createEpoll(DatagramChannel rcv, int msgCount, CountDownLatch latch) {
        EPoll e = new EPoll("epoll", 1, 16, 8);
        e.start();
        e.register(rcv, new DatagramReader() {
            int cnt = 0;
            long totalLatency = 0;

            @Override
            public EventResult onRead(Unsafe unsafe, long readBufferAddress) {
                totalLatency += (System.nanoTime() - unsafe.getLong(readBufferAddress));
                if (++cnt == msgCount) {
                    latch.countDown();
                    System.out.println("Epoll Receive nanos " + (totalLatency / msgCount));
                }
                return EventResult.Continue;
            }

            @Override
            public void onRemove() {
            }
        });
        return () -> {
            e.close();
            try {
                e.getThread().join(10_000);
            } catch (InterruptedException ex) {

            }
        };
    }

    private static Runnable createNio(DatagramChannel rcv, final int msgCount, CountDownLatch latch) {
        try {
            Selector s = Selector.open();
            rcv.register(s, SelectionKey.OP_READ);
            Thread t = new Thread() {
                @Override
                public void run() {
                    final ByteBuffer b = ByteBuffer.allocateDirect(8).order(ByteOrder.LITTLE_ENDIAN);
                    int count = 0;
                    long latency = 0;
                    boolean running = true;
                    while (running) {
                        try {
                            int keys = s.select();
                            if (keys > 0) {
                                Set<SelectionKey> selected = s.selectedKeys();
                                while (rcv.receive(b) != null && running) {
                                    b.flip();
                                    latency += (System.nanoTime() - b.getLong());
                                    b.clear();
                                    if (++count == msgCount) {
                                        latch.countDown();
                                        System.out.println("Nio Receive nanos " + (latency / msgCount));
                                        running = false;
                                    }
                                }
                                selected.clear();
                            } else {
                                System.err.println(keys + " selected ");
                            }
                        } catch (Exception failed) {
                            failed.printStackTrace();
                        }
                    }
                }
            };
            t.start();
            return () -> {
                try {
                    s.close();
                } catch (IOException e) {

                }
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
