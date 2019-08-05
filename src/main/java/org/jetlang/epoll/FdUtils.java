package org.jetlang.epoll;

import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.nio.channels.DatagramChannel;

public class FdUtils {
    public static int getFd(DatagramChannel channel) {
        try {
            Field f = channel.getClass().getDeclaredField("fdVal");
            FileDescriptor fd;
            f.setAccessible(true);
            return (int) f.get(channel);
        }catch(Exception failed){
            throw new RuntimeException(failed);
        }
    }
}
