package org.jetlang.epoll;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public class EPoll implements Executor {

    private static final int EVENT_SIZE = 8 + 4 + 4 + 8;

    private static final Unsafe unsafe;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        }catch(Exception failed){
            throw new ExceptionInInitializerError(failed);
        }
        System.loadLibrary("jetlang-epoll");
    }

    private final Object lock = new Object();
    private final long ptrAddress;
    private final long readBufferAddress;
    private final long eventArrayAddress;
    private final Thread thread;
    private boolean running = true;
    private ArrayList<Runnable> pending = new ArrayList<>();
    private final ArrayList<State> unused = new ArrayList<>();
    private final ArrayList<State> fds = new ArrayList<State>();
    private final Map<Integer, State> stateMap = new HashMap<>();
    private final AtomicBoolean started = new AtomicBoolean();

    private static class State {

        public int fd;
        public final int idx;

        public State(int idx) {
            this.idx = idx;
            this.fd = fd;
        }

        public void onEvent(Unsafe unsafe, long readBufferAddress) {

        }
    }

    public EPoll(String threadName, int maxSelectedEvents, int maxDatagramsPerRead, int readBufferBytes){
        this.ptrAddress = init(maxSelectedEvents, maxDatagramsPerRead, readBufferBytes);
        this.readBufferAddress = getReadBufferAddress(ptrAddress);
        this.eventArrayAddress = getEventArrayAddress(ptrAddress);
        Runnable eventLoop = () -> {
            ArrayList<Runnable> swap = new ArrayList<>();
            while(running){
                int events = select(ptrAddress, -1);
                for(int i = 0; i < events; i++){
                    int idx = unsafe.getInt(eventArrayAddress + EVENT_SIZE * i);
                    fds.get(idx).onEvent(unsafe, readBufferAddress);
                }
                synchronized (lock){
                    ArrayList<Runnable> tmp = pending;
                    pending = swap;
                    swap = tmp;
                }
                for(int i = 0, size = swap.size(); i < size; i++){
                    runEvent(swap.get(i));
                }
            }
            freeNativeMemory(ptrAddress);
        };
        this.thread = new Thread(eventLoop, threadName);
    }

    public void start(){
        if(started.compareAndExchange(false, true)) {
            thread.start();
        }
    }

    public void close(){
        if(started.compareAndExchange(false, true)){
            freeNativeMemory(ptrAddress);
        }
        else {
            execute(()->{
                running = false;
            });
        }
    }


    protected void runEvent(Runnable runnable) {
        runnable.run();
    }

    private native int select(long ptrAddress, int timeout);

    private static native long getEventArrayAddress(long ptrAddress);

    private static native long getReadBufferAddress(long ptrAddress);

    private static native long init(int maxSelectedEvents, int maxDatagramsPerRead, int readBufferBytes);

    private static native void freeNativeMemory(long ptrAddress);

    private static native void interrupt(long ptrAddress);

    private static native int ctl(long ptrAddress, int op, int eventTypes, int fd, int idx);

    public Runnable register(DatagramChannel channel, DatagramReader reader){
        int fd = 1; //channel.socket().
        execute(() -> {
            State e = claimState();
            e.fd = fd;
            ctl(ptrAddress, Ops.Add.ordinal(), EventTypes.EPOLLIN.ordinal(), fd, e.idx);
            stateMap.put(fd, e);
        });
        return ()->{
            execute(()->{
                State st = stateMap.remove(fd);
                if(st != null){
                    ctl(ptrAddress, Ops.Del.ordinal(), 0, fd, st.idx);
                    unused.add(st);
                }
            });
        };
    }

    private State claimState() {
        if(!unused.isEmpty()){
            return unused.remove(unused.size() -1);
        }else {
            State st = new State(fds.size());
            fds.add(st);
            return st;
        }
    }

    @Override
    public void execute(Runnable runnable) {
        synchronized (lock){
            if(running){
                pending.add(runnable);
                if(pending.size() == 1){
                    interrupt(ptrAddress);
                }
            }
        }
    }

    enum Ops {
        Add, Mod, Del
    }

    enum EventTypes {
        EPOLLIN, EPOLLOUT
    }
}
