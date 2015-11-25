#define FUSE_USE_VERSION 30
#include <fuse.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>

#include "block_io.h"
#include "ilist.h"

static int h2_getattr(const char *path, struct stat *stbuf) {
  printf("Getting attributes: %s\n", path);
  if (strcmp(path, "/") == 0) {
    inode root;
    get_inode(0, &root);
    stbuf->st_ino = 0;
    stbuf->st_mode = root.mode;
    stbuf->st_nlink = root.links;
    stbuf->st_uid = root.user_id;
    stbuf->st_gid = root.group_id;
    stbuf->st_size = root.size;
    stbuf->st_atime = root.atime;
    stbuf->st_mtime = root.mtime;
    stbuf->st_ctime = root.ctime;
    return 0;
  }
  return -ENOENT;
}

static int h2_readdir(const char *path, void *buf, fuse_fill_dir_t filler,
                         off_t offset, struct fuse_file_info *fi) {
  printf("Reading dir: %s\n", path);
  if (strcmp(path, "/") != 0) {
    return -ENOENT;
  }
  filler(buf, ".", NULL, 0);
  filler(buf, "..", NULL, 0);
  return 0;
}

static struct fuse_operations h2_oper = {
  .getattr = h2_getattr,
  .readdir = h2_readdir,
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

  int status = fuse_main(argc, argv, &h2_oper, NULL);
  cleanup_ilist();
  close_device();
  return status;
}
