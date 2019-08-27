package org.jetlang.epoll;

import java.lang.reflect.Field;
import java.nio.channels.spi.AbstractSelectableChannel;

public class FdUtils {
    public static int getFd(AbstractSelectableChannel channel) {
        try {
            Field f = channel.getClass().getDeclaredField("fdVal");
            f.setAccessible(true);
            return (int) f.get(channel);
        } catch (Exception failed) {
            throw new RuntimeException(failed);
        }
    }
}
