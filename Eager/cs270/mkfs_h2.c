#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <string.h>

#include "ilist.h"
#include "block_io.h"

int main(int agrc, char** argv) {
  char *block_device = argv[1];
  float inode_percentage = atof(argv[2]);
  long total_num_bytes = atol(argv[3]);

  /* number of blocks on disk */
  long block_count = total_num_bytes / BLOCK_SIZE;
  if (block_count == 0) {
    printf("Not enough space on the device for installing a file system\n");
    return 1;
  }

  long inode_block_count = inode_percentage * block_count;
  if (inode_block_count == 0) {
    printf("Not enough blocks available for inodes\n");
    return 1;
  }

  /* number of i-nodes in the FS */
  long inode_count = inode_block_count * BLOCK_SIZE / sizeof(inode);

  /* number of blocks for a bit map */
  long bit_map_blocks = ceil(ceil(inode_count / sizeof(char)) / BLOCK_SIZE); 

  if (open_device(block_device) < 0) {
    printf("Failed to open device: %s\n", block_device);
    return 1;
  }

  int *bit_map = malloc(BLOCK_SIZE);
  memset(bit_map, 0, BLOCK_SIZE);  
  int i;
  block_id current = 1;
  for (i = 0; i < bit_map_blocks; i++) {
    printf("Writing inode bitmap at block %d\n", current);
    write_block(current++, bit_map, BLOCK_SIZE);
  }
  free(bit_map);

  block_id ilist_head = current;
  inode *inodes = malloc(BLOCK_SIZE);
  memset(inodes, 0, BLOCK_SIZE);
  for (i = 0; i < inode_block_count; i++) {
    printf("Writing inodes at block %d\n", current);
    write_block(current++, inodes, BLOCK_SIZE);
  }
  free(inodes);

  long remaining = block_count - current;
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
  write_block(0, sb, sizeof(superblock));
  free(sb);

  close_device();
  return 0;
}
