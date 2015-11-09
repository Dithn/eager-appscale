#ifndef _FREE_LIST_H
#define _FREE_LIST_H

#include "block_io.h"

#define TRUE 1

void init_free_list(block_id head, block_count blocks);
int check_free_list(block_id head, block_count blocks);
block_id allocate_block(block_id head);
void free_block(block_id head, block_id block);

#endif
