# Jetlang Epoll
optimized jni epoll wrapper

## Features
 * Zero Allocations 
 * Zero Copies
 * Multi-message receive using  [recvmmsg](http://man7.org/linux/man-pages/man2/recvmmsg.2.html)
 * Easier to use Api compared to Nio
 
```java
DatagramChannel rcv = DatagramChannel.open();
rcv.socket().bind(new InetSocketAddress(9999));
rcv.configureBlocking(false);
EPoll e = new EPoll("epoll", 5, 16, 1024 * 2, new PollStrategy.Wait(), EventBatch.NO_OP);
e.start();
e.register(rcv, new DatagramReader() {
    @Override
    public EventResult readPackets(int numRecv, EPoll.Packet[] pkts) {
        System.out.println("Received " + numRecv + " Packets!");
        return EventResult.Continue;
    }

    @Override
    public void onRemove() {
        try {
            rcv.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
});

```   
