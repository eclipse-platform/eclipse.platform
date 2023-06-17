/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime.perf;

import java.util.HashMap;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.runtime.RuntimeTest;

public class BenchPath extends RuntimeTest {
	public BenchPath() {
		super();
	}

	public BenchPath(String testName) {
		super(testName);
	}

	/**
	 * Tests performance of equals and hashCode by using paths
	 * as keys in a hash map.
	 */
	public void testHash() {
		final int REPEAT = 500000;
		final IPath[] paths = generateVariousPaths();
		final HashMap<IPath, String> map = new HashMap<>(32);
		for (IPath path : paths) {
			map.put(path, "");
		}
		final int numPaths = paths.length;
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				for (int p = 0; p < numPaths; p++) {
					map.get(paths[p]);
				}
			}
		}.run(this, 10, REPEAT);
	}

	/**
	 * Tests the performance of path creation
	 */
	public void testPathCreation() {
		final int REPEAT = 50000;
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				//folders (5)
				IPath.fromOSString("/");
				IPath.fromOSString("/Foo");
				IPath.fromOSString("/Foo/bar");
				IPath.fromOSString("/Foo/bar/baz");
				IPath.fromOSString("/Foo/bar/baz/blap");

				//files (15)
				IPath.fromOSString("/Foo/abc.txt");
				IPath.fromOSString("/Foo/bar/abc.txt");
				IPath.fromOSString("/Foo/bar/baz/abc.txt");
				IPath.fromOSString("/Foo/bar/baz/blap/abc.txt");
				IPath.fromOSString("/Foo/bar/abc.txt");
				IPath.fromOSString("/Foo/bar/baz/abc.txt");
				IPath.fromOSString("/Foo/bar/baz/blap/abc.txt");
				IPath.fromOSString("/Foo/bar/baz/abc.txt");
				IPath.fromOSString("/Foo/bar/baz/blap/abc.txt");
				IPath.fromOSString("/Foo/bar/baz/abc.txt");
				IPath.fromOSString("/Foo/bar/baz/blap/abc.txt");
				IPath.fromOSString("/Foo/bar/baz/abc.txt");
				IPath.fromOSString("/Foo/bar/baz/blap/abc.txt");
				IPath.fromOSString("/Foo/bar/baz/blap/blam/abc.txt");
				IPath.fromOSString("/Foo/bar/baz/blap/blam/blip/boop/abc.txt");
			}
		}.run(this, 20, REPEAT);
	}

	/**
	 * Tests the performance of Path.toOSString
	 */
	public void testToOSString() {
		final int REPEAT = 50000;
		final IPath[] paths = generateVariousPaths();
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				for (int p = paths.length; --p >= 0;) {
					paths[p].toOSString();
				}
			}
		}.run(this, 10, REPEAT);
	}

	/**
	 * Tests the performance of Path.toOSString
	 */
	public void testToString() {
		final int REPEAT = 50000;
		final IPath[] paths = generateVariousPaths();
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				for (int p = paths.length; --p >= 0;) {
					paths[p].toString();
				}
			}
		}.run(this, 10, REPEAT);
	}

	/**
	 * Returns an array containing various paths.
	 */
	private IPath[] generateVariousPaths() {
		IPath[] paths = new IPath[20];
		int i = 0;
		paths[i++] = IPath.fromOSString("/");
		paths[i++] = IPath.fromOSString("/Foo");
		paths[i++] = IPath.fromOSString("/Foo/bar");
		paths[i++] = IPath.fromOSString("/Foo/bar/baz");
		paths[i++] = IPath.fromOSString("/Foo/bar/baz/blap");

		//files (15)
		paths[i++] = IPath.fromOSString("/Foo/abc.txt");
		paths[i++] = IPath.fromOSString("/Foo/bar/abc.txt");
		paths[i++] = IPath.fromOSString("/Foo/bar/baz/abc.txt");
		paths[i++] = IPath.fromOSString("/Foo/bar/baz/blap/abc.txt");
		paths[i++] = IPath.fromOSString("/Foo/bar/abc.txt");
		paths[i++] = IPath.fromOSString("/Foo/bar/baz/abc.txt");
		paths[i++] = IPath.fromOSString("/Foo/bar/baz/blap/abc.txt");
		paths[i++] = IPath.fromOSString("/Foo/bar/baz/abc.txt");
		paths[i++] = IPath.fromOSString("/Foo/bar/baz/blap/abc.txt");
		paths[i++] = IPath.fromOSString("/Foo/bar/baz/abc.txt");
		paths[i++] = IPath.fromOSString("/Foo/bar/baz/blap/abc.txt");
		paths[i++] = IPath.fromOSString("/Foo/bar/baz/abc.txt");
		paths[i++] = IPath.fromOSString("/Foo/bar/baz/blap/abc.txt");
		paths[i++] = IPath.fromOSString("/Foo/bar/baz/blap/blam/abc.txt");
		paths[i++] = IPath.fromOSString("/Foo/bar/baz/blap/blam/blip/boop/abc.txt");

		return paths;
	}
}
