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
package org.eclipse.core.tests.internal.builders;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

/**
 * Tests the PRE_BUILD and POST_BUILD events.
 */
public class BuilderEventTest extends AbstractBuilderTest {
	private BuildEventListener listener;

	public BuilderEventTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		listener = new BuildEventListener();
		int mask = IResourceChangeEvent.PRE_BUILD | IResourceChangeEvent.POST_BUILD | IResourceChangeEvent.POST_CHANGE;
		getWorkspace().addResourceChangeListener(listener, mask);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		getWorkspace().removeResourceChangeListener(listener);
	}

	public void testEventsOnClean() {
		// Create some resource handles
		IProject project = getWorkspace().getRoot().getProject("PROJECT");
		try {
			// Turn auto-building off
			setAutoBuilding(false);
			// Create and open a project
			project.create(getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		// Create and set a build spec for the project
		try {
			IProjectDescription desc = project.getDescription();
			desc.setBuildSpec(new ICommand[] {createCommand(desc, DeltaVerifierBuilder.BUILDER_NAME, "Project2Build2")});
			project.setDescription(desc, getMonitor());
		} catch (CoreException e) {
			fail("1.1", e);
		}
		listener.reset();
		//start with an incremental build
		try {
			getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("2.99", e);
		}
		assertEquals("2.0", getWorkspace(), listener.getSource());
		assertEquals("2.1", IncrementalProjectBuilder.INCREMENTAL_BUILD, listener.getBuildKind());
		assertEquals("2.2", true, listener.hadPreBuild());
		assertEquals("2.3", true, listener.hadPostBuild());
		assertEquals("2.4", true, listener.hadPostChange());

		//do a second incremental build and ensure we still get the events
		listener.reset();
		try {
			getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("2.99", e);
		}
		assertEquals("2.0", getWorkspace(), listener.getSource());
		assertEquals("2.1", IncrementalProjectBuilder.INCREMENTAL_BUILD, listener.getBuildKind());
		assertEquals("2.2", true, listener.hadPreBuild());
		assertEquals("2.3", true, listener.hadPostBuild());
		assertEquals("2.4", true, listener.hadPostChange());

		//do a full build and ensure we still get the event
		listener.reset();
		try {
			getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("3.99", e);
		}
		assertEquals("3.0", getWorkspace(), listener.getSource());
		assertEquals("3.1", IncrementalProjectBuilder.FULL_BUILD, listener.getBuildKind());
		assertEquals("3.2", true, listener.hadPreBuild());
		assertEquals("3.3", true, listener.hadPostBuild());
		assertEquals("3.4", true, listener.hadPostChange());

		//do a clean build and ensure we get the same events
		listener.reset();
		try {
			getWorkspace().build(IncrementalProjectBuilder.CLEAN_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("4.99", e);
		}
		assertEquals("4.0", getWorkspace(), listener.getSource());
		assertEquals("4.1", IncrementalProjectBuilder.CLEAN_BUILD, listener.getBuildKind());
		assertEquals("4.2", true, listener.hadPreBuild());
		assertEquals("4.3", true, listener.hadPostBuild());
		assertEquals("4.4", true, listener.hadPostChange());
	}
}
