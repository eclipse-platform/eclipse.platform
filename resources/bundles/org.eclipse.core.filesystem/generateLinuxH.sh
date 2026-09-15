#!/bin/sh
# Regenerates the glibc bindings in src-gen. Needs jextract on the PATH,
# https://jdk.java.net/jextract/, and has to run on Linux.
#
# No --library: that would emit libraryLookup("libc.so"), which cannot be opened
# on glibc, where the library is libc.so.6. Without it the generated lookup falls
# back to the linker's default lookup, which already sees libc.
set -eu
cd "$(dirname "$0")"

cat > /tmp/LibC.h <<'HEADER'
#define _GNU_SOURCE
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <linux/stat.h>
#include <sys/stat.h>
#include <unistd.h>
HEADER

jextract --output src-gen \
	--include-function statx \
	--include-function opendir \
	--include-function readdir \
	--include-function closedir \
	--include-function dirfd \
	--include-function readlinkat \
	--include-function chmod \
	--include-struct statx \
	--include-struct statx_timestamp \
	--include-struct dirent \
	--include-constant AT_FDCWD \
	--include-constant AT_SYMLINK_NOFOLLOW \
	--include-constant AT_STATX_SYNC_AS_STAT \
	--include-constant STATX_TYPE \
	--include-constant STATX_MODE \
	--include-constant STATX_SIZE \
	--include-constant STATX_MTIME \
	--include-constant ENOENT \
	--include-constant PATH_MAX \
	--include-constant S_IFMT \
	--include-constant S_IFDIR \
	--include-constant S_IFLNK \
	--include-constant S_IRUSR --include-constant S_IWUSR --include-constant S_IXUSR \
	--include-constant S_IRGRP --include-constant S_IWGRP --include-constant S_IXGRP \
	--include-constant S_IROTH --include-constant S_IWOTH --include-constant S_IXOTH \
	--target-package org.linux \
	--header-class-name LibC \
	/tmp/LibC.h
