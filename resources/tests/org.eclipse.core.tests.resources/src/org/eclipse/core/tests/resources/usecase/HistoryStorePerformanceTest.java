/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.usecase;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.ResourceTest;

public class HistoryStorePerformanceTest extends ResourceTest {

	@Override
	public void setUp() throws Exception {
		IProject project = getWorkspace().getRoot().getProject("Project");
		project.create(getMonitor());
		project.open(getMonitor());
		IWorkspaceDescription description = getWorkspace().getDescription();
		description.setFileStateLongevity(1000 * 3600 * 24); // 1 day
		description.setMaxFileStates(10000);
		description.setMaxFileStateSize(1024 * 1024); // 1 Mb
		getWorkspace().setDescription(description);
	}

	@Override
	protected void tearDown() throws Exception {
		IProject project = getWorkspace().getRoot().getProject("Project");
		project.clearHistory(getMonitor());
		project.delete(true, true, getMonitor());
	}

	public void testPerformance() {

		/* Create common objects. */
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile file = project.getFile("file.txt");
		try {
			file.create(null, true, null);
		} catch (CoreException e) {
			fail("0.0", e);
		}
		String contents = "fixed contents for performance test";

		int nTimes = 1000;
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < nTimes; i++) {
			try {
				file.setContents(getContents(contents), true, true, null);
			} catch (CoreException e) {
				fail("1.0", e);
			}
		}
		long endTime = System.currentTimeMillis();
		System.out.println("Adding " + nTimes + " states: " + (endTime - startTime) + " milliseconds.");

		startTime = System.currentTimeMillis();
		try {
			file.getHistory(null);
		} catch (CoreException e) {
			fail("2.0", e);
		}
		endTime = System.currentTimeMillis();
		System.out.println("Retrieving " + nTimes + " states: " + (endTime - startTime) + " milliseconds.");

		startTime = System.currentTimeMillis();
		try {
			file.clearHistory(null);
		} catch (CoreException e) {
			fail("3.0", e);
		}
		endTime = System.currentTimeMillis();
		System.out.println("Removing " + nTimes + " states: " + (endTime - startTime) + " milliseconds.");
	}
}
