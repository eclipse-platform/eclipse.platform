/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.localstore;

import java.io.File;
import java.util.Date;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.tests.resources.ResourceTest;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RefreshLocalPerformanceTest extends ResourceTest {
	/** big site default volume (windows) */
	public static final String bigSiteDevice = "d:";

	/** big site initial location */
	public static final IPath bigSiteLocation = new Path(bigSiteDevice, "/bigsite");

	/** benchmark */
	public Date startDate;

	public RefreshLocalPerformanceTest() {
		super();
	}

	public RefreshLocalPerformanceTest(String name) {
		super(name);
	}

	protected int countChildren(File root) {
		String[] children = root.list();
		if (children == null) {
			return 0;
		}
		int result = 0;
		for (String element : children) {
			File child = new File(root, element);
			if (child.isDirectory()) {
				result += countChildren(child);
			}
			result++;
		}
		return result;
	}

	public String dispTime(long diff) {
		return String.valueOf(diff);
	}

	public void startClock() {
		startDate = new Date();
	}

	public long stopClock() {
		Date stopDate = new Date();
		return stopDate.getTime() - startDate.getTime();
	}

	/**
	 * Defines only a default mapping to a project and refreshes locally.
	 */
	@Test
	public void testLocalRefreshPerformance() throws Exception {
		// test if the test can be done in this machine
		Assume.assumeTrue(bigSiteLocation.toFile().isDirectory());

		// create common objects
		int n = 10;
		IProject project = getWorkspace().getRoot().getProject("MyTestProject");
		IProjectDescription description = getWorkspace().newProjectDescription(project.getName());
		description.setLocation(bigSiteLocation);

		// performance data
		long averageWithTree = 0;
		long averageWithoutTree = 0;
		long[] withoutTree = new long[n];
		long[] withTree = new long[n];

		// report the number of files to be refreshed
		int numberOfFiles = countChildren(bigSiteLocation.toFile());
		System.out.println("Number of local resources: " + numberOfFiles);

		// test each project
		for (int i = 0; i < n; i++) {
			project.create(description, null);
			project.open(null);

			// refresh local (new project)
			System.out.print("NO TREE: ");
			startClock();
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			withoutTree[i] = stopClock();
			System.out.println(dispTime(withoutTree[i]));
			// test with existing workspace tree
			System.out.print("TREE: ");
			startClock();
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			withTree[i] = stopClock();
			System.out.println(dispTime(withTree[i]));

			// calculate average
			averageWithoutTree += withoutTree[i];
			averageWithTree += withTree[i];

			// delete project but leave contents
			project.delete(false, false, null);
		}
		averageWithoutTree /= n;
		averageWithTree /= n;
		System.out.println("Average without tree: " + averageWithoutTree);
		System.out.println("Average with tree: " + averageWithTree);
	}
}
