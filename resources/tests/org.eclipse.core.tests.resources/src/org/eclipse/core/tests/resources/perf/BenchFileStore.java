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
import org.eclipse.core.filesystem.*;
import org.eclipse.core.internal.filesystem.local.LocalFileNativesManager;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Benchmarks basic operations on the IFileStore interface
 */
public class BenchFileStore extends ResourceTest {

	private static final int LOOP_SIZE = 5000;

	private static final int REPEATS = 300;

	class StoreTestRunner extends PerformanceTestRunner {
		private boolean exits;
		protected IFileStore store;

		public StoreTestRunner(boolean exits) {
			this.exits = exits;
		}

		@Override
		protected void setUp() throws CoreException {
			store = EFS.getFileSystem(EFS.SCHEME_FILE).getStore(getRandomLocation());
			if (exits) {
				try {
					store.openOutputStream(EFS.NONE, null).close();
				} catch (IOException e) {
					fail("BenchFileStore.createStores", e);
				}
			}
		}

		@Override
		protected void tearDown() throws CoreException {
			store.delete(EFS.NONE, null);
		}

		@Override
		protected void test() {
			IFileInfo info = store.fetchInfo();
			if (info.exists()) {
				info.getAttribute(EFS.ATTRIBUTE_READ_ONLY);
				info.getLastModified();
			}
		}
	}

	public void testStoreExitsNative() {
		withNatives(true, () -> {
			new StoreTestRunner(true).run(this, REPEATS, LOOP_SIZE);
		});

	}

	public void testStoreNotExitsNative() {
		withNatives(true, () -> {
			new StoreTestRunner(false).run(this, REPEATS, LOOP_SIZE);
		});
	}

	public void testStoreExitsNio() {
		withNatives(false, () -> {
			new StoreTestRunner(true).run(this, REPEATS, LOOP_SIZE);
		});

	}

	public void testStoreNotExitsNio() {
		withNatives(false, () -> {
			new StoreTestRunner(false).run(this, REPEATS, LOOP_SIZE);
		});
	}

	private static void withNatives(boolean natives, Runnable runnable) {
		try {
			assertEquals("can't set natives to the desired value", natives,
					LocalFileNativesManager.setUsingNative(natives));
			runnable.run();
		} finally {
			LocalFileNativesManager.reset();
		}
	}

}