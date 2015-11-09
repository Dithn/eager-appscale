#ifndef _BLOCK_IO_H
#define _BLOCK_IO_H

typedef unsigned long block_id;
typedef unsigned long block_count;

extern const int BLOCK_SIZE;

int open_device(char *device);
int read_block(block_id block, void* buffer, int size);
int write_block(block_id block, void* buffer, int size);
void close_device();

#endif
