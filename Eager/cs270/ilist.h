#ifndef _ILIST_H
#define _ILIST_H

#include "block_io.h"

typedef struct {
  unsigned int user_id;
  unsigned int group_id;
  long created_time;
  long access_time;
  long modified_time;
  int protection;
  long size;
  long links;
  int flags;
  block_id direct_blocks[16];
  block_id l1_indirect_blocks[16];
  block_id l2_indirect_blocks[16];
} inode;

typedef struct {
  block_id bitmap;
  block_id ilist;
  long ilist_length;
  block_id freelist_head;
} superblock;

#endif
