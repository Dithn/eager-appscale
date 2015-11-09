#include <stdlib.h>
#include <stdio.h>

#include "ilist.h"
#include "block_io.h"

static block_id ILIST_HEAD;
static unsigned long ILIST_LENGTH;
static int *INODE_BITMAP;
static long INODES_PER_BLOCK;

void init_ilist(superblock *sb) {
  ILIST_HEAD = sb->ilist;
  ILIST_LENGTH = sb->ilist_length;
  long bitmap_blocks = sb->ilist - sb->bitmap;
  INODE_BITMAP = malloc(bitmap_blocks * BLOCK_SIZE);
  long i;
  block_id current = sb->bitmap;
  for (i = 0; i < bitmap_blocks; i++) {
    read_block(current++, INODE_BITMAP + i * BLOCK_SIZE, BLOCK_SIZE);
  }
  INODES_PER_BLOCK = BLOCK_SIZE / sizeof(inode);
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

void get_inode(inumber i_number, inode* i_node) {
  if (i_number >= ILIST_LENGTH) {
    return;
  }
  block_id block = i_number / INODES_PER_BLOCK;
  inode* inodes = malloc(BLOCK_SIZE);
  read_block(block, inodes, BLOCK_SIZE);
  i_node = inodes + i_number % INODES_PER_BLOCK;
  free(inodes);
}
