#include <stdio.h>
#include <stdlib.h>

#include "freelist.h"

static int POINTERS_PER_BLOCK;

void init_free_list(block_id head, block_count blocks) {
  if (blocks <= 0) {
    printf("Block count must be positive\n");
    return;
  }
  POINTERS_PER_BLOCK = BLOCK_SIZE / sizeof(int);

  printf("\nBuilding Free List\n");
  printf("======================\n");
  block_id current = head, last = head + blocks - 1;
  block_id *pointers = malloc(BLOCK_SIZE);
  int i;
  while (current <= last) {
    for (i = POINTERS_PER_BLOCK - 1; i > 0; i--) {
      if (current == last) {
	break;
      }
      pointers[i] = last--;
    }
    if (current < last) {
      pointers[0] = current + 1;
    } else {
      while (i >= 0) {
	pointers[i--] = 0;
      }
    }
    
    for (i = 0; i < POINTERS_PER_BLOCK; i++) {
      printf("[Block %ld][%d] %ld\n", current, i, pointers[i]);
    }
    write_block(current, pointers, BLOCK_SIZE);
    current++;
  }
  free(pointers);
}

int check_free_list(block_id head, block_count blocks) {
  block_id current = head, count = 0;
  block_id *pointers = malloc(BLOCK_SIZE);
  while (TRUE) {
    read_block(current, pointers, BLOCK_SIZE);
    int i;
    for (i = 0; i < POINTERS_PER_BLOCK; i++) {
      if (pointers[i] > 0) {
	count++;
      }
    }
    if (pointers[0] > 0) {
      current = pointers[0];
    } else {
      break;
    }
  }

  free(pointers);
  return blocks - count;
}

block_id allocate_block(block_id head) {
  block_id *pointers = malloc(BLOCK_SIZE);
  read_block(head, pointers, BLOCK_SIZE);
  int i;
  block_id allocation = 0;
  for (i = POINTERS_PER_BLOCK - 1; i > 0; i--) {
    if (pointers[i] > 0) {
      allocation = pointers[i];
      pointers[i] = 0;
      write_block(head, pointers, BLOCK_SIZE);
      break;
    }
  }

  if (allocation == 0 && pointers[0] > 0) {
    allocation = pointers[0];
    read_block(allocation, pointers, BLOCK_SIZE);
    write_block(head, pointers, BLOCK_SIZE);
  }

  free(pointers);
  return allocation;
}

void free_block(block_id head, block_id block) {
  if (block == 0) {
    printf("Invalid block address: 0\n");
    return;
  }
  int done = 0;
  block_id *pointers = malloc(BLOCK_SIZE);
  read_block(head, pointers, BLOCK_SIZE);
  int i;
  for (i = 1; i < POINTERS_PER_BLOCK - 1; i++) {
    if (pointers[i] == 0) {
      pointers[i] = block;
      write_block(head, pointers, BLOCK_SIZE);
      done = 1;
      break;
    }
  }

  if (!done) {
    write_block(block, pointers, BLOCK_SIZE);
    pointers[0] = block;
    for (i = 1; i < POINTERS_PER_BLOCK - 1; i++) {
      pointers[i] = 0;
    }
    write_block(head, pointers, BLOCK_SIZE);
  }
  free(pointers);
}
