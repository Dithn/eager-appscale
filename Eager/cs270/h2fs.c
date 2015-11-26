#define FUSE_USE_VERSION 30

#include <fuse.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <libgen.h>
#include <time.h>
#include <unistd.h>

#include "block_io.h"
#include "ilist.h"

static void h2_init_stat(inode *in, struct stat *stbuf);
static int h2_resolve(const char *path, inumber *in);

static void h2_init_stat(inode *in, struct stat *stbuf) {
  stbuf->st_ino = 0;
  stbuf->st_mode = in->mode;
  stbuf->st_nlink = in->links;
  stbuf->st_uid = in->user_id;
  stbuf->st_gid = in->group_id;
  stbuf->st_size = in->size;
  stbuf->st_atime = in->atime;
  stbuf->st_mtime = in->mtime;
  stbuf->st_ctime = in->ctime;
}

static int h2_resolve(const char *path, inumber *in) {
  if (strcmp(path, "/") == 0) {
    *in = 0;
    return 1;
  }
  return 0;
}

static int h2_getattr(const char *path, struct stat *stbuf) {
  printf("Getting attributes: %s\n", path);
  inumber in;
  if (strcmp(path, "/") == 0) {
    inode root;
    get_inode(0, &root);
    h2_init_stat(&root, stbuf);
    return 0;
  } else if (strcmp(path, "/foo") == 0) {
    inode root;
    get_inode(1, &root);
    h2_init_stat(&root, stbuf);
    return 0;
  }
  return -ENOENT;
}

static int h2_mkdir(const char *path, mode_t mode) {
  printf("mkdir %s\n", path);
  char *parent = "/";
  char *base = path + 1;
  printf("%s %s\n", parent, base);
  inumber parent_num;
  if (h2_resolve(parent, &parent_num)) {
    printf("Resolved %s to %ld\n", parent, parent_num);
    inode parent;
    get_inode(parent_num, &parent);
    off_t newsize = parent.size + sizeof(direntry);
    printf("New dir size: %ld\n", newsize);
    direntry *parentdir = malloc(newsize);
    off_t read = read_dir(&parent, parentdir);
    printf("Read parent dir: %ld %ld\n", read, parent.size);
    if (read != parent.size) {
      return -ENOENT;
    }
    int newdir_index = parent.size / sizeof(direntry);

    inumber newdir_number;
    time_t now;
    time(&now);
    if (allocate_inode(&newdir_number)) {
      printf("New inode allocated: %ld\n", newdir_number);
      inode newdir;
      newdir.mode = mode | S_IFDIR;
      newdir.links = 1;
      newdir.atime = now;
      newdir.mtime = now;
      newdir.ctime = now;
      newdir.size = 0;
      newdir.user_id = getuid();
      newdir.group_id = getgid();
      write_inode(newdir_number, &newdir);
      printf("Wrote new inode\n");

      direntry newentry;
      strcpy(newentry.name, base);
      newentry.number = newdir_number;
      printf("New entry: %s %ld\n", newentry.name, newentry.number);
      *(parentdir + newdir_index) = newentry;
      off_t written = write_dir(parent_num, &parent, parentdir, newsize);
      printf("Write parent: %ld %ld\n", written, newsize);
      if (written != newsize) {
	return -ENOENT;
      }
      return 0;
    }
  }
  return -ENOENT;
}


static int h2_readdir(const char *path, void *buf, fuse_fill_dir_t filler,
                         off_t offset, struct fuse_file_info *fi) {
  printf("Reading dir: %s\n", path);
  if (strcmp(path, "/") != 0) {
    return -ENOENT;
  }
  inode root;
  get_inode(0, &root);
  direntry *dir = malloc(root.size);
  read_dir(&root, dir);
  filler(buf, ".", NULL, 0);
  filler(buf, "..", NULL, 0);
  int i;
  int lim = root.size / sizeof(direntry);
  for (i = 0; i < lim; i++) {
    filler(buf, (dir+i)->name, NULL, 0);
  }
  return 0;
}

static struct fuse_operations h2_oper = {
  .getattr = h2_getattr,
  .readdir = h2_readdir,
  .mkdir = h2_mkdir,
};

int main(int argc, char **argv) {
  if (open_device("/dev/loop0") < 0) {
    printf("Failed to open device\n");
    return 1;
  }

  superblock *sb = malloc(sizeof(superblock));
  read_block(0, sb, sizeof(superblock));
  init_ilist(sb);
  free(sb);

  h2_mkdir("/foo", 0755);

  int status = fuse_main(argc, argv, &h2_oper, NULL);
  cleanup_ilist();
  close_device();
  return status;
}
