package org.jetlang.epoll;

public interface EventBatch {

    void start(int numSelectedEvents);

    void end();

    EventBatch NO_OP = new EventBatch() {
        @Override
        public void start(int numSelectedEvents) {

        }

        @Override
        public void end() {

        }
    };
}
