#include <jni.h>
#include "org_jetlang_epoll_EPoll.h"

JNIEXPORT jint JNICALL Java_org_jetlang_epoll_EPoll_select
  (JNIEnv *, jobject, jlong){
    return 0;
 }


JNIEXPORT jlong JNICALL Java_org_jetlang_epoll_EPoll_getEventArrayAddress
  (JNIEnv *, jclass, jlong){
    return 0;
}


JNIEXPORT jlong JNICALL Java_org_jetlang_epoll_EPoll_getReadBufferAddress
  (JNIEnv *, jclass, jlong){
    return 0;
}

JNIEXPORT jlong JNICALL Java_org_jetlang_epoll_EPoll_init
  (JNIEnv *, jclass, jint, jint, jint){
    return 0;
  }

JNIEXPORT void JNICALL Java_org_jetlang_epoll_EPoll_freeNativeMemory
  (JNIEnv *, jclass, jlong){
  }

JNIEXPORT void JNICALL Java_org_jetlang_epoll_EPoll_interrupt
  (JNIEnv *, jclass, jlong){
  }

JNIEXPORT jint JNICALL Java_org_jetlang_epoll_EPoll_ctl
  (JNIEnv *, jclass, jlong, jint, jint, jint, jint){
    return 0;
  }
