package org.jetlang.epoll;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        EPoll e = new EPoll("epoll", 5, 16, 1024 * 8);
        e.start();
        CountDownLatch latch = new CountDownLatch(1);
        e.execute(latch::countDown);
        System.out.println("latch.await(5, TimeUnit.SECONDS) = " + latch.await(5, TimeUnit.SECONDS));
        e.close();
    }
}
