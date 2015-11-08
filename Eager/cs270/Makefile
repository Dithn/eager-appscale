CC=gcc
CFLAGS=-I.
DEPS=block_io.h freelist.h ilist.h

%.o: %.c $(DEPS)
	$(CC) -c -o $@ $< $(CFLAGS)

freelist_test: freelist.o block_io.o freelist_test.o 
	gcc -o freelist_test freelist.o block_io.o freelist_test.o $(CFLAGS)

mkfs_h2: mkfs_h2.o block_io.o freelist.o
	gcc -o mkfs.h2 mkfs_h2.o block_io.o freelist.o $(CFLAGS)

clean:
	rm *.o