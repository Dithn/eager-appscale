#include <unistd.h>
#include <fcntl.h>
#include "block_io.h"

#define MIN(X,Y) ((X) < (Y) ? (X) : (Y))

static char *raw_device = "/dev/disk1";
static int device_id;

const int BLOCK_SIZE = 4096;
const int TOTAL_SIZE = 1024 * 1024;

int open_device() {
  device_id = open(raw_device, O_RDWR);
  return device_id;
}

int read_block(block_id block, void* buffer, int size) {
  return pread(device_id, buffer, MIN(size, BLOCK_SIZE), block * BLOCK_SIZE);
}

int write_block(block_id block, void* buffer, int size) {
  return pwrite(device_id, buffer, MIN(size, BLOCK_SIZE), block * BLOCK_SIZE);
}

void close_device() {
  close(device_id);
}
