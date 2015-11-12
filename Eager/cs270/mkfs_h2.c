#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <string.h>

#include "ilist.h"
#include "block_io.h"

int main(int argc, char** argv) {
  if (argc != 4) {
    printf("Usage: mkfs.h2 device_path inode_percentage total_size\n");
    return 1;
  }

  char *block_device = argv[1];
  float inode_percentage = atof(argv[2]);
  unsigned long total_num_bytes = atol(argv[3]);

  /* number of blocks on disk */
  block_count blocks = total_num_bytes / BLOCK_SIZE;
  if (blocks < 4) {
    // Need at least 4 blocks: superblock + inode bitmap + inodes + data
    printf("Not enough space on the device for installing a file system\n");
    return 1;
  }

  block_count inode_blocks = inode_percentage * blocks;
  if (inode_blocks == 0) {
    printf("Not enough blocks available for inodes\n");
    return 1;
  }

  printf("Total blocks on device: %ld\n", blocks);
  printf("Blocks allocated for inodes: %ld\n", inode_blocks);

  /* number of i-nodes in the FS */
  unsigned long inode_count = inode_blocks * BLOCK_SIZE / sizeof(inode);

  /* number of blocks for a bitmap */
  block_count bitmap_blocks = ceil(ceil(inode_count / sizeof(char)) / BLOCK_SIZE); 

  if (open_device(block_device) < 0) {
    printf("Failed to open device: %s\n", block_device);
    return 1;
  }

  int *bitmap = malloc(BLOCK_SIZE);
  memset(bitmap, 0, BLOCK_SIZE);  
  block_count i;
  block_id current = 1;
  for (i = 0; i < bitmap_blocks; i++) {
    printf("Writing inode bitmap at block %ld\n", current);
    write_block(current++, bitmap, BLOCK_SIZE);
  }
  free(bitmap);

  block_id ilist_head = current;
  inode *inodes = malloc(BLOCK_SIZE);
  memset(inodes, 0, BLOCK_SIZE);
  for (i = 0; i < inode_blocks; i++) {
    printf("Writing inodes at block %ld\n", current);
    write_block(current++, inodes, BLOCK_SIZE);
  }
  free(inodes);

  block_count remaining = blocks - current;
  printf("\nBlocks available for data: %ld\n", remaining);
  init_free_list(current, remaining);
  if (check_free_list(current, remaining) != 1) {
    printf("Problem in creating free list\n");
    close_device();
    return 1;
  }
  
  superblock *sb = malloc(sizeof(superblock));
  sb->bitmap = 1;
  sb->ilist = ilist_head;
  sb->ilist_length = inode_count;
  sb->freelist_head = current;
  printf("\nWriting superblock\n");
  write_block(0, sb, sizeof(superblock));
  free(sb);

  printf("H2 file system initialized successfully\n");
  close_device();
  return 0;
}