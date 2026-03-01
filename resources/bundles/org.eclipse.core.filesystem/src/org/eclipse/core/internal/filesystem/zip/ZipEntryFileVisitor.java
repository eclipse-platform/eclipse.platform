package org.eclipse.core.internal.filesystem.zip;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;

public class ZipEntryFileVisitor extends SimpleFileVisitor<Path> {
	private final Path zipRoot;
	private final List<ZipEntry> entryList;

	public ZipEntryFileVisitor(Path zipRoot) {
		this.zipRoot = zipRoot;
		this.entryList = new ArrayList<>();
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
		String entryName = zipRoot.relativize(file).toString();
		if (!Files.isDirectory(file)) {
			ZipEntry zipEntry = new ZipEntry(entryName);
			zipEntry.setSize(attrs.size());
			zipEntry.setTime(attrs.lastModifiedTime().toMillis());
			zipEntry.setMethod(ZipEntry.DEFLATED);
			entryList.add(zipEntry);
		} else {
			entryList.add(new ZipEntry(entryName + "/")); //$NON-NLS-1$
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
		if (!dir.equals(zipRoot)) {
			String dirName = zipRoot.relativize(dir).toString() + "/"; //$NON-NLS-1$
			entryList.add(new ZipEntry(dirName));
			return FileVisitResult.SKIP_SUBTREE; // Skip the subdirectories
		}
		return FileVisitResult.CONTINUE;
	}

	public List<ZipEntry> getEntries() {
		return entryList;
	}
}
