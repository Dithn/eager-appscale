#include <unistd.h>
#include <fcntl.h>
#include "block_io.h"

#define MIN(X,Y) ((X) < (Y) ? (X) : (Y))

static int DEVICE_ID;
const int BLOCK_SIZE = 4096;

int open_device(char *device) {
  DEVICE_ID = open(device, O_RDWR);
  return DEVICE_ID;
}

int read_block(block_id block, void* buffer, int size) {
  return pread(DEVICE_ID, buffer, MIN(size, BLOCK_SIZE), block * BLOCK_SIZE);
}

int write_block(block_id block, void* buffer, int size) {
  return pwrite(DEVICE_ID, buffer, MIN(size, BLOCK_SIZE), block * BLOCK_SIZE);
}

void close_device() {
  close(DEVICE_ID);
}
