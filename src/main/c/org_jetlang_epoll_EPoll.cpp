#include <jni.h>
#include <sys/epoll.h>
#include "org_jetlang_epoll_EPoll.h"
#include <cstdlib>
#include <unistd.h>
#include <sys/eventfd.h>
#include <stdio.h>
#include <netinet/ip.h>
#include <string.h>
#include <sys/uio.h>
#include <errno.h>
#include <inttypes.h>
#include <immintrin.h>
#include <chrono>
#include <sstream>
#include <iostream>

struct epoll_state {
   int fd;
   int efd;
   struct epoll_event * events;
   int max_events;
   struct epoll_event efd_event;

   int udp_rcv_len;
   struct mmsghdr * udp_rcv;
};

void throwFailure(JNIEnv *env, std::string msg, int result, int errcode){
    std::stringstream s;
    s << msg << " failed. result: " << result << " errcode: " << errcode;
    jclass clzz = env->FindClass("java/lang/RuntimeException");
    if(clzz == 0){
        printf("jetlang epoll failed result %d errno %d\n", result, errcode);
        fflush(stdout);
        return;
    }
    env->ThrowNew(clzz, s.str().c_str());
}

JNIEXPORT jint JNICALL Java_org_jetlang_epoll_EPoll_epollWait
  (JNIEnv *, jclass, jlong ptrAddress){
    struct epoll_state *state = (struct epoll_state *) ptrAddress;
    return epoll_wait(state->fd, state->events, state->max_events, -1);
 }

 JNIEXPORT jint JNICALL Java_org_jetlang_epoll_EPoll_epollSpin
   (JNIEnv *, jclass, jlong ptrAddress){
     const struct epoll_state *state = (struct epoll_state *) ptrAddress;
     const int epoll_fd = state->fd;
     const int max_events = state->max_events;
     struct epoll_event * const events = state->events;
     int result = 0;
     for(result = epoll_wait(epoll_fd, events, max_events, 0); result == 0; result = epoll_wait(epoll_fd, events, max_events, 0)){
          _mm_pause();
     }
     return result;
  }

 JNIEXPORT jint JNICALL Java_org_jetlang_epoll_EPoll_epollSpinWait
   (JNIEnv *, jclass, jlong ptrAddress, jlong microseconds_to_spin){
     const struct epoll_state *state = (struct epoll_state *) ptrAddress;
     const int epoll_fd = state->fd;
     const int max_events = state->max_events;
     struct epoll_event * const events = state->events;
     int result = epoll_wait(epoll_fd, events, max_events, 0);
     if(result != 0){
        return result;
     }
     const auto start = std::chrono::high_resolution_clock::now();
     for(result = epoll_wait(epoll_fd, events, max_events, 0); result == 0; result = epoll_wait(epoll_fd, events, max_events, 0)){
        auto elapsed = std::chrono::high_resolution_clock::now() - start;
        long long microseconds = std::chrono::duration_cast<std::chrono::microseconds>(elapsed).count();
        if(microseconds >= microseconds_to_spin){
           return epoll_wait(epoll_fd, events, max_events, -1);
        }
        _mm_pause();
     }
     return result;
  }

JNIEXPORT jlong JNICALL Java_org_jetlang_epoll_EPoll_getReadBufferAddress
  (JNIEnv *, jclass, jlong ptrAddress, jint idx){
    struct epoll_state *state = (struct epoll_state *) ptrAddress;
    return (jlong) state->udp_rcv[idx].msg_hdr.msg_iov->iov_base;
}

JNIEXPORT jlong JNICALL Java_org_jetlang_epoll_EPoll_getMsgLengthAddress
  (JNIEnv *, jclass, jlong ptrAddress, jint idx){
    struct epoll_state *state = (struct epoll_state *) ptrAddress;
    return (jlong) &state->udp_rcv[idx].msg_len;
}

JNIEXPORT jlong JNICALL Java_org_jetlang_epoll_EPoll_getEpollEventIdxAddress
  (JNIEnv *, jclass, jlong ptrAddress, jint idx){
      struct epoll_state *state = (struct epoll_state *) ptrAddress;
      return (jlong) &state->events[idx].data.u32;
}
JNIEXPORT jlong JNICALL Java_org_jetlang_epoll_EPoll_getEpollEventsAddress
  (JNIEnv *, jclass, jlong ptrAddress, jint idx){
      struct epoll_state *state = (struct epoll_state *) ptrAddress;
      return (jlong) &state->events[idx].events;
}

JNIEXPORT jlong JNICALL Java_org_jetlang_epoll_EPoll_init
  (JNIEnv *env, jclass, jint maxSelectedEvents, jint maxDatagramsPerRead, jint readBufferBytes){
    int epoll_fd = epoll_create1(0);
    if(epoll_fd < 0){
        throwFailure(env, "epoll_create1(0)", epoll_fd, errno);
    }
    struct epoll_state *state = (struct epoll_state *) malloc(sizeof(struct epoll_state));
    state->fd = epoll_fd;
    state->events = (struct epoll_event *) malloc(maxSelectedEvents * (sizeof(struct epoll_event)));
    state->max_events = maxSelectedEvents;
    state->efd = eventfd(0, EFD_NONBLOCK);
    if(state->efd < 0){
        throwFailure(env, "eventfd(0, EFD_NONBLOCK)", state->efd, errno);
    }
    state->efd_event.events = EPOLLHUP | EPOLLERR | EPOLLIN;
    state->efd_event.data.u32 = 0;
    int result = epoll_ctl(epoll_fd, EPOLL_CTL_ADD, state->efd, &state->efd_event);
    if(result != 0){
        throwFailure(env, "epoll_ctl", result, errno);
    }
    state->udp_rcv_len = maxDatagramsPerRead;
    int udp_rcv_size = maxDatagramsPerRead * (sizeof(struct mmsghdr));
    state->udp_rcv = (struct mmsghdr *) malloc( udp_rcv_size );
    memset(state->udp_rcv, 0, udp_rcv_size );
    for (int i = 0; i < maxDatagramsPerRead; i++) {
       char* buffer = (char *) malloc(readBufferBytes);
       struct iovec *io = (struct iovec *)malloc(sizeof(struct iovec));
       io->iov_base = buffer;
       io->iov_len = readBufferBytes;
       state->udp_rcv[i].msg_hdr.msg_iov = io;
       state->udp_rcv[i].msg_hdr.msg_iovlen= 1;
    }
    return (jlong) state;
  }

JNIEXPORT jint JNICALL Java_org_jetlang_epoll_EPoll_recvmmsg
    (JNIEnv *, jclass, jlong ptrAddress, jint fd){
    struct epoll_state *state = (struct epoll_state *) ptrAddress;
    return recvmmsg(fd, state->udp_rcv, state->udp_rcv_len, 0, NULL);
}

JNIEXPORT void JNICALL Java_org_jetlang_epoll_EPoll_freeNativeMemory
  (JNIEnv *, jclass, jlong ptrAddress){
    struct epoll_state *state = (struct epoll_state *) ptrAddress;
    close(state->fd);
    close(state->efd);
    free(state->events);
    free(state->udp_rcv->msg_hdr.msg_iov->iov_base);
    free(state->udp_rcv->msg_hdr.msg_iov);
    free(state->udp_rcv);
    free(state);
  }

JNIEXPORT void JNICALL Java_org_jetlang_epoll_EPoll_interrupt
  (JNIEnv *env, jclass, jlong ptrAddress){
      struct epoll_state *state = (struct epoll_state *) ptrAddress;
      uint64_t d = 1;
      int result = write(state->efd, &d, sizeof(uint64_t));
      if(result != 8){
          throwFailure(env, "write", result, errno);
      }
  }

JNIEXPORT void JNICALL Java_org_jetlang_epoll_EPoll_clearInterrupt
  (JNIEnv *env, jclass, jlong ptrAddress){
      struct epoll_state *state = (struct epoll_state *) ptrAddress;
      uint64_t d;
      int result = read(state->efd, &d, sizeof(uint64_t));
    if(result != 8){
        throwFailure(env, "failed to clear interrupt", result, errno);
    }
  }

JNIEXPORT jlong JNICALL Java_org_jetlang_epoll_EPoll_ctl
  (JNIEnv *env, jclass, jlong ptrAddress, jint op, jint eventTypes, jint fd, jint idx){
    struct epoll_state *state = (struct epoll_state *) ptrAddress;
    struct epoll_event *event = (struct epoll_event *) malloc(sizeof(struct epoll_event));
    event->events = eventTypes;
    event->data.u32 = idx;
    int result = epoll_ctl(state->fd, op, fd, event);
    if(result != 0){
        int errcode = errno;
        free(event);
        throwFailure(env, "epoll_ctl", result, errcode);
    }
    return (jlong) event;
  }
