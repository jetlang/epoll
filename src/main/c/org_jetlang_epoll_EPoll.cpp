#include <jni.h>
#include <sys/epoll.h>
#include "org_jetlang_epoll_EPoll.h"
#include <cstdlib>
#include <unistd.h>
#include <sys/eventfd.h>

struct epoll_state {
   int fd;
   struct epoll_event * events;
   int max_events;

   struct epoll_event efd_event;
};

JNIEXPORT jint JNICALL Java_org_jetlang_epoll_EPoll_select
  (JNIEnv *, jobject, jlong ptrAddress, jint timeout){
    struct epoll_state *state = (struct epoll_state *) ptrAddress;
    return epoll_wait(state->fd, state->events, state->max_events, timeout);
 }


JNIEXPORT jlong JNICALL Java_org_jetlang_epoll_EPoll_getEventArrayAddress
  (JNIEnv *, jclass, jlong ptrAddress){
    struct epoll_state *state = (struct epoll_state *) ptrAddress;
    return (jlong) state->events;
}


JNIEXPORT jlong JNICALL Java_org_jetlang_epoll_EPoll_getReadBufferAddress
  (JNIEnv *, jclass, jlong){
    return 0;
}

JNIEXPORT jlong JNICALL Java_org_jetlang_epoll_EPoll_init
  (JNIEnv *, jclass, jint maxSelectedEvents, jint, jint){
    int epoll_fd = epoll_create1(0);
    struct epoll_state *state = (struct epoll_state *) malloc(sizeof(struct epoll_state));
    state->fd = epoll_fd;
    state->events = (struct epoll_event *) malloc(maxSelectedEvents * (sizeof(struct epoll_event)));
    state->max_events = maxSelectedEvents;
    state->efd_event.events = EPOLLHUP | EPOLLERR | EPOLLIN;
    state->efd_event.data.fd = eventfd(0, EFD_NONBLOCK);
    epoll_ctl(epoll_fd, EPOLL_CTL_ADD, state->efd_event.data.fd, &state->efd_event);
    return (jlong) state;
  }

JNIEXPORT void JNICALL Java_org_jetlang_epoll_EPoll_freeNativeMemory
  (JNIEnv *, jclass, jlong ptrAddress){
    struct epoll_state *state = (struct epoll_state *) ptrAddress;
    close(state->fd);
    close(state->efd_event.data.fd);
    free(state->events);
    free(state);
  }

JNIEXPORT void JNICALL Java_org_jetlang_epoll_EPoll_interrupt
  (JNIEnv *, jclass, jlong){
  }

JNIEXPORT jint JNICALL Java_org_jetlang_epoll_EPoll_ctl
  (JNIEnv *, jclass, jlong, jint, jint, jint, jint){
    return 0;
  }
