# Jetlang Epoll
optimized jni epoll wrapper

## Features
 * Zero Allocations 
 * Zero Copies
 * Multi-message receive using  [recvmmsg](http://man7.org/linux/man-pages/man2/recvmmsg.2.html)
 * Easier to use Api compared to Nio

## Limitations
 * UDP only (unicast & multicast). Tcp support is in development.
 * Linux Only
 * JDK 8+
 
## Building
```
make all JAVA_HOME=/path/to/your/jdk1.8
``` 
 
## Example 
```java
DatagramChannel rcv = DatagramChannel.open();
rcv.socket().bind(new InetSocketAddress(9999));
rcv.configureBlocking(false);
EPoll e = new EPoll("epoll", 5, 16, 1024 * 2, new PollStrategy.Wait(), EventBatch.NO_OP);
e.start();
e.register(rcv, new DatagramReader() {
    @Override
    public EventResult readPackets(int numRecv, EPoll.Packet[] pkts) {
        for (int i = 0; i < numRecv; i++) {
            EPoll.Packet pkt = pkts[i];
            System.out.println(i + "/" + numRecv +" packet length " + pkt.getLength());
            System.out.println("first byte of msg: " + pkt.unsafe.getByte(pkt.bufferAddress));
        }
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

//The Epoll instance is also an executor
e.execute(()-> System.out.println("Hello on epoll thread."));

```   
