JAVA_HOME=/usr/lib/jvm/java-12-openjdk-amd64

all :
	g++ -c -fPIC -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux src/main/c/org_jetlang_epoll_EPoll.cpp -o jetlang-epoll.o
	g++ -shared -fPIC -o libjetlang-epoll.so jetlang-epoll.o -lc

clean :
	rm jetlang-epoll.o libjetlang-epoll.so

