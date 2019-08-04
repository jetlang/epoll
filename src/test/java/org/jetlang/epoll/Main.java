package org.jetlang.epoll;

public class Main {

    public static void main(String[] args) {
        EPoll e = new EPoll("epoll", 5, 16, 1024 * 8);

    }
}
