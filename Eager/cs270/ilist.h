#ifndef _ILIST_H
#define _ILIST_H

#include <sys/types.h>
#include "block_io.h"

typedef struct {
  uid_t user_id;
  gid_t group_id;
  time_t ctime; // Last status change time (change to inode)
  time_t atime; // Last accessed time
  time_t mtime; // Last modified time (change to content)
  mode_t mode;
  off_t size;
  nlink_t links;
  block_id direct_blocks[22];
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
int allocate_inode(inumber *in);
void release_inode(inumber in);
void get_inode(inumber i_number, inode* i_node);

#endif
