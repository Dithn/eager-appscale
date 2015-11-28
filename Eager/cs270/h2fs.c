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

static void h2_split_filename(const char *path, char *parent, char *base);
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
  inumber current_inumber = 0;
  inode current;
  char path_copy[128];
  strcpy(path_copy, path);
  char *segment = strtok(path_copy, "/");
  while (segment != NULL) {
    get_inode(current_inumber, &current);
    if (S_ISDIR(current.mode)) {
      direntry *dir = malloc(current.size);
      int entries = current.size / sizeof(direntry);
      read_dir(&current, dir);
      int i;
      int found_name = 0;
      for (i = 0; i < entries; i++) {
	if (strcmp((dir+i)->name, segment) == 0) {
	  current_inumber = (dir+i)->number;
	  found_name = 1;
	  break;
	}
      }
      free(dir);
      if (!found_name) {
	return 0;
      }
      segment = strtok(NULL, "/");
    } else if (strtok(NULL, "/") != NULL) {
      return 0;
    }
  }
  *in = current_inumber;
  printf("Resolve: %s => %ld\n", path, *in);
  return 1;
}

static int h2_create(const char *path, mode_t mode, struct fuse_file_info *file_info) {
  printf("create file %s\n", path);
  char parent[32], base[32];
  memset(parent, '\0', sizeof(parent));
  memset(base, '\0', sizeof(base));
  h2_split_filename(path, parent, base);
  inumber parent_num;
  if (h2_resolve(parent, &parent_num)) {
    inode parent;
    get_inode(parent_num, &parent);
    off_t newsize = parent.size + sizeof(direntry);
    printf("New dir size: %ld\n", newsize);
    direntry *parentdir = malloc(newsize);
    if (!read_dir(&parent, parentdir)) {
      free(parentdir);
      return -EIO;
    }

    int newdir_index = parent.size / sizeof(direntry);
    inumber newdir_number;
    time_t now;
    time(&now);
    if (allocate_inode(&newdir_number)) {
      printf("New inode allocated: %ld\n", newdir_number);
      inode newdir;
      newdir.mode = mode | S_IFREG;
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
      if (!write_dir(parent_num, &parent, parentdir, newsize)) {
	free(parentdir);
	return -EIO;
      }
      free(parentdir);
      return 0;
    }
  }
  return -ENOENT;
}

static int h2_utimens(const char *path, const struct timespec tv[2]) {
  inumber in;
  if (h2_resolve(path, &in)) {
    inode i_node;
    get_inode(in, &i_node);
    i_node.atime = tv[0].tv_sec;
    i_node.mtime = tv[1].tv_sec;
    write_inode(in, &i_node);
    return 0;
  }
  return -ENOENT;
}

static int h2_getattr(const char *path, struct stat *stbuf) {
  printf("Getting attributes: %s\n", path);
  inumber in;
  if (h2_resolve(path, &in)) {
    inode node;
    get_inode(in, &node);
    h2_init_stat(&node, stbuf);
    return 0;
  }
  return -ENOENT;
}

static int h2_opendir(const char *path, struct fuse_file_info *file_info) {
  printf("Open dir: %s\n", path);
  inumber in;
  if (h2_resolve(path, &in)) {
    return 0;
  }
  return -ENOENT;
}

static void h2_split_filename(const char *path, char *parent, char *base) {
  char *last = strrchr(path, '/');
  if (last != NULL) {
    int parent_length = strlen(path) - strlen(last);
    if (parent_length == 0) {
      strcpy(parent, "/");
    } else {
      strncpy(parent, path, parent_length);
    }
    strcpy(base, last + 1);
  } else {
    strcpy(parent, "/");
    strcpy(base, path);
  }
}

static int h2_rmdir(const char *path) {
  inumber number;
  if (h2_resolve(path, &number)) {
    inode target;
    get_inode(number, &target);
    if (target.size > 0) {
      return -ENOTEMPTY;
    } else if (!S_ISDIR(target.mode)) {
      return -ENOTDIR;
    }

    char parent[32], base[32];
    memset(parent, '\0', sizeof(parent));
    memset(base, '\0', sizeof(base));
    h2_split_filename(path, parent, base);
    inumber parent_num;
    h2_resolve(parent, &parent_num);
    inode parent_node;
    get_inode(parent_num, &parent_node);
    direntry* parent_dir = malloc(parent_node.size);
    int new_size = parent_node.size - sizeof(direntry);
    direntry* new_parent_dir = malloc(new_size);
    int file_count = parent_node.size / sizeof(direntry);
    read_dir(&parent_node, parent_dir);
    int i;
    int j = 0;
    for (i = 0; i < file_count; i++) {
      if (strcmp((parent_dir+i)->name, base) != 0) {
	*(new_parent_dir + j) = *(parent_dir+i);
	j++;
      }
    }
    write_dir(parent_num, &parent_node, new_parent_dir, new_size);
    release_inode(number);
    free(parent_dir);
    free(new_parent_dir);
    return 0;
  }
  return -ENOENT;
}

static int h2_mkdir(const char *path, mode_t mode) {
  char parent[32], base[32];
  memset(parent, '\0', sizeof(parent));
  memset(base, '\0', sizeof(base));
  h2_split_filename(path, parent, base);
  printf("mkdir parent: %s; base: %s\n", parent, base);
  inumber parent_num;
  if (h2_resolve(parent, &parent_num)) {
    inode parent;
    get_inode(parent_num, &parent);
    off_t newsize = parent.size + sizeof(direntry);
    printf("New dir size: %ld\n", newsize);
    direntry *parentdir = malloc(newsize);
    if (!read_dir(&parent, parentdir)) {
      return -EIO;
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
      if (!write_dir(parent_num, &parent, parentdir, newsize)) {
	return -EIO;
      }
      free(parentdir);
      return 0;
    }
  }
  return -ENOENT;
}


static int h2_readdir(const char *path, void *buf, fuse_fill_dir_t filler,
                         off_t offset, struct fuse_file_info *fi) {
  printf("Reading dir: %s\n", path);
  inumber in;
  if (h2_resolve(path, &in)) {
    inode node;
    get_inode(in, &node);
    filler(buf, ".", NULL, 0);
    filler(buf, "..", NULL, 0);
    int i;
    if (node.size > 0) {
      direntry *dir = malloc(node.size);
      read_dir(&node, dir);
      int lim = node.size / sizeof(direntry);
      for (i = 0; i < lim; i++) {
	filler(buf, (dir+i)->name, NULL, 0);
      }
      free(dir);
    }
    return 0;
  }
  return -ENOENT;
}

static struct fuse_operations h2_oper = {
  .getattr = h2_getattr,
  .readdir = h2_readdir,
  .mkdir = h2_mkdir,
  .rmdir = h2_rmdir,
  .opendir = h2_opendir,
  .create = h2_create,
  .utimens = h2_utimens,
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
