package org.jetlang.epoll;

import sun.misc.Unsafe;

public interface DatagramReader {

    EventResult onRead(Unsafe unsafe, long readBufferAddress);

    void onRemove();
}
