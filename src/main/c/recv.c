
#include <netinet/ip.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>
#include <fcntl.h>

int
main(void)
{
#define VLEN 10
#define BUFSIZE 200
#define TIMEOUT 1
   int sockfd, retval, i;
   struct sockaddr_in addr;
   struct mmsghdr msgs[VLEN];
   struct iovec iovecs[VLEN];
   char bufs[VLEN][BUFSIZE+1];
   struct timespec timeout;

   sockfd = socket(AF_INET, SOCK_DGRAM, 0);
   if (sockfd == -1) {
       perror("socket()");
       exit(EXIT_FAILURE);
   }

//   int flags = fcntl(sockfd, F_GETFL, 0);
//   fcntl(sockfd, F_SETFL, flags | O_NONBLOCK);

   addr.sin_family = AF_INET;
   addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
   addr.sin_port = htons(1234);
   if (bind(sockfd, (struct sockaddr *) &addr, sizeof(addr)) == -1) {
       perror("bind()");
       exit(EXIT_FAILURE);
   }

   memset(msgs, 0, sizeof(msgs));
   for (i = 0; i < VLEN; i++) {
       iovecs[i].iov_base         = bufs[i];
       iovecs[i].iov_len          = BUFSIZE;
       msgs[i].msg_hdr.msg_iov    = &iovecs[i];
       msgs[i].msg_hdr.msg_iovlen = 1;
   }


   for(int l = 0; l < 10; l++){
      timeout.tv_sec = 0;
      timeout.tv_nsec = 0;
       retval = recvmmsg(sockfd, msgs, VLEN, 0, NULL);
       if (retval == -1) {
           perror("recvmmsg()");
           exit(EXIT_FAILURE);
       }

       printf("%d messages received\n", retval);
       for (i = 0; i < retval; i++) {
           bufs[i][msgs[i].msg_len] = 0;
           printf("%d %s", i+1, bufs[i]);
       }
       sleep(3);
   }
   exit(EXIT_SUCCESS);
}