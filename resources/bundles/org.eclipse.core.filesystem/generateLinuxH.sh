#!/bin/sh
# Regenerates the glibc bindings in src-gen. Needs jextract on the PATH,
# https://jdk.java.net/jextract/, and has to run on Linux.
#
# No --library: that would emit libraryLookup("libc.so"), which cannot be opened
# on glibc, where the library is libc.so.6. Without it the generated lookup falls
# back to the linker's default lookup, which already sees libc.
set -eu
cd "$(dirname "$0")"

rm -rf src-gen/org/linux
jextract --output src-gen \
	--define-macro _GNU_SOURCE \
	--include-function statx \
	--include-function opendir \
	--include-function readdir \
	--include-function closedir \
	--include-function dirfd \
	--include-function readlinkat \
	--include-struct statx \
	--include-struct statx_timestamp \
	--include-struct dirent \
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
	dirent.h errno.h fcntl.h limits.h sys/stat.h unistd.h

# jextract cannot select single struct fields or accessors, so drop every generated
# member that neither the sources in src nor another kept member refer to.
find src -name '*.java' | xargs awk -v gen="$(echo src-gen/org/linux/*.java)" '
function code(line) {
	# Drops comments and string literals, which name fields without using them.
	if (line ~ /^[[:space:]]*(\/\*|\*|\/\/)/) {
		return ""
	}
	gsub(/"[^"]*"/, "\"\"", line)
	return line "\n"
}
function mentions(text, token,    rest, i, before, after) {
	rest = text
	while ((i = index(rest, token)) > 0) {
		before = i == 1 ? "" : substr(rest, i - 1, 1)
		after = substr(rest, i + length(token), 1)
		if (before !~ /[[:alnum:]_$.]/ && after !~ /[[:alnum:]_$]/) {
			return 1
		}
		rest = substr(rest, i + length(token))
	}
	return 0
}
{ sources = sources code($0) }
END {
	n = split(gen, files, " ")
	for (f = 1; f <= n; f++) {
		cls = files[f]; sub(/.*\//, "", cls); sub(/\.java$/, "", cls)
		unit = cls; sub(/\$shared$/, "", unit)
		depth = 0; javadoc = 0; complete = 1; phase = "head"
		while ((getline line < files[f]) > 0) {
			if (phase == "head") {
				head[f] = head[f] line "\n"
				if (line ~ /^(public )?(final )?class /) { phase = "body" }
				continue
			}
			if (phase == "tail" || (depth == 0 && line == "}")) {
				phase = "tail"; tail[f] = tail[f] line "\n"
				continue
			}
			if (m == 0 || complete) {
				m++; mfile[m] = f; mcls[m] = cls; munit[m] = unit; complete = 0
				if (line ~ /^[[:space:]]*$/) { blank[m] = 1; complete = 1 }
			}
			text[m] = text[m] line "\n"
			if (blank[m]) { continue }
			if (line ~ /^[[:space:]]*\/\*\*/) { javadoc = 1 }
			if (javadoc) {
				if (line ~ /\*\//) { javadoc = 0 }
				continue
			}
			mcode[m] = mcode[m] code(line)
			if (name[m] == "") {
				decl = line
				if (decl ~ /class [[:alnum:]_$]+/) {
					sub(/.*class /, "", decl); sub(/[^[:alnum:]_$].*/, "", decl)
				} else {
					sub(/[[:space:]]*[=(;].*/, "", decl); sub(/.*[[:space:]]/, "", decl)
				}
				name[m] = decl
				public[m] = line ~ /^[[:space:]]*public /
			}
			opened = gsub(/\{/, "{", line); closed = gsub(/\}/, "}", line)
			depth += opened - closed
			if (depth == 0 && line ~ /[;}][[:space:]]*$/) { complete = 1 }
		}
		close(files[f])
	}
	# Keeps what is reachable from the sources, repeating until nothing changes.
	do {
		changed = 0
		used = sources
		for (i = 1; i <= m; i++) { if (keep[i]) { used = used mcode[i] } }
		for (i = 1; i <= m; i++) {
			if (keep[i] || blank[i]) { continue }
			found = name[i] == mcls[i] || mentions(used, mcls[i] "." name[i]) || mentions(used, munit[i] "." name[i])
			for (j = 1; !found && j <= m; j++) {
				if (keep[j] && munit[j] == munit[i] && mentions(mcode[j], name[i])) { found = 1 }
			}
			if (found) { keep[i] = 1; changed = 1 }
		}
	} while (changed)
	for (f = 1; f <= n; f++) {
		out = head[f]
		for (i = 1; i <= m; i++) {
			if (mfile[i] == f && (keep[i] || blank[i])) { out = out text[i] }
		}
		printf "%s%s", out, tail[f] > files[f]
		close(files[f])
	}
}'

# The bundle's compiler settings report unused imports and every non-NLS string, so drop
# the imports a file does not use and suppress the rest on each top-level class.
used() {
	grep -vE '^[[:space:]]*(import |\*|/\*\*|//)' "$2" \
		| grep -qE "(^|[^.[:alnum:]_\$])($1)([^[:alnum:]_\$]|\$)"
}
for f in src-gen/org/linux/*.java; do
	cat -s "$f" > "$f.tmp" && mv "$f.tmp" "$f"
	for entry in \
		'java.lang.invoke.*=MethodHandles?|MethodType|VarHandle' \
		'java.nio.ByteOrder=ByteOrder' \
		'java.util.*=Arrays|Objects|Optional|List|Map' \
		'java.util.function.*=Consumer|Function|Supplier|BiFunction|IntFunction' \
		'java.util.stream.*=Stream|IntStream|Collectors' \
		'static java.lang.foreign.ValueLayout.*=JAVA_[A-Z_]+|ADDRESS(_UNALIGNED)?|Of[A-Z][a-z]+' \
		'static java.lang.foreign.MemoryLayout.PathElement.*=groupElement|sequenceElement|dereferenceElement'; do
		import=$(printf '%s' "${entry%%=*}" | sed 's/[.*]/\\&/g')
		used "${entry#*=}" "$f" || sed -i "\\|^import $import;\$|d" "$f"
	done
	sed -i 's/^\(public \)\{0,1\}\(final \)\{0,1\}class /@SuppressWarnings("all")\n&/' "$f"
done
