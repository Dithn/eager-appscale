CC=gcc
CFLAGS=-I.
DEPS=block_io.h freelist.h

%.o: %.c $(DEPS)
	$(CC) -c -o $@ $< $(CFLAGS)

freelist_test: freelist.o block_io.o freelist_test.o 
	gcc -o freelist_test freelist.o block_io.o freelist_test.o $(CFLAGS)

clean:
	rm *.o