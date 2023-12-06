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
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setAutoBuilding;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForBuild;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.internal.builders.AbstractBuilderTest;
import org.eclipse.core.tests.internal.builders.ClearMarkersBuilder;

/**
 * Tests duplicate resource change events caused by a builder that makes
 * no changes.
 */
public class Bug_147232 extends AbstractBuilderTest implements IResourceChangeListener {
	/**
	 * Records the number of times we have seen the file creation delta
	 */
	int deltaSeenCount;

	IFile file;
	IProject project;

	public Bug_147232(String name) {
		super(name);
	}

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

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// make the builder wait after running to all a POST_CHANGE event to occur before POST_BUILD
		ClearMarkersBuilder.pauseAfterBuild = true;
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		getWorkspace().removeResourceChangeListener(this);
		ClearMarkersBuilder.pauseAfterBuild = false;
	}

	public void testBug() throws CoreException {
		project = getWorkspace().getRoot().getProject("Bug_147232");
		file = project.getFile("file.txt");
		getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_BUILD | IResourceChangeEvent.POST_BUILD);
		setAutoBuilding(false);
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		addBuilder(project, ClearMarkersBuilder.BUILDER_NAME);
		setAutoBuilding(true);
		//create a file in the project to trigger a build
		createInWorkspace(file);
		waitForBuild();
		assertEquals("2.0", 1, deltaSeenCount);
	}

}
