JAVA_HOME :=/usr/lib/jvm/java-12-openjdk-amd64
CC := g++

all : java
	$(CC) -O3 -c -fPIC -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux src/main/c/org_jetlang_epoll_EPoll.cpp -o jetlang-epoll.o
	$(CC) -O3 -shared -fPIC -o libjetlang-epoll.so jetlang-epoll.o -lc

clean :
	rm jetlang-epoll.o libjetlang-epoll.so

java-prod :
	${JAVA_HOME}/bin/javac -g -h src/main/c -d out/production/epoll -sourcepath src/main/java src/main/java/org/jetlang/epoll/EPoll.java

java-test :
	${JAVA_HOME}/bin/javac -g -cp out/production/epoll -d out/test/epoll -sourcepath src/test/java src/test/java/org/jetlang/epoll/Main.java src/test/java/org/jetlang/epoll/LatencyMain.java

java : java-prod java-test

jar : java-prod
	${JAVA_HOME}/bin/jar cvf out/jetlang-epoll.jar -C out/production/epoll/ .

java-clean :
	rm -rf out/
