JAVA_HOME=/usr/lib/jvm/java-12-openjdk-amd64

LIB_OUT=out/production/epoll
TEST_OUT=out/test/epoll

$JAVA_HOME/bin/java -Djava.library.path=. -cp $LIB_OUT:$TEST_OUT org.jetlang.epoll.Main
