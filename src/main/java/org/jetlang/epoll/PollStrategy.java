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

    class SpinWait implements PollStrategy {
        private final long microsecondsToSpin;

        public SpinWait(long microsecondsToSpin) {
            this.microsecondsToSpin = microsecondsToSpin;
        }

        @Override
        public int poll(long ptrAddress) {
            return EPoll.epollSpinWait(ptrAddress, microsecondsToSpin);
        }
    }
}
