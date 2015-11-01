#ifndef _BLOCK_IO_H
#define _BLOCK_IO_H

typedef unsigned int block_id;

extern const int BLOCK_SIZE;
extern const int TOTAL_SIZE;

int open_device();
int read_block(block_id block, void* buffer, int size);
int write_block(block_id block, void* buffer, int size);
void close_device();

#endif
