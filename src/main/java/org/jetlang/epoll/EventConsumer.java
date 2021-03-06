package org.jetlang.epoll;

import sun.misc.Unsafe;

public interface EventConsumer {

    EventResult onEvent(int events);

    void onRemove();

    interface Factory {
        EventConsumer create(int fd, Unsafe unsafe, EPoll.Controls controls, EPoll.Packet[] pkts);
    }
}
