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
  block_id direct_blocks[21];
  block_id l1_indirect_blocks;
  block_id l2_indirect_blocks;
  block_id l3_indirect;
} inode;

typedef struct {
  block_id bitmap;
  block_id ilist;
  unsigned long ilist_length;
  block_id freelist_head;
} superblock;

typedef unsigned long inumber;

void init_ilist(superblock *sb);
void cleanup_ilist();
inumber allocate_inode();
void release_inode(inumber in);

#endif
