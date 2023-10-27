/*******************************************************************************
 * Copyright (c) Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
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
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A test rule for automatically creating and disposing a file store for the
 * local file system or in memory.
 */
public class FileStoreCreationRule extends ExternalResource {
	public enum FileSystemType {
		LOCAL, IN_MEMORY
	}

	private final FileSystemType fileSystemType;

	private String testName;

	private IFileStore fileStore;

	public FileStoreCreationRule(FileSystemType fileSystemType) {
		this.fileSystemType = fileSystemType;
	}

	public IFileStore getFileStore() {
		return fileStore;
	}

	@Override
	public Statement apply(Statement base, Description description) {
		testName = description.getDisplayName();
		return super.apply(base, description);
	}

	@Override
	protected void before() throws Throwable {
		switch(fileSystemType) {
		case LOCAL:
			var fileStoreLocation = FileSystemHelper
					.getRandomLocation(FileSystemHelper.getTempDir()).append(IPath.SEPARATOR + testName);
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
	protected void after() {
		try {
			fileStore.delete(EFS.NONE, null);
		} catch (CoreException e) {
			TestUtil.log(IStatus.ERROR, testName, "Could not delete file store: " + fileStore, e);
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
