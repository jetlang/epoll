
export LD_LIBRARY_PATH=.
CP=out/production/epoll:out/test/epoll

$JAVA_HOME/bin/java -cp $CP org.jetlang.epoll.LatencyMain $*