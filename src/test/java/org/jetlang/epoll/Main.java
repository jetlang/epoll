package org.jetlang.epoll;

import java.io.IOException;
import java.nio.channels.DatagramChannel;

public class Main {

    public static void main(String[] args) throws IOException {
        EPoll e = new EPoll("epoll", 5, 16, 1024 * 8);
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.close();
        e.close();
    }
}
