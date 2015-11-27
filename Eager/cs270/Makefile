CC=gcc
CFLAGS=-I. -D_FILE_OFFSET_BITS=64
DEPS=block_io.h freelist.h ilist.h

%.o: %.c $(DEPS)
	$(CC) -c -o $@ $< $(CFLAGS)

h2fs: freelist_test mkfs_h2 h2fs_impl
	@echo "Building all"

freelist_test: freelist.o block_io.o freelist_test.o 
	gcc -o freelist_test freelist.o block_io.o freelist_test.o $(CFLAGS)

mkfs_h2: mkfs_h2.o block_io.o freelist.o ilist.o
	gcc -o mkfs.h2 mkfs_h2.o block_io.o freelist.o ilist.o -lm $(CFLAGS)

h2fs_impl: h2fs.o block_io.o ilist.o freelist.o
	gcc -Wall -o h2fs h2fs.o block_io.o ilist.o freelist.o `pkg-config fuse --cflags --libs` $(CFLAGS)
clean:
	rm *.o