JAVA_HOME :=/usr/lib/jvm/java-12-openjdk-amd64
CC := g++

all : cpp jar

cpp : java
	$(CC) -O3 -c -flto -fPIC -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux src/main/c/org_jetlang_epoll_EPoll.cpp -o jetlang-epoll.o
	$(CC) -O3 -shared -flto -fPIC -o libjetlang-epoll.so jetlang-epoll.o

clean :
	rm jetlang-epoll.o libjetlang-epoll.so

java-prod :
	${JAVA_HOME}/bin/javac -g -h src/main/c -d out/production/epoll -sourcepath src/main/java src/main/java/org/jetlang/epoll/EPoll.java

java-test :
	${JAVA_HOME}/bin/javac -g -cp out/production/epoll -d out/test/epoll -sourcepath src/test/java src/test/java/org/jetlang/epoll/Main.java src/test/java/org/jetlang/epoll/LatencyMain.java

java : java-prod java-test

jar : java-prod
	${JAVA_HOME}/bin/jar cvf out/jetlang-epoll.jar -C out/production/epoll/ .
	${JAVA_HOME}/bin/jar cvf out/jetlang-epoll-src.jar -C src/main/java/ .

java-clean :
	rm -rf out/
