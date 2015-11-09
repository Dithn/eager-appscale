#include <stdio.h>
#include <string.h>
#include "freelist.h"
#include "block_io.h"

static char* DEVICE = "/dev/disk1";
static unsigned long TOTAL_SIZE = 1024 * 1024;

int main() {
  if (open_device(DEVICE) < 0) {
    printf("Failed to open device\n");
    return 1;
  }

  block_id head = 0;
  block_count blocks = TOTAL_SIZE / BLOCK_SIZE;
  printf("Block size: %d, Block count: %ld\n", BLOCK_SIZE, blocks);
  
  // Create free list on disk
  init_free_list(head, blocks);

  // Check free list correctness (check the number of free blocks)
  int check = check_free_list(head, blocks);
  if (check != 1) {
    printf("Block free list error: %d\n", check);
    close_device();
    return 1;
  }

  printf("\nFree list initialized successfully.\n");

  // Allocate all blocks and write to them
  char *str = "Data payload";
  int alloc_count = 0;
  block_id allocated_blocks[2048];
  while (TRUE) {
    block_id allocation = allocate_block(head);
    if (allocation == 0) {
      printf("Ran out of blocks\n");
      break;
    }
    allocated_blocks[alloc_count++] = allocation;
    printf("Allocated block: %ld\n", allocation);
    write_block(allocation, str, strlen(str));
  }
  printf("Total blocks allocated: %d\n", alloc_count);

  // Free the allocated blocks
  int i;
  for (i = 0; i < alloc_count; i++) {
    free_block(head, allocated_blocks[i]);
    printf("Freed block: %ld\n", allocated_blocks[i]);
  }

  // Re-check the free block count
  check = check_free_list(head, blocks);
  if (check != 1) {
    printf("Block free list error: %d\n", check);
    close_device();
    return 1;
  }
  
  close_device();
  return 0;
}
