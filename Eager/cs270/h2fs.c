#include <stdio.h>

#include "block_io.h"
#include "ilist.h"

int main() {
  if (open_device("/dev/disk1") < 0) {
    printf("Failed to open device\n");
    return 1;
  }

  superblock *sb = malloc(sizeof(superblock));
  read_block(0, sb, sizeof(superblock));
  init_ilist(sb);
  free(sb);
  cleanup_ilist();
  close_device();
  return 0;
}
