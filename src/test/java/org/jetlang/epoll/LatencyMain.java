package org.jetlang.epoll;

import sun.misc.Unsafe;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LatencyMain {

    interface Factory {
        Runnable create(DatagramChannel rcv, int msgCount, CountDownLatch latch);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        long seed = Long.parseLong(args[1]);
        final Factory f;
        switch (args[0].toLowerCase()) {
            case "epoll":
                f = LatencyMain::createEpoll;
                break;
            case "nio":
                f = LatencyMain::createNio;
                break;
            default:
                throw new RuntimeException("unknown: " + args[0]);
        }
        ;
        System.out.println("seed = " + seed);
        run(f, 5_000_000, seed, 5);
        for (int i = 0; i < 10; i++) {
            run(f, 1000, seed, 500);
        }
    }

    private static void run(Factory f, int msgCount, long seed, int maxSleepMicros) throws IOException, InterruptedException {
        Random random = new Random(seed);
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
            long sentNanos = System.nanoTime();
            int sleepTimeNanos = random.nextInt((int) TimeUnit.MICROSECONDS.toNanos(maxSleepMicros));
            while (System.nanoTime() - sentNanos < sleepTimeNanos) {
                Thread.yield();
            }
        }

        boolean result = latch.await(5, TimeUnit.MINUTES);
        e.run();

        try {
            rcv.close();
        } catch (IOException closeFailed) {
            closeFailed.printStackTrace();
        }
        if (!result) {
            throw new RuntimeException("Failed to complete: " + f);
        }
    }

    private static Runnable createEpoll(DatagramChannel rcv, int msgCount, CountDownLatch latch) {
        EPoll e = new EPoll("epoll", 1, 16, 8, 0);
        e.start();
        DatagramReader datagramReader = new DatagramReader() {
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
        };
        e.register(rcv, datagramReader);
        return () -> {
            boolean result = e.awaitClose(10_000);
            if (!result) {
                throw new RuntimeException("Thread failed to exit: " + e.getThread());
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
                            int keys = s.selectNow();
                            if (keys > 0) {
                                Set<SelectionKey> selected = s.selectedKeys();
                                while (running && rcv.receive(b) != null) {
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
