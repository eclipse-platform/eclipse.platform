/*******************************************************************************
 * Copyright (c) 2026 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.tests.filesystem;

import java.net.URI;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.tests.harness.FileSystemHelper;
import org.eclipse.core.tests.internal.filesystem.ram.MemoryTree;
import org.eclipse.core.tests.resources.TestUtil;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * A test extension for automatically creating and disposing a file store for
 * the local file system or in memory.
 */
public class FileStoreCreationExtension implements BeforeEachCallback, AfterEachCallback {
	public enum FileSystemType {
		LOCAL, IN_MEMORY
	}

	private final FileSystemType fileSystemType;

	private IFileStore fileStore;

	public FileStoreCreationExtension(FileSystemType fileSystemType) {
		this.fileSystemType = fileSystemType;
	}

	public IFileStore getFileStore() {
		return fileStore;
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		switch(fileSystemType) {
		case LOCAL:
			var fileStoreLocation = FileSystemHelper
					.getRandomLocation(FileSystemHelper.getTempDir())
					.append(IPath.SEPARATOR + context.getDisplayName());
			fileStore = EFS.getLocalFileSystem().getStore(fileStoreLocation);
			break;
		case IN_MEMORY:
			MemoryTree.TREE.deleteAll();
			fileStore = EFS.getStore(URI.create("mem:/baseStore"));
			break;
		}
		fileStore.mkdir(EFS.NONE, null);

	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		try {
			fileStore.delete(EFS.NONE, null);
		} catch (CoreException e) {
			TestUtil.log(IStatus.ERROR, context.getDisplayName(), "Could not delete file store: " + fileStore, e);
		}
		switch (fileSystemType) {
		case IN_MEMORY:
			MemoryTree.TREE.deleteAll();
			break;
		case LOCAL:
			// Nothing to do
		}
	}

}
