/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.regression;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setAutoBuilding;
import static org.eclipse.core.tests.resources.ResourceTestUtil.updateProjectDescription;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForBuild;
import static org.junit.Assert.assertEquals;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.internal.builders.ClearMarkersBuilder;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests duplicate resource change events caused by a builder that makes
 * no changes.
 */
public class Bug_147232 implements IResourceChangeListener {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	/**
	 * Records the number of times we have seen the file creation delta
	 */
	int deltaSeenCount;

	IFile file;
	IProject project;

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		//we are only concerned with seeing duplicate post change events
		if (event.getType() != IResourceChangeEvent.POST_CHANGE) {
			return;
		}
		//record occurrence of the file creation delta if we find it
		IResourceDelta delta = event.getDelta().findMember(file.getFullPath());
		if (delta != null && delta.getKind() == IResourceDelta.ADDED) {
			deltaSeenCount++;
		}
	}

	@Before
	public void setUp() throws Exception {
		// make the builder wait after running to all a POST_CHANGE event to occur before POST_BUILD
		ClearMarkersBuilder.pauseAfterBuild = true;
	}

	@After
	public void tearDown() throws Exception {
		getWorkspace().removeResourceChangeListener(this);
		ClearMarkersBuilder.pauseAfterBuild = false;
	}

	@Test
	public void testBug() throws CoreException {
		project = getWorkspace().getRoot().getProject("Bug_147232");
		file = project.getFile("file.txt");
		getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_BUILD | IResourceChangeEvent.POST_BUILD);
		setAutoBuilding(false);
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		updateProjectDescription(project).addingCommand(ClearMarkersBuilder.BUILDER_NAME).withTestBuilderId("builder")
				.apply();
		setAutoBuilding(true);
		//create a file in the project to trigger a build
		createInWorkspace(file);
		waitForBuild();
		assertEquals("2.0", 1, deltaSeenCount);
	}

}
