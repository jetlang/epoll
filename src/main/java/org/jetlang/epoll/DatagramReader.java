package org.jetlang.epoll;

import sun.misc.Unsafe;

public interface DatagramReader {

    EventResult onRead(Unsafe unsafe, long readBufferAddress);

    void onRemove();


    public class Factory implements EventConsumer.Factory {

        private final DatagramReader reader;

        public Factory(DatagramReader reader) {
            this.reader = reader;
        }

        @Override
        public EventConsumer create(int fd, Unsafe unsafe, EPoll.Controls c, long[] readBufferAddress) {
            return new EventConsumer() {
                @Override
                public EventResult onEvent() {
                    for (int numRecv = c.receive(fd); numRecv > 0; numRecv = c.receive(fd)) {
                        for (int i = 0; i < numRecv; i++) {
                            EventResult r = reader.onRead(unsafe, readBufferAddress[i]);
                            if (r == EventResult.Remove) {
                                return r;
                            }
                        }
                    }
                    return EventResult.Continue;
                }

                @Override
                public void onRemove() {
                    reader.onRemove();
                }
            };
        }
    }
}
