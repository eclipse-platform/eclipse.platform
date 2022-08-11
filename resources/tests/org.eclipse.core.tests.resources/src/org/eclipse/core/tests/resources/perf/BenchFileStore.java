/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.perf;

import java.io.IOException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Benchmarks basic operations on the IFileStore interface
 */
public class BenchFileStore extends ResourceTest {

	abstract class StoreTestRunner extends PerformanceTestRunner {
		@Override
		protected void setUp() throws CoreException {
			createStores();
		}

		@Override
		protected void tearDown() throws CoreException {
			deleteStores();
		}
	}

	private static final int LOOP_SIZE = 5000;

	private static final int REPEATS = 30;
	protected IFileStore existingStore;

	protected IFileStore nonexistingStore;

	protected void createStores() throws CoreException {
		existingStore = EFS.getFileSystem(EFS.SCHEME_FILE).getStore(getRandomLocation());
		try {
			existingStore.openOutputStream(EFS.NONE, null).close();
		} catch (IOException e) {
			fail("BenchFileStore.createStores", e);
		}
		nonexistingStore = EFS.getFileSystem(EFS.SCHEME_FILE).getStore(getRandomLocation());
	}

	protected void deleteStores() throws CoreException {
		existingStore.delete(EFS.NONE, null);
	}

	public void testStoreExists() {
		new StoreTestRunner() {
			@Override
			protected void test() {
				existingStore.fetchInfo().exists();
				nonexistingStore.fetchInfo().exists();
			}
		}.run(this, REPEATS, LOOP_SIZE);
	}

	public void testStoreIsReadOnly() {
		StoreTestRunner storeTestRunner = new StoreTestRunner() {
			@Override
			protected void test() {
				existingStore.fetchInfo().getAttribute(EFS.ATTRIBUTE_READ_ONLY);
				nonexistingStore.fetchInfo().getAttribute(EFS.ATTRIBUTE_READ_ONLY);
			}
		};
		storeTestRunner.setRegressionReason("Performance slowed down because new functionality was added in Windows filessytem natives (see Bug 318170).");
		storeTestRunner.run(this, REPEATS, LOOP_SIZE);
	}

	public void testStoreLastModified() {
		StoreTestRunner runner = new StoreTestRunner() {
			@Override
			protected void test() {
				existingStore.fetchInfo().getLastModified();
				nonexistingStore.fetchInfo().getLastModified();
			}
		};
		runner.setFingerprintName("Get file last modified time");
		runner.run(this, REPEATS, LOOP_SIZE);
	}
}