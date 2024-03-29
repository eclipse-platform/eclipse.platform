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
package org.eclipse.core.tests.resources.perf;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInputStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class HistoryStorePerformanceTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	@Before
	public void setUp() throws Exception {
		IProject project = getWorkspace().getRoot().getProject("Project");
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		IWorkspaceDescription description = getWorkspace().getDescription();
		description.setFileStateLongevity(1000 * 3600 * 24); // 1 day
		description.setMaxFileStates(10000);
		description.setMaxFileStateSize(1024 * 1024); // 1 Mb
		getWorkspace().setDescription(description);
	}

	@After
	public void tearDown() throws Exception {
		IProject project = getWorkspace().getRoot().getProject("Project");
		project.clearHistory(createTestMonitor());
	}

	@Test
	public void testPerformance() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile file = project.getFile("file.txt");
		file.create(null, true, null);
		String contents = "fixed contents for performance test";

		int nTimes = 1000;
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < nTimes; i++) {
			file.setContents(createInputStream(contents), true, true, null);
		}
		long endTime = System.currentTimeMillis();
		System.out.println("Adding " + nTimes + " states: " + (endTime - startTime) + " milliseconds.");

		startTime = System.currentTimeMillis();
		file.getHistory(null);
		endTime = System.currentTimeMillis();
		System.out.println("Retrieving " + nTimes + " states: " + (endTime - startTime) + " milliseconds.");

		startTime = System.currentTimeMillis();
		file.clearHistory(null);
		endTime = System.currentTimeMillis();
		System.out.println("Removing " + nTimes + " states: " + (endTime - startTime) + " milliseconds.");
	}
}
