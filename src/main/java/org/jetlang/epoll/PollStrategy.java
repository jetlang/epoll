package org.jetlang.epoll;

public interface PollStrategy {
    int poll(long ptrAddress);

    class Spin implements PollStrategy {

        @Override
        public int poll(long ptrAddress) {
            return EPoll.epollSpin(ptrAddress);
        }
    }

    class Wait implements PollStrategy {

        @Override
        public int poll(long ptrAddress) {
            return EPoll.epollWait(ptrAddress);
        }
    }
}
