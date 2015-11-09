#include <stdlib.h>
#include <stdio.h>

#include "ilist.h"
#include "block_io.h"

static unsigned long ILIST_LENGTH;
static int *INODE_BITMAP;

void init_ilist(superblock *sb) {
  ILIST_LENGTH = sb->ilist_length;
  long bitmap_blocks = sb->ilist - sb->bitmap;
  INODE_BITMAP = malloc(bitmap_blocks * BLOCK_SIZE);
  long i;
  block_id current = sb->bitmap;
  for (i = 0; i < bitmap_blocks; i++) {
    read_block(current++, INODE_BITMAP + i * BLOCK_SIZE, BLOCK_SIZE);
  }
  printf("Initialized ilist with %ld entries\n", ILIST_LENGTH);
}

void cleanup_ilist() {
  free(INODE_BITMAP);
}

inumber allocate_inode() {
  return 0;
}

void release_inode(inumber i) {

}
