package org.jetlang.epoll;

import sun.misc.Unsafe;

public interface DatagramReader {

    EventResult readPackets(int numRecv, EPoll.Packet[] pkts);

    void onRemove();


    public class Factory implements EventConsumer.Factory {

        private final DatagramReader reader;

        public Factory(DatagramReader reader) {
            this.reader = reader;
        }

        @Override
        public EventConsumer create(int fd, Unsafe unsafe, EPoll.Controls c, EPoll.Packet[] pkts) {
            return new EventConsumer() {
                @Override
                public EventResult onEvent() {
                    for (int numRecv = c.receive(fd); numRecv > 0; numRecv = c.receive(fd)) {
                        EventResult r = reader.readPackets(numRecv, pkts);
                        if (r == EventResult.Remove) {
                            return r;
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
