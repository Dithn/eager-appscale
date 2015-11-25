#include <stdlib.h>
#include <stdio.h>

#include "ilist.h"
#include "block_io.h"

static block_id ILIST_HEAD;
static block_id BITMAP_HEAD;
static unsigned long ILIST_LENGTH;
static unsigned int *INODE_BITMAP;
static int INODE_BITMAP_INT_LENGTH;
static int INTS_PER_BLOCK;
static long INODES_PER_BLOCK;

void init_ilist(superblock *sb) {
  ILIST_HEAD = sb->ilist;
  BITMAP_HEAD = sb->bitmap;
  ILIST_LENGTH = sb->ilist_length;
  long bitmap_blocks = sb->ilist - sb->bitmap;
  INODE_BITMAP = malloc(bitmap_blocks * BLOCK_SIZE);
  INODE_BITMAP_INT_LENGTH = ILIST_LENGTH / (8  * sizeof(unsigned int));
  INTS_PER_BLOCK = BLOCK_SIZE / sizeof(unsigned int);
  long i;
  block_id current = sb->bitmap;
  for (i = 0; i < bitmap_blocks; i++) {
    read_block(current++, INODE_BITMAP + i * INTS_PER_BLOCK, BLOCK_SIZE);
  }
  INODES_PER_BLOCK = BLOCK_SIZE / sizeof(inode);
  printf("Initialized ilist with %ld entries\n", ILIST_LENGTH);
}

void cleanup_ilist() {
  free(INODE_BITMAP);
}

int allocate_inode(inumber *in) {
  int i, j;
  int bits_per_int = 8 * sizeof(unsigned int);
  for (i = 0; i < INODE_BITMAP_INT_LENGTH; i++) {
    unsigned int current = INODE_BITMAP[i];
    unsigned int mask = 1;
    for (j = 0; j < bits_per_int; j++) {
      if (current & mask) {
	current = current >> 1;
      } else {
	*in = i * bits_per_int + j;
	current |= 1 << j;
	INODE_BITMAP[i] = current;
	int block_offset = current / INTS_PER_BLOCK;
	block_id bm_block= BITMAP_HEAD + block_offset;
	write_block(bm_block, INODE_BITMAP + block_offset * INTS_PER_BLOCK, BLOCK_SIZE);
	return 1;
      }
    }
  }
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
