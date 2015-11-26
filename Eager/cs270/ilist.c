#include <stdlib.h>
#include <stdio.h>

#include "ilist.h"
#include "block_io.h"
#include "freelist.h"

#define MIN(X,Y) ((X) < (Y) ? (X) : (Y))

static block_id ILIST_HEAD;
static block_id BITMAP_HEAD;
static unsigned long ILIST_LENGTH;
static unsigned int *INODE_BITMAP;
static int INODE_BITMAP_INT_LENGTH;
static int INTS_PER_BLOCK;
static long INODES_PER_BLOCK;
static block_id FREELIST_HEAD;

void bitmap_set(int k);
int bitmap_test(int k);

off_t read_fully(inode *i_node, void *buffer);
off_t write_fully(inumber i_number, inode *i_node, void *buffer, off_t size);

void bitmap_set(int k) {
  *(INODE_BITMAP + k/32) |= 1 << (k % 32);
}

int bitmap_test(int k) {
  return (*(INODE_BITMAP + k/32) & (1 << (k % 32))) != 0;
}

void init_ilist(superblock *sb) {
  ILIST_HEAD = sb->ilist;
  BITMAP_HEAD = sb->bitmap;
  FREELIST_HEAD = sb->freelist_head;
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
  int i;
  int bits_per_int = 8 * sizeof(unsigned int);
  for (i = 0; i < INODE_BITMAP_INT_LENGTH; i++) {
    if (!bitmap_test(i)) {
      bitmap_set(i);
      *in = i;
      int block_offset = (i / 32) / INTS_PER_BLOCK;
      block_id bm_block= BITMAP_HEAD + block_offset;
      write_block(bm_block, INODE_BITMAP + block_offset * INTS_PER_BLOCK, BLOCK_SIZE);
      return 1;
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
  block_id block = ILIST_HEAD + (i_number / INODES_PER_BLOCK);
  inode* inodes = malloc(BLOCK_SIZE);
  read_block(block, inodes, BLOCK_SIZE);
  *i_node = *(inodes + i_number % INODES_PER_BLOCK);
  free(inodes);
}

void write_inode(inumber i_number, inode* i_node) {
  if (i_number >= ILIST_LENGTH) {
    return;
  }
  block_id block = ILIST_HEAD + (i_number / INODES_PER_BLOCK);
  inode* inodes = malloc(BLOCK_SIZE);
  read_block(block, inodes, BLOCK_SIZE);
  *(inodes + i_number % INODES_PER_BLOCK) = *i_node;
  write_block(block, inodes, BLOCK_SIZE);
  free(inodes);
}

off_t read_dir(inode *i_node, direntry *buffer) {
  return read_fully(i_node, buffer);
}

off_t write_dir(inumber i_number, inode *i_node, direntry *buffer, off_t size) {
  return write_fully(i_number, i_node, buffer, size);
}

off_t read_fully(inode *i_node, void *buffer) {
  off_t read = 0;
  int indirection = 0;
  int iteration = 0;
  while (read < i_node->size) {
    if (indirection == 0) {
      int read_size = MIN(BLOCK_SIZE, i_node->size - read);
      read += read_block(i_node->direct_blocks[iteration], buffer + read, read_size);
      iteration++;
      if (iteration == 22) {
	indirection = 1;
	iteration = 0;
      }
    } else {
      // TODO: Handle other levels of indirection
      return read;
    }
  }
  return read;
}

off_t write_fully(inumber i_number, inode *i_node, void *buffer, off_t size) {
  off_t written = 0;
  int indirection = 0;
  int iteration = 0;
  while (written < size) {
    if (indirection == 0) {
      int write_size = MIN(BLOCK_SIZE, size - written);
      block_id current;
      if (i_node->direct_blocks[iteration] != 0) {
	current = i_node->direct_blocks[iteration];
      } else {
	current = allocate_block(FREELIST_HEAD);
	if (current == 0) {
	  return written;
	}
	time_t now;
	time(&now);
	i_node->mtime = now;
	i_node->ctime = now;
	i_node->direct_blocks[iteration] = current;
	write_inode(i_number, i_node);
      }

      written += write_block(current, buffer + written, write_size);
      iteration++;
      if (iteration == 22) {
	indirection = 1;
	iteration = 0;
      }
    } else {
      // TODO: Handle other levels of indirection
      return written;
    }
  }

  i_node->size = written;
  write_inode(i_number, i_node);
  return written;
}
