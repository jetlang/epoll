JAVA_HOME :=/usr/lib/jvm/java-12-openjdk-amd64

all :
	g++ -O3 -c -fPIC -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux src/main/c/org_jetlang_epoll_EPoll.cpp -o jetlang-epoll.o
	g++ -O3 -shared -fPIC -o libjetlang-epoll.so jetlang-epoll.o -lc

clean :
	rm jetlang-epoll.o libjetlang-epoll.so

java-prod :
	${JAVA_HOME}/bin/javac -h src/main/c -d out/production/epoll -sourcepath src/main/java src/main/java/org/jetlang/epoll/EPoll.java

java-test :
	${JAVA_HOME}/bin/javac -cp out/production/epoll -d out/test/epoll -sourcepath src/test/java src/test/java/org/jetlang/epoll/Main.java src/test/java/org/jetlang/epoll/LatencyMain.java

java : java-prod java-test

java-clean :
	rm -rf out/
