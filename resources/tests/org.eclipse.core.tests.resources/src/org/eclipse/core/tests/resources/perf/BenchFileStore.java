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

import static org.eclipse.core.tests.harness.FileSystemHelper.getRandomLocation;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.filesystem.local.LocalFileNativesManager;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.rules.TestName;

/**
 * Benchmarks basic operations on the IFileStore interface
 */
public class BenchFileStore {

	@Rule
	public TestName testName = new TestName();

	private static final int LOOP_SIZE = 5000;

	private static final int REPEATS = 300;

	class StoreTestRunner extends PerformanceTestRunner {
		private final boolean exits;
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
					throw new IllegalStateException("setting up store failed", e);
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

	@Test
	public void testStoreExitsNative() throws Throwable{
		withNatives(true, () -> {
			new StoreTestRunner(true).run(getClass(), testName.getMethodName(), REPEATS, LOOP_SIZE);
		});

	}

	@Test
	public void testStoreNotExitsNative() throws Throwable {
		withNatives(true, () -> {
			new StoreTestRunner(false).run(getClass(), testName.getMethodName(), REPEATS, LOOP_SIZE);
		});
	}

	@Test
	public void testStoreExitsNio() throws Throwable {
		withNatives(false, () -> {
			new StoreTestRunner(true).run(getClass(), testName.getMethodName(), REPEATS, LOOP_SIZE);
		});

	}

	@Test
	public void testStoreNotExitsNio() throws Throwable {
		withNatives(false, () -> {
			new StoreTestRunner(false).run(getClass(), testName.getMethodName(), REPEATS, LOOP_SIZE);
		});
	}

	private static void withNatives(boolean natives, ThrowingRunnable runnable) throws Throwable {
		try {
			assertEquals("can't set natives to the desired value", natives,
					LocalFileNativesManager.setUsingNative(natives));
			runnable.run();
		} finally {
			LocalFileNativesManager.reset();
		}
	}

}