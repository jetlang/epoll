package org.jetlang.epoll;

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

    public static class Stats {

        private final String name;
        private final int msgCount;
        long maxLatency = 0;
        int cnt = 0;
        long totalLatency = 0;

        public Stats(String name, int msgCount) {
            this.name = name;
            this.msgCount = msgCount;
        }

        public void record(long nanos) {
            long latency = System.nanoTime() - nanos;
            this.maxLatency = Math.max(latency, maxLatency);
            this.totalLatency += latency;
            cnt++;
        }

        public boolean isComplete() {
            return cnt == msgCount;
        }

        public void logStats() {
            System.out.println(name + " Avg " + (totalLatency / msgCount) + " max " + maxLatency);
        }
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
        run(f, 1_000_000, seed, 50);
        for (int i = 0; i < 10; i++) {
            run(f, 100_000, seed, 100);
        }
        run(f, 1_000_000, seed, 50);
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
            if (maxSleepMicros > 0) {
                long sentNanos = System.nanoTime();
                int sleepTimeNanos = random.nextInt((int) TimeUnit.MICROSECONDS.toNanos(maxSleepMicros));
                while (System.nanoTime() - sentNanos < sleepTimeNanos) {
                    Thread.yield();
                }
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
        PollStrategy poller = new PollStrategy.SpinWait(1000);
        //PollStrategy poller = new PollStrategy.Wait();
        EPoll e = new EPoll("epoll", 1, 16, 8, poller);
        e.start();
        DatagramReader datagramReader = new DatagramReader() {
            private final Stats s = new Stats("epoll", msgCount);

            @Override
            public EventResult readPackets(int numRecv, EPoll.Packet[] pkts) {
                for (int i = 0; i < numRecv; i++) {
                    EPoll.Packet pkt = pkts[i];
                    s.record(pkt.unsafe.getLong(pkt.bufferAddress));
                    if (s.isComplete()) {
                        latch.countDown();
                        s.logStats();
                    }
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
                    boolean running = true;
                    Stats stats = new Stats("nio", msgCount);
                    while (running) {
                        try {
                            int keys = s.select();
                            if (keys > 0) {
                                Set<SelectionKey> selected = s.selectedKeys();
                                while (running && rcv.receive(b) != null) {
                                    b.flip();
                                    stats.record(b.getLong());
                                    b.clear();
                                    if (stats.isComplete()) {
                                        latch.countDown();
                                        stats.logStats();
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
